package com.sloimay

import com.sloimay.smath.vectors.ivec3
import com.sloimay.mcvolume.McVolume
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.deserializeNbtCompound
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.makePackedLongArrLF
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.serializeNbtCompound
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.unpackLongArrLFIntoShortArray
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.io.exportToMcv
import com.sloimay.mcvolume.io.fromSchem
import com.sloimay.smath.geometry.boundary.IntBoundary
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
 *  That's what World Edit's block state cache use
 *
 */










private fun blockGridTesting() {

    val vol = McVolume.new(IVec3.splat(-400), IVec3.splat(1500), chunkBitSize = 5)
    val vol2 = McVolume.new(IVec3.splat(-400), IVec3.splat(1500), chunkBitSize = 5)


    val blocks = listOf(
        "glass",
        "stone",
        "glowstone",
        "redstone_block",
        "iron_block",
        "gold_block",
        "diamond_block",
        "emerald_block",
        "grass_block",
        "dirt",
        "stone",
        "andesite",
        "diorite",
        "netherite_block",
    )
        .map { "minecraft:$it" }
        .map { BlockState.fromStr(it) }



    val blocks2 = listOf(
        "red_wool",
        "polished_andesite",
        "oak_log",
        "spruce_log",
        "bedrock",
    )
        .map { "minecraft:$it" }
        .map { BlockState.fromStr(it) }


    val cubeBounds = IntBoundary.new(IVec3(0), IVec3(1022 / 20, 512, 1021 / 20)).shift(IVec3(10))
    //println("cube bounds $cubeBounds")
    val rand = Random(39194)
   /* val blocksToPlace = (0 until cubeBounds.dims.eProd())
        .map { blocks.random(rand) }*/
    val pbStart = TimeSource.Monotonic.markNow()
    var i = 0
    val chunksToLeaveEmpty = arrayOf(
        IVec3(2, 1, 2),
        IVec3(4, 3, 1),
        IVec3(6, 2, 3),
    )
    for (pos in cubeBounds.iterYzx()) {
        if (i % 8 == 0) {
            if (vol.posToChunkPos(pos).eSum() % 4 > 0) {
                i++
                continue
            }
            vol.setBlockState(pos, blocks.random(rand))
        } else if (i % 92 == 0) {
            // Fill vol2 with junk to test
            vol2.setBlockState(pos, blocks2.random(rand))
        }
        i++
    }

    println("placed $i blocks in ${pbStart.elapsedNow()}")

    // Extraction testing
    val extractStart = TimeSource.Monotonic.markNow()
    val bgBounds = cubeBounds//IntBoundary.new(IVec3(0), IVec3(512, 384, 512)).shift(IVec3(10))
    val (dims, blockArr, mappings) = vol.extractBlockGrid(bgBounds)

    val extractTimeTaken = extractStart.elapsedNow()
    println("extracted in ${extractTimeTaken}, extracting ${(i.toDouble() / extractTimeTaken.toDouble(DurationUnit.SECONDS)).toLong()} blocks per second")


    // Placing testing
    val bgPlacingStart = TimeSource.Monotonic.markNow()
    // TODO: figure out how the hecc is placing as fast as getting
    vol2.placeBlockGrid(bgBounds.a, blockArr, dims, mappings)
    val placingTimeTaken = bgPlacingStart.elapsedNow()
    println("placed in $placingTimeTaken, placing ${(i.toDouble() / placingTimeTaken.toDouble(DurationUnit.SECONDS)).toLong()} blocks per second")



    /*for (p in bgBounds.iterYzx()) {
        val idxP = p - bgBounds.a
        val b = mappings[blockArr[idxP.x + bgBounds.dims.x*idxP.z + bgBounds.dims.x*bgBounds.dims.z*idxP.y]]!!
        val b2 = vol.getBlockState(p)
        if (b != b2) error("not equal at $p")
    }*/

    //println("all vol and extracted blocks match!")

    for (p in bgBounds.iterYzx()) {
        val b = vol.getBlockState(p)
        val b2 = vol2.getBlockState(p)
        if (b != b2) error("not equal at $p")
    }

    println("Extraction then placing matches")


    /*val p = IVec3(37, 107, 60)
    println(mappings[blockArr[p.x + bgDims.x*p.z + bgDims.x*bgDims.z*p.y]])

    println(vol.getBlockState(bgOrig + p))

     */

    //vol.exportToMcv("""D:\Minecraft Servers\Paper 1.20.4 - Building\plugins\nicepreview\build.mcv""")
}





private fun randomBenchmarkThing() {

    val vol = McVolume.new(IVec3.ZERO, IVec3.splat(500))


    val blocks = listOf(
        "glass",
        "stone",
        "glowstone",
        "redstone_block",
        "iron_block"
    )
        .map { "minecraft:$it" }
        .map { BlockState.fromStr(it) }


    val bounds = IntBoundary.new(IVec3.ZERO, IVec3.splat(500))

    val rand = Random(309094)

    val blocksToPlace = (0 until (500*500*500))
        .map { blocks.random(rand) }


    val pbStart = TimeSource.Monotonic.markNow()

    var i = 0
    for (pos in bounds.iterYzx()) {
        vol.setBlockState(pos, blocksToPlace[i])
        i++
    }

    println("placed blocks in ${pbStart.elapsedNow()}")

    val exportStart = TimeSource.Monotonic.markNow()
    /*vol.exportToSchem(
        """C:\Users\Bananas Man\AppData\Roaming\.minecraft\config\worldedit\schematics\test_500_cubed.schem""",
        McVersion.JE_1_20_4,
    )*/
    vol.exportToMcv("""C:\Users\Bananas Man\AppData\Roaming\.minecraft\config\worldedit\schematics\test_500_cubed.mcv""")

    println("exported in ${exportStart.elapsedNow()}")


}






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

    val blockCount = 10
    val blockStates = (0 until blockCount).map {
        BlockState.fromStr((0 until 100).joinToString(separator = "") { randChar().toString() })
    }

    val size = 400
    val blocksToPlace = (0 until size*size*size)
        .map { vol.getEnsuredPaletteBlock(blockStates.random(rand)) }
        //.map { vol.getEnsuredPaletteBlock(blockStates.random(rand).stateStr) }
        //.map { blockStates.random(rand) }
        //.map { blockStates.random(rand).stateStr }

    val start = ts.markNow()
    var i = 0
    //val volBlock = blocksToPlace[0]
    for (y in 0 until size) for (z in 0 until size) for (x in 0 until size) {
        //vol.setBlockStateStr(ivec3(x, y, z), blocksToPlace[i].state.stateStr)
        //vol.setBlockStateStr(ivec3(x, y, z), blocksToPlace[i])
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

    val wfcInput = McVolume.fromSchem("""F:\Intellij Workspaces\mc_building\assets\build21\wfc_1.schem""")

    for (p in wfcInput.computeBuildBounds().iterYzx()) {
        println("blockstate at ${p} is : ${wfcInput.getBlockState(p).stateStr}")
        //vol.setBlockState(p + IVec3(32), wfcInput.getBlockState(p))
    }



    return

    blockGridTesting()


    return

    //randomBenchmarkThing()

    //tagSerializationTests()
    //longPackingTesting()


    return

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