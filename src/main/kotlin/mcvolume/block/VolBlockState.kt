package com.sloimay.mcvolume.block

typealias BlockPaletteId = Short

const val DEFAULT_BLOCK_ID = 0.toShort()

/**
 * Cached block state for volumes
 */
data class VolBlockState(internal val paletteId: BlockPaletteId, val state: BlockState) {

    companion object {
        fun new(paletteId: BlockPaletteId, state: BlockState): VolBlockState {
            return VolBlockState(paletteId, state)
        }
    }

    fun eq(other: VolBlockState): Boolean {
        return this.paletteId == other.paletteId
    }

    override fun toString(): String {
        return "VolBlockState(${paletteId}, ${state})"
    }

}