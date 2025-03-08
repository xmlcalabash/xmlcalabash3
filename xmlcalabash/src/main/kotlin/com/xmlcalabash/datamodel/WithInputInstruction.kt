package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.namespace.NsS
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind

open class WithInputInstruction(parent: XProcInstruction, stepConfig: InstructionConfiguration): InputBindingInstruction(parent, stepConfig, NsP.withInput) {
    constructor(parent: XProcInstruction, stepConfig: InstructionConfiguration, port: String) : this(parent, stepConfig) {
        this.port = port
    }

    override fun elaborateInstructions() {
        // We haven't elaborated this input yet, so consider the possibility that
        // elaboration might add a connection
        val connected = children.isNotEmpty() || href != null || pipe != null

        var connectDrp = !connected && primary == true
        if (!connectDrp && !connected && parent != null) {
            connectDrp = parent!!.instructionType in listOf(NsP.forEach, NsP.viewport)
        }

        if (!connected && connectDrp) {
            if (stepConfig.drp != null) {
                pipe()
            } else {
                if (defaultBindings.isEmpty()) {
                    throw stepConfig.exception(XProcError.xsNoConnection(port).at(stepConfig.location))
                }
            }
        }

        super.elaborateInstructions()
    }
}