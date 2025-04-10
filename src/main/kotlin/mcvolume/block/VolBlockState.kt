package com.sloimay.mcvolume.block

import com.sloimay.mcvolume.McVolume

typealias BlockPaletteId = Short

const val DEFAULT_BLOCK_ID = 0.toShort()

/**
 * Cached block state for volumes
 */
data class VolBlockState(
    internal var parentVolUuid: Long,
    internal var paletteId: BlockPaletteId,
    //internal val parentVolVersion: Int,

    val state: BlockState
) {

    companion object {
        fun new(parentVolUuid: Long, paletteId: BlockPaletteId, state: BlockState): VolBlockState {
            return VolBlockState(parentVolUuid, paletteId, state)
        }
    }




    fun eq(other: VolBlockState): Boolean {
        return this.paletteId == other.paletteId
    }

    override fun toString(): String {
        return "VolBlockState(${paletteId}, ${state})"
    }

}