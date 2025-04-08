package com.sloimay.mcvolume.io

import com.sloimay.mcvolume.Chunk
import com.sloimay.mcvolume.GrowableByteBuf
import com.sloimay.mcvolume.IntBoundary
import com.sloimay.mcvolume.McVolume
import com.sloimay.mcvolume.McvUtils.Companion.gzipCompress
import com.sloimay.mcvolume.McvUtils.Companion.gzipDecompress
import com.sloimay.mcvolume.McvUtils.Companion.makePackedLongArrLF
import com.sloimay.mcvolume.McvUtils.Companion.unpackLongArrLFIntoShortArray
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.VolBlockState
import com.sloimay.mcvolume.blockpalette.BlockPalette
import com.sloimay.mcvolume.blockpalette.HashedBlockPalette
import com.sloimay.mcvolume.blockpalette.ListBlockPalette
import com.sloimay.smath.vectors.IVec3
import java.io.File
import java.nio.ByteOrder
import java.nio.charset.Charset
import kotlin.math.min
import kotlin.time.TimeSource

/**
 *
 *
 * IVec3:
 *  [i32, i32, i32]
 *
 * Boundary:
 *  [IVec3, IVec3]
 *
 * MiscData:
 *  chunkGridBound: IntBoundary
 *  loadedBound: IntBoundary
 *  wantedBound: IntBoundary
 *
 * String:
 *  len: Int
 *  bytes: Array<Byte>
 *
 * VolBlockState:
 *  globalPaletteId: Short
 *  blockStateStr: String
 *
 * BlockPalette:
 *  type: Byte   // 0 -> list, 1 -> hashed
 *  entries: Array<VolBlockState>
 *
 * ShortPackedLongData:
 *  valueCount: Int,
 *  bitLength: Byte,
 *  shortPackedLongArr: Array<Long>
 *
 * Chunk:
 *  pos: IVec3
 *  localPaletteToGlobalPalette: Array<Short>
 *  shortPackedLongData: ShortPackedLongData
 *
 *
 * McvFile (gzipped file):
 *  miscData: MiscData
 *  blockPalette: BlockPalette
 *  chunkFileLocs: Array<Int>  // points to the start byte of the chunk data
 *  chunks: [Chunk]   // array without a length
 *
 *
 *
 *
 *
 *
 *
 */



private class McvMiscData(
    val chunkGridBound: IntBoundary,
    val loadedBound: IntBoundary,
    val wantedBound: IntBoundary,
)

private class ShortPackedLongArr(
    val longArr: LongArray,
    val bitLength: Int,
    val valueCount: Int,
)



private val BYTE_ORDER = ByteOrder.LITTLE_ENDIAN




/**
 * McvFile:
 *  miscData: MiscData
 *  blockPalette: BlockPalette
 *  chunkFileLocs: Array<Int>  // points to the start byte of the chunk data
 *  chunks: [Chunk]
 */
fun McVolume.exportToMcv(filePath: String) {
    val ts = TimeSource.Monotonic
    val exportStart = ts.markNow()


    // Mcv files always end in ".mcv"
    val fp = if (filePath.endsWith(".mcv")) filePath else "$filePath.mcv"

    // TODO: add this clean function
    // this.clean()


    val byteBuf = GrowableByteBuf(byteOrder = BYTE_ORDER)


    // McvMiscData
    byteBuf.putMcvMiscData(
        McvMiscData(
            chunkGridBound,
            loadedBound,
            wantedBound,
        )
    )

    // Block palette
    byteBuf.putBlockPalette(blockPalette)


    // # Chunks
    val chunkCount = chunks.count { it != null }
    val chunkArrStartByteIdx = byteBuf.size()
    byteBuf.putIntArr(IntArray(chunkCount))

    var fileChunkIdx = 0
    val buildChunkBounds = this.getBuildChunkBounds().orElse(IntBoundary.new(IVec3.ZERO, IVec3.ZERO))
    for (chunkPos in buildChunkBounds.iterYzx()) {
        val chunkIdx = this.chunkGridBound.posToYzxIdx(chunkPos);
        val chunk = this.chunks[chunkIdx] ?: continue

        val chunkDataStartByteIdx = byteBuf.size()
        byteBuf.putMcvChunk(this, chunkPos, chunk)
        // Write the position of this chunk's data
        byteBuf.seek(chunkArrStartByteIdx + Int.SIZE_BYTES + (Int.SIZE_BYTES * fileChunkIdx))
        byteBuf.putInt(chunkDataStartByteIdx)
        // Get back to the end of the byte buf
        byteBuf.seek(byteBuf.size())

        fileChunkIdx += 1
    }

    // Write file
    val fileBytes = byteBuf.toByteArray()
    val compressedBytes = gzipCompress(fileBytes)
    val file = File(fp)
    file.writeBytes(compressedBytes)

    println("Exported to Mcv in ${exportStart.elapsedNow()}")
}


/**
 * McvFile:
 *  miscData: MiscData
 *  blockPalette: BlockPalette
 *  chunkFileLocs: Array<Int>  // points to the start byte of the chunk data
 *  chunks: [Chunk]
 */
fun McVolume.Companion.fromMcv(filePath: String): McVolume {
    val ts = TimeSource.Monotonic
    val deserStart = ts.markNow()

    val file = File(filePath)
    require(file.isFile) { "Inputted file path needs to be a file." }

    val fileBytes = file.readBytes()
    val uncompressedBytes = gzipDecompress(fileBytes)

    //println(uncompressedBytes.slice(4*3*2*3 until min(200, uncompressedBytes.size)))
    //println(uncompressedBytes.size)

    // Setup byte buf
    val byteBuf = GrowableByteBuf(uncompressedBytes.size, byteOrder = BYTE_ORDER)
    byteBuf.putBytes(uncompressedBytes)
    byteBuf.seek(0)

    // # Misc data
    val mcvMiscData = byteBuf.getMcvMiscData()

    // # Block Palette
    val blockPalette = byteBuf.getBlockPalette()

    // # ChunkFileLocs
    val chunkFileLocs = byteBuf.getIntArr()

    //println("chunk file locs array of size: ${chunkFileLocs.size}")

    val totalChunkCount = mcvMiscData.chunkGridBound.volume()
    val chunks = Array<Chunk?>(totalChunkCount) { null }

    // Make volume
    val vol = McVolume(
        chunks,
        blockPalette,
        blockStateCache = hashMapOf(),
    )
    vol.chunkGridBound = mcvMiscData.chunkGridBound
    vol.loadedBound = mcvMiscData.loadedBound
    vol.wantedBound = mcvMiscData.wantedBound

    // Populate chunks
    for (chunkFileLoc in chunkFileLocs) {
        byteBuf.seek(chunkFileLoc)
        val (chunkPos, chunk) = byteBuf.getMcvChunk()
        val chunkIdx = vol.chunkGridBound.posToYzxIdx(chunkPos)
        chunks[chunkIdx] = chunk
    }

    println("Volume initialized from Mcv file in ${deserStart.elapsedNow()}.")

    return vol
}








private fun McVolume.mcvRemapChunkBlockIds(chunk: Chunk): Pair<ShortArray, ShortArray> {
    val globalToLocalMap = HashMap<Short, Short>()
    val localToGlobalMap = mutableListOf<Short>()
    val remapped = ShortArray(chunk.blocks.size)

    for ((idx, value) in chunk.blocks.withIndex()) {
        // If the bi-directional mapping doesn't exist, create it
        val localMapping = globalToLocalMap[value] ?: run {
            val v = localToGlobalMap.size.toShort()
            globalToLocalMap[value] = v
            localToGlobalMap.add(value)
            v
        }

        remapped[idx] = localMapping
    }

    return Pair(localToGlobalMap.toShortArray(), remapped)
}


/**
 * Chunk:
 *  pos: IVec3
 *  localPaletteToGlobalPalette: Array<Short>
 *  shortPackedLongData:
 *      valueCount: Int,
 *      bitLength: Byte,
 *      shortPackedLongArr: Array<Long>
 */
private fun GrowableByteBuf.putMcvChunk(vol: McVolume, chunkPos: IVec3, chunk: Chunk) {
    val (localToGlobalMap, remappedBlocks) = vol.mcvRemapChunkBlockIds(chunk)
    val maxVal = (localToGlobalMap.size - 1)
    val maxBitLen = (maxVal.takeHighestOneBit() shl 1).countTrailingZeroBits()
    val packedRemappedBlock = makePackedLongArrLF(remappedBlocks, maxBitLen)

    putIVec3(chunkPos)
    putShortArr(localToGlobalMap)
    val shortPackedLongArr = ShortPackedLongArr(packedRemappedBlock, maxBitLen, remappedBlocks.size)
    putShortPackedLongArr(shortPackedLongArr)
}

private fun GrowableByteBuf.getMcvChunk(): Pair<IVec3, Chunk> {
    //println("deserializing mcv chunk")
    val chunkPos = getIVec3()
    val localToGlobalMap = getShortArr()
    val shortPackedLongArr = getShortPackedLongArr()

    val remappedBlocks = unpackLongArrLFIntoShortArray(
        shortPackedLongArr.longArr,
        shortPackedLongArr.bitLength,
        shortPackedLongArr.valueCount
    )
    //println("local to global map: ${localToGlobalMap.toList()}")
    //println("remapped blocks: ${remappedBlocks.toList().filter {it != 0.toShort()}}")
    val originalBlocks = ShortArray(remappedBlocks.size) { localToGlobalMap[remappedBlocks[it].toInt()] }

    return Pair(
        chunkPos,
        Chunk(
            originalBlocks,
            hashMapOf(),
        )
    )
}



private fun GrowableByteBuf.putBlockPalette(blockPalette: BlockPalette) {
    val idToVolBlockState = blockPalette.serialize()

    val paletteTypeId = when (blockPalette) {
        is ListBlockPalette -> 0.toByte()
        is HashedBlockPalette -> 1.toByte()
    }

    //println("size before ${size()}")
    putByte(paletteTypeId)
    //println("size after ${size()}")
    putVolBlockStateArray(idToVolBlockState)
}

private fun GrowableByteBuf.getBlockPalette(): BlockPalette {
    val paletteTypeId = getByte()
    val volBlockStates = getVolBlockStateArray()

    val blockPalette = when (paletteTypeId) {
        0.toByte() -> ListBlockPalette()
        1.toByte() -> HashedBlockPalette()
        else -> error("Unsupported palette type id '${paletteTypeId}'.")
    }
    blockPalette.populateFromDeserializedVbsArr(volBlockStates)

    return blockPalette
}

private fun GrowableByteBuf.putVolBlockStateArray(arr: List<VolBlockState>) {
    putInt(arr.size)
    //seek(pos() - 4)
    //println("got ${getInt()}")
    //return
    //println("put vol block state array of size ${arr.size}")
    for (v in arr) putVolBlockState(v)
}

private fun GrowableByteBuf.getVolBlockStateArray(): List<VolBlockState> {
    val size = getInt()
    //println("getting vol block state array of size ${size}")
    return List(size) { getVolBlockState() }
}





private fun GrowableByteBuf.putVolBlockState(volBlockState: VolBlockState) {
    putShort(volBlockState.paletteId)
    putString(volBlockState.state.stateStr)
}

private fun GrowableByteBuf.getVolBlockState(): VolBlockState {
    return VolBlockState(
        getShort(),
        BlockState.fromStr(getString())
    )
}



private fun GrowableByteBuf.putMcvMiscData(mcvMiscData: McvMiscData) {
    putIntBoundary(mcvMiscData.chunkGridBound)
    putIntBoundary(mcvMiscData.loadedBound)
    putIntBoundary(mcvMiscData.wantedBound)
}

private fun GrowableByteBuf.getMcvMiscData(): McvMiscData {
    return McvMiscData(
        getIntBoundary(),
        getIntBoundary(),
        getIntBoundary(),
    )
}



private fun GrowableByteBuf.putIntBoundary(intBoundary: IntBoundary) {
    putIVec3(intBoundary.a)
    putIVec3(intBoundary.b)
}

private fun GrowableByteBuf.getIntBoundary(): IntBoundary {
    return IntBoundary.new(
        getIVec3(),
        getIVec3(),
    )
}


private fun GrowableByteBuf.putString(s: String, charSet: Charset = Charsets.UTF_8) {
    val bytes = s.toByteArray(charset = charSet)
    putInt(bytes.size)
    putBytes(bytes)
}

private fun GrowableByteBuf.getString(charSet: Charset = Charsets.UTF_8): String {
    val size = getInt()
    val stringBytes = getBytes(size)
    return String(stringBytes, charSet)
}


private fun GrowableByteBuf.putIVec3(ivec3: IVec3) {
    putInt(ivec3.x)
    putInt(ivec3.y)
    putInt(ivec3.z)
}

private fun GrowableByteBuf.getIVec3(): IVec3 {
    return IVec3(
        getInt(),
        getInt(),
        getInt(),
    )
}

private fun GrowableByteBuf.putLongArr(longArr: LongArray) {
    putInt(longArr.size)
    for (l in longArr) putLong(l)
}
private fun GrowableByteBuf.getLongArr(): LongArray {
    val size = getInt()
    return LongArray(size) { getLong() }
}

private fun GrowableByteBuf.putShortArr(arr: ShortArray) {
    putInt(arr.size)
    for (e in arr) putShort(e)
}
private fun GrowableByteBuf.getShortArr(): ShortArray {
    val size = getInt()
    return ShortArray(size) { getShort() }
}

private fun GrowableByteBuf.putIntArr(arr: IntArray) {
    putInt(arr.size)
    for (e in arr) putInt(e)
}
private fun GrowableByteBuf.getIntArr(): IntArray {
    val size = getInt()
    return IntArray(size) { getInt() }
}



private fun GrowableByteBuf.putShortPackedLongArr(arr: ShortPackedLongArr) {
    putInt(arr.valueCount)
    putByte(arr.bitLength.toByte())
    putLongArr(arr.longArr)
}

private fun GrowableByteBuf.getShortPackedLongArr(): ShortPackedLongArr {
    val valueCount = getInt()
    val bitLength = getByte().toInt()
    val shortPackedLongArr = getLongArr()
    return ShortPackedLongArr(
        shortPackedLongArr,
        bitLength,
        valueCount,
    )
}
