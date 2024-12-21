package com.xmlcalabash.tracing

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.XdmNode

class NopTraceListener(): TraceListener {
    override val trace: List<TraceDetail>
        get() = emptyList()

    override fun startExecution(step: AbstractStep) {
        // nop
    }

    override fun stopExecution(step: AbstractStep, duration: Long) {
        // nop
    }

    override fun sendDocument(from: Pair<String, String>, to: Pair<String, String>, document: XProcDocument) {
        // nop
    }

    override fun summary(config: XProcStepConfiguration): XdmNode {
        val builder = SaxonTreeBuilder(config)
        builder.startDocument(null)
        builder.addStartElement(NsTrace.trace)
        builder.addEndElement()
        builder.endDocument()
        return builder.result
    }

    override fun documentSummary(
        config: XProcStepConfiguration,
        builder: SaxonTreeBuilder,
        detail: DocumentDetail
    ) {
        // nop
    }
}