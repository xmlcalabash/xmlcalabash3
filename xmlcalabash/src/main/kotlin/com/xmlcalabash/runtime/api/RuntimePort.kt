package com.xmlcalabash.runtime.api

import com.xmlcalabash.datamodel.ConnectionInstruction
import com.xmlcalabash.datamodel.MediaType

open class RuntimePort(val name: String, val unbound: Boolean, val sequence: Boolean, val contentTypes: List<MediaType>) {
    val defaultBindings = mutableListOf<ConnectionInstruction>()
    internal var weldedShut = false

    override fun toString(): String {
        return name
    }
}

