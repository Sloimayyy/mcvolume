package com.sloimay.mcvolume.blockpalette

import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.VolBlockState

class ListBlockPalette() : BlockPalette() {

    internal var palette = mutableListOf<VolBlockState>()

    override val size
        get() = this.palette.size

    internal constructor(defaultBlock: BlockState) : this() {
        addBlock(defaultBlock)
    }

    override fun getBlock(bs: BlockState): VolBlockState? {
        val id = idOf(bs)
        return if (id == -1) return null else palette[id]
    }

    /**
     * Returns -1 if not found
     */
    private fun idOf(bs: BlockState): Int {
        return palette.indexOfFirst { bs == it.state }
    }

    private fun addBlock(bs: BlockState): VolBlockState {
        val volBlockState = VolBlockState.new(palette.size.toShort(), bs)
        palette.add(volBlockState)
        return volBlockState
    }


    override fun getDefaultBlock(): VolBlockState {
        return this.palette[0]
    }

    override fun getOrAddBlock(bs: BlockState): VolBlockState {
        val id = idOf(bs)
        if (id == -1) {
            return addBlock(bs)
        } else {
            return palette[id]
        }
    }

    override fun getFromId(id: BlockPaletteId): VolBlockState {
        return palette[id.toInt()]
    }

    override fun iter(): Iterator<VolBlockState> {
        return this.palette.iterator()
    }


    override fun populateFromDeserializedVbsArr(volBlockStates: List<VolBlockState>) {
        palette = volBlockStates.toMutableList()
    }

    override fun serialize(): List<VolBlockState> {
        return palette.toList()
    }


}