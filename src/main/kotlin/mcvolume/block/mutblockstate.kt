package me.sloimay.me.sloimay.mcvolume.block

import com.sloimay.mcvolume.block.BlockState
import com.sloimay.mcvolume.block.PropEntry
import java.util.*


class MutBlockState internal constructor
    (resLoc: String, name: String, props: MutableList<PropEntry>)
    : BlockState(resLoc, name, props)
{
    fun setProp(propName: String, propVal: String) {
        for (prop in props) {
            if (prop.k == propName) {
                prop.v = propVal
                return
            }
        }
        // Didn't find a prop of the name, so create it
        this.props.add(PropEntry(propName, propVal))
    }

    fun toImmutable(): BlockState {
        return BlockState(resLoc, name, props.toMutableList())
    }
}