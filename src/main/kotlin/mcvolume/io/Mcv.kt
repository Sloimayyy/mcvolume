package com.sloimay.mcvolume.io

import com.sloimay.mcvolume.Chunk
import com.sloimay.mcvolume.utils.GrowableByteBuf
import com.sloimay.mcvolume.McVolume
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.deserializeNbtCompound
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.gzipCompress
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.gzipDecompress
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.makePackedLongArrLF
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.serializeNbtCompound
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.unpackLongArrLFIntoShortArray
import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.blockpalette.BlockPalette
import com.sloimay.mcvolume.blockpalette.HashedBlockPalette
import com.sloimay.mcvolume.blockpalette.ListBlockPalette
import com.sloimay.smath.geometry.boundary.IntBoundary
import com.sloimay.smath.vectors.IVec3
import net.querz.mca.CompressionType
import net.querz.nbt.tag.CompoundTag
import java.io.File
import java.nio.ByteOrder
import java.nio.charset.Charset
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
 *  chunkBitSize: Byte,
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
 *  chunkBitSize: Byte,
 *  chunkFileLocs: Array<Int>  // points to the start byte of the chunk data
 *  chunks: [Chunk]
 */
fun McVolume.exportToMcv(filePath: String) {

    /**
     * TODO:
     *  - use a buffered file output stream (example: for each chunk, instantiate a growable
     *      byte buf, then write the chunk data to the mcv file through the buffered writer)
     */

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
            targetBounds,
        )
    )

    // Block palette
    byteBuf.putBlockPalette(blockPalette)

    // # Chunk bit size
    byteBuf.putByte(CHUNK_BIT_SIZE.toByte())


    // # Chunks
    val chunkStartTime = ts.markNow()
    val chunkCount = chunks.count { it != null }
    val chunkArrStartByteIdx = byteBuf.size()
    byteBuf.putIntArr(IntArray(chunkCount))

    var fileChunkIdx = 0
    val buildChunkBounds = this.getBuildChunkBounds() ?: IntBoundary.new(IVec3.ZERO, IVec3.ZERO)
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

    //println("Serialized chunks in ${chunkStartTime.elapsedNow()}")

    // Write file
    // TODO: use streams instead
    val fileBytes = byteBuf.toByteArray()
    val compressedBytes = gzipCompress(fileBytes)
    val file = File(fp)
    file.writeBytes(compressedBytes)

    //println("Exported to Mcv in ${exportStart.elapsedNow()}")
}


/**
 * McvFile:
 *  miscData: MiscData
 *  blockPalette: BlockPalette
 *  chunkFileLocs: Array<Int>  // points to the start byte of the chunk data
 *  chunks: [Chunk]
 */
fun McVolume.Companion.fromMcv(filePath: String): McVolume {

    /**
     * TODO:
     *  - use a buffered file input stream
     */

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
    if (blockPalette !is HashedBlockPalette) {
        error("Other block palettes than HashedBlockPalettes aren't supported by this version of McVolume.")
    }

    // # Chunk bit size
    val chunkBitSize = byteBuf.getByte().toInt()

    // # ChunkFileLocs
    val chunkFileLocs = byteBuf.getIntArr()

    //println("chunk file locs array of size: ${chunkFileLocs.size}")

    val totalChunkCount = mcvMiscData.chunkGridBound.volume().toInt()
    val chunks = Array<Chunk?>(totalChunkCount) { null }

    // Make volume
    val vol = McVolume(
        chunks,
        blockPalette,
        blockStateCache = HashMap(),
        CHUNK_BIT_SIZE = chunkBitSize
    )
    vol.chunkGridBound = mcvMiscData.chunkGridBound
    vol.loadedBound = mcvMiscData.loadedBound
    vol.targetBounds = mcvMiscData.wantedBound

    // Populate chunks
    for (chunkFileLoc in chunkFileLocs) {
        byteBuf.seek(chunkFileLoc)
        val (chunkPos, chunk) = byteBuf.getMcvChunk(vol)
        val chunkIdx = vol.chunkGridBound.posToYzxIdx(chunkPos)
        chunks[chunkIdx] = chunk
    }

    // # Link the block palette
    vol.blockPalette.link(vol)

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
 *
 * NbtCompound:
 *  ZLib zipped NbtCompound bytes
 *
 * TileData:
 *  Array<(IVec3, NbtCompound)>
 *
 *
 * Chunk:
 *  pos: IVec3
 *  localPaletteToGlobalPalette: Array<Short>
 *  shortPackedLongData:
 *      valueCount: Int,
 *      bitLength: Byte,
 *      shortPackedLongArr: Array<Long>
 *  tileData: TileData
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

    putTileData(chunk.tileData)
}

private fun GrowableByteBuf.getMcvChunk(parentVol: McVolume): Pair<IVec3, Chunk> {
    //println("deserializing mcv chunk")
    val chunkPos = getIVec3()
    val localToGlobalMap = getShortArr()
    val shortPackedLongArr = getShortPackedLongArr()

    val remappedBlocks = unpackLongArrLFIntoShortArray(
        shortPackedLongArr.longArr,
        shortPackedLongArr.bitLength,
        shortPackedLongArr.valueCount
    )

    val originalBlocks = ShortArray(remappedBlocks.size) { localToGlobalMap[remappedBlocks[it].toInt()] }

    val tileData = getTileData()

    return Pair(
        chunkPos,
        Chunk(
            parentVol.CHUNK_BIT_SIZE,
            originalBlocks,
            tileData,
        )
    )
}


private fun GrowableByteBuf.putTileData(tileData: HashMap<IVec3, CompoundTag>) {
    putInt(tileData.size)
    for ((k, v) in tileData) {
        putIVec3(k)
        val nbtBytes = serializeNbtCompound(v, "main", CompressionType.ZLIB)
        putByteArr(nbtBytes)
    }
}

private fun GrowableByteBuf.getTileData(): HashMap<IVec3, CompoundTag> {
    val outMap = HashMap<IVec3, CompoundTag>()
    val size = getInt()
    for (i in 0 until size) {
        val pos = getIVec3()
        val nbtBytes = getByteArr()
        val namedTag = deserializeNbtCompound(nbtBytes, CompressionType.ZLIB)
        outMap[pos] = namedTag.tag as CompoundTag
    }
    return outMap
}


private fun GrowableByteBuf.putBlockPalette(blockPalette: BlockPalette) {
    val blockStateMappings = blockPalette.toUnlinkedBlockStateMappings()

    val paletteTypeId = when (blockPalette) {
        is ListBlockPalette -> 0.toByte()
        is HashedBlockPalette -> 1.toByte()
    }

    //println("size before ${size()}")
    putByte(paletteTypeId)
    //println("size after ${size()}")
    putBlockStateMappings(blockStateMappings)
}

private fun GrowableByteBuf.getBlockPalette(): BlockPalette {
    val paletteTypeId = getByte()
    val mappings = getBlockStateMappings()

    val blockPalette = when (paletteTypeId) {
        0.toByte() -> ListBlockPalette()
        1.toByte() -> HashedBlockPalette()
        else -> error("Unsupported palette type id '${paletteTypeId}'.")
    }
    blockPalette.populateFromUnlinkedMappings(mappings)

    return blockPalette
}

private fun GrowableByteBuf.putBlockStateMappings(arr: HashMap<BlockPaletteId, BlockState>) {
    putInt(arr.size)
    for (v in arr) putMappedBlockState(Pair(v.key, v.value))
}

private fun GrowableByteBuf.getBlockStateMappings(): HashMap<BlockPaletteId, BlockState> {
    val size = getInt()
    //println("getting vol block state array of size ${size}")
    val mappings = hashMapOf<BlockPaletteId, BlockState>()
    mappings.putAll (
        (0 until size).map { getMappedBlockState() }
    )
    return mappings
}





private fun GrowableByteBuf.putMappedBlockState(v: Pair<BlockPaletteId, BlockState>) {
    putShort(v.first)
    putString(v.second.stateStr)
}

private fun GrowableByteBuf.getMappedBlockState(): Pair<BlockPaletteId, BlockState> {
    return Pair(getShort(), BlockState.fromStr(getString()))
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

private fun GrowableByteBuf.putByteArr(arr: ByteArray) {
    putInt(arr.size)
    for (e in arr) putByte(e)
}
private fun GrowableByteBuf.getByteArr(): ByteArray {
    val size = getInt()
    return ByteArray(size) { getByte() }
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
