package com.sloimay.mcvolume.block


// Note: don't mutate those values in the BlockState class, only in the MutBlockState one.
internal data class PropEntry(var name: String, var value: String)


open class BlockState internal constructor(val resLoc: String,
                                           val name: String,
                                           internal val props: MutableList<PropEntry>) {

    /**
     * The main reason why we're using strings is so that it can
     * represent modded blocks too. If anyone contributing has a better
     * idea than strings, or ideas on how to make this class much faster
     * and efficient please do send a pull request or let me know lol
     */

    val fullName = "$resLoc:$name"
    val hasProps = props.isNotEmpty()
    open val stateStr = computeStateStr()

    companion object {

        fun new(resLoc: String, name: String, props: List<Pair<String, String>>): BlockState {
            // List copy so it only becomes a view. Check for duplicates
            val propsSeen = mutableListOf<String>()
            val propList = props.map {
                if (it.first in propsSeen) {
                    throw IllegalArgumentException(
                        "Block state properties are unique, but found multiple instances of '${it.first}'."
                    )
                } else {
                    propsSeen.add(it.first)
                }
                PropEntry(it.first, it.second)
            }
                // Sorting ensures BlockStates are equal even if their properties are
                // not in the same order.
                // Sorted a second time in computeStateStr() in case I want to define
                // equality another way in the future. The impact on performance is
                // minimal for reasonable block states with at most a few properties.
                .sortedBy { it.name }
                .toMutableList()

            return BlockState(
                resLoc,
                name,
                propList,
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

    fun getProp(propName: String): String? {
        //props.firstOrNull { it.k == propName }?.v
        for ((prop, v) in props) if (prop == propName) return v
        return null
    }
    fun getPropDefault(propName: String, default: String): String = getProp(propName) ?: default


    /**
     * A blockstate loosely matches another if they have the same fullName +
     * every state that are common between the two are the same
     */
    fun looselyMatches(other: BlockState): Boolean {
        if (this.fullName != other.fullName) return false
        for ((pn, pv) in this.props) {
            val otherPv = other.getProp(pn) ?: continue// Other prop is not defined
            // Got two props in common, check if they're the same. If not we know
            // they don't loosely match
            if (pv != otherPv) return false
        }
        return true
    }

    fun toMutable(): MutBlockState {
        return MutBlockState(resLoc, name, props.toMutableList())
    }

    protected fun computeStateStr(): String {
        val s = StringBuilder(this.fullName)
        if (this.hasProps) {
            s.append('[')
            // Sorting ensures this stateStr is equal to another equal BlockState
            // regardless of the order of the properties
            s.append(this.props.sortedBy { it.name }.joinToString (separator = ",") { (prop, v) ->
                "${prop}=${v}"
            })
            s.append(']')
        }
        return s.toString()
    }

    override fun toString() = stateStr

    override fun hashCode() = stateStr.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockState

        return stateStr == other.stateStr
    }

}

