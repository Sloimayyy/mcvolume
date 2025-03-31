package com.sloimay.mcvolume.io

import com.sloimay.mcvolume.*
import com.sloimay.smath.vectors.IVec3
import com.sloimay.smath.vectors.ivec3
import java.io.File
import kotlin.concurrent.thread
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource


private class SerialChunk() {
    var pos = ivec3(0, 0, 0)
    var chunkSideLenBitCount: Int = 5
    var localPointsArray: MutableList<Pair<IVec3, IVec3>> = mutableListOf()
    var colorPaletteIdxBitCount: Int = 16
    var colorPaletteIdxArray: MutableList<Int> = mutableListOf()
    var normalsIdxArray: MutableList<Int> = mutableListOf()

    companion object {

        fun new(chunkPos: IVec3, colorPaletteIdxBitCount: Int, chunkSideLenBitCount: Int): SerialChunk {
            var sc = SerialChunk()
            sc.pos = chunkPos
            sc.chunkSideLenBitCount = chunkSideLenBitCount
            sc.localPointsArray = mutableListOf()
            sc.colorPaletteIdxBitCount = colorPaletteIdxBitCount
            sc.colorPaletteIdxArray = mutableListOf()
            sc.normalsIdxArray = mutableListOf()
            return sc
        }

    }

    fun addQuad(nCorner: IVec3, pCorner: IVec3, colorIdx: Int, normalIdx: Int) {
        this.localPointsArray.add(Pair(nCorner, pCorner))
        this.colorPaletteIdxArray.add(colorIdx)
        this.normalsIdxArray.add(normalIdx)
    }

    fun compileToBuf(buf: MutableList<Byte>) {
        // Add chunk pos
        McvUtils.writeIVec3ToByteListLE(buf, this.pos)
        // Add the number of quads
        McvUtils.writeIntToByteListLE(buf, this.localPointsArray.size)

        // # Add the points packed long array
        // First make an array with only integers
        var pointArray: MutableList<Int> = mutableListOf()
        for (pair in this.localPointsArray) {
            pointArray.add(pair.first.x)
            pointArray.add(pair.first.y)
            pointArray.add(pair.first.z)
            pointArray.add(pair.second.x)
            pointArray.add(pair.second.y)
            pointArray.add(pair.second.z)
        }
        // Now make the packed array. Local coords need an extra bit cuz
        // there's 2^chunk_bit_count + 1 possibilities
        val packedPointArray = McvUtils.makePackedLongArrHF(pointArray, this.chunkSideLenBitCount + 1)
        // Write array to bytes
        //println(packedPointArray.size)
        McvUtils.writeIntToByteListLE(buf, packedPointArray.size)
        McvUtils.writeLongBufToByteListLE(buf, packedPointArray)

        // # Add the color indexes
        val packedColorIndexes = McvUtils.makePackedLongArrHF(this.colorPaletteIdxArray, this.colorPaletteIdxBitCount)
        McvUtils.writeIntToByteListLE(buf, packedColorIndexes.size)
        McvUtils.writeIntToByteListLE(buf, this.colorPaletteIdxBitCount)
        McvUtils.writeLongBufToByteListLE(buf, packedColorIndexes)

        // # Add the normals
        val packedNormalIndexes = McvUtils.makePackedLongArrHF(this.normalsIdxArray, 3)
        McvUtils.writeIntToByteListLE(buf, packedNormalIndexes.size)
        McvUtils.writeLongBufToByteListLE(buf, packedNormalIndexes)
    }

}





/**
 *
 * Rewrite of the rust implementation
 *
 * Export to the new Sloimay Quad file format. For any contributors stumbling upon this,
 * it's a format I personally use for a workflow program I made, don't worry about it
 *
 */

@OptIn(ExperimentalTime::class)
fun McVolume.exportToSQuads(filePath: String,
                            colorMap: HashMap<String, ByteArray>,
                            greedyMesh: Boolean = true,
                            targetThreadCount: Int = 10
) {

    val timeSource = TimeSource.Monotonic
    val exportStart = timeSource.markNow()


    this.cleanChunks()
    val buildChunksBound = this.getBuildChunkBounds().get()


    // # Init file bytes
    var fileBytes = mutableListOf<Byte>()

    // # Make color table (vec of colors instead of hashmap
    val airBlock = this.getEnsuredPaletteBlock("minecraft:air")
    val airColor = byteArrayOf(0, 0, 0, 0)
    val unknownColor = byteArrayOf(0, 0, 0, 255.toByte())
    var colorTable: MutableList<ByteArray> = mutableListOf()
    for (b in this.blockPalette.iter()) {
        var col: ByteArray
        if (b.paletteId == airBlock.paletteId) {
            col = airColor
        } else {
            if (colorMap.containsKey(b.state.stateStr)) {
                col = colorMap[b.state.stateStr]!!
            } else {
                col = unknownColor
            }
        }
        colorTable.add(col)
    }

    // # Make file color table header
    McvUtils.writeIntToByteListLE(fileBytes, colorTable.size)
    for (entry in colorTable) {
        McvUtils.writeByteArrToByteList(fileBytes, entry)
    }

    // # More setup
    val colorPaletteIdxMaxBitCount = McvUtils.getBitCount(colorTable.size - 1)
    val normals = arrayOf(
        IVec3.X,
        -IVec3.X,
        IVec3.Y,
        -IVec3.Y,
        IVec3.Z,
        -IVec3.Z,
    )
    val chunkDims = IVec3.splat(com.sloimay.mcvolume.CHUNK_SIDE_LEN)
    val localChunkBounds = com.sloimay.mcvolume.IntBoundary.new(ivec3(0, 0, 0), chunkDims)
    var coordIndexesPerAxis = mutableListOf<IVec3>()
    for (axis in 0 until 3) {
        var xCoordIdx = 0
        var yCoordIdx = 0
        var xFound = false
        for (i in 0 until 3) {
            if (i == axis) { continue }
            if (!xFound) { xCoordIdx = i; xFound = true }
            else { yCoordIdx = i }
        }
        coordIndexesPerAxis.add(ivec3(axis, yCoordIdx, xCoordIdx))
    }


    // # Finalize file header by adding the chunk side len bit count
    McvUtils.writeIntToByteListLE(fileBytes, com.sloimay.mcvolume.CHUNK_BIT_SIZE)

    println("Init in: ${exportStart.elapsedNow()}")

    // # Set up threads and dispatch
    val buildChunksGridDim = buildChunksBound.dim
    val longestAxisIdx = buildChunksGridDim.longestAxis()
    val jobs = McvUtils.distributeRange(
        buildChunksBound.a[longestAxisIdx],
        buildChunksBound.b[longestAxisIdx],
        targetThreadCount
    )

    // Chunk planes per axis in the format (Axis Planes, ivec3( normal_len, y_len, x_len ))
    // Give each thread one
    var chunkPlanesPerNormalList = MutableList<MutableList<Pair<ShortArray, IVec3>>>(jobs.size) { mutableListOf() };
    for (axis in 0 until 6) {
        val coordIndexes = coordIndexesPerAxis[axis / 2]

        val axisCoordIdx = coordIndexes[0]
        val yCoordIdx = coordIndexes[2]
        val xCoordIdx = coordIndexes[1]

        val lenAlongAxis = chunkDims[axisCoordIdx]
        val lenAlongY = chunkDims[yCoordIdx]
        val lenAlongX = chunkDims[xCoordIdx]

        val planesOfAxisArrSize = lenAlongAxis * lenAlongY * lenAlongX
        chunkPlanesPerNormalList.forEach {
            val planesOfAxis = ShortArray(planesOfAxisArrSize) { airBlock.paletteId }
            it.add(Pair(planesOfAxis, ivec3(lenAlongAxis, lenAlongY, lenAlongX)))
        }
    }
    var threadQuadCounts = Array(jobs.size) { 0 }
    var threadSerialChunks = Array<MutableList<SerialChunk>>(jobs.size) { mutableListOf() }

    val jobsBounds = jobs.map {
        com.sloimay.mcvolume.IntBoundary.new(
            buildChunksBound.a.withAxis(longestAxisIdx, it.first),
            buildChunksBound.b.withAxis(longestAxisIdx, it.second),
        )
    }
    val threads = List(jobsBounds.size) { threadIdx ->

        // # Each thread will greedy mesh chunks in their allocated boundaries
        // # Once a chunk is done being greedy meshed, its data (SerialChunk class)
        // # will be put inside the SerialChunk list of each thread (threadSerialChunks[threadIdx])
        val t = thread (start = false) {

            var chunkPlanesPerNormal = chunkPlanesPerNormalList[threadIdx]

            // # Greedy mesh each chunk and add them to the file
            val jobBounds = jobsBounds[threadIdx]
            for (chunkPos in jobBounds.iterYzx()) {
                val chunk = this.chunks[this.chunkGridBound.posToYzxIdx(chunkPos)] ?: continue
                val chunkWorldPos = chunkPos shl com.sloimay.mcvolume.CHUNK_BIT_SIZE
                val chunkWorldBounds = com.sloimay.mcvolume.IntBoundary.new(chunkWorldPos, chunkWorldPos + chunkDims)

                // # Populate chunk planes
                // Reset
                chunkPlanesPerNormal.forEach { it.first.fill(airBlock.paletteId) }


                // Populate with block state IDs
                for ((blockIdx, blockId) in chunk.blocks.withIndex()) {
                    // We're not greedy meshing air blocks
                    if (blockId == airBlock.paletteId) { continue; }

                    val currPosInChunk = com.sloimay.mcvolume.Chunk.blockIdxToLocal(blockIdx)
                    for ((normalIdx, normal) in normals.withIndex()) {
                        val adjPosInChunk = currPosInChunk + normal
                        val adjPosInChunkInside = localChunkBounds.posInside(adjPosInChunk)

                        val adjIsTransparent = if (adjPosInChunkInside) {
                            val adjBlockIdx = com.sloimay.mcvolume.Chunk.localToBlockIdx(adjPosInChunk)
                            val adjBlockId = chunk.blocks[adjBlockIdx]
                            colorTable[adjBlockId.toInt()][3].toUByte() < 255.toUByte()
                        } else {
                            true
                        }
                        // Block not transparent so we can't see the quad
                        if (!adjIsTransparent) { continue }

                        // Set the face to the block
                        val axisIdx = normalIdx / 2
                        val coordIndexes = coordIndexesPerAxis[axisIdx]
                        val (normalChunkPlanes, dims) = chunkPlanesPerNormal[normalIdx]
                        val planeIdx = currPosInChunk[coordIndexes[0]]
                        val yInPlane = currPosInChunk[coordIndexes[1]]
                        val xInPlane = currPosInChunk[coordIndexes[2]]
                        val yLen = dims[1]
                        val xLen = dims[2]
                        // Iteration order: Axis Y X
                        val blockIdxInPlane3dArr = (
                                planeIdx * yLen * xLen +
                                        yInPlane * xLen +
                                        xInPlane
                                )
                        normalChunkPlanes[blockIdxInPlane3dArr] = blockId
                    }
                }

                // # Greedy mesh
                var serialChunk = com.sloimay.mcvolume.io.SerialChunk.new(
                    chunkWorldPos,
                    colorPaletteIdxMaxBitCount,
                    com.sloimay.mcvolume.CHUNK_BIT_SIZE
                )
                for ((normalIdx, planeNormal) in normals.withIndex()) {

                    val (normalChunkPlanes, dims) = chunkPlanesPerNormal[normalIdx]
                    val axisIdx = normalIdx / 2
                    val coordIndexes = coordIndexesPerAxis[axisIdx]

                    for (planeDepth in 0 until dims[0]) {

                        val yLen = dims[1]
                        val xLen = dims[2]

                        val planeBaseIdx = planeDepth * yLen * xLen

                        for (y in 0 until yLen) for (x in 0 until xLen) {

                            val idxInCurrPlane = { bX: Int, bY: Int ->
                                planeBaseIdx + bY * xLen + bX
                            }

                            // Can't start greedy meshing on air
                            val idx = idxInCurrPlane(x, y)
                            if (normalChunkPlanes[idx] == airBlock.paletteId) { continue; }

                            val quadBlockId = normalChunkPlanes[idx]

                            // Expand in X
                            val startX = x
                            var endX = startX
                            while (true) {
                                val xExpandNotDone = (endX < xLen) && (normalChunkPlanes[idxInCurrPlane(endX, y)] == quadBlockId);
                                if (!xExpandNotDone) { break }
                                endX += 1
                                if (!greedyMesh) { break }
                            }

                            // Expand in Y now
                            val startY = y
                            var endY = startY
                            while (true) {
                                if (!(endY < yLen)) { break }

                                // Check if the entire row is of the quad's material
                                val rowStartIdx = idxInCurrPlane(startX, endY)
                                val rowEndIdx = idxInCurrPlane(endX, endY)
                                var validRow = true
                                for (idx_ in rowStartIdx until rowEndIdx) {
                                    if (normalChunkPlanes[idx_] != quadBlockId) {
                                        validRow = false
                                        break
                                    }
                                }
                                if (!validRow) {
                                    break
                                }
                                // The row we're checking is valid, so we grow by one
                                endY += 1

                                if (!greedyMesh) { break }
                            }

                            // The quad has been determined, replace by air
                            for (y_ in startY until endY) for (x_ in startX until endX) {
                                normalChunkPlanes[idxInCurrPlane(x_, y_)] = airBlock.paletteId
                            }

                            //println("planeDepth:${planeDepth} x:${x} y:${y}, NormalIdx: $normalIdx, vals: $startX, $endX, $startY, $endY")

                            //println("NormalIdx: $normalIdx, vals: $startX, $endX, $startY, $endY")

                            // Get the world coords of the quad NN and PP corners
                            fun planeCoordsToLocalChunkCoords(planeCoords: IVec3, coordIndexes: IVec3, normal: IVec3): IVec3 {
                                val localCoordsXIdx = coordIndexes.toArray().indexOfFirst { it -> it == 0 }
                                val localCoordsYIdx = coordIndexes.toArray().indexOfFirst { it -> it == 1 }
                                val localCoordsZIdx = coordIndexes.toArray().indexOfFirst { it -> it == 2 }
                                val localChunkCoords = ivec3(
                                    planeCoords[localCoordsXIdx],
                                    planeCoords[localCoordsYIdx],
                                    planeCoords[localCoordsZIdx],
                                )
                                val normalOffset = normal.max(IVec3.splat(0))
                                return localChunkCoords + normalOffset
                            }
                            val planeCoordsNC = ivec3(planeDepth, startY, startX)
                            val planeCoordsPC = ivec3(planeDepth, endY, endX)
                            val localChunkCoordsNC = planeCoordsToLocalChunkCoords(planeCoordsNC, coordIndexes, planeNormal)
                            val localChunkCoordsPC = planeCoordsToLocalChunkCoords(planeCoordsPC, coordIndexes, planeNormal)

                            //println("Quad: ${localChunkCoordsNC} ${localChunkCoordsPC}")

                            // Add the quads to the chunk
                            serialChunk.addQuad(localChunkCoordsNC, localChunkCoordsPC, quadBlockId.toInt(), normalIdx)
                            threadQuadCounts[threadIdx] += 1
                        }
                    }
                }
                // Serial chunk is done so we add it
                threadSerialChunks[threadIdx].add(serialChunk)
            }
        }
        t
    }
    threads.forEach { thread -> thread.start() }
    threads.forEach { thread -> thread.join() }



    threadSerialChunks.forEach { serialChunks ->
        serialChunks.forEach { serialChunk ->
            serialChunk.compileToBuf(fileBytes)
        }
    }



    //val exportTimeTaken = (currentNanoTime() / 1_000_000) - exportStartMillis
    val quadCount = threadQuadCounts.sum()
    println("Quad count: ${quadCount}")
    println("Export time taken: ${exportStart.elapsedNow()}")

    val file = File(filePath)
    val fileByteArr = McvUtils.byteVecToByteArray(fileBytes)
    file.writeBytes(fileByteArr)
}