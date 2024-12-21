package com.xmlcalabash.debugger

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.steps.AbstractStep

class NopDebugger: Debugger {
    override fun startStep(step: AbstractStep) {
        // nop
    }
    override fun endStep(step: AbstractStep) {
        // nop
    }

    override fun sendDocument(from: Pair<String, String>, to: Pair<String, String>, document: XProcDocument): XProcDocument {
        return document
    }
}