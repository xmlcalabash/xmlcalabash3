package com.xmlcalabash.runtime.api

import com.xmlcalabash.datamodel.ConnectionInstruction
import com.xmlcalabash.datamodel.MediaType
import net.sf.saxon.s9api.XdmNode

open class RuntimePort(val name: String, val unbound: Boolean, val sequence: Boolean, val contentTypes: List<MediaType>) {
    val schematron = mutableListOf<XdmNode>()
    val defaultBindings = mutableListOf<ConnectionInstruction>()
    internal var weldedShut = false

    override fun toString(): String {
        return name
    }
}

