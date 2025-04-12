package com.sloimay.mcvolume

import com.sloimay.smath.vectors.IVec3
import com.sloimay.smath.vectors.ivec3
import com.sloimay.mcvolume.block.VolBlockState
import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.DEFAULT_BLOCK_ID
import com.sloimay.mcvolume.blockpalette.BlockPalette
import com.sloimay.mcvolume.blockpalette.HashedBlockPalette
import com.sloimay.mcvolume.blockpalette.ListBlockPalette
import net.querz.nbt.tag.CompoundTag
import java.util.*
import java.util.concurrent.atomic.AtomicLong


private val rollingUuid = AtomicLong(0)
private fun getNewUuid() = rollingUuid.getAndAdd(1)




/**
 * Non thread safe
 */
class McVolume internal constructor(
    internal var chunks: Array<Chunk?>,

    // INVARIANT: VolBlockStates in the block palette are always up-to-date with the
    //            version of this McVolume.
    internal var blockPalette: HashedBlockPalette,

    internal val blockStateCache: HashMap<String, BlockState>,
    //internal val blockStateCache: Object2ObjectOpenHashMap<String, BlockState>,

    internal val CHUNK_BIT_SIZE: Int,
) {

    // Start and end in chunk grid space
    internal var chunkGridBound: IntBoundary = IntBoundary.new(ivec3(0, 0, 0), ivec3(0, 0, 0))

    internal var loadedBound: IntBoundary = IntBoundary.new(ivec3(0, 0, 0), ivec3(0, 0, 0))
    internal var wantedBound: IntBoundary = IntBoundary.new(ivec3(0, 0, 0), ivec3(0, 0, 0))

    // # VolBlockState safety
    internal val uuid: Long = getNewUuid()

    /**
     * Not related to Minecraft versions. (McVolume is version agnostic)
     * If, from the outside, you get a VolBlockState object from the palette,
     * then "clean" this volume (like getting rid of unnecessary palette entries),
     * if the VolBlockState object you got is not in the palette anymore then its
     * cached ID is wrong.
     */
    //internal val volumeVersion: Int = 0

    // Derived constants
    internal val CHUNK_SIDE_LEN = 1 shl CHUNK_BIT_SIZE
    internal val CHUNK_SIZE_BIT_MASK = CHUNK_SIDE_LEN - 1
    internal val CHUNK_BLOCK_COUNT = CHUNK_SIDE_LEN * CHUNK_SIDE_LEN * CHUNK_SIDE_LEN



    companion object {

        fun new(
            loadedAreaMin: IVec3,
            loadedAreaMax: IVec3,
            defaultBlockStr: String = "minecraft:air",
            chunkBitSize: Int = 5,
        ): McVolume {
            require(chunkBitSize in 1..8) {
                "Chunk bit size can only be between 1 and 8, but got ${chunkBitSize}."
            }
            var vol = McVolume(
                chunks = Array(0) { null },
                blockPalette = HashedBlockPalette(),
                blockStateCache = HashMap(),
                CHUNK_BIT_SIZE = chunkBitSize,
            )
            vol.setLoadedArea(loadedAreaMin, loadedAreaMax)
            vol.blockPalette.getOrAddBlock(BlockState.fromStr(defaultBlockStr), vol.uuid)

            return vol
        }

    }

    fun getEnsuredPaletteBlock(blockState: BlockState): VolBlockState {
        /*
        // Actually we're gonna use a hashed block palette the entire way through for now lol

        // If the palette gets big enough, replace it by a hashmap block palette instead
        if (this.blockPalette.size > 5 && blockPalette is ListBlockPalette) {
            val newPalette = HashedBlockPalette(this.blockPalette.getDefaultBlock().state, this)
            // MAKE SURE!! that blocks in the palette map to the same id when handing over
            // Feels like this is gonna bite me in the ahh later down the line lmao
            for (b in this.blockPalette.iter()) {
                newPalette.getOrAddBlock(b.state)
            }
            this.blockPalette = newPalette
        }*/

        return this.blockPalette.getOrAddBlock(blockState, this.uuid)
    }

    fun getEnsuredPaletteBlock(blockStateStr: String): VolBlockState {
        return getEnsuredPaletteBlock(strToBsCached(blockStateStr))
    }

    fun getPaletteBlock(blockState: BlockState): VolBlockState? {
        return this.blockPalette.getBlock(blockState)
    }

    fun getPaletteBlock(blockStateStr: String): VolBlockState? {
        return getPaletteBlock(BlockState.fromStr(blockStateStr))
    }


    fun getDefaultBlock() = this.blockPalette.getDefaultBlock()

    /**
     * About 1.5-2.0x slower than setVolBlockState
     */
    fun setBlockState(pos: IVec3, blockState: BlockState) {
        setVolBlockState(pos, getEnsuredPaletteBlock(blockState))
    }

    /**
     * About 6x slower than setVolBlockState
     */
    fun setBlockStateStr(pos: IVec3, blockStateString: String) {
        setBlockState(pos, strToBsCached(blockStateString))
    }

    fun setVolBlockState(pos: IVec3, volBlockState: VolBlockState) {
        if (!loadedBound.posInside(pos)) { error("Pos not in loaded area") }
        if (volBlockState.parentVolUuid != this.uuid) { error("Trying to place an unregistered block.") }

        val chunkIdx = chunkGridBound.posToYzxIdx(posToChunkPos(pos))
        var chunk = chunks[chunkIdx]
        if (chunk == null) {
            if (volBlockState.paletteId != DEFAULT_BLOCK_ID) {
                val newChunk = Chunk.new(CHUNK_BIT_SIZE)
                chunks[chunkIdx] = newChunk
                val localBlockCoords = posToChunkLocalCoords(pos)
                newChunk.setBlock(localBlockCoords, volBlockState)
            }
        } else {
            val localBlockCoords = posToChunkLocalCoords(pos)
            chunk.setBlock(localBlockCoords, volBlockState)
        }
    }

    /**
     * We're storing a reference to the inputted nbt, so any modification made outside the volume will have
     * an effect inside too.
     */
    fun setTileData(pos: IVec3, tileData: CompoundTag?) {
        if (!loadedBound.posInside(pos)) { throw Error("Pos not in loaded area") }

        val chunkIdx = chunkGridBound.posToYzxIdx(posToChunkPos(pos))
        var chunk = chunks[chunkIdx]
        if (chunk == null) {
            if (tileData != null) {
                val newChunk = Chunk.new(CHUNK_BIT_SIZE)
                chunks[chunkIdx] = newChunk
                val localBlockCoords = posToChunkLocalCoords(pos)
                newChunk.setTileData(localBlockCoords, tileData)
            }
        } else {
            val localBlockCoords = posToChunkLocalCoords(pos)
            chunk.setTileData(localBlockCoords, tileData)
        }
    }



    fun getVolBlockState(pos: IVec3): VolBlockState {
        if (!loadedBound.posInside(pos)) { throw Error("Pos not in loaded area") }

        val chunkIdx = chunkGridBound.posToYzxIdx(posToChunkPos(pos))
        var chunk = chunks[chunkIdx]
        if (chunk == null) {
            return blockPalette.getFromId(DEFAULT_BLOCK_ID)
        } else {
            val localBlockCoords = posToChunkLocalCoords(pos)
            return blockPalette.getFromId(chunk.getBlock(localBlockCoords))
        }
    }

    fun getBlockState(pos: IVec3): BlockState = getVolBlockState(pos).state

    /**
     * The tile data returned is mutable
     */
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
            posToChunkPos(newBoundary.b + (CHUNK_SIDE_LEN - 1))
        )
        val newChunkCount = newChunkGridBound.dim.eProd()
        val newChunks: Array<Chunk?> = Array(newChunkCount) { null }
        val newWantedBound = newBoundary
        val newLoadedBound =
            IntBoundary.new(newChunkGridBound.a * CHUNK_SIDE_LEN, (newChunkGridBound.b) * CHUNK_SIDE_LEN)

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

        return Optional.of(IntBoundary.new(minChunkPos, maxChunkPos + 1))
    }

    private fun strToBsCached(bsStr: String): BlockState {
        return blockStateCache.getOrPut(bsStr) { BlockState.fromStr(bsStr) }
    }

    private fun posToChunkPos(pos: IVec3): IVec3 {
        return pos shr CHUNK_BIT_SIZE
    }

    private fun posToChunkLocalCoords(pos: IVec3): IVec3 {
        return pos and CHUNK_SIZE_BIT_MASK
    }

}