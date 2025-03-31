package com.sloimay

import com.sloimay.smath.vectors.ivec3
import com.sloimay.mcvolume.McVolume
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.smath.vectors.IVec3
import net.querz.nbt.io.SNBTUtil
import net.querz.nbt.tag.ByteTag
import net.querz.nbt.tag.CompoundTag
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource



@OptIn(ExperimentalTime::class)
internal fun blockPaletteSpeedTesting() {

    val vol = McVolume.new(
        IVec3.new(0, 0, 0),
        IVec3.new(1000, 1000, 1000),
    )

    val rand = Random(95837)

    val chars = "abcdefghijklmnopqrstuvwxyz1234567890"

    fun getRandChar(): Char {
        return chars[rand.nextInt(0, chars.length)]
    }

    val blockCount = 1000
    val blocks = mutableListOf<BlockState>()

    for (i in 0 until blockCount) {
        val b = (0 until 100).map { getRandChar() }.joinToString(separator = "")
        blocks.add(BlockState.fromStr(b))
    }


    /*for (b in blocks) {
        vol.getPaletteBlock(b)
    }*/




    val size = 400

    println(size*size*size)

    val blocksToPlace = (0 until (size*size*size)).map { blocks[rand.nextInt(0, blocks.size)] }

    println("started")
    val ts = TimeSource.Monotonic
    val start = ts.markNow()


    var i = 0

    for (y in 0 until size) for (z in 0 until size) for (x in 0 until size) {

        val b = blocksToPlace[i]
        val volBlock = vol.getEnsuredPaletteBlock(b)
        vol.setBlock(ivec3(x, y, z), volBlock)

        i++
    }

    println("done in ${start.elapsedNow()}")


}










internal fun main() {


    val a = SNBTUtil.fromSNBT("{test:'hello'}", true)

    println(a)

    return

    blockPaletteSpeedTesting()

    return

    //

    //SNBTUtil.fromSNBT("{CustomName:'{\"text\":\"\",\"extra\":[\"2\"]}',Items:[{Count:64b,Slot:0b,id:\"minecraft:redstone\"},{Count:60b,Slot:1b,id:\"minecraft:redstone\"}],id:\"minecraft:barrel\"}\n", true)
    //val a = SNBTUtil.fromSNBT("{CustomName:\"{'text':'azeaze','extra':[\\\"2\\\"]}\",Items:[{Count:64b,Slot:0b,id:\"minecraft:redstone\"},{Count:60b,Slot:1b,id:\"minecraft:redstone\"}],id:\"minecraft:barrel\"}\n")


    //val s = "{CustomName:'{\"text\":\"\",\"extra\":[\"2\"]}',Items:[{Count:64b,Slot:0b,id:\"minecraft:redstone\"},{Count:60b,Slot:1b,id:\"minecraft:redstone\"}],id:\"minecraft:barrel\"}"




    //println(a.toString())

    return

    val v = McVolume.new(ivec3(0, 0, 0), ivec3(100, 100, 100))

    val stone = v.getEnsuredPaletteBlock(
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