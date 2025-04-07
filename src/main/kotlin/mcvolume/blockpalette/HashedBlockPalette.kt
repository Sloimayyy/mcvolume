package com.sloimay.mcvolume.blockpalette

import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.VolBlockState

class HashedBlockPalette(defaultBlock: BlockState) : BlockPalette() {

    private val listPalette = mutableListOf<VolBlockState>()
    private val hashPalette = hashMapOf<BlockState, VolBlockState>()

    override val size
        get() = listPalette.size

    init {
        addBlock(defaultBlock)
    }

    override fun getDefaultBlock(): VolBlockState {
        return this.listPalette[0]
    }

    override fun getBlock(bs: BlockState): VolBlockState? {
        return hashPalette[bs]
    }

    override fun getOrAddBlock(bs: BlockState): VolBlockState {
        val b = hashPalette[bs]
        if (b != null) return b
        return addBlock(bs)
    }

    override fun getFromId(id: BlockPaletteId): VolBlockState {
        return listPalette[id.toInt()]
    }

    override fun iter(): Iterator<VolBlockState> {
        return listPalette.iterator()
    }



    private fun addBlock(bs: BlockState): VolBlockState {
        val volBlockState = VolBlockState.new(listPalette.size.toShort(), bs)
        listPalette.add(volBlockState)
        hashPalette[bs] = volBlockState
        return volBlockState
    }


}