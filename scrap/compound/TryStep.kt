package com.xmlcalabash.steps.compound

import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.RuntimeCompoundStep
import com.xmlcalabash.runtime.RuntimeSubpipelineStep
import com.xmlcalabash.runtime.RuntimeWhenStep
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceMap
import org.apache.logging.log4j.kotlin.logger

class TryStep(pipelineConfig: XProcRuntime): RuntimeCompoundStep(pipelineConfig) {

    override fun runStep() {
        val exec = stepConfig.newExecutionContext()
        stepConfig.setExecutionContext(exec)

        for ((port, documents) in inputDocuments) {
            for (document in documents) {
                head.input(port, document)
            }
        }

        head.run()
        head.receiver.close()

        var group: RuntimeSubpipelineStep? = null
        val catches = mutableListOf<RuntimeSubpipelineStep>()
        var finally: RuntimeSubpipelineStep? = null
        for (step in steps) {
            when (step.tag) {
                NsP.catch -> catches.add(step as RuntimeSubpipelineStep)
                NsP.group -> group = step as RuntimeSubpipelineStep
                NsP.finally -> finally = step as RuntimeSubpipelineStep
                else -> Unit
            }
        }

        var throwException: Exception? = null
        try {
            group!!.run()
        } catch (ex: Exception) {
            throwException = ex
            val errorCode = if (ex is XProcException) {
                ex.error.code
            } else {
                logger.warn { "Caught unwrapped exception: ${ex}"}
                null
            }

            for (step in catches) {
                val codes = (step.subpipeline as CatchStep).codes
                if (codes.isEmpty() || (errorCode != null && codes.contains(errorCode))) {
                    throwException = null
                    try {
                        runCatch(step, ex)
                    } catch (cex: Exception) {
                        throwException = cex
                    }
                    break;
                }
            }
        }

        if (finally != null) {
            try {
                finally.run()
            } catch (ex: Exception) {
                if (throwException == null) {
                    throwException = ex
                }
            }
        }

        inputDocuments.clear()
        stepConfig.runtime.releaseExecutionContext()

        if (throwException != null) {
            throw throwException
        }
    }

    private fun runCatch(step: RuntimeSubpipelineStep, exception: Exception) {
        var nsmap = NamespaceMap.emptyMap()
        nsmap = nsmap.put("c", NsC.namespace)
        nsmap = nsmap.put("cx", NsCx.namespace)

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsC.errors, step.stepConfig.stringAttributeMap(emptyMap()), nsmap)

        val attr = mutableMapOf<String, String?>()

        if (exception is XProcException) {
            val error = exception.error
            attr["name"] = error.stackTrace[0]?.stepName
            attr["type"] = error.stackTrace[0]?.stepType.toString()
            attr["code"] = error.code.toString()
            attr["href"] = error.inputLocation.baseURI?.toString()
            if (error.inputLocation.lineNumber > 0) {
                attr["line"] = error.inputLocation.lineNumber.toString()
            }
            if (error.inputLocation.columnNumber > 0) {
                attr["column"] = error.inputLocation.columnNumber.toString()
            }
            builder.addStartElement(NsC.error, step.stepConfig.stringAttributeMap(attr), nsmap)
            if (exception.message != null) {
                builder.addStartElement(NsCx.message)
                builder.addText(exception.message!!)
                builder.addEndElement()
            }

            if (error.stackTrace.isNotEmpty()) {
                builder.addStartElement(NsCx.stackTrace, step.stepConfig.stringAttributeMap(emptyMap()), nsmap)

                for (frame in error.stackTrace) {
                    attr.clear()
                    attr["type"] = frame.stepType.toString()
                    attr["name"] = frame.stepName
                    builder.addStartElement(NsCx.stackFrame, step.stepConfig.stringAttributeMap(attr), nsmap)
                    builder.addEndElement()
                }

                builder.addEndElement()
            }

            builder.addEndElement()
        } else {
            builder.addStartElement(NsC.error, step.stepConfig.stringAttributeMap(attr), nsmap)
            builder.addText(exception.message ?: "")
            builder.addEndElement()
        }

        builder.addEndElement()
        builder.endDocument()
        step.receive("error", XProcDocument.ofXml(builder.result, stepConfig))
        step.run()
    }

    override fun connectReceivers() {
        super.connectReceivers()
        for (step in steps.filterIsInstance<RuntimeWhenStep>()) {
            for ((portName, _) in step.subpipeline.outputManifold) {
                step.receiver.addDispatcher(portName, foot, portName)
            }
        }
    }
}