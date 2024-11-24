package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP

open class WithInputInstruction(parent: XProcInstruction, stepConfig: StepConfiguration): InputBindingInstruction(parent, stepConfig, NsP.withInput) {
    constructor(parent: XProcInstruction, stepConfig: StepConfiguration, port: String) : this(parent, stepConfig) {
        this.port = port
    }

    override fun elaborateInstructions() {
        // We haven't elaborated this input yet, so consider the possibility that
        // elaboration might add a connection
        val connected = children.isNotEmpty() || href != null || pipe != null

        if (!connected && primary != false) {
            if (stepConfig.drp != null) {
                pipe()
            } else {
                if (defaultBindings.isEmpty()) {
                    throw XProcError.xsNoConnection(port).exception()
                }
            }
        }

        super.elaborateInstructions()
    }
}