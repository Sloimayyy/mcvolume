package com.sloimay.mcvolume.blockpalette

import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.VolBlock

abstract class BlockPalette {

    abstract val size: Int

    abstract fun getDefaultBlock(): VolBlock

    abstract fun getOrAddBlock(bs: BlockState): VolBlock

    abstract fun getFromId(id: BlockPaletteId): VolBlock

    abstract fun iter(): Iterator<VolBlock>

}