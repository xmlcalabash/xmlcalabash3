package com.xmlcalabash.runtime.api

import com.xmlcalabash.datamodel.ConnectionInstruction
import com.xmlcalabash.io.MediaType
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode

open class RuntimePort(val name: String, val unbound: Boolean, val sequence: Boolean, val contentTypes: List<MediaType>, val serialization: XdmMap = XdmMap()) {
    val assertions = mutableListOf<XdmNode>()
    val defaultBindings = mutableListOf<ConnectionInstruction>()
    internal var weldedShut = false

    override fun toString(): String {
        return name
    }
}

