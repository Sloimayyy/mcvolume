package com.sloimay.mcvolume

import com.sloimay.smath.vectors.IVec3
import com.sloimay.smath.vectors.ivec3


/**
 * [a; b)
 * Immutable (?)
 */
data class IntBoundary(
    private var start: IVec3,
    private var end: IVec3,
    private var dims: IVec3,
    )
{

    val a get() = start
    val b get() = end
    val dim get() = dims

    companion object {

        fun new(a: IVec3, b: IVec3): IntBoundary {
            val cornerA = a.min(b)
            val cornerB = a.max(b)
            return IntBoundary(cornerA, cornerB, cornerB - cornerA)
        }

        private fun rangeIntersect(r1Start: Int, r1End: Int, r2Start: Int, r2End: Int): Boolean {
            return r1Start < r2End && r1End > r2Start
        }

        private fun valInRange(v: Int, rStart: Int, rEnd: Int): Boolean {
            return v in rStart until rEnd
        }

    }


    fun posInside(pos: IVec3): Boolean {
        return (valInRange(pos.x, start.x, end.x)
                && valInRange(pos.y, start.y, end.y)
                && valInRange(pos.z, start.z, end.z))
    }

    fun intersects(other: IntBoundary): Boolean {
        return (rangeIntersect(start.x, end.x, other.start.x, other.end.x)
                && rangeIntersect(start.y, end.y, other.start.y, other.end.y)
                && rangeIntersect(start.z, end.z, other.start.z, other.end.z))
    }

    fun posOnBorder(pos: IVec3): Boolean {
        return posOnMinBorder(pos) || posOnMaxBorder(pos)
    }

    fun posOnMinBorder(pos: IVec3): Boolean {
        return (pos.x == a.x || pos.y == a.y || pos.z == a.z)
    }

    fun posOnMaxBorder(pos: IVec3): Boolean {
        return (pos.x == (b.x-1) || pos.y == (b.y-1) || pos.z == (b.z-1))
    }

    fun posToYzxIdx(pos: IVec3): Int {
        val localPos = pos - start
        val idx = (
                localPos.x
                + localPos.z * dims.x
                + localPos.y * dims.x * dims.z)
        return idx
    }

    fun getClampedInside(container: IntBoundary): IntBoundary {
        return new(
            a.clamp(container.a, container.b),
            b.clamp(container.a, container.b),
        )
    }

    fun getClampedInside(p: IVec3): IVec3 {
        return p.clamp(this.a, this.b - 1)
    }

    fun withMidpoint(point: IVec3): IntBoundary {
        val lowCorner = point - dims / 2
        return new(
            lowCorner,
            lowCorner + dims
        )
    }

    fun move(v: IVec3) = new(a + v, b + v)

    fun expand(dims: IVec3) = new(a - dims, b + dims)

    fun eq(other: IntBoundary) = this.a.equality(other.a) && this.b.equality(other.b)


    fun rangeX() = a.x until b.x
    fun rangeY() = a.y until b.y
    fun rangeZ() = a.z until b.z


    fun iterYzx(): Iterator<IVec3> {
        val boundary = this

        return object : Iterator<IVec3> {
            private var x = boundary.start.x
            private var y = boundary.start.y
            private var z = boundary.start.z

            override fun hasNext(): Boolean {
                return y < boundary.end.y
            }

            override fun next(): IVec3 {
                val value = ivec3(x, y, z)

                x += 1
                if (x >= boundary.end.x) {
                    x = boundary.start.x
                    z += 1

                    if (z >= boundary.end.z) {
                        z = boundary.start.z
                        y += 1
                    }
                }
                return value
            }
        }
    }


    override fun toString(): String {
        return "IntBoundary( a=${this.a} b=${this.b} dims=${this.dims} )"
    }
}


internal infix fun IVec3.onBorderOf(boundary: IntBoundary) = boundary.posOnBorder(this)
