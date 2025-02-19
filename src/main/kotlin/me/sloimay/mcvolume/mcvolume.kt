package me.sloimay.mcvolume

import me.sloimay.mcvolume.block.VolBlock
import me.sloimay.mcvolume.block.BlockState
import me.sloimay.mcvolume.block.DEFAULT_BLOCK_ID
import me.sloimay.smath.vectors.IVec3
import me.sloimay.smath.vectors.ivec3
import net.querz.nbt.tag.CompoundTag
import java.util.*


class McVolume {

    internal var chunks: Array<Chunk?> = Array(0) { null }
    internal var volBlockPalette: MutableList<VolBlock> = mutableListOf()

    // Start and end in chunk grid space
    internal var chunkGridBound: IntBoundary = IntBoundary.new(ivec3(0, 0, 0), ivec3(0, 0, 0))

    internal var loadedBound: IntBoundary = IntBoundary.new(ivec3(0, 0, 0), ivec3(0, 0, 0))
    internal var wantedBound: IntBoundary = IntBoundary.new(ivec3(0, 0, 0), ivec3(0, 0, 0))


    companion object {
        fun new(
            loadedAreaMin: IVec3,
            loadedAreaMax: IVec3,
            defaultBlockStr: String = "minecraft:air"
        ): McVolume {
            var vol = McVolume()
            vol.addPaletteBlock(defaultBlockStr)
            vol.setLoadedArea(loadedAreaMin, loadedAreaMax)

            return vol
        }
    }

    fun getPaletteBlock(blockState: BlockState): VolBlock {
        //println(blockState.stateStr)
        return this.getPaletteBlock(blockState.stateStr)
    }

    fun getPaletteBlock(blockStateStr: String): VolBlock {
        val blockIdx = volBlockPalette.indexOfFirst { blockStateStr == it.state.stateStr }
        if (blockIdx == -1) {
            return addPaletteBlock(blockStateStr)
        } else {
            return volBlockPalette[blockIdx]
        }
    }

    fun getDefaultBlock() = this.volBlockPalette[0]

    internal fun addPaletteBlock(blockState: String): VolBlock {
        val volBlock = VolBlock.new(volBlockPalette.size.toShort(), BlockState.fromStr(blockState))
        volBlockPalette.add(volBlock)
        return volBlock
    }



    fun setBlock(pos: IVec3, volBlock: VolBlock) {
        if (!loadedBound.posInside(pos)) { throw Error("Pos not in loaded area") }

        val chunkIdx = chunkGridBound.posToYzxIdx(posToChunkPos(pos))
        var chunk = chunks[chunkIdx]
        if (chunk == null) {
            if (volBlock.paletteId != DEFAULT_BLOCK_ID) {
                val newChunk = Chunk.new()
                chunks[chunkIdx] = newChunk
                val localBlockCoords = posToChunkLocalCoords(pos)
                newChunk.setBlock(localBlockCoords, volBlock)
            }
        } else {
            val localBlockCoords = posToChunkLocalCoords(pos)
            chunk.setBlock(localBlockCoords, volBlock)
        }
    }

    fun setTileData(pos: IVec3, tileData: CompoundTag?) {
        if (!loadedBound.posInside(pos)) { throw Error("Pos not in loaded area") }

        val chunkIdx = chunkGridBound.posToYzxIdx(posToChunkPos(pos))
        var chunk = chunks[chunkIdx]
        if (chunk == null) {
            if (tileData != null) {
                val newChunk = Chunk.new()
                chunks[chunkIdx] = newChunk
                val localBlockCoords = posToChunkLocalCoords(pos)
                newChunk.setTileData(localBlockCoords, tileData)
            }
        } else {
            val localBlockCoords = posToChunkLocalCoords(pos)
            chunk.setTileData(localBlockCoords, tileData)
        }
    }



    fun getBlock(pos: IVec3): VolBlock {
        if (!loadedBound.posInside(pos)) { throw Error("Pos not in loaded area") }

        val chunkIdx = chunkGridBound.posToYzxIdx(posToChunkPos(pos))
        var chunk = chunks[chunkIdx]
        if (chunk == null) {
            return volBlockPalette[DEFAULT_BLOCK_ID.toInt()]
        } else {
            val localBlockCoords = posToChunkLocalCoords(pos)
            return volBlockPalette[chunk.getBlock(localBlockCoords).toInt()]
        }
    }

    fun getTileData(pos: IVec3): CompoundTag? {
        if (!loadedBound.posInside(pos)) { throw Error("Pos not in loaded area") }

        val chunkIdx = chunkGridBound.posToYzxIdx(posToChunkPos(pos))
        var chunk = chunks[chunkIdx]
        if (chunk == null) {
            return null
        } else {
            val localBlockCoords = posToChunkLocalCoords(pos)
            return chunk.getTileData(localBlockCoords)
        }
    }



    fun expandLoadedArea(expansion: IVec3) {
        this.setLoadedArea(
            this.wantedBound.a - expansion,
            this.wantedBound.b + expansion
        )
    }

    fun setLoadedArea(areaMin: IVec3, areaMax: IVec3) {

        val newBoundary = IntBoundary.new(areaMin, areaMax)
        val newChunkGridBound = IntBoundary.new(
            posToChunkPos(newBoundary.a),
            posToChunkPos(newBoundary.b + (CHUNK_SIDE_LEN - 1)))
        val newChunkCount = newChunkGridBound.dim.elementProduct()
        val newChunks: Array<Chunk?> = Array(newChunkCount) { null }
        val newWantedBound = newBoundary
        val newLoadedBound = IntBoundary.new(newChunkGridBound.a * CHUNK_SIDE_LEN, (newChunkGridBound.b) * CHUNK_SIDE_LEN)

        // Populate new chunks
        if (chunks.isNotEmpty()) {
            for (chunkPos in this.chunkGridBound.iterYzx()) {
                val oldChunkInNewGrid = newChunkGridBound.posInside(chunkPos)
                if (oldChunkInNewGrid) {
                    val oldChunkGridIdx = this.chunkGridBound.posToYzxIdx(chunkPos)
                    val oldChunk = this.chunks[oldChunkGridIdx]
                    if (oldChunk != null) {
                        val newChunkGridIdx = newChunkGridBound.posToYzxIdx(chunkPos)
                        newChunks[newChunkGridIdx] = oldChunk
                    }
                }
            }
        }

        // Set
        this.chunks = newChunks
        this.chunkGridBound = newChunkGridBound
        this.loadedBound = newLoadedBound
        this.wantedBound = newWantedBound
    }


    internal fun cleanChunks() {
        for (chunkPos in chunkGridBound.iterYzx()) {
            val chunkIdx = chunkGridBound.posToYzxIdx(chunkPos)
            val chunk = chunks[chunkIdx] ?: continue
            val chunkCanBeCleanedUp = chunk.canBeCleanedUp()
            if (chunkCanBeCleanedUp) {
                chunks[chunkIdx] = null
            }
        }
    }

    fun getBuildBounds(): IntBoundary {
        val buildChunkBoundsOption = this.getBuildChunkBounds()
        // All chunks empty, so we return the block at 0, 0, 0
        if (buildChunkBoundsOption.isEmpty) {
            return IntBoundary.new(IVec3.splat(0), IVec3.splat(1))
        }

        val buildChunkBounds = buildChunkBoundsOption.get()
        var minPos = IVec3.MAX
        var maxPos = IVec3.MIN
        for (chunkPos in buildChunkBounds.iterYzx()) {
            if (!(chunkPos onBorderOf buildChunkBounds)) { continue; }

            // If chunk doesn't exist, don't iterate over it
            val chunkIdx = chunkGridBound.posToYzxIdx(chunkPos)
            val chunk = chunks[chunkIdx]
            if (chunk == null) { continue; }

            // Compute the min and max block positions
            val onMinBorder = buildChunkBounds.posOnMinBorder(chunkPos)
            val onMaxBorder = buildChunkBounds.posOnMaxBorder(chunkPos)
            val chunkWorldPos = chunkPos shl CHUNK_BIT_SIZE
            if (onMinBorder) {
                val minLocalPos = chunk.computeMinLocalPos()
                minLocalPos.ifPresent { localPos ->
                    val worldPos = chunkWorldPos + localPos
                    minPos = minPos.min(worldPos)
                }
            }
            if (onMaxBorder) {
                val maxLocalPos = chunk.computeMaxLocalPos()
                maxLocalPos.ifPresent { localPos ->
                    val worldPos = chunkWorldPos + localPos
                    maxPos = maxPos.max(worldPos)
                }
            }
        }

        return IntBoundary.new(minPos, maxPos + 1)
    }

    internal fun getBuildChunkBounds(): Optional<IntBoundary> {
        var minChunkPos: IVec3? = null
        var maxChunkPos: IVec3? = null

        for (chunkPos in chunkGridBound.iterYzx()) {
            val chunkIdx = chunkGridBound.posToYzxIdx(chunkPos)
            val chunk = chunks[chunkIdx]
            if (chunk == null) { continue }

            if (minChunkPos == null) { minChunkPos = chunkPos }
            if (maxChunkPos == null) { maxChunkPos = chunkPos }

            minChunkPos = minChunkPos.min(chunkPos)
            maxChunkPos = maxChunkPos.max(chunkPos)
        }

        if (minChunkPos == null || maxChunkPos == null) {
            return Optional.empty()
        }

        return Optional.of(IntBoundary.new(minChunkPos, maxChunkPos+1))
    }

    private fun posToChunkPos(pos: IVec3): IVec3 {
        return pos shr CHUNK_BIT_SIZE
    }

    private fun posToChunkLocalCoords(pos: IVec3): IVec3 {
        return pos and CHUNK_SIZE_BIT_MASK
    }

}