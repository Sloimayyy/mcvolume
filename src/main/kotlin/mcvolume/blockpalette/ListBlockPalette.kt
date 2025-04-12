package com.sloimay.mcvolume.blockpalette

import com.sloimay.mcvolume.McVolume
import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.VolBlockState

class ListBlockPalette : BlockPalette() {


    private var linkedVolume: McVolume? = null
    private fun isLinked() = linkedVolume != null
    private fun requireIsLinked() {
        require(isLinked()) { "Block palette isn't linked to any volume." }
    }
    private fun requireNotLinked() {
        require(!isLinked()) { "Block palette is already linked to a volume." }
    }


    internal var palette = mutableListOf<VolBlockState>()

    override val size
        get() = this.palette.size

    override fun getBlock(bs: BlockState): VolBlockState? {
        requireIsLinked()
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
        requireIsLinked()
        val volBlockState = VolBlockState.new(linkedVolume!!.uuid, palette.size.toShort(), bs)
        palette.add(volBlockState)
        return volBlockState
    }


    override fun getDefaultBlock(): VolBlockState {
        requireIsLinked()
        return this.palette[0]
    }

    override fun getOrAddBlock(bs: BlockState): VolBlockState {
        requireIsLinked()
        val id = idOf(bs)
        if (id == -1) {
            return addBlock(bs)
        } else {
            return palette[id]
        }
    }

    override fun getFromId(id: BlockPaletteId): VolBlockState {
        requireIsLinked()
        return palette[id.toInt()]
    }

    override fun iter(): Iterator<VolBlockState> {
        requireIsLinked()
        return this.palette.iterator()
    }


    override fun populateFromUnlinkedMappings(mappings: HashMap<BlockPaletteId, BlockState>) {
        requireNotLinked()
        require(mappings.size > 0) { "Mappings should at least be of length 1." }

        palette = mutableListOf()
        val maxId = mappings.maxOf { it.key }
        for (idi in 0..maxId) {
            val id = idi.toShort()
            val state = mappings[id]
                ?: error("Invalid palette, its max id is $maxId but an earlier id $id is unmapped.")
            val vbs = VolBlockState.newUnlinked(id, state)
            palette.add(vbs)
        }
    }

    override fun toUnlinkedBlockStateMappings(): HashMap<BlockPaletteId, BlockState> {
        val mappings = HashMap<BlockPaletteId, BlockState>()
        mappings.putAll(
            palette.map { it.paletteId to it.state }
        )
        return mappings
    }

    override fun link(volume: McVolume) {
        requireNotLinked()
        palette.forEach { it.parentVolUuid = volume.uuid }
        linkedVolume = volume
    }

}