package com.sloimay

import com.sloimay.smath.vectors.ivec3
import com.sloimay.mcvolume.McVolume
import com.sloimay.mcvolume.McVolumeUtils.Companion.deserializeNbtCompound
import com.sloimay.mcvolume.McVolumeUtils.Companion.makePackedLongArrLF
import com.sloimay.mcvolume.McVolumeUtils.Companion.serializeNbtCompound
import com.sloimay.mcvolume.McVolumeUtils.Companion.unpackLongArrLFIntoShortArray
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.smath.vectors.IVec3
import net.querz.mca.CompressionType
import net.querz.nbt.io.SNBTUtil
import net.querz.nbt.tag.ByteTag
import net.querz.nbt.tag.CompoundTag
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource


/**
 *
 * TODO:
 *  Look into unimi's fast utils:
 *      https://github.com/karussell/fastutil/blob/master/src/it/unimi/dsi/fastutil/ints/Int2ObjectMap.java
 *  That's how World Edit's block state cache works
 *
 */









private fun tagSerializationTests() {

    val tag = CompoundTag()
    tag.putInt("hello", 2)

    val nbtBytesIn = serializeNbtCompound(tag, "main", CompressionType.ZLIB)

    val nbtBytesOut = deserializeNbtCompound(nbtBytesIn, CompressionType.ZLIB)

    println(nbtBytesOut.tag as CompoundTag)


}






private fun longPackingTesting() {


    //val a = IntBigArrays.newBigArray(10L)
    //println(a)


    val rand = Random(35898)

    var i = 0

    while (true) {
        if (i and 0xFFFF == 0) {
            println("test ${i}")
        }
        //println("=========== new test")
        val valuesBitSize = rand.nextInt(1..16)
        //println("values bit size: ${valuesBitSize}")
        val valueMax = (1 shl valuesBitSize)
        val arrSize = rand.nextInt(0, 2000)
        //println("arr size: ${arrSize}")


        val arr = ShortArray(arrSize) { rand.nextInt(0, valueMax).toShort() }
        //println("start arr ${arr.toList()}")

        val packedArr = makePackedLongArrLF(arr, valuesBitSize)
        //println("long arr ${packedArr.toList()}")

        val unpackedArr = unpackLongArrLFIntoShortArray(packedArr, valuesBitSize, arr.size)
        //println("end arr ${unpackedArr.toList()}")

        if (!arr.withIndex().all { (i, v) -> v == unpackedArr[i] }) {

            //println("start arr ${arr.toList()}")
            //println("long arr ${packedArr.toList()}")
            //println("end arr ${unpackedArr.toList()}")
            error("didn't work")
        }

        i += 1
    }


}







@OptIn(ExperimentalTime::class)
private fun blockVersioningTest() {

    val ts = TimeSource.Monotonic

    val vol = McVolume.new(
        IVec3.new(0, 0, 0),
        IVec3.new(1000, 1000, 1000),
    )

    val rand = Random(95837)
    fun randChar() = "abcdefghijklmnopqrstuvwxyz1234567890_".random(rand)

    val blockCount = 1000
    val blockStates = (0 until blockCount).map {
        BlockState.fromStr((0 until 100).joinToString(separator = "") { randChar().toString() })
    }

    val size = 400
    val blocksToPlace = (0 until size*size*size)
        //.map { vol.getEnsuredPaletteBlock(blockStates.random(rand)) }
        .map { vol.getEnsuredPaletteBlock(blockStates.random(rand).stateStr) }
        //.map { blockStates.random(rand) }

    val start = ts.markNow()
    var i = 0
    //val volBlock = blocksToPlace[0]
    for (y in 0 until size) for (z in 0 until size) for (x in 0 until size) {
        //vol.setBlockStateStr(ivec3(x, y, z), blocksToPlace[i].state.stateStr)
        vol.setVolBlockState(ivec3(x, y, z), blocksToPlace[i])
        i++
    }
    println(start.elapsedNow())
    println("blocks per second: ${i.toDouble() / start.elapsedNow().toDouble(DurationUnit.SECONDS)}")

}





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

    val blocksToPlace = (0 until (size*size*size))
        .map { blocks[rand.nextInt(0, blocks.size)]
    }

    println("started")
    val ts = TimeSource.Monotonic
    val start = ts.markNow()


    var i = 0

    for (y in 0 until size) for (z in 0 until size) for (x in 0 until size) {

        val b = blocksToPlace[i]
        val volBlock = vol.getEnsuredPaletteBlock(b)
        vol.setVolBlockState(ivec3(x, y, z), volBlock)

        i++
    }

    println("done in ${start.elapsedNow()}")


}










internal fun main() {

    //tagSerializationTests()
    //longPackingTesting()


    blockVersioningTest()





    return


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