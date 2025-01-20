package me.sloimay

import me.sloimay.mcvolume.McVolume
import me.sloimay.mcvolume.block.BlockState
import me.sloimay.smath.vectors.ivec3
import net.querz.nbt.tag.ByteTag
import net.querz.nbt.tag.CompoundTag

internal fun main() {

    val v = McVolume.new(ivec3(0, 0, 0), ivec3(100, 100, 100))

    val stone = v.getPaletteBlock(
        BlockState.new("minecraft", "stone", listOf(
            "up" to "false",
            "down" to "side"
        ))
    )

    val d = CompoundTag()
    d.put("Bonjour", ByteTag(1))

    v.setTileData(ivec3(0, 2, 4), d)

    val d2 = v.getTileData(ivec3(0, 2, 4))
    println(d2)

    d2?.put("Bnour", ByteTag(2))
    println(v.getTileData(ivec3(0, 2, 4)))

}