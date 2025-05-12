package com.sloimay.mcvolume.block

typealias BlockPaletteId = Short
typealias BlockStateIdArray = ShortArray

// TODO: move away from using that constant, as block palettes don't offer any guarantee of 0 being
//       the default block id, but usually implements it that way
const val DEFAULT_BLOCK_ID = 0.toShort()

const val NO_PARENT_DEFINED_UUID: Long = -1

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

        fun newUnlinked(paletteId: BlockPaletteId, state: BlockState): VolBlockState {
            return new(NO_PARENT_DEFINED_UUID, paletteId, state)
        }
    }




    fun eq(other: VolBlockState): Boolean {
        return this.paletteId == other.paletteId
    }

    override fun toString(): String {
        return "VolBlockState(${paletteId}, ${state})"
    }

}