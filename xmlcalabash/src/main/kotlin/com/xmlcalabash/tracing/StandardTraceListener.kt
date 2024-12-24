package com.xmlcalabash.tracing

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.Consumer
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.time.Instant
import java.time.ZoneId

open class StandardTraceListener: TraceListener {
    private val _trace = mutableListOf<TraceDetail>()
    override val trace: List<TraceDetail>
        get() = _trace

    protected val threads = mutableSetOf<Long>()

    override fun startStep(step: AbstractStep) {
        threads.add(Thread.currentThread().id)
        val detail = StepStartDetail(step, Thread.currentThread().id, System.currentTimeMillis())
        synchronized(_trace) {
            _trace.add(detail)
        }
    }

    override fun endStep(step: AbstractStep) {
        val detail = StepStopDetail(step, Thread.currentThread().id)
        synchronized(_trace) {
            _trace.add(detail)
        }
    }

    override fun abortStep(step: AbstractStep, ex: Exception) {
        val detail = StepStopDetail(step, Thread.currentThread().id)
        synchronized(_trace) {
            _trace.add(detail)
        }
    }

    override fun sendDocument(from: Pair<AbstractStep, String>, to: Pair<Consumer, String>, document: XProcDocument): XProcDocument {
        synchronized(_trace) {
            val fromDetail = Pair(from.first.id, from.second)
            val toDetail = Pair(to.first.id, to.second)
            _trace.add(DocumentDetail(Thread.currentThread().id, fromDetail, toDetail, document))
        }
        return document
    }

    override fun summary(config: XProcStepConfiguration): XdmNode {
        val _startTime = QName("start-time")
        val _durationMs = QName("duration-ms")

        var nsMap = NamespaceMap.emptyMap()
        nsMap = nsMap.put("", NsTrace.namespace)
        nsMap = nsMap.put("p", NsP.namespace)
        nsMap = nsMap.put("cx", NsCx.namespace)

        val builder = SaxonTreeBuilder(config)
        builder.startDocument(null)
        builder.addStartElement(NsTrace.trace, EmptyAttributeMap.getInstance(), nsMap)

        val utc = ZoneId.of("UTC")

        for (thread in threads) {
            builder.addStartElement(NsTrace.thread, config.attributeMap(mapOf(Ns.id to "${thread}")), nsMap)
            val threadTrace = trace.filter { it.threadId == thread }
            for ((index, detail) in threadTrace.withIndex()) {
                when (detail) {
                    is StepStartDetail -> {
                        val end = findEnd(threadTrace, index, detail.id)

                        val instant = Instant.ofEpochMilli(detail.startTime)
                        val dt = instant.atZone(utc).toOffsetDateTime()

                        var localNsMap = if (detail.type.namespaceUri in listOf(NsP.namespace, NsCx.namespace)) {
                            nsMap
                        } else {
                            nsMap.put(detail.type.prefix, detail.type.namespaceUri)
                        }

                        val ms = (end.nanoSeconds - detail.nanoSeconds) / 1_000_000
                        builder.addStartElement(NsTrace.step, config.attributeMap(mapOf(
                            Ns.id to detail.id,
                            Ns.name to detail.name,
                            Ns.type to "${detail.type}",
                            _startTime to "${dt}",
                            _durationMs to "${ms}"
                        )), localNsMap)
                    }
                    is StepStopDetail -> {
                        builder.addEndElement()
                    }
                    is DocumentDetail -> {
                        documentSummary(config, builder, detail)
                    }
                }
            }

            builder.addEndElement()
        }

        builder.addEndElement()
        builder.endDocument()
        return builder.result
    }

    override fun documentSummary(config: XProcStepConfiguration, builder: SaxonTreeBuilder, detail: DocumentDetail) {
        val atts = mutableMapOf<QName, String>()
        atts[Ns.id] = "${detail.id}"
        if (detail.contentType != null) {
            atts[Ns.contentType] = detail.contentType.toString()
        }
        builder.addStartElement(NsTrace.document, config.attributeMap(atts))

        builder.addStartElement(NsTrace.from, config.attributeMap(mapOf(
            Ns.id to detail.from.first,
            Ns.port to detail.from.second)))
        builder.addEndElement()
        builder.addStartElement(NsTrace.to, config.attributeMap(mapOf(
            Ns.id to detail.to.first,
            Ns.port to detail.to.second)))
        builder.addEndElement()
        builder.addEndElement()
    }

    private fun findEnd(trace: List<TraceDetail>, start: Int, stepId: String): StepStopDetail {
        var depth = 0
        for (pos in start+1 ..< trace.size) {
            val detail = trace[pos]
            when (detail) {
                is StepStopDetail -> {
                    if (detail.id == stepId) {
                        if (depth == 0) {
                            return detail
                        }
                        depth--
                    }
                }
                is StepStartDetail -> {
                    if (detail.id == stepId) {
                        depth++
                    }
                }
            }
        }
        throw XProcError.xiImpossible("Did not find stop detail for ${stepId} starting at ${start}").exception()
    }
}