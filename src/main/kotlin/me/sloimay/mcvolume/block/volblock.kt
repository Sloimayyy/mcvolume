package me.sloimay.mcvolume.block

typealias BlockPaletteId = Short

const val DEFAULT_BLOCK_ID = 0.toShort()

data class VolBlock(internal val paletteId: BlockPaletteId, val state: BlockState) {

    companion object {
        fun new(paletteId: BlockPaletteId, state: BlockState): VolBlock {
            return VolBlock(paletteId, state)
        }
    }

    fun eq(other: VolBlock): Boolean {
        return this.paletteId == other.paletteId
    }

    override fun toString(): String {
        return "Block(${paletteId}, ${state})"
    }

}