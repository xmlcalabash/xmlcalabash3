package com.xmlcalabash.debugger

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.steps.AbstractStep

interface Debugger {
    fun startStep(step: AbstractStep)
    fun endStep(step: AbstractStep)
    fun sendDocument(from: Pair<String,String>, to: Pair<String,String>, document: XProcDocument): XProcDocument
}