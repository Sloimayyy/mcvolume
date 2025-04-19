package com.sloimay.mcvolume.io


import com.sloimay.mcvolume.IntBoundary
import com.sloimay.smath.vectors.IVec3
import com.sloimay.smath.vectors.ivec3
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.readVarint
import com.sloimay.mcvolume.block.VolBlockState
import com.sloimay.mcvolume.McVolume
import com.sloimay.mcvolume.utils.McVersion
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.pushVarint
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.serializeNbtCompound
import com.sloimay.mcvolume.utils.McVolumeUtils.Companion.stringListAsStringNbtList
import net.querz.mca.CompressionType
import net.querz.nbt.io.NBTUtil
import net.querz.nbt.io.NamedTag
import net.querz.nbt.tag.*
import kotlin.io.path.Path
import kotlin.math.max
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource



enum class SchemVersion(val int: Int) {
    VERSION_2(2),
    VERSION_3(3);
}

class SchemMetadata(
    val name: String? = null,
    val authors: List<String>? = null,
    val requiredMods: List<String>? = null,
    val extraNbt: CompoundTag? = null,
) {

    internal fun getNbt(): CompoundTag {
        val metadataNbt = CompoundTag()

        if (name != null)  metadataNbt.putString("Name", name)
        if (!authors.isNullOrEmpty()) metadataNbt.put("Authors", stringListAsStringNbtList(authors))
        if (!requiredMods.isNullOrEmpty()) metadataNbt.put("RequiredMods", stringListAsStringNbtList(requiredMods))

        metadataNbt.put("Extra", extraNbt)

        return metadataNbt
    }

}


/**
 * Exports this volume to a schematic file.
 *
 * @param filePath The file path of the resulting schem file.
 * @param recommendedMcVersion The recommended Minecraft version this schematic should be loaded in. But it
 *                              usually doesn't matter too much what you put in.
 * @param schemVersion The version of the sponge schematic format. (2 is default, 3 isn't supported
 *                      nearly as much as version 2, but is more modern).
 */
fun McVolume.exportToSchem(
    filePath: String,
    recommendedMcVersion: McVersion,
    schemVersion: SchemVersion = SchemVersion.VERSION_2,
    metadata: SchemMetadata? = null,
    // TODO: maybe make a "data version override"? for version that aren't
    //       documented in the lib yet
) {
    // TODO: add volume cleaning
    // this.clean()

    require(getDefaultBlock().state.fullName == "minecraft:air") {
        "Schematic exporting for volumes with a default block other than 'minecraft:air' isn't supported."
    }

    when(schemVersion) {
        SchemVersion.VERSION_2 -> exportToSchem2(filePath, recommendedMcVersion, metadata)
        SchemVersion.VERSION_3 -> exportToSchem3(filePath, recommendedMcVersion, metadata)
    }
}


/**
 * Creates a new McVolume from a schem file. Only schematic file version 2 and 3s are supported.
 * As I'm lazy but also version 1 is very old.
 *
 * @param schemPath The path of the schematic file.
 */
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
    val schemSize = IVec3(
        schemNbt.getShort("Width").toInt() and 0xFFFF, // Actually unsigned shorts
        schemNbt.getShort("Height").toInt() and 0xFFFF,
        schemNbt.getShort("Length").toInt() and 0xFFFF,
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
            val block = vol.getEnsuredPaletteBlock(k)
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

    // # Tile entities
    run {
        if (blocksNbt.containsKey("BlockEntities")) {
            val blockEntitiesNbt = blocksNbt.getListTag("BlockEntities") as ListTag<CompoundTag>

            for (blockEntNbt in blockEntitiesNbt) {
                val schemPos = IVec3.fromArray(blockEntNbt.getIntArray("Pos"))
                val volPos = schemPos + schemOffset
                //val blockId = blockEntNbt.getString("Id")
                if (blockEntNbt.containsKey("Data")) {
                    val volNbt = blockEntNbt.get("Data") as CompoundTag
                    vol.setTileData(volPos, volNbt)
                }
            }
        }
    }


    return vol
}

private fun McVolume.Companion.fromSchem2(fileNbt: NamedTag): McVolume {

    // # Get data
    val schemNbt = fileNbt.tag as CompoundTag
    val version = schemNbt.getInt("Version")
    val dataVersion = schemNbt.getInt("DataVersion")
    val schemSize = IVec3(
        schemNbt.getShort("Width").toInt() and 0xFFFF, // Actually unsigned shorts
        schemNbt.getShort("Height").toInt() and 0xFFFF,
        schemNbt.getShort("Length").toInt() and 0xFFFF,
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
        IVec3(0, 0, 0)
    }
    schemOffset = schemOffset.withX( metadataNbt.getIntTag("WEOffsetX")?.asInt() ?: schemOffset.x )
    schemOffset = schemOffset.withY( metadataNbt.getIntTag("WEOffsetY")?.asInt() ?: schemOffset.y )
    schemOffset = schemOffset.withZ( metadataNbt.getIntTag("WEOffsetZ")?.asInt() ?: schemOffset.z )

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
            val block = vol.getEnsuredPaletteBlock(k)
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

    // # Tile entities
    run {
        if (schemNbt.containsKey("BlockEntities")) {
            val blockEntitiesNbt = schemNbt.getListTag("BlockEntities") as ListTag<CompoundTag>

            for (blockEntNbt in blockEntitiesNbt) {
                val schemPos = IVec3.fromArray(blockEntNbt.getIntArray("Pos"))
                val volPos = schemPos + schemOffset
                //val blockId = blockEntNbt.getString("Id")
                val volNbt = blockEntNbt.clone()
                volNbt.remove("Pos")
                volNbt.remove("Id")

                vol.setTileData(volPos, volNbt)
            }
        }
    }


    return vol
}




private fun McVolume.exportToSchem2(
    schemPath: String,
    mcVersion: McVersion,
    metadata: SchemMetadata?,
) {
    val schemBounds = this.getBuildBounds()
    val schemOffset = schemBounds.a
    val schemDims = schemBounds.b - schemBounds.a

    val schemNbt = CompoundTag()


    val versionTag = IntTag(SchemVersion.VERSION_2.int)
    schemNbt.put("Version", versionTag)
    val dataVersionTag = IntTag(mcVersion.dataVersion)
    schemNbt.put("DataVersion", dataVersionTag)


    // # Metadata
    val metadataNbt = metadata?.getNbt() ?: CompoundTag()
    // Add McVolume's signature
    metadataNbt.putString("McVolume", "Generated with the McVolume library, written by Sloimay & Contributors.")
    // Add WE metadata
    metadataNbt.putInt("WEOffsetX", schemOffset.x)
    metadataNbt.putInt("WEOffsetY", schemOffset.y)
    metadataNbt.putInt("WEOffsetZ", schemOffset.z)

    schemNbt.put("Metadata", metadataNbt)

    // # Height, Length, Width
    schemNbt.putShort("Height", schemDims.y.toShort())
    schemNbt.putShort("Length", schemDims.z.toShort())
    schemNbt.putShort("Width", schemDims.x.toShort())

    // # Palette
    val (paletteMax, paletteNbt) = blockPaletteAsSchemPaletteNbt()
    schemNbt.putInt("PaletteMax", paletteMax)
    schemNbt.put("Palette", paletteNbt)

    // # BlockData (block states byte array)
    val blockDataNbt = getSchemBlockStateByteArrayTag(schemBounds)
    schemNbt.put("BlockData", blockDataNbt)

    // # Block entities
    val blockEntitiesNbt = getSchem2BlockEntitiesObject(schemOffset)
    schemNbt.put("BlockEntities", blockEntitiesNbt)


    // # Export
    // TODO: use streams instead
    val bytes = serializeNbtCompound(schemNbt, "Schematic", CompressionType.GZIP)
    val fp = Path(schemPath)
    fp.toFile().writeBytes(bytes)
}


private fun McVolume.exportToSchem3(
    schemPath: String,
    mcVersion: McVersion,
    metadata: SchemMetadata?,
) {

    val schemBounds = this.getBuildBounds()
    val schemOffset = schemBounds.a
    val schemDims = schemBounds.b - schemBounds.a

    val schemNbt = CompoundTag()


    val versionTag = IntTag(SchemVersion.VERSION_3.int)
    schemNbt.put("Version", versionTag)
    val dataVersionTag = IntTag(mcVersion.dataVersion)
    schemNbt.put("DataVersion", dataVersionTag)


    // # Metadata
    val metadataNbt = metadata?.getNbt() ?: CompoundTag()
    // Add McVolume's signature
    metadataNbt.putString("McVolume", "Generated with the McVolume library, written by Sloimay & Contributors.")

    schemNbt.put("Metadata", metadataNbt)

    // # Height, Length, Width
    schemNbt.putShort("Height", schemDims.y.toShort())
    schemNbt.putShort("Length", schemDims.z.toShort())
    schemNbt.putShort("Width", schemDims.x.toShort())

    // # Offset
    schemNbt.putIntArray("Offset", schemOffset.toArray())

    // # BlockContainer Nbt
    run {
        val blockContainerNbt = CompoundTag()

        // # Palette
        val (paletteMax, paletteNbt) = blockPaletteAsSchemPaletteNbt()
        //schemNbt.putInt("PaletteMax", paletteMax)
        blockContainerNbt.put("Palette", paletteNbt)

        // # data
        val dataNbt = getSchemBlockStateByteArrayTag(schemBounds)
        blockContainerNbt.put("Data", dataNbt)

        // # Block entities
        val blockEntitiesNbt = getSchem3BlockEntitiesObject(schemOffset)
        blockContainerNbt.put("BlockEntities", blockEntitiesNbt)

        schemNbt.put("Blocks", blockContainerNbt)
    }


    // # Export
    // TODO: use streams instead
    val fileNbt = CompoundTag().also { it.put("Schematic", schemNbt) }
    val bytes = serializeNbtCompound(fileNbt, "", CompressionType.GZIP)
    val fp = Path(schemPath)
    fp.toFile().writeBytes(bytes)

}













@OptIn(ExperimentalTime::class)
private fun McVolume.loadVarintBlockStates(
    schemSize: IVec3,
    schemOffset: IVec3,
    blockVolumeData: ByteArray,
    palette: List<VolBlockState>,
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
            this.setVolBlockState(worldPos, block)
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

private fun McVolume.blockPaletteAsSchemPaletteNbt(): Pair<Int, CompoundTag> {
    // Schem palettes are basically 1-to-1 mappings to McVolume palettes
    val mappings = blockPalette.toUnlinkedBlockStateMappings()
    println(mappings)
    val paletteMax = mappings.size

    val paletteNbt = CompoundTag().also {
        for ((paletteId, blockState) in mappings) {
            it.putInt(blockState.stateStr, paletteId.toInt())
        }
    }

    return Pair(paletteMax, paletteNbt)
}

private fun McVolume.getSchemBlockStateByteArrayTag(buildBounds: IntBoundary): ByteArrayTag {
    val blockDataList = mutableListOf<Byte>()

    for (p in buildBounds.iterYzx()) {
        val vbs = getVolBlockState(p)
        val id = vbs.paletteId.toInt()
        pushVarint(blockDataList, id)
    }

    val nbt = ByteArrayTag(blockDataList.toByteArray())
    return nbt
}

private fun McVolume.getSchem2BlockEntitiesObject(schemOffset: IVec3): ListTag<CompoundTag> {
    val blockEntities = ListTag(CompoundTag::class.java)

    val buildChunkBounds = getBuildChunkBounds().get()
    for (chunkPos in buildChunkBounds.iterYzx()) {
        val chunk = chunks[this.chunkPosToChunkIdx(chunkPos)] ?: continue
        for ((localPos, tileData) in chunk.tileData) {
            val chunkPosInVolume = this.chunkPosToPos(chunkPos)
            val tileDataPos = chunkPosInVolume + localPos
            val blockEntityObject = tileDataToSchem2Compound(schemOffset, tileDataPos, tileData)

            blockEntities.add(blockEntityObject)
        }
    }

    return blockEntities
}

private fun McVolume.getSchem3BlockEntitiesObject(schemOffset: IVec3): ListTag<CompoundTag> {
    val blockEntities = ListTag(CompoundTag::class.java)

    val buildChunkBounds = getBuildChunkBounds().get()
    for (chunkPos in buildChunkBounds.iterYzx()) {
        val chunk = chunks[this.chunkPosToChunkIdx(chunkPos)] ?: continue
        for ((localPos, tileData) in chunk.tileData) {
            val chunkPosInVolume = this.chunkPosToPos(chunkPos)
            val tileDataPos = chunkPosInVolume + localPos
            val blockEntityObject = tileDataToSchem3Compound(schemOffset, tileDataPos, tileData)

            blockEntities.add(blockEntityObject)
        }
    }

    return blockEntities
}

private fun McVolume.tileDataToSchem2Compound(
    schemOffset: IVec3,
    posInVolume: IVec3,
    tileData: CompoundTag
): CompoundTag {
    val blockEntityNbt = tileData.clone()

    val vbs = getVolBlockState(posInVolume)

    val id = "${vbs.state.resLoc}:${vbs.state.name}"
    blockEntityNbt.putIntArray("Pos", (posInVolume - schemOffset).toArray())
    blockEntityNbt.putString("Id", id)

    return blockEntityNbt
}

private fun McVolume.tileDataToSchem3Compound(
    schemOffset: IVec3,
    posInVolume: IVec3,
    tileData: CompoundTag
): CompoundTag {
    val blockEntityNbt = CompoundTag()

    val vbs = getVolBlockState(posInVolume)

    val id = "${vbs.state.resLoc}:${vbs.state.name}"
    blockEntityNbt.putIntArray("Pos", (posInVolume - schemOffset).toArray())
    blockEntityNbt.putString("Id", id)

    blockEntityNbt.put("Data", tileData)

    return blockEntityNbt
}

