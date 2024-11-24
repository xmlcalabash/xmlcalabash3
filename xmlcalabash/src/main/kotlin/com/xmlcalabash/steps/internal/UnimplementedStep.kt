package com.xmlcalabash.steps.internal

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.parameters.UnimplementedStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep

open class UnimplementedStep(val params: UnimplementedStepParameters): AbstractAtomicStep() {
    override fun input(port: String, doc: XProcDocument) {
        // Just ignore the inputs, if there are any.
    }

    override fun run() {
        super.run()
        throw XProcError.xsMissingStepDeclaration(params.unimplemented).exception()
    }

    override fun toString(): String = params.unimplemented.toString()
}