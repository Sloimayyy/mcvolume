package com.sloimay.mcvolume.blockpalette

import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.VolBlock

class HashedBlockPalette(defaultBlock: BlockState) : BlockPalette() {

    private val listPalette = mutableListOf<VolBlock>()
    private val hashPalette = hashMapOf<BlockState, VolBlock>()

    override val size
        get() = listPalette.size

    init {
        addBlock(defaultBlock)
    }

    override fun getDefaultBlock(): VolBlock {
        return this.listPalette[0]
    }

    override fun getBlock(bs: BlockState): VolBlock? {
        return hashPalette[bs]
    }

    override fun getOrAddBlock(bs: BlockState): VolBlock {
        val b = hashPalette[bs]
        if (b != null) return b
        return addBlock(bs)
    }

    override fun getFromId(id: BlockPaletteId): VolBlock {
        return listPalette[id.toInt()]
    }

    override fun iter(): Iterator<VolBlock> {
        return listPalette.iterator()
    }



    private fun addBlock(bs: BlockState): VolBlock {
        val volBlock = VolBlock.new(listPalette.size.toShort(), bs)
        listPalette.add(volBlock)
        hashPalette[bs] = volBlock
        return volBlock
    }


}