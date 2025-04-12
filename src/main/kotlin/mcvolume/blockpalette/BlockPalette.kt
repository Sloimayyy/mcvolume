package com.sloimay.mcvolume.blockpalette

import com.sloimay.mcvolume.McVolume
import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.VolBlockState

abstract sealed class BlockPalette {

    abstract val size: Int

    abstract fun getDefaultBlock(): VolBlockState

    abstract fun getOrAddBlock(bs: BlockState, parentVolUuid: Long): VolBlockState

    abstract fun getBlock(bs: BlockState): VolBlockState?

    abstract fun getFromId(id: BlockPaletteId): VolBlockState

    abstract fun iter(): Iterator<VolBlockState>

    // TODO: Make ser and deser much better
    abstract fun populateFromDeserializedVbsArr(volBlockStates: List<VolBlockState>)
    abstract fun serialize(): List<VolBlockState>

}