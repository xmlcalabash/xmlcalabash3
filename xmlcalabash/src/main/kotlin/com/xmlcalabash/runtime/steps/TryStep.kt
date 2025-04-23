package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
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

        stepConfig.saxonConfig.newExecutionContext(stepConfig)
        try {
            head.runStep(this)

            var errorDocument: XProcDocument? = null
            var throwException: Exception? = null
            try {
                runSubpipeline()
                group!!.runStep(this)
            } catch (ex: Exception) {
                foot.cache.clear() // anything that accumulated so far? nope.

                throwException = ex
                val errorCode = if (ex is XProcException) {
                    ex.error.code
                } else {
                    stepConfig.warn { "Caught unwrapped exception: ${ex}" }
                    null
                }

                errorDocument = errorDocument(group!!, ex)

                for (step in catches) {
                    val codes = step.codes
                    if (codes.isEmpty() || (errorCode != null && codes.contains(errorCode))) {
                        throwException = null
                        try {
                            step.head.input("error", errorDocument)
                            step.runStep(this)
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
                    finally.runStep(this)
                } catch (ex: Exception) {
                    if (throwException == null) {
                        throwException = ex
                    }
                }
            }

            if (throwException != null) {
                throw throwException
            }

            for ((port, documents) in foot.cache) {
                for (document in documents) {
                    foot.write(port, document)
                }
            }

            foot.runStep(this)
        } finally {
            stepConfig.saxonConfig.releaseExecutionContext()
        }
    }

    private fun errorDocument(step: AbstractStep, exception: Exception): XProcDocument {
        var nsmap = NamespaceMap.emptyMap()
        nsmap = nsmap.put("c", NsC.namespace)
        nsmap = nsmap.put("cx", NsCx.namespace)

        if (exception is XProcException) {
            if (exception.error.code.prefix.isNotEmpty()) {
                nsmap = nsmap.put(exception.error.code.prefix, exception.error.code.namespaceUri)
            }
            val type = exception.error.stackTrace[0]?.stepType
            if (type != null) {
                if (type.prefix.isNotEmpty()) {
                    nsmap = nsmap.put(type.prefix, type.namespaceUri)
                }
            }
        }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addStartElement(NsC.errors, step.stepConfig.typeUtils.stringAttributeMap(emptyMap()), nsmap)

        val attr = mutableMapOf<String, String?>()

        if (exception is XProcException) {
            val error = exception.error
            attr["name"] = error.stackTrace[0]?.stepName
            attr["type"] = error.stackTrace[0]?.stepType.toString()
            attr["code"] = error.code.toString()
            attr["href"] = error.location.baseUri?.toString()
            if (error.location.lineNumber > 0) {
                attr["line"] = error.location.lineNumber.toString()
            }
            if (error.location.columnNumber > 0) {
                attr["column"] = error.location.columnNumber.toString()
            }

            builder.addStartElement(NsC.error, step.stepConfig.typeUtils.stringAttributeMap(attr), nsmap)

            if (error.inputLocation.baseUri != null || error.inputLocation.lineNumber > 0) {
                attr.clear()
                attr["href"] = error.inputLocation.baseUri?.toString()
                if (error.inputLocation.lineNumber > 0) {
                    attr["line"] = error.inputLocation.lineNumber.toString()
                }
                if (error.inputLocation.columnNumber > 0) {
                    attr["column"] = error.inputLocation.columnNumber.toString()
                }
                builder.addStartElement(NsCx.inputLocation, step.stepConfig.typeUtils.stringAttributeMap(attr), nsmap)
                builder.addEndElement()
            }

            builder.addStartElement(NsCx.message)
            builder.addText(stepConfig.environment.errorExplanation.message(exception.error, false))
            builder.addEndElement()

            val explanation = stepConfig.environment.errorExplanation.explanation(exception.error)
            if (explanation.isNotBlank()) {
                builder.addStartElement(NsCx.explanation)
                builder.addText(explanation)
                builder.addEndElement()
            }

            if (exception.cause != null && exception.cause!!.message != null) {
                builder.addStartElement(NsCx.cause)
                builder.addText(exception.cause!!.message!!)
                builder.addEndElement()
            }

            if (error.details.isNotEmpty() && error.details[0] is XProcDocument) {
                builder.addSubtree((error.details[0] as XProcDocument).value)
            }

            if (error.stackTrace.isNotEmpty()) {
                builder.addStartElement(NsCx.stackTrace, step.stepConfig.typeUtils.stringAttributeMap(emptyMap()), nsmap)

                for (frame in error.stackTrace) {
                    attr.clear()
                    attr["type"] = frame.stepType.toString()
                    attr["name"] = frame.stepName
                    builder.addStartElement(NsCx.stackFrame, step.stepConfig.typeUtils.stringAttributeMap(attr), nsmap)
                    builder.addEndElement()
                }

                builder.addEndElement()
            }

            builder.addEndElement()
        } else {
            builder.addStartElement(NsC.error, step.stepConfig.typeUtils.stringAttributeMap(attr), nsmap)
            builder.addText(exception.message ?: "")
            builder.addEndElement()
        }

        builder.addEndElement()
        builder.endDocument()
        return XProcDocument.ofXml(builder.result, step.stepConfig)
    }
}