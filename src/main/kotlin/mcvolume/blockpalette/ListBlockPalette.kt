package com.sloimay.mcvolume.blockpalette

import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.VolBlock

class ListBlockPalette(defaultBlock: BlockState) : BlockPalette() {

    private val palette = mutableListOf<VolBlock>()

    override val size
        get() = this.palette.size

    init {
        addBlock(defaultBlock)
    }


    /**
     * Returns -1 if not found
     */
    private fun idOf(bs: BlockState): Int {
        val stateStr = bs.stateStr
        return palette.indexOfFirst { stateStr == it.state.stateStr }
    }

    private fun addBlock(bs: BlockState): VolBlock {
        val volBlock = VolBlock.new(palette.size.toShort(), bs)
        palette.add(volBlock)
        return volBlock
    }


    override fun getDefaultBlock(): VolBlock {
        return this.palette[0]
    }

    override fun getOrAddBlock(bs: BlockState): VolBlock {
        println(this.palette.size)
        val id = idOf(bs)
        if (id == -1) {
            return addBlock(bs)
        } else {
            return palette[id]
        }
    }

    override fun getFromId(id: BlockPaletteId): VolBlock {
        return palette[id.toInt()]
    }

    override fun iter(): Iterator<VolBlock> {
        return this.palette.iterator()
    }


}