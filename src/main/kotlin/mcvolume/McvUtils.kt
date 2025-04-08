package com.sloimay.mcvolume

import com.sloimay.smath.vectors.IVec3
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max

internal const val INT_BIT_MASK = 0b0111_1111
internal const val CONTINUE_BIT_MASK = 0b1000_0000

internal class McvUtils {

    companion object {

        fun writeIntToByteListLE(buf: MutableList<Byte>, int: Int) {
            for (i in 0 until 4) buf.add( (int shr (i*8)).toByte() )
        }

        fun writeIntToByteListBE(buf: MutableList<Byte>, int: Int) {
            //println("Int: ${int}")
            for (i in 3 downTo 0)  {
                //println("Byte: ${(int shr (i*8)).toByte()}")
                buf.add( (int shr (i*8)).toByte() )
            }
        }

        fun intToBEBytes(i: Int): ByteArray {
            var bytes = mutableListOf<Byte>()
            writeIntToByteListBE(bytes, i)
            return bytes.toByteArray()
        }

        fun writeLongToByteListLE(buf: MutableList<Byte>, long: Long) {
            for (i in 0 until 8) buf.add( (long shr (i*8)).toByte() )
        }

        fun writeIVec3ToByteListLE(buf: MutableList<Byte>, v: IVec3) {
            writeIntToByteListLE(buf, v.x)
            writeIntToByteListLE(buf, v.y)
            writeIntToByteListLE(buf, v.z)
        }

        fun writeLongBufToByteListLE(byteBuf: MutableList<Byte>, longBuf: MutableList<Long>) {
            for (long in longBuf) {
                writeLongToByteListLE(byteBuf, long)
            }
        }

        fun writeByteArrToByteList(byteBuf: MutableList<Byte>, byteArray: ByteArray) {
            for (byte in byteArray) {
                byteBuf.add(byte)
            }
        }

        /**
         * HF = Higher first, the longs are getting packed with data (anchor is still at the LSB though)
         */
        fun makePackedLongArrHF(ints: MutableList<Int>, bitCount: Int): MutableList<Long> {
            val bitMask = (1 shl bitCount) - 1
            var longs = mutableListOf<Long>()

            var l = 0L
            var bitsTaken = 0

            val finishLong = {
                longs.add(l)
                l = 0L
                bitsTaken = 0
            }

            for (i in ints) {
                var v = i and bitMask
                l = l or v.toLong()
                bitsTaken += bitCount
                if ((bitsTaken + bitCount) > 64 ) {
                    // If the next iteration will go over the number of bits in the long, reset
                    finishLong()
                } else {
                    l = l shl bitCount
                }
            }
            // If a long was still getting filled when we were done,
            if (bitsTaken > 0) {
                finishLong()
            }

            return longs
        }

        /**
         * LF = Lower first. The longs are getting packed from the LSB to the MSB
         */
        fun makePackedLongArrLF(data: ShortArray, bitLength: Int): LongArray {
            if (data.isEmpty()) return LongArray(0)
            require(bitLength > 0) { "Bit length must be greater than 0 but got ${bitLength}." }

            var longs = mutableListOf<Long>()
            val bitMask = ((1 shl bitLength) - 1)
            val valuesPerLong = 64 / bitLength
            var currentLong = 0L
            var idxInLong = 0
            for (d in data) {
                currentLong = currentLong or (((d.toInt() and bitMask).toLong()) shl (idxInLong * bitLength))
                idxInLong += 1
                if (idxInLong >= valuesPerLong) {
                    idxInLong = 0
                    longs.add(currentLong)
                    currentLong = 0
                }
            }
            if (idxInLong != 0) {
                longs.add(currentLong)
            }
            return longs.toLongArray()
        }


        fun unpackLongArrLFIntoShortArray(
            longs: LongArray,
            bitLength: Int,
            valueCount: Int
        ): ShortArray {
            if (longs.isEmpty() || valueCount == 0) return ShortArray(0)

            val outArr = ShortArray(valueCount)

            val bitMask = ((1 shl bitLength) - 1).toLong()
            var ptrInLong = 0
            var currLongIdx = 0
            var currLong = longs[currLongIdx]

            for (valIdx in 0 until valueCount) {
                val v = ( (currLong ushr ptrInLong) and bitMask ).toInt()
                outArr[valIdx] = v.toShort()

                ptrInLong += bitLength
                // Ptr in long is now out of bounds
                if (ptrInLong > (64 - bitLength)) {
                    currLongIdx += 1
                    if (currLongIdx < longs.size) currLong = longs[currLongIdx]
                    ptrInLong = 0
                }
            }

            return outArr
        }



        fun byteVecToByteArray(byteVec: MutableList<Byte>): ByteArray {
            var byteArr = ByteArray(byteVec.size)
            for (i in 0 until byteVec.size) byteArr[i] = byteVec[i]
            return byteArr
        }


        fun getBitCount(int: Int): Int {
            if (int == 0) { return 0 }
            return ceil( log2( ((abs(int))+1).toDouble() ) ).toInt()
        }

        fun distributeRange(rStart: Int, rEnd: Int, threadCount: Int): List<Pair<Int, Int>> {
            val totalElements = rEnd - rStart
            val baseChunkSize = totalElements / threadCount
            val remainingElements = totalElements % threadCount

            var out: MutableList<Pair<Int, Int>> = mutableListOf();
            var currentStart = rStart
            for (threadIdx in 0 until threadCount) {
                val extra = if (threadIdx < remainingElements) 1 else 0
                val currChunkSize = baseChunkSize + extra
                if (currChunkSize != 0) {
                    out.add(Pair(currentStart, currentStart + currChunkSize))
                }
                currentStart += currChunkSize
            }

            return out.toList()
        }



        fun readVarint(buf: ByteArray, cursorIdx: Int): Pair<Int, Int> {
            var value = 0
            var varintLength = 0
            var idx = cursorIdx
            while (true) {
                val data = buf[idx]
                value = value or ((data.toInt() and INT_BIT_MASK) shl (varintLength * 7))
                varintLength += 1
                if (varintLength > 5) {
                    throw Exception("Varint too long")
                }
                if ((data.toInt() and CONTINUE_BIT_MASK) != 128) {
                    idx += 1
                    break
                }
                idx += 1
            }
            return value to idx
        }

        fun pushVarint(buf: MutableList<Byte>, i: Int) {
            var int = i
            while ((int and 0xFF_FF_FF_80.toInt()) != 0) {
                buf.add(((int and INT_BIT_MASK) or CONTINUE_BIT_MASK).toByte())
                int = int ushr 7
            }
            buf.add(int.toByte())
        }

        // TODO: Written by Claude so idk if it's that good lol
        fun gzipCompress(data: ByteArray): ByteArray {
            ByteArrayOutputStream().use { byteArrayOutputStream ->
                GZIPOutputStream(byteArrayOutputStream).use { gzipOutputStream ->
                    gzipOutputStream.write(data)
                }
                return byteArrayOutputStream.toByteArray()
            }
        }

        // TODO: Written by Claude so idk if it's that good lol
        fun gzipDecompress(compressedData: ByteArray): ByteArray {
            ByteArrayInputStream(compressedData).use { byteArrayInputStream ->
                GZIPInputStream(byteArrayInputStream).use { gzipInputStream ->
                    ByteArrayOutputStream().use { byteArrayOutputStream ->
                        val buffer = ByteArray(1024)
                        var len: Int
                        while (gzipInputStream.read(buffer).also { len = it } > 0) {
                            byteArrayOutputStream.write(buffer, 0, len)
                        }
                        return byteArrayOutputStream.toByteArray()
                    }
                }
            }
        }

    }

}





class GrowableByteBuf(initialCapacity: Int = 16, private val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN) {

    private var buffer: ByteBuffer
    private var size: Int = 0

    init {
        require(initialCapacity > 0) { "Initial capacity must be greater than 0 but got ${initialCapacity}." }
        this.buffer = ByteBuffer.allocate(initialCapacity)
        this.buffer.order(byteOrder)
    }

    private fun writeAndEnsureCap(neededBytes: Int) {
        val currPos = buffer.position()
        val endPos = currPos + neededBytes
        size = max(size, endPos)
        while (size > buffer.limit()) {
            val pos = buffer.position()
            val oldCap = size
            val newCapAttempt = buffer.capacity() * 2
            // No idea what the max VM amount is but 2_000_000_000 is decently close to
            // 2^31 - 1 without the VM yelling at me XD
            val newCap = if (newCapAttempt > 0) newCapAttempt else max(2_000_000_000, oldCap)
            val newBuffer = ByteBuffer.allocate(newCap)
            newBuffer.order(byteOrder)
            buffer.flip()
            newBuffer.put(buffer)
            buffer = newBuffer
            buffer.position(pos)
        }
    }



    fun seek(index: Int) {
        buffer.position(index)
    }

    fun pos() = buffer.position()

    fun size() = size

    fun toByteArray(): ByteArray {
        val bufArr = buffer.array()
        val out = ByteArray(this.size) { bufArr[it] }
        return out
    }

    fun getByte() = buffer.get()
    fun getShort() = buffer.getShort()
    fun getInt() = buffer.getInt()
    fun getLong() = buffer.getLong()
    fun getFloat() = buffer.getFloat()
    fun getDouble() = buffer.getDouble()
    fun getBytes(amount: Int) =  ByteArray(amount) { this.getByte() }

    fun putByte(b: Byte) { writeAndEnsureCap(Byte.SIZE_BYTES); buffer.put(b) }
    fun putShort(s: Short) { writeAndEnsureCap(Short.SIZE_BYTES); buffer.putShort(s)}
    fun putInt(i: Int) { writeAndEnsureCap(Int.SIZE_BYTES); buffer.putInt(i) }
    fun putFloat(f: Float) { writeAndEnsureCap(Float.SIZE_BYTES); buffer.putFloat(f) }
    fun putDouble(d: Double) { writeAndEnsureCap(Double.SIZE_BYTES); buffer.putDouble(d) }
    fun putLong(l: Long) { writeAndEnsureCap(Long.SIZE_BYTES); buffer.putLong(l) }
    fun putBytes(b: ByteArray) { writeAndEnsureCap(b.size); buffer.put(b) }
}
