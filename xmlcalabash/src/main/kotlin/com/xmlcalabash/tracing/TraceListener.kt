package com.xmlcalabash.tracing

import com.xmlcalabash.api.Monitor
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.XdmNode

interface TraceListener: Monitor {
    val trace: List<TraceDetail>
    fun summary(config: XProcStepConfiguration): XdmNode
    fun documentSummary(config: XProcStepConfiguration, builder: SaxonTreeBuilder, detail: DocumentDetail)
}