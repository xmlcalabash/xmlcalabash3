package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceMap
import org.apache.logging.log4j.kotlin.logger

open class TryStep(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep(config, compound) {
    override fun run() {
        if (runnables.isEmpty()) {
            instantiate()
        }

        var group: GroupStep? = null
        val catches = mutableListOf<TryCatchStep>()
        var finally: TryFinallyStep? = null
        stepsToRun.clear()

        for (step in runnables) {
            when (step) {
                is TryFinallyStep -> finally = step
                is TryCatchStep -> catches.add(step)
                is GroupStep -> group = step
                else -> stepsToRun.add(step)
            }
        }

        stepConfig.environment.newExecutionContext(stepConfig)
        head.runStep()

        var errorDocument: XProcDocument? = null
        var throwException: Exception? = null
        try {
            runSubpipeline()
            group!!.runStep()
        } catch (ex: Exception) {
            foot.cache.clear() // anything that accumulated so far? nope.

            throwException = ex
            val errorCode = if (ex is XProcException) {
                ex.error.code
            } else {
                logger.warn { "Caught unwrapped exception: ${ex}" }
                null
            }

            errorDocument = errorDocument(group!!, ex)

            for (step in catches) {
                val codes = step.codes
                if (codes.isEmpty() || (errorCode != null && codes.contains(errorCode))) {
                    throwException = null
                    try {
                        step.head.input("error", errorDocument)
                        step.runStep()
                    } catch (cex: Exception) {
                        throwException = cex
                    }
                    break;
                }
            }
        }

        if (finally != null) {
            try {
                if (errorDocument != null) {
                    finally.head.input("error", errorDocument)
                }
                finally.runStep()
            } catch (ex: Exception) {
                if (throwException == null) {
                    throwException = ex
                }
            }
        }

        stepConfig.environment.releaseExecutionContext()

        if (throwException != null) {
            throw throwException
        }

        for ((port, documents) in foot.cache) {
            for (document in documents) {
                foot.write(port, document)
            }
        }

        foot.runStep()
    }

    private fun errorDocument(step: AbstractStep, exception: Exception): XProcDocument {
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

            if (error.details.isNotEmpty() && error.details[0] is XProcDocument) {
                builder.addSubtree((error.details[0] as XProcDocument).value)
            } else {
                if (exception.message != null) {
                    builder.addStartElement(NsCx.message)
                    builder.addText(exception.message!!)
                    builder.addEndElement()
                }
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
        return XProcDocument.ofXml(builder.result, step.stepConfig)
    }
}