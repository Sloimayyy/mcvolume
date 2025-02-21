package com.sloimay.mcvolume.block

import me.sloimay.me.sloimay.mcvolume.block.MutBlockState
import java.util.*



internal data class PropEntry(var k: String, var v: String)



open class BlockState internal constructor(val resLoc: String,
                                           val name: String,
                                           internal val props: MutableList<PropEntry>) {

    val fullName = "$resLoc:$name"
    val hasProps = props.isNotEmpty()
    val stateStr = computeStateStr()

    companion object {

        fun new(resLoc: String, name: String, props: List<Pair<String, String>>): BlockState {
            // List copy so it only becomes a view
            return BlockState(
                resLoc,
                name,
                props.map { PropEntry(it.first, it.second) }.toMutableList()
            )
        }

        fun fromStr(str: String): BlockState {

            // # Full name
            // Get if states, if so trim
            val firstBrackIdx = str.indexOfFirst { c -> c == '[' }
            val hasProps = firstBrackIdx != -1
            val fullName = if (!hasProps) {
                str
            } else {
                str.slice(0 until firstBrackIdx)
            }

            // Separate name from resource location
            val fullNameColonIdx = fullName.indexOfFirst { c -> c == ':' }
            val (resLoc, name) = if (fullNameColonIdx == -1) {
                "minecraft" to fullName
            } else {
                (fullName.slice(0 until fullNameColonIdx)
                    to
                        fullName.slice((fullNameColonIdx+1) until fullName.length))
            }

            // # Don't need to go further if there's no props
            if (!hasProps) {
                return new(resLoc, name, listOf())
            }

            // # Properties
            val propsStartIdx = firstBrackIdx + 1
            val propsEndIdx = str.length - 1
            val propsStr = str.slice(propsStartIdx until propsEndIdx)
            val props = mutableListOf<Pair<String, String>>()
            // Split props
            propsStr.split(',').forEach { propStr ->
                val propStrStripped = propStr.trim()
                val propNameAndVal = propStrStripped.split('=')
                if (propNameAndVal.size != 2) throw Exception("Prop string too long, smth went wrong")
                val propName = propNameAndVal[0].trim()
                val propVal = propNameAndVal[1].trim()
                props.add(propName to propVal)
            }

            return new(resLoc, name, props)
        }

    }

    fun getProp(propName: String): Optional<String> {
        for ((prop, v) in props)
            if (prop == propName) return Optional.of(v)
        return Optional.empty()
    }

    /**
     * A blockstate loosely matches another if they have the same fullName +
     * every state that are common between the two are the same
     */
    fun looselyMatches(other: BlockState): Boolean {
        if (this.fullName != other.fullName) return false
        for ((pn, pv) in this.props) {
            val otherPvOpt = other.getProp(pn)
            if (otherPvOpt.isEmpty) continue // Other prop is not defined
            // Got two props in common, check if they're the same. If not we know
            // they don't loosely match
            val otherPv = otherPvOpt.get()
            if (pv != otherPv) return false
        }
        return true
    }

    fun toMutable(): MutBlockState {
        return MutBlockState(resLoc, name, props.toMutableList())
    }

    private fun computeStateStr(): String {
        val s = StringBuilder(this.fullName)
        if (this.hasProps) {
            s.append('[')
            s.append(this.props.joinToString (separator = ",") { (prop, v) ->
                "${prop}=${v}"
            })
            s.append(']')
        }
        return s.toString()
    }

    override fun toString() = stateStr
}

