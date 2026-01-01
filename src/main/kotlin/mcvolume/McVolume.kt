package com.sloimay.mcvolume

import com.sloimay.mcvolume.block.*
import com.sloimay.mcvolume.blockpalette.BlockPaletteMappings
import com.sloimay.smath.vectors.IVec3
import com.sloimay.smath.vectors.ivec3
import com.sloimay.mcvolume.blockpalette.HashedBlockPalette
import com.sloimay.mcvolume.utils.McVolumeUtils
import com.sloimay.mcvolume.utils.onBorderOf
import com.sloimay.smath.geometry.boundary.IntBoundary
import net.querz.nbt.tag.CompoundTag
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread


private val rollingUuid = AtomicLong(0)
private fun getNewUuid() = rollingUuid.getAndAdd(1)




/**
 * Non thread safe
 *
 * TODO:
 *  - Make McVolumes be more modular. + RAII style
 *      Objects that make volumes work can be instantiated outside of a volume independently,
 *      but work along volumes if they are attached to them. Idk, I'm not sure. I'm just
 *      trying to break dependencies and implicit contracts to have the least possible while
 *      still retaining efficient communication between Volumes and their internals, not sure
 *      how to go about it.
 *  - Change the set and get block API to use function overloading probably?
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
    internal var targetBounds: IntBoundary = IntBoundary.new(ivec3(0, 0, 0), ivec3(0, 0, 0))

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

        /**
         * Creates a new McVolume.
         *
         * The loadedAreaMin and loadedAreaMax are *target* bounds, meaning the volume
         * is likely to load a bit more than this inputted area, as it allocates chunks, and not
         * individual blocks.
         */
        fun new(
            targetLoadedAreaMin: IVec3,
            targetLoadedAreaMax: IVec3,
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
            vol.setLoadedArea(targetLoadedAreaMin, targetLoadedAreaMax)
            vol.blockPalette.link(vol)
            vol.blockPalette.getOrAddBlock(BlockState.fromStr(defaultBlockStr))

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

        return this.blockPalette.getOrAddBlock(blockState)
    }

    /**
     * Returns a Volume Block State (VolBlockState) that's essentially a block palette entry, or
     * creates it if it doesn't exist yet.
     */
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
     *
     * TODO: using a str to VolBlockState cache would be better
     */
    fun setBlockStateStr(pos: IVec3, blockStateString: String) {
        setBlockState(pos, strToBsCached(blockStateString))
    }

    fun setVolBlockState(pos: IVec3, volBlockState: VolBlockState) {
        require(loadedBound.posInside(pos)) { "Pos is outside the loaded area" }
        require(volBlockState.parentVolUuid == this.uuid) { "Trying to place an unregistered block." }

        val chunkIdx = posToChunkIdx(pos)
        val chunk = chunks[chunkIdx]
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
     * If null is inputted, the tile data that may be present is deleted.
     */
    fun setTileData(pos: IVec3, tileData: CompoundTag?) {
        require(loadedBound.posInside(pos)) { "Pos is outside the loaded area" }

        val chunkIdx = posToChunkIdx(pos)
        var chunk = chunks[chunkIdx]
        if (chunk == null) {
            // New chunk
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
        require(loadedBound.posInside(pos)) { "Pos is outside the loaded area" }

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
     * Returns the Volume BlockState at the inputted position. Returns
     * the volume's default VolBlockState if the position is outside of the loaded area.
     */
    fun getVolBlockStateSafe(pos: IVec3): VolBlockState {
        if (!loadedBound.posInside(pos)) return getDefaultBlock()
        return getVolBlockState(pos)
    }

    fun getBlockStateSafe(pos: IVec3) = getVolBlockStateSafe(pos).state


    /**
     * The tile data returned is mutable
     */
    fun getTileData(pos: IVec3): CompoundTag? {
        require(loadedBound.posInside(pos)) { "Pos is outside the loaded area" }

        val chunkIdx = chunkGridBound.posToYzxIdx(posToChunkPos(pos))
        val chunk = chunks[chunkIdx]
        if (chunk == null) {
            return null
        } else {
            val localBlockCoords = posToChunkLocalCoords(pos)
            return chunk.getTileData(localBlockCoords)
        }
    }

    /**
     * Returns null if we try getting a block that's outside the loaded blocks.
     */
    fun getTileDataSafe(pos: IVec3): CompoundTag? {
        if (!loadedBound.posInside(pos)) return null
        return getTileData(pos)
    }

    /**
     * Shifts the volume around by XYZ amount of chunks.
     * This method shifts the chunks in O(1) time.
     */
    fun shiftChunks(shift: IVec3) {
        this.chunkGridBound = this.chunkGridBound.shift(shift)

        val blockShift = shift * this.CHUNK_SIDE_LEN
        this.loadedBound = this.loadedBound.shift(blockShift)
        this.targetBounds = this.targetBounds.shift(blockShift)
    }

    fun expandLoadedArea(expansion: IVec3) {
        this.setLoadedArea(
            this.targetBounds.a - expansion,
            this.targetBounds.b + expansion
        )
    }

    fun setLoadedArea(areaMin: IVec3, areaMax: IVec3) {

        val newBoundary = IntBoundary.new(areaMin, areaMax)
        val newChunkGridBound = IntBoundary.new(
            posToChunkPos(newBoundary.a),
            posToChunkPos(newBoundary.b + (CHUNK_SIDE_LEN - 1))
        )
        val newChunkCount = newChunkGridBound.dims.eProd()
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
        this.targetBounds = newWantedBound
    }

    fun computeBuildBounds(): IntBoundary {
        val buildChunkBounds = this.getBuildChunkBounds() ?:
        // All chunks empty, so we return the block at 0, 0, 0
        return IntBoundary.new(IVec3.splat(0), IVec3.splat(1))

        var minPos = IVec3.MAX
        var maxPos = IVec3.MIN
        for (chunkPos in buildChunkBounds.iterYzx()) {
            if (!(chunkPos onBorderOf buildChunkBounds)) continue

            // If chunk doesn't exist, don't iterate over it
            val chunkIdx = chunkGridBound.posToYzxIdx(chunkPos)
            val chunk = chunks[chunkIdx] ?: continue

            // Compute the min and max block positions
            val onMinBorder = buildChunkBounds.posOnMinBorder(chunkPos)
            val onMaxBorder = buildChunkBounds.posOnMaxBorder(chunkPos)
            val chunkWorldPos = chunkPos shl CHUNK_BIT_SIZE
            if (onMinBorder) {
                val minLocalPos = chunk.computeMinLocalPos()
                if (minLocalPos != null) {
                    val worldPos = chunkWorldPos + minLocalPos
                    minPos = minPos.min(worldPos)
                }
            }
            if (onMaxBorder) {
                val maxLocalPos = chunk.computeMaxLocalPos()
                if (maxLocalPos != null) {
                    val worldPos = chunkWorldPos + maxLocalPos
                    maxPos = maxPos.max(worldPos)
                }
            }
        }

        return IntBoundary.new(minPos, maxPos + 1)
    }

    /**
     * Extracts a block grid out of this McVolume. Useful for outside uses.
     * The outputted block state id mappings are a direct map to this volume's internal
     * palette. So some mapping entries may be unnecessary.
     *
     * @return A triple (IVec3 (Dimensions), BlockStateIdArray, BlockPaletteMappings)
     */
    fun extractBlockGrid(
        blockGridBounds: IntBoundary,
        targetThreadCount: Int = 1,
    ): Triple<IVec3, BlockStateIdArray, BlockPaletteMappings> {
        require(blockGridBounds.fullyInside(loadedBound)) { "Inputted bounds are outside the volume's loaded bounds." }
        require(targetThreadCount >= 1) { "Target thread count must be greater or equal to 1." }

        val bgChunkBounds = posBoundsToChunkBounds(blockGridBounds)

        // # Init array
        val arrLen = blockGridBounds.dims.toLVec3().eProd()
        // Constant blindly copied from: https://stackoverflow.com/questions/3038392 lol
        // Also see https://github.com/openjdk/jdk14u/blob/84917a040a81af2863fddc6eace3dda3e31bf4b5/src/java.base/share/classes/jdk/internal/util/ArraysSupport.java#L583
        val MAX_SAFE_ARR_LEN = (Int.MAX_VALUE - 8)
        if (arrLen > MAX_SAFE_ARR_LEN.toLong()) {
            error("Block grid bounds are too big to fit into a single array. Block count (array length): $arrLen. " +
                    "Max allowed array length: $MAX_SAFE_ARR_LEN.")
        }

        //val initArrayStart = TimeSource.Monotonic.markNow()
        val defaultBlockId = getDefaultBlock().paletteId
        val blockArr = BlockStateIdArray(arrLen.toInt()) { defaultBlockId }
        //println("init block arr in ${initArrayStart.elapsedNow()}")

        // # Function that fills the array from a chunk
        fun fillArrFromChunk(chunk: Chunk, chunkBounds: IntBoundary) {
            val clampedChunkBounds = chunkBounds.getClampedInside(blockGridBounds)
            val ccbRangeY = clampedChunkBounds.rangeY()
            val ccbRangeZ = clampedChunkBounds.rangeZ()

            val xRowLen = clampedChunkBounds.dims.x

            for (worldY in ccbRangeY) {
                for (worldZ in ccbRangeZ) {
                    val rowStartWorldPos = IVec3(clampedChunkBounds.a.x, worldY, worldZ)
                    val chunkRowStartIdx = chunk.localToBlockIdx(posToChunkLocalCoords(rowStartWorldPos))
                    val blockArrRowStartIdx = blockGridBounds.posToYzxIdx(rowStartWorldPos)

                    System.arraycopy(chunk.blocks, chunkRowStartIdx, blockArr, blockArrRowStartIdx, xRowLen)
                }
            }
        }

        // # Function that fills the array from chunks in the inputted chunk bound
        fun fillArrFromChunksInChunkBounds(chunksBounds: IntBoundary) {
            for (chunkPos in chunksBounds.iterYzx()) {
                val chunk = chunks[chunkPosToChunkIdx(chunkPos)] ?: continue
                val chunkBounds = IntBoundary.new(
                    chunkPosToPos(chunkPos),
                    chunkPosToPos(chunkPos) + CHUNK_SIDE_LEN,
                )
                fillArrFromChunk(chunk, chunkBounds)
            }
        }


        // # Block array filling
        if (targetThreadCount == 1) {
            // Single threaded, avoids the cost of thread spawning
            fillArrFromChunksInChunkBounds(bgChunkBounds)
        } else {
            // Multithreaded

            // # Build per-thread jobs
            val buildChunksGridDim = bgChunkBounds.dims
            val longestAxisIdx = buildChunksGridDim.indexOfMax()
            val chunkSlicesJobs = McVolumeUtils.distributeRange(
                bgChunkBounds.a[longestAxisIdx],
                bgChunkBounds.b[longestAxisIdx],
                targetThreadCount
            )
            val chunkBoundsJobs = chunkSlicesJobs.map {
                IntBoundary.new(
                    bgChunkBounds.a.withElement(longestAxisIdx, it.first),
                    bgChunkBounds.b.withElement(longestAxisIdx, it.second),
                )
            }

            val threads = chunkBoundsJobs.map { chunkBoundsJob ->
                thread (start = false) {
                    fillArrFromChunksInChunkBounds(chunkBoundsJob)
                }
            }
            threads.forEach { thread -> thread.start() }
            threads.forEach { thread -> thread.join() }
        }

        return Triple(blockGridBounds.dims, blockArr, blockPalette.toUnlinkedBlockStateMappings())
    }


    fun placeBlockGrid(
        pos: IVec3,
        blockGrid: BlockStateIdArray,
        blockGridDims: IVec3,
        blockIdMappings: BlockPaletteMappings,
        targetThreadCount: Int = 1,
    ) {
        // BlockGridId to VolumeIdMap
        val bgMapToVolMap = BlockStateIdArray(blockIdMappings.keys.max().toInt() + 1) {
            val bgBlockState = blockIdMappings[it.toShort()]
                ?: getDefaultBlock().state // If we find an unmapped value, default to this vol's default
            // Get the vol block state associated to this blockstate, also ensures
            // the palette id exists
            val volBlockState = getEnsuredPaletteBlock(bgBlockState)
            val volBlockStatePaletteId = volBlockState.paletteId
            volBlockStatePaletteId
        }

        // Grid bounds
        val blockGridBounds = IntBoundary.new(IVec3(0), blockGridDims).shift(pos)
        val bgChunkBounds = posBoundsToChunkBounds(blockGridBounds)

        // Whether the default block is present inside of the main volume
        // (Optimisation purposes)
        var bgHasDefaultVolBlock = true
        val volDefaultBlockIdInBlockGrid = bgMapToVolMap.firstOrNull { volId ->
            volId == getDefaultBlock().paletteId
        } ?: run { bgHasDefaultVolBlock = false; -1 }


        fun fillChunkFromArr(chunkPos: IVec3, chunkBounds: IntBoundary) {
            var chunk = chunks[chunkPosToChunkIdx(chunkPos)]

            val clampedChunkBounds = chunkBounds.getClampedInside(blockGridBounds)
            val ccbRangeY = clampedChunkBounds.rangeY()
            val ccbRangeZ = clampedChunkBounds.rangeZ()

            val xRowLen = clampedChunkBounds.dims.x

            for (worldY in ccbRangeY) {
                for (worldZ in ccbRangeZ) {
                    val rowStartWorldPos = IVec3(clampedChunkBounds.a.x, worldY, worldZ)
                    val blockArrRowStartIdx = blockGridBounds.posToYzxIdx(rowStartWorldPos)

                    // Create new chunk if needed
                    if (chunk == null) {
                        // If it doesn't exist check every block until you find one that isn't the
                        // default volume block, otherwise you're just going to be allocated a useless
                        // chunk that can be represented by a null pointer instead

                        val doCreateChunk = run {
                            // If the grid doesn't contain the default vol block,
                            // then the chunk will get created no matter what
                            if (!bgHasDefaultVolBlock) return@run true

                            // If any of the blocks of the row is
                            for (i in blockArrRowStartIdx..<(blockArrRowStartIdx+xRowLen)) {
                                if (blockGrid[i] != volDefaultBlockIdInBlockGrid) return@run true
                            }

                            return@run false
                        }

                        if (doCreateChunk) {
                            chunk = Chunk.new(CHUNK_BIT_SIZE)
                            val chunkIdx = chunkPosToChunkIdx(chunkPos)
                            chunks[chunkIdx] = chunk
                        }
                    }

                    // Chunk exists / was created so we place without worry
                    if (chunk != null) {
                        val chunkRowStartIdx = chunk.localToBlockIdx(posToChunkLocalCoords(rowStartWorldPos))

                        for (i in 0..<xRowLen) {
                            val bgId = blockGrid[blockArrRowStartIdx + i]
                            val volIdToPlace = bgMapToVolMap[bgId.toInt()]
                            chunk.blocks[chunkRowStartIdx + i] = volIdToPlace
                        }
                    }
                }
            }
        }

        fun fillChunksInChunksBounds(chunksBounds: IntBoundary) {
            for (chunkPos in chunksBounds.iterYzx()) {
                val chunkBounds = IntBoundary.new(
                    chunkPosToPos(chunkPos),
                    chunkPosToPos(chunkPos) + CHUNK_SIDE_LEN,
                )
                fillChunkFromArr(chunkPos, chunkBounds)
            }
        }


        // # Block array placing
        if (targetThreadCount == 1) {
            // Single threaded, avoids the cost of thread spawning
            fillChunksInChunksBounds(bgChunkBounds)
        } else {
            // Multithreaded

            // # Build per-thread jobs
            val buildChunksGridDim = bgChunkBounds.dims
            val longestAxisIdx = buildChunksGridDim.indexOfMax()
            val chunkSlicesJobs = McVolumeUtils.distributeRange(
                bgChunkBounds.a[longestAxisIdx],
                bgChunkBounds.b[longestAxisIdx],
                targetThreadCount
            )
            val chunkBoundsJobs = chunkSlicesJobs.map {
                IntBoundary.new(
                    bgChunkBounds.a.withElement(longestAxisIdx, it.first),
                    bgChunkBounds.b.withElement(longestAxisIdx, it.second),
                )
            }

            val threads = chunkBoundsJobs.map { chunkBoundsJob ->
                thread (start = false) {
                    fillChunksInChunksBounds(chunkBoundsJob)
                }
            }
            threads.forEach { thread -> thread.start() }
            threads.forEach { thread -> thread.join() }
        }
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

    internal fun getBuildChunkBounds(): IntBoundary? {
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
            return null
        }

        return IntBoundary.new(minChunkPos, maxChunkPos + 1)
    }


    internal fun chunkPosToChunkIdx(chunkPos: IVec3): Int {
        return chunkGridBound.posToYzxIdx(chunkPos)
    }

    internal fun posToChunkIdx(pos: IVec3): Int {
        return chunkPosToChunkIdx(posToChunkPos(pos))
    }

    private fun strToBsCached(bsStr: String): BlockState {
        return blockStateCache.getOrPut(bsStr) { BlockState.fromStr(bsStr) }
    }

    internal fun posToChunkPos(pos: IVec3): IVec3 {
        return pos shr CHUNK_BIT_SIZE
    }

    internal fun chunkPosToPos(chunkPos: IVec3): IVec3 {
        return chunkPos shl CHUNK_BIT_SIZE
    }

    private fun posToChunkLocalCoords(pos: IVec3): IVec3 {
        return pos and CHUNK_SIZE_BIT_MASK
    }

    private fun posBoundsToChunkBounds(posBounds: IntBoundary): IntBoundary {
        return IntBoundary.new(posToChunkPos(posBounds.a), posToChunkPos(posBounds.b - 1) + 1)
    }

}