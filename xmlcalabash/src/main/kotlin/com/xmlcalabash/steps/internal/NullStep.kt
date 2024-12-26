package com.xmlcalabash.steps.internal

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import net.sf.saxon.s9api.QName

class NullStep(): XProcStep {
    override fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters) {
        // nop
    }

    override fun extensionAttributes(attributes: Map<QName, String>) {
        // nop
    }

    override fun teardown() {
        // nop
    }

    override fun reset() {
        // nop
    }

    override fun abort() {
        // nop
    }

    override fun input(port: String, doc: XProcDocument) {
        throw RuntimeException("Configuration error: input called on null step")
    }

    override fun option(name: QName, binding: LazyValue) {
        throw RuntimeException("Configuration error: option called on null step")
    }

    override fun run() {
        throw RuntimeException("Configuration error: run called on null step")
    }
}