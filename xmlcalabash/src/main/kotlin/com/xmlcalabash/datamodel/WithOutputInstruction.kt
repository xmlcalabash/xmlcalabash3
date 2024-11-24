package com.xmlcalabash.datamodel

import com.xmlcalabash.namespace.NsP

class WithOutputInstruction(parent: XProcInstruction, stepConfig: StepConfiguration): PortBindingContainer(parent, stepConfig, NsP.withOutput) {
    constructor(parent: XProcInstruction, stepConfig: StepConfiguration, port: String, primary: Boolean?, sequence: Boolean?) : this(parent, stepConfig) {
        this.port = port
        this.primary = primary
        this.sequence = sequence
    }
}