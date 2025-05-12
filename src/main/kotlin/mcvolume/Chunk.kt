package com.sloimay.mcvolume

import com.sloimay.smath.vectors.IVec3
import com.sloimay.smath.vectors.ivec3
import com.sloimay.mcvolume.block.VolBlockState
import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.DEFAULT_BLOCK_ID
import net.querz.nbt.tag.CompoundTag
import java.util.*




class Chunk internal constructor (
    internal val CHUNK_BIT_SIZE: Int,
    internal val blocks: ShortArray,
    // Local coords -> Tile data compound tag
    internal val tileData: HashMap<IVec3, CompoundTag>,
) {

    // Derived constants
    internal val CHUNK_SIDE_LEN = computeChunkSideLen(CHUNK_BIT_SIZE)
    internal val CHUNK_SIZE_BIT_MASK = computeChunkSizeBitMask(CHUNK_BIT_SIZE)
    internal val CHUNK_BLOCK_COUNT = computeChunkBlockCount(CHUNK_BIT_SIZE)


    companion object {

        private fun computeChunkSideLen(chunkBitSize: Int) = 1 shl chunkBitSize
        private fun computeChunkSizeBitMask(chunkBitSize: Int) = computeChunkSideLen(chunkBitSize) - 1
        private fun computeChunkBlockCount(chunkBitSize: Int): Int {
            val sideLen = computeChunkSideLen(chunkBitSize)
            return sideLen * sideLen * sideLen
        }

        fun new(
            chunkBitSize: Int,
        ): Chunk {
            return Chunk(
                chunkBitSize,
                blocks = ShortArray(computeChunkBlockCount(chunkBitSize)) { DEFAULT_BLOCK_ID },
                tileData = hashMapOf(),
            )
        }

    }

    fun getTileData(localCoords: IVec3): CompoundTag? {
        return this.tileData[localCoords]
    }

    fun setTileData(localCoords: IVec3, data: CompoundTag?) {
        if (data == null) {
            this.tileData.remove(localCoords)
        } else {
            this.tileData[localCoords] = data
        }
    }

    fun setBlock(localCoords: IVec3, volBlockState: VolBlockState) {
        blocks[localToBlockIdx(localCoords)] = volBlockState.paletteId
    }

    fun getBlock(localCoords: IVec3): BlockPaletteId {
        return blocks[localToBlockIdx(localCoords)]
    }

    internal fun localToBlockIdx(local: IVec3): Int {
        return local.x + (local.z shl CHUNK_BIT_SIZE) + (local.y shl (CHUNK_BIT_SIZE * 2))
    }

    internal fun blockIdxToLocal(idx: Int): IVec3 {
        return ivec3(
            ((idx shr (0 * CHUNK_BIT_SIZE)) and CHUNK_SIZE_BIT_MASK),
            ((idx shr (2 * CHUNK_BIT_SIZE)) and CHUNK_SIZE_BIT_MASK),
            ((idx shr (1 * CHUNK_BIT_SIZE)) and CHUNK_SIZE_BIT_MASK),
        )
    }

    internal fun canBeCleanedUp(): Boolean {
        return this.blocks.all { it == DEFAULT_BLOCK_ID }
    }

    internal fun computeMinLocalPos(): IVec3? {
        var minBlockPos: IVec3? = null;
        for ((blockIdx, blockId) in blocks.withIndex()) {
            if (blockId == DEFAULT_BLOCK_ID) { continue; }
            val localPos = blockIdxToLocal(blockIdx)
            if (minBlockPos == null) { minBlockPos = localPos }
            minBlockPos = minBlockPos.min(localPos)
        }
        return minBlockPos
    }

    internal fun computeMaxLocalPos(): IVec3? {
        var maxBlockPos: IVec3? = null;
        for ((blockIdx, blockId) in blocks.withIndex()) {
            if (blockId == DEFAULT_BLOCK_ID) { continue; }
            val localPos = blockIdxToLocal(blockIdx)
            if (maxBlockPos == null) { maxBlockPos = localPos }
            maxBlockPos = maxBlockPos.max(localPos)
        }

        return maxBlockPos
    }
}