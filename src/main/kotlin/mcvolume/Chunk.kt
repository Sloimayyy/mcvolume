package com.sloimay.mcvolume

import com.sloimay.smath.vectors.IVec3
import com.sloimay.smath.vectors.ivec3
import com.sloimay.mcvolume.block.VolBlockState
import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.DEFAULT_BLOCK_ID
import net.querz.nbt.tag.CompoundTag
import java.util.*


const val CHUNK_BIT_SIZE = 5
const val CHUNK_SIDE_LEN = 1 shl CHUNK_BIT_SIZE
const val CHUNK_SIZE_BIT_MASK = CHUNK_SIDE_LEN - 1
const val CHUNK_BLOCK_COUNT = CHUNK_SIDE_LEN * CHUNK_SIDE_LEN * CHUNK_SIDE_LEN





class Chunk internal constructor (
    internal val blocks: ShortArray,
    internal val tileData: HashMap<IVec3, CompoundTag>,
) {

    companion object {

        fun new(): Chunk {
            return Chunk(
                blocks = ShortArray(CHUNK_BLOCK_COUNT) { DEFAULT_BLOCK_ID },
                tileData = hashMapOf(),
            )
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

    internal fun canBeCleanedUp(): Boolean {
        return this.blocks.all { it == DEFAULT_BLOCK_ID }
    }

    internal fun computeMinLocalPos(): Optional<IVec3> {
        var minBlockPos: IVec3? = null;
        for ((blockIdx, blockId) in blocks.withIndex()) {
            if (blockId == DEFAULT_BLOCK_ID) { continue; }
            val localPos = blockIdxToLocal(blockIdx)
            if (minBlockPos == null) { minBlockPos = localPos }
            minBlockPos = minBlockPos.min(localPos)
        }
        if (minBlockPos == null) { return Optional.empty() }
        return Optional.of(minBlockPos)
    }

    internal fun computeMaxLocalPos(): Optional<IVec3> {
        var maxBlockPos: IVec3? = null;
        for ((blockIdx, blockId) in blocks.withIndex()) {
            if (blockId == DEFAULT_BLOCK_ID) { continue; }
            val localPos = blockIdxToLocal(blockIdx)
            if (maxBlockPos == null) { maxBlockPos = localPos }
            maxBlockPos = maxBlockPos.max(localPos)
        }
        if (maxBlockPos == null) { return Optional.empty() }
        return Optional.of(maxBlockPos)
    }
}