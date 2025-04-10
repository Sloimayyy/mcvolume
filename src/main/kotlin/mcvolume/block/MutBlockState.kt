package com.sloimay.mcvolume.block


class MutBlockState internal constructor
    (resLoc: String, name: String, props: MutableList<PropEntry>)
    : BlockState(resLoc, name, props)
{

    private var stateStrDirty = true
    private var stateStrPriv: String = ""
    override val stateStr: String
        get() = run {
            if (stateStrDirty) {
                stateStrPriv = computeStateStr()
                stateStrDirty = false
            }
            stateStrPriv
        }

    fun setProp(propName: String, propVal: String) {
        for (prop in props) {
            if (prop.name == propName) {
                prop.value = propVal
                return
            }
        }
        // Didn't find a prop of the name, so create it
        this.stateStrDirty = true
        this.props.add(PropEntry(propName, propVal))
    }

    fun toImmutable(): BlockState {
        return BlockState(resLoc, name, props.toMutableList())
    }
}