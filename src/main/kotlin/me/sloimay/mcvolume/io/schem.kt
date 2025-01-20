package me.sloimay.mcvolume.io


import me.sloimay.mcvolume.McvUtils.Companion.readVarint
import me.sloimay.mcvolume.block.VolBlock
import me.sloimay.mcvolume.McVolume
import me.sloimay.smath.vectors.IVec3
import me.sloimay.smath.vectors.ivec3
import net.querz.nbt.io.NBTUtil
import net.querz.nbt.io.NamedTag
import net.querz.nbt.tag.ByteArrayTag
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.IntTag
import net.querz.nbt.tag.ListTag
import kotlin.math.max
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource


fun McVolume.Companion.fromSchem(schemPath: String): McVolume {
    val fileNbt = NBTUtil.read(schemPath)

    if (fileNbt.name == "") {
        return fromSchem3(fileNbt)
    } else {
        return fromSchem2(fileNbt)
    }
}



private fun McVolume.Companion.fromSchem3(fileNbt: NamedTag): McVolume {
    // # Get data
    val schemNbt = (fileNbt.tag as CompoundTag).get("Schematic") as CompoundTag
    val version = schemNbt.getInt("Version")
    val dataVersion = schemNbt.getInt("DataVersion")
    val schemSize = IVec3.new(
        schemNbt.getShort("Width").toInt(),
        schemNbt.getShort("Height").toInt(),
        schemNbt.getShort("Length").toInt(),
    )
    val metadataNbt = if (schemNbt.containsKey("Metadata")) {
        schemNbt.getCompoundTag("Metadata")
    } else {
        CompoundTag()
    }
    val schemOffset = if (schemNbt.containsKey("Offset")) {
        IVec3.fromArray(schemNbt.getIntArray("Offset"))
    } else {
        IVec3.new(0, 0, 0)
    }

    // [Min; Max)
    val lowBound = schemOffset
    val highBound = schemOffset + schemSize

    // # Build volume
    val defaultBlockStr = "minecraft:air"
    var vol = McVolume.new(lowBound, highBound, defaultBlockStr)
    var blocksNbt = if (schemNbt.containsKey("Blocks")) {
        schemNbt.getCompoundTag("Blocks")
    } else {
        var c = CompoundTag()
        c.put("Palette", CompoundTag())
        c.put("Data", ByteArrayTag())
        c.put("BlockEntities", ListTag(CompoundTag::class.java))
        c
    }

    // Blocks first
    run {
        val paletteNbt = blocksNbt.getCompoundTag("Palette")
        val paletteMaxId = paletteNbt.map { t -> (t.value as IntTag).asInt() }.fold(0) { m, it -> max(m, it) }
        val paletteLength = paletteMaxId + 1
        val paletteString = MutableList(paletteLength) { defaultBlockStr }
        for ((k, v) in paletteNbt)  paletteString[(v as IntTag).asInt()] = k

        // Add the default block to the palette if not present
        // Idk if it's needed but just in case
        var volPaletteLength = paletteLength
        val defaultBlockPosInPalette = paletteString.indexOfFirst { it == defaultBlockStr }
        if (defaultBlockPosInPalette != -1) volPaletteLength += 1
        val defaultBlockInPaletteId = if (defaultBlockPosInPalette == -1) {
            volPaletteLength - 1
        } else {
            defaultBlockPosInPalette
        }

        // If the default block isn't in the schem's palette; it will be the last one for sure
        // as we set every index to the default block
        val defaultBlock = vol.getDefaultBlock()
        var palette = MutableList(volPaletteLength){ defaultBlock }
        paletteNbt.forEach { k, v ->
            val block = vol.getPaletteBlock(k)
            val idx = (v as IntTag).asInt()
            palette[idx] = block
        }
        val blockVolumeData = blocksNbt.getByteArray("Data")

        vol.loadVarintBlockStates(
            schemSize,
            schemOffset,
            blockVolumeData,
            palette,
            defaultBlockInPaletteId,
        )
    }

    return vol
}

private fun McVolume.Companion.fromSchem2(fileNbt: NamedTag): McVolume {

    // # Get data
    val schemNbt = fileNbt.tag as CompoundTag
    val version = schemNbt.getInt("Version")
    val dataVersion = schemNbt.getInt("DataVersion")
    val schemSize = IVec3.new(
        schemNbt.getShort("Width").toInt(),
        schemNbt.getShort("Height").toInt(),
        schemNbt.getShort("Length").toInt(),
    )
    val metadataNbt = if (schemNbt.containsKey("Metadata")) {
        schemNbt.getCompoundTag("Metadata")
    } else {
        CompoundTag()
    }

    // Prioritise WEOffsets
    var schemOffset = if (schemNbt.containsKey("Offset")) {
        IVec3.fromArray(schemNbt.getIntArray("Offset"))
    } else {
        IVec3.new(0, 0, 0)
    }
    schemOffset = schemOffset.withX(
        if (metadataNbt.containsKey("WEOffsetX")) { metadataNbt.getInt("WEOffsetX") } else { schemOffset.x }
    )
    schemOffset = schemOffset.withY(
        if (metadataNbt.containsKey("WEOffsetY")) { metadataNbt.getInt("WEOffsetY") } else { schemOffset.y }
    )
    schemOffset = schemOffset.withZ(
        if (metadataNbt.containsKey("WEOffsetZ")) { metadataNbt.getInt("WEOffsetZ") } else { schemOffset.z }
    )

    // [Min; Max)
    val lowBound = schemOffset
    val highBound = schemOffset + schemSize

    // # Build volume
    val defaultBlockStr = "minecraft:air"
    var vol = McVolume.new(lowBound, highBound, defaultBlockStr)

    // Blocks first
    run {
        val paletteNbt = schemNbt.getCompoundTag("Palette")
        val paletteMaxId = paletteNbt.map { t -> (t.value as IntTag).asInt() }.fold(0) { m, it -> max(m, it) }
        val paletteLength = paletteMaxId + 1
        val paletteString = MutableList(paletteLength) { defaultBlockStr }
        for ((k, v) in paletteNbt)  paletteString[(v as IntTag).asInt()] = k

        // Add the default block to the palette if not present
        // Idk if it's needed but just in case
        var volPaletteLength = paletteLength
        val defaultBlockPosInPalette = paletteString.indexOfFirst { it == defaultBlockStr }
        if (defaultBlockPosInPalette != -1) volPaletteLength += 1
        val defaultBlockInPaletteId = if (defaultBlockPosInPalette == -1) {
            volPaletteLength - 1
        } else {
            defaultBlockPosInPalette
        }

        // If the default block isn't in the schem's palette; it will be the last one for sure
        // as we set every index to the default block
        val defaultBlock = vol.getDefaultBlock()
        var palette = MutableList(volPaletteLength){ defaultBlock }
        paletteNbt.forEach { k, v ->
            val block = vol.getPaletteBlock(k)
            val idx = (v as IntTag).asInt()
            palette[idx] = block
        }
        val blockVolumeData = schemNbt.getByteArray("BlockData")

        vol.loadVarintBlockStates(
            schemSize,
            schemOffset,
            blockVolumeData,
            palette,
            defaultBlockInPaletteId,
        )
    }

    return vol
}


@OptIn(ExperimentalTime::class)
private fun McVolume.loadVarintBlockStates(
    schemSize: IVec3,
    schemOffset: IVec3,
    blockVolumeData: ByteArray,
    palette: List<VolBlock>,
    defaultBlockInPaletteId: Int,
) {
    val timeSource = TimeSource.Monotonic
    val timeStart = timeSource.markNow()

    var blockVolumeIdx = 0
    var bvX = 0
    var bvY = 0
    var bvZ = 0
    while (blockVolumeIdx < blockVolumeData.size) {
        val (blockId, newBVIdx) = readVarint(blockVolumeData, blockVolumeIdx)
        blockVolumeIdx = newBVIdx

        // By default every block is the default one. So don't place if not needed
        if (blockId != defaultBlockInPaletteId) {
            /*println("BONJOUR")
            println(schemOffset)
            println(schemOffset + schemSize)
            println(ivec3(bvX, bvY, bvZ) + schemOffset)
            println(this.wantedBound)
            println(this.loadedBound)*/
            val worldPos = ivec3(bvX, bvY, bvZ) + schemOffset
            val block = palette[blockId]
            this.setBlock(worldPos, block)
        }

        // Iterate position in block data
        bvX += 1
        if (bvX >= schemSize.x) {
            bvX = 0
            bvZ += 1
            if (bvZ >= schemSize.z) {
                bvZ = 0
                bvY += 1
            }
        }
    }

    println("Loaded blocks in ${timeStart.elapsedNow()}")
}


