package com.xmlcalabash.debugger

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.Monitor
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.Consumer

class NopDebugger: Monitor {
    override fun startStep(step: AbstractStep) {
        // nop
    }
    override fun endStep(step: AbstractStep) {
        // nop
    }

    override fun abortStep(step: AbstractStep, ex: Exception) {
        // nop
    }

    override fun sendDocument(from: Pair<AbstractStep, String>, to: Pair<Consumer, String>, document: XProcDocument): XProcDocument {
        return document
    }
}