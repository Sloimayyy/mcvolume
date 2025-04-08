package com.sloimay.mcvolume.blockpalette

import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.VolBlockState

class HashedBlockPalette() : BlockPalette() {

    internal var idToVolBlockState = mutableListOf<VolBlockState>()
    internal var hashPalette = hashMapOf<BlockState, VolBlockState>()

    override val size
        get() = idToVolBlockState.size

    internal constructor(defaultBlock: BlockState): this() {
        addBlock(defaultBlock)
    }

    override fun getDefaultBlock(): VolBlockState {
        return this.idToVolBlockState[0]
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
        return idToVolBlockState[id.toInt()]
    }

    override fun iter(): Iterator<VolBlockState> {
        return idToVolBlockState.iterator()
    }


    override fun populateFromDeserializedVbsArr(volBlockStates: List<VolBlockState>) {
        // Repopulate exactly the same way the serialized block palette would have been

        // Reset
        idToVolBlockState = mutableListOf()
        hashPalette = hashMapOf()
        // Populate
        for (v in volBlockStates) addBlock(v.state)
    }

    override fun serialize(): List<VolBlockState> {
        return idToVolBlockState
    }


    private fun addBlock(bs: BlockState): VolBlockState {
        val volBlockState = VolBlockState.new(idToVolBlockState.size.toShort(), bs)
        idToVolBlockState.add(volBlockState)
        hashPalette[bs] = volBlockState
        return volBlockState
    }


}