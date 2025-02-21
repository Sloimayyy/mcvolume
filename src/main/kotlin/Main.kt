package com.sloimay

import com.sloimay.smath.vectors.ivec3
import com.sloimay.mcvolume.McVolume
import com.sloimay.mcvolume.block.BlockState
import net.querz.nbt.io.SNBTUtil
import net.querz.nbt.tag.ByteTag
import net.querz.nbt.tag.CompoundTag

internal fun main() {

    //

    //SNBTUtil.fromSNBT("{CustomName:'{\"text\":\"\",\"extra\":[\"2\"]}',Items:[{Count:64b,Slot:0b,id:\"minecraft:redstone\"},{Count:60b,Slot:1b,id:\"minecraft:redstone\"}],id:\"minecraft:barrel\"}\n", true)
    //val a = SNBTUtil.fromSNBT("{CustomName:\"{'text':'azeaze','extra':[\\\"2\\\"]}\",Items:[{Count:64b,Slot:0b,id:\"minecraft:redstone\"},{Count:60b,Slot:1b,id:\"minecraft:redstone\"}],id:\"minecraft:barrel\"}\n")


    //val s = "{CustomName:'{\"text\":\"\",\"extra\":[\"2\"]}',Items:[{Count:64b,Slot:0b,id:\"minecraft:redstone\"},{Count:60b,Slot:1b,id:\"minecraft:redstone\"}],id:\"minecraft:barrel\"}"




    //println(a.toString())

    return

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