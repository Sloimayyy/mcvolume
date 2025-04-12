package com.sloimay.mcvolume.blockpalette

import com.sloimay.mcvolume.McVolume
import com.sloimay.mcvolume.block.BlockPaletteId
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.VolBlockState

class HashedBlockPalette : BlockPalette() {


    private var linkedVolume: McVolume? = null
    private fun isLinked() = linkedVolume != null
    private fun requireIsLinked() {
        require(isLinked()) { "Block palette isn't linked to any volume." }
    }
    private fun requireNotLinked() {
        require(!isLinked()) { "Block palette is already linked to a volume." }
    }


    internal var idToVolBlockState = mutableListOf<VolBlockState>()
    internal var hashPalette = hashMapOf<BlockState, VolBlockState>()

    override val size
        get() = idToVolBlockState.size

    override fun getDefaultBlock(): VolBlockState {
        requireIsLinked()
        return this.idToVolBlockState[0]
    }

    override fun getBlock(bs: BlockState): VolBlockState? {
        requireIsLinked()
        return hashPalette[bs]
    }

    override fun getOrAddBlock(bs: BlockState): VolBlockState {
        requireIsLinked()
        val b = hashPalette[bs]
        if (b != null) return b
        return addBlock(bs)
    }

    override fun getFromId(id: BlockPaletteId): VolBlockState {
        requireIsLinked()
        return idToVolBlockState[id.toInt()]
    }

    override fun iter(): Iterator<VolBlockState> {
        requireIsLinked()
        return idToVolBlockState.iterator()
    }


    override fun populateFromUnlinkedMappings(mappings: HashMap<BlockPaletteId, BlockState>) {
        requireNotLinked()
        require(mappings.size > 0) { "Mappings should at least be of length 1." }

        idToVolBlockState = mutableListOf()
        val maxId = mappings.maxOf { it.key }
        for (idi in 0..maxId) {
            val id = idi.toShort()
            val state = mappings[id]
                ?: error("Invalid palette, its max id is $maxId but an earlier id $id is unmapped.")
            val vbs = VolBlockState.newUnlinked(id, state)
            idToVolBlockState.add(vbs)
        }

        hashPalette = hashMapOf()
        hashPalette.putAll(
            idToVolBlockState.map { it.state to it }
        )
    }

    override fun toUnlinkedBlockStateMappings(): HashMap<BlockPaletteId, BlockState> {
        val mappings = HashMap<BlockPaletteId, BlockState>()
        mappings.putAll(
            idToVolBlockState.map { it.paletteId to it.state }
        )
        return mappings
    }

    override fun link(volume: McVolume) {
        requireNotLinked()
        idToVolBlockState.forEach { it.parentVolUuid = volume.uuid }
        linkedVolume = volume
    }


    private fun addBlock(bs: BlockState): VolBlockState {
        requireIsLinked()
        val volBlockState = VolBlockState.new(linkedVolume!!.uuid, idToVolBlockState.size.toShort(), bs)
        idToVolBlockState.add(volBlockState)
        hashPalette[bs] = volBlockState
        return volBlockState
    }


}