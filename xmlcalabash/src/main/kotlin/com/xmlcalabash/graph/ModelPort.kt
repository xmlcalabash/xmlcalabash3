package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.InputInstruction
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.datamodel.PortBindingContainer
import com.xmlcalabash.datamodel.WithInputInstruction

open class ModelPort(val parent: Model, val name: String, val unbound: Boolean, val primary: Boolean, val sequence: Boolean, val contentTypes: List<MediaType>) {
    internal var weldedShut = false

    constructor(parent: Model, portBinding: PortBindingContainer): this(parent, portBinding.port,
        ((portBinding is InputInstruction || portBinding is WithInputInstruction)
                && !portBinding.weldedShut  && portBinding.children.isEmpty()),
        portBinding.primary == true, portBinding.sequence == true, portBinding.contentTypes.toList()) {
        weldedShut = portBinding.weldedShut
    }
    constructor(copy: ModelPort): this(copy.parent, copy.name, copy.unbound, copy.primary, copy.sequence, copy.contentTypes.toList()) {
        weldedShut = copy.weldedShut
    }

    companion object {
        private var idCounter = 0L
    }
    val id = ++idCounter

    override fun toString(): String {
        return "${parent}.${name}"
    }
}