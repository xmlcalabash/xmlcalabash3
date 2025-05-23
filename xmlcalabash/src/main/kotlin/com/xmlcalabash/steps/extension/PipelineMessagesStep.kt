package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.BufferingMessageReporter
import com.xmlcalabash.util.Report
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.s9api.QName

class PipelineMessagesStep(): AbstractAtomicStep() {
    companion object {
        private val _clear = QName("clear")

    }
    override fun run() {
        super.run()

        val levelString = stringBinding(Ns.level)
        val level = when (levelString?.lowercase()) {
            null -> Verbosity.TRACE
            "error" -> Verbosity.ERROR
            "warning" -> Verbosity.WARN
            "info" -> Verbosity.INFO
            "debug" -> Verbosity.DEBUG
            "trace" -> Verbosity.TRACE
            else -> {
                throw stepConfig.exception(XProcError.xdStepFailed("No such verbosity: ${levelString}"))
            }
        }
        val clear = booleanBinding(_clear) ?: false

        val messages: List<Report> = if (stepConfig.environment.messageReporter is BufferingMessageReporter) {
            val reporter = stepConfig.environment.messageReporter as BufferingMessageReporter
            val msgs = reporter.messages(level)
            if (clear) {
                reporter.clear()
            }
            msgs
        } else {
            emptyList()
        }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsCx.messages)
        for (message in messages) {
            val attributes = mutableMapOf<QName, String>()
            attributes.putAll(message.extraDetail)
            attributes[Ns.level] = "${message.severity}"
            attributes[Ns.message] = message.message

            builder.addStartElement(NsCx.message, stepConfig.typeUtils.attributeMap(attributes))
            builder.addEndElement()
        }
        builder.addEndElement()
        builder.endDocument()

        receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig))
    }

    override fun toString(): String = "cx:pipeline-messages"
}