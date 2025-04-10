@file:OptIn(ExperimentalTime::class)

package com.sloimay.mcvolume.io


import com.sloimay.mcvolume.*
import com.sloimay.mcvolume.McVolumeUtils
import com.sloimay.smath.vectors.IVec3
import com.sloimay.smath.vectors.ivec3
import com.sloimay.mcvolume.block.BlockPaletteId
import net.querz.mca.CompressionType
import net.querz.nbt.io.NBTSerializer
import net.querz.nbt.io.NBTUtil
import net.querz.nbt.io.NamedTag
import net.querz.nbt.tag.*
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import kotlin.math.max
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource


private const val BLOCK_TO_REGION_SHIFT = 4 + 5


/**
 *
 * Rewrite of the rust implementation
 *
 */
fun McVolume.saveToRegions(regionFolderPath: String,
                           targetThreadCount: Int = 20,
) {

    val timeSource = TimeSource.Monotonic
    val saveStart = timeSource.markNow()


    this.cleanChunks()


    val lowestSection = -4 // To change later?

    val buildChunkBounds = this.getBuildChunkBounds().get()

    // # Get every regions to make
    // For each region, keep a list of every chunk that intersects it
    var regionsToMake = hashMapOf<IVec3, MutableList<Pair<IVec3, Chunk>>>()
    for (chunkPos in buildChunkBounds.iterYzx()) {
        // If the chunk doesn't exist, don't iterate over it
        val chunkIdx = this.chunkGridBound.posToYzxIdx(chunkPos)
        val chunk = this.chunks[chunkIdx] ?: continue

        // Get every regions this chunk is in (doesn't happen before a chunk is > 512 blocks in size
        // or if chunks aren't on a 2^n blocks grid but wtv lol)
        val chunkBlockBounds = IntBoundary.new(
            chunkPos shl CHUNK_BIT_SIZE,
            (chunkPos+1) shl CHUNK_BIT_SIZE
        )
        // Uses a 3d Boundary but only X and Z are used, Y dim is 1
        val regionBounds = IntBoundary.new(
            (chunkBlockBounds.a shr BLOCK_TO_REGION_SHIFT).withY(0),
            (chunkBlockBounds.b shr BLOCK_TO_REGION_SHIFT).withY(1),
        )

        for (regionPos in regionBounds.iterYzx()) {
            if (regionPos !in regionsToMake) {
                regionsToMake[regionPos] = mutableListOf()
            }
            regionsToMake[regionPos]!!.add(Pair(chunkPos, chunk))
        }
    }

    //println("Regions to make VVV")
    //println(regionsToMake)


    // # Pre build the palette NBT's as we only have one per volume
    var paletteNbt = mutableListOf<CompoundTag>()
    for (paletteBlock in this.blockPalette.iter()) {
        //val paletteId = paletteBlock.paletteId
        val blockState = paletteBlock.state.stateStr

        var nbtEntry = CompoundTag()

        // Take care of the name of the entry
        var blockName = blockState
        val firstSquareBracketIdx = blockName.indexOfFirst { c -> c == '[' }
        when (firstSquareBracketIdx) {
            -1 -> { }
            else -> blockName = blockName.slice(0 until firstSquareBracketIdx)
        }
        nbtEntry.putString("Name", blockName)

        // Take care of the properties
        val hasProperties = blockState.contains('[')
        if (hasProperties) {
            val propertiesNbt = CompoundTag()
            val propsStartIdx = blockState.indexOfFirst { c -> c == '[' } + 1
            val propsEndIdx = blockState.length - 1
            val propsStr = blockState.slice(propsStartIdx until propsEndIdx)
            // Split every property: "[side=south, power= 6]" -> ["side=south", "power= 6"]
            propsStr.split(',').forEach { propStr ->
                val propStrStripped = propStr.trim()
                val propNameAndVal = propStrStripped.split('=')
                val propName = propNameAndVal[0].trim()
                val propVal = propNameAndVal[1].trim()
                propertiesNbt.put(propName, StringTag(propVal))
            }
            nbtEntry.put("Properties", propertiesNbt)
        }
        paletteNbt.add(nbtEntry)
    }

    //println("PALETTE NBT VVV")
    //println(paletteNbt)

    // # Make regions
    for ((regionPos, chunksInRegion) in regionsToMake) {

        // Instantiate header table and that one other timestamp table
        val regionBinary = MutableList<Byte>(4096 * 2) { 0 }

        // Get which col chunks to make
        val regionBlockBounds = IntBoundary.new(
            IVec3.new(regionPos.x, 0, regionPos.z) shl BLOCK_TO_REGION_SHIFT,
            (IVec3.new(regionPos.x, 0, regionPos.z)+1) shl BLOCK_TO_REGION_SHIFT,
        )
        val colChunksToMake = hashMapOf<IVec3, MutableList<Pair<IVec3, Chunk>>>()
        for ((chunkPos, chunk) in chunksInRegion) {
            // Get every column chunks that intersects this chunk
            val chunkBlockBounds = IntBoundary.new(
                chunkPos shl CHUNK_BIT_SIZE,
                (chunkPos+1) shl CHUNK_BIT_SIZE,
            )
            val colChunksBlockBounds = chunkBlockBounds.getClampedInside(regionBlockBounds)
            val colChunkBounds = IntBoundary.new(
                (colChunksBlockBounds.a shr 4).withY(0),
                (colChunksBlockBounds.b shr 4).withY(1),
            )

            // Add this chunk to every column chunks it's a part of
            //println(chunkBlockBounds)
            for (ccz in colChunkBounds.rangeZ()) for (ccx in colChunkBounds.rangeX()) {
                val colChunkPos = IVec3.new(ccx, 0, ccz)
                if (colChunkPos !in colChunksToMake) {
                    colChunksToMake[colChunkPos] = mutableListOf()
                }
                colChunksToMake[colChunkPos]!!.add(Pair(chunkPos, chunk))
            }
        }

        //println("Col chunks to make VVV")
        //println(colChunksToMake)

        // Make every col chunk binaries
        for ((colChunkPos, chunksInCol) in colChunksToMake) {
            // Figure out which chunk intersects which sections
            // A chunk is (usually) bigger than a section, so for each chunk
            // you gotta add every section it crosses
            var sectionsToMake = hashMapOf<Int, MutableList<Pair<IVec3, Chunk>>>()
            for ((chunkPos, chunk) in chunksInCol) {
                val chunkBlockBounds = IntBoundary.new(
                    chunkPos shl CHUNK_BIT_SIZE,
                    (chunkPos+1) shl CHUNK_BIT_SIZE,
                )
                val minSectionY = chunkBlockBounds.a.y shr 4
                val maxSectionY = chunkBlockBounds.b.y shr 4

                for (sy in minSectionY until maxSectionY) {
                    if (sy !in sectionsToMake) {
                        sectionsToMake[sy] = mutableListOf()
                    }
                    sectionsToMake[sy]!!.add(Pair(chunkPos, chunk))
                }
            }

            // Build sections nbt!!! finally!!!!
            var sectionsNbt = ListTag(CompoundTag::class.java)
            for ((sectionY, chunksInSection) in sectionsToMake) {

                // Gather the block ids in a YZX 3D array
                var blocks = ShortArray(16 * 16 * 16)

                var sectionMappings = MutableList<BlockPaletteId?>(this.blockPalette.size) { null }
                var sectionPalette = ListTag(CompoundTag::class.java)
                var runningMappedBId = 0.toShort()

                var sectionBlockBounds = IntBoundary.new(
                    IVec3.new(colChunkPos.x, sectionY, colChunkPos.z) shl 4,
                    (IVec3.new(colChunkPos.x, sectionY, colChunkPos.z)+1) shl 4,
                )

                // Populate the blocks in this section by iterating over every chunk
                // that are intersecting it and placing the blocks in common
                for ((chunkPos, chunk) in chunksInSection) {
                    val chunkBlockBounds = IntBoundary.new(
                        chunkPos shl CHUNK_BIT_SIZE,
                        (chunkPos+1) shl CHUNK_BIT_SIZE,
                    )
                    // Iterate over every block this chunk and this section have in common,
                    // and place the blocks
                    val chunkInsideSectionBounds = chunkBlockBounds.getClampedInside(sectionBlockBounds)
                    val yRange = chunkInsideSectionBounds.rangeY()
                    val zRange = chunkInsideSectionBounds.rangeZ()
                    val xRange = chunkInsideSectionBounds.rangeX()
                    //println("CHUNK INSIDE SECTION BOUNDS !! VVV")
                    //println(chunkInsideSectionBounds)
                    var oneCount = 0
                    for (y in yRange) for (z in zRange) for (x in xRange) {
                        val pos = ivec3(x, y, z)
                        // # Get the block id of the block we're currently going over and
                        // # convert it into this section's palette
                        val posInChunk = pos - chunkBlockBounds.a
                        val blockIdx = chunk.localToBlockIdx(posInChunk)
                        val blockId = chunk.blocks[blockIdx]

                        if (blockId == 1.toShort()) { oneCount += 1 }

                        // If the mapping doesn't exist, create it
                        var mapping = sectionMappings[blockId.toInt()]
                        val idInSection = if (mapping != null) {
                            mapping
                        } else {
                            val newId = runningMappedBId
                            sectionMappings[blockId.toInt()] = newId
                            runningMappedBId = (runningMappedBId + 1).toShort()
                            // Push the nbt of this block into palette at [newId]
                            sectionPalette.add(paletteNbt[blockId.toInt()])
                            newId
                        }

                        // Place the mapping in the section (optimised with shifts instead of
                        // using boundary implementations)
                        val posInSection = pos and 0xF
                        val idxInSection = ((posInSection.y shl 8) + (posInSection.z shl 4) + posInSection.x)
                        blocks[idxInSection] = idInSection
                    }
                    //println("ONE COUNT !!")
                    //println(oneCount)
                }


                //println("CHUNK BLOCKS !!!")
                //for (b in blocks) print("${b}, ")
                //println()

                // The block data is full, so we build the block data thingy up
                val maxSectionPaletteId = sectionPalette.size() - 1
                val bitLength = max(McVolumeUtils.getBitCount(maxSectionPaletteId), 4)
                val longArray = McVolumeUtils.makePackedLongArrLF(blocks, bitLength)
                val blockStateDataNbt = LongArrayTag(longArray)

                // Make section NBT
                var sectionNbt = CompoundTag()

                var biomesTag = CompoundTag()
                var biomePaletteListTag = ListTag(StringTag::class.java)
                biomePaletteListTag.add(StringTag("minecraft:plains"))
                biomesTag.put("palette", biomePaletteListTag)
                sectionNbt.put("biomes", biomesTag)

                var blockStatesNbt = CompoundTag()
                blockStatesNbt.put("palette", sectionPalette)
                blockStatesNbt.put("data", blockStateDataNbt)
                sectionNbt.put("block_states", blockStatesNbt)

                val skyLight = ByteArray(2048) { -1 }
                sectionNbt.putByteArray("SkyLight", skyLight)

                sectionNbt.putByte("Y", sectionY.toByte())

                // Add it
                sectionsNbt.add(sectionNbt)
            }

            // Build col chunk nbt
            val colChunkNbt = CompoundTag()

            colChunkNbt.putInt("DataVersion", 3578)
            colChunkNbt.put("sections", sectionsNbt)
            colChunkNbt.putString("Status", "minecraft:full")

            colChunkNbt.putInt("xPos", colChunkPos.x)
            colChunkNbt.putInt("xPos", colChunkPos.z)
            colChunkNbt.putInt("yPos", lowestSection)

            colChunkNbt.put("blockEntities", ListTag(CompoundTag::class.java))

            // Build col chunk binary
            //val colChunkNbtBinary = NBTSerializer(true).toBytes(NamedTag("root-tag", colChunkNbt))
            val baos = ByteArrayOutputStream(4096)
            BufferedOutputStream(CompressionType.ZLIB.compress(baos)).use {
                    nbtOut -> NBTSerializer(false).toStream(NamedTag("root-tag", colChunkNbt), nbtOut)
            }
            val colChunkNbtBinary = baos.toByteArray()
            //println("Col chunk nbt binary VVV")
            //println(colChunkNbtBinary.size)
            val colChunkBinary = mutableListOf<Byte>()
            McVolumeUtils.writeIntToByteListBE(colChunkBinary,colChunkNbtBinary.size + 1)
            colChunkBinary.add(2) // Zlib compression
            for (b in colChunkNbtBinary) colChunkBinary.add(b)

            // # Modify table
            val sectorCount = ((max(colChunkBinary.size, 1) - 1) / 4096) + 1
            val headerOffset = (4 * ((colChunkPos.x and 0x1F) + (colChunkPos.z and 0x1F) * 32))
            //println("COL CHUNK POS VVV")
            //println(colChunkPos)
            //println(headerOffset)
            // Modify header
            var headerWritingIdx = headerOffset
            val headerEntryChunkBinaryOffset = (regionBinary.size / 4096)
            val headerEntryChunkBinaryLength = sectorCount.toByte()
            for (byte in McVolumeUtils.intToBEBytes(headerEntryChunkBinaryOffset).slice(1..3)) {
                //println("Writing byte ${byte} for int: ${headerEntryChunkBinaryOffset}")
                regionBinary[headerWritingIdx] = byte
                headerWritingIdx += 1
            }
            regionBinary[headerWritingIdx] = headerEntryChunkBinaryLength

            // Pad chunk so it's exactly an amount of sectors in length + save it inside the region file
            while (colChunkBinary.size < (sectorCount * 4096)) {
                colChunkBinary.add(0)
            }
            regionBinary.addAll(colChunkBinary)
        }

        // Save region file

        val folderPath = Path.of(regionFolderPath)
        val regionFileName = "r.${regionPos.x}.${regionPos.z}.mca"
        val regionFilePath = folderPath.resolve(regionFileName)
        val regionFile = regionFilePath.toFile()
        //println("region binary size: ${regionBinary.size}")
        regionFile.writeBytes(regionBinary.toByteArray())

        println("Saved world region file ${regionFileName}")
    }

    println("Saved to regions in ${saveStart.elapsedNow()}")
}