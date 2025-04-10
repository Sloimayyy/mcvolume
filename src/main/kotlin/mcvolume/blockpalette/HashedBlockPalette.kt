package com.sloimay.mcvolume.blockpalette

import com.sloimay.mcvolume.McVolume
import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.VolBlockState

class HashedBlockPalette : BlockPalette() {

    internal var idToVolBlockState = mutableListOf<VolBlockState>()
    internal var hashPalette = hashMapOf<BlockState, VolBlockState>()

    override val size
        get() = idToVolBlockState.size

    override fun getDefaultBlock(): VolBlockState {
        return this.idToVolBlockState[0]
    }

    override fun getBlock(bs: BlockState): VolBlockState? {
        return hashPalette[bs]
    }

    override fun getOrAddBlock(bs: BlockState, parentVolUuid: Long): VolBlockState {
        val b = hashPalette[bs]
        if (b != null) return b
        return addBlock(bs, parentVolUuid)
    }

    override fun getFromId(id: BlockPaletteId): VolBlockState {
        return idToVolBlockState[id.toInt()]
    }

    override fun iter(): Iterator<VolBlockState> {
        return idToVolBlockState.iterator()
    }


    override fun populateFromDeserializedVbsArr(volBlockStates: List<VolBlockState>) {
        // Repopulate exactly the same way the serialized block palette would have been


        /*val maxId = volBlockStates.maxOf { it.paletteId }
        val idToVolBlockStateHashMap = HashMap<BlockPaletteId, VolBlockState>()
        volBlockStates.forEach { idToVolBlockStateHashMap[it.paletteId] = it }*/

        idToVolBlockState = volBlockStates.toMutableList()
        hashPalette = hashMapOf()
        idToVolBlockState.forEach { hashPalette[it.state] = it }

        //for (v in volBlockStates) addBlock(v.state, parentVol)
    }

    override fun fillParentVolUuid(parentVol: McVolume) {
        idToVolBlockState.forEach { it.parentVolUuid = parentVol.uuid }
    }

    override fun serialize(): List<VolBlockState> {
        return idToVolBlockState
    }


    private fun addBlock(bs: BlockState, parentVolUuid: Long): VolBlockState {
        val volBlockState = VolBlockState.new(parentVolUuid, idToVolBlockState.size.toShort(), bs)
        idToVolBlockState.add(volBlockState)
        hashPalette[bs] = volBlockState
        return volBlockState
    }


}