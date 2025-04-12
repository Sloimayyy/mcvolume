package com.sloimay.mcvolume.blockpalette

import com.sloimay.mcvolume.McVolume
import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.VolBlockState

abstract sealed class BlockPalette {

    abstract val size: Int

    abstract fun getDefaultBlock(): VolBlockState

    abstract fun getOrAddBlock(bs: BlockState): VolBlockState

    abstract fun getBlock(bs: BlockState): VolBlockState?

    abstract fun getFromId(id: BlockPaletteId): VolBlockState

    abstract fun iter(): Iterator<VolBlockState>

    abstract fun populateFromUnlinkedMappings(mappings: HashMap<BlockPaletteId, BlockState>)
    abstract fun toUnlinkedBlockStateMappings(): HashMap<BlockPaletteId, BlockState>

    /**
     * The idea of a link is that a block palette is unusable as long as its not linked
     */
    abstract fun link(volume: McVolume)

}