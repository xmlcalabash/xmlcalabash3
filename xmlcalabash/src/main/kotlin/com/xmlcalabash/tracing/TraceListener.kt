package com.xmlcalabash.tracing

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.XdmNode

interface TraceListener {
    val trace: List<TraceDetail>
    fun startExecution(step: AbstractStep)
    fun stopExecution(step: AbstractStep, duration: Long)
    fun sendDocument(from: Pair<String,String>, to: Pair<String,String>, document: XProcDocument)
    fun summary(config: XProcStepConfiguration): XdmNode
    fun documentSummary(config: XProcStepConfiguration, builder: SaxonTreeBuilder, detail: DocumentDetail)
}