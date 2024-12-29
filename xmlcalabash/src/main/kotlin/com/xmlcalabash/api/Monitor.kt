package com.xmlcalabash.api

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.Consumer

interface Monitor {
    fun startStep(step: AbstractStep)
    fun endStep(step: AbstractStep)
    fun abortStep(step: AbstractStep, ex: Exception)
    fun sendDocument(from: Pair<AbstractStep,String>, to: Pair<Consumer,String>, document: XProcDocument): XProcDocument
}