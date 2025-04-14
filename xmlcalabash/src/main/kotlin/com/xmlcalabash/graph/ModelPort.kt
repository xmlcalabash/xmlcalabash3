package com.xmlcalabash.graph

import com.xmlcalabash.datamodel.InputInstruction
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.datamodel.OutputInstruction
import com.xmlcalabash.datamodel.PortBindingContainer
import com.xmlcalabash.datamodel.WithInputInstruction
import com.xmlcalabash.util.AssertionsLevel
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode

open class ModelPort(val parent: Model, val name: String, val unbound: Boolean, val primary: Boolean, val sequence: Boolean, val contentTypes: List<MediaType>) {
    internal var weldedShut = false
    val assertions = mutableListOf<XdmNode>()
    var serialization = XdmMap()

    constructor(parent: Model, portBinding: PortBindingContainer): this(parent, portBinding.port,
        ((portBinding is InputInstruction || portBinding is WithInputInstruction)
                && !portBinding.weldedShut  && portBinding.children.isEmpty()),
        portBinding.primary == true, portBinding.sequence == true, portBinding.contentTypes.toList()) {
        weldedShut = portBinding.weldedShut
        if (portBinding is OutputInstruction) {
            serialization = portBinding.serialization.evaluate(portBinding.stepConfig) as XdmMap
        }
        if (portBinding.stepConfig.environment.assertions != AssertionsLevel.IGNORE) {
            assertions.addAll(portBinding.assertions)
        }
    }
    constructor(copy: ModelPort): this(copy.parent, copy.name, copy.unbound, copy.primary, copy.sequence, copy.contentTypes.toList()) {
        weldedShut = copy.weldedShut
        assertions.addAll(copy.assertions)
        serialization = copy.serialization
    }

    companion object {
        private var idCounter = 0L
    }
    val id = ++idCounter

    override fun toString(): String {
        return "${parent}.${name}"
    }
}