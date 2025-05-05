package com.xmlcalabash.steps.internal

import com.xmlcalabash.datamodel.XProcAvtExpression
import com.xmlcalabash.datamodel.XProcConstantExpression
import com.xmlcalabash.datamodel.XProcSelectExpression
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.LazyDocumentValue
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.Ns.port
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.parameters.ExpressionStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.expr.parser.RoleDiagnostic
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import java.util.function.Supplier

open class ExpressionStep(val params: ExpressionStepParameters): AbstractAtomicStep() {
    val contextItems = mutableListOf<XProcDocument>()
    val expression = params.expression
    val collection = params.collection

    override fun run() {
        super.run()

        params.expression.details.error?.let {
            throw stepConfig.exception(XProcError.xsXPathStaticError(params.expression.toString()), it)
        }

        // Expression steps are unusual in that source may not exist
        contextItems.addAll(queues["source"] ?: emptyList())

        stepConfig.debug { "  Expression: ${params.expression}" }
        stepConfig.debug { "  Context items: ${contextItems.size}" }

        for (item in contextItems) {
            stepConfig.debug {
                val strValue = item.value.toString()
                if (strValue.length > 512) {
                    "  Context item: ${strValue.substring(0, 511)}…"
                } else {
                    "  Context item: ${strValue}"
                }
            }
        }

        if (collection) {
            expression.defaultCollection(contextItems)
        } else {
            // We don't raise an error for a sequence, we just leave the context item undefined.
            // This is to handle cases like ab-choose-054 where the context expression makes
            // no reference to the context but the drp provides a sequence...
            if (contextItems.size == 1) {
                expression.contextItem = contextItems.first()
            }
        }

        // All the required variable bindings are in params.options; but
        // some of those may have been provided at runtime...
        for ((name, option) in params.options) {
            if (name in options) {
                val value = options[name]!!.value
                stepConfig.typeUtils.checkType(name, value, option.asType, option.values)
                stepConfig.debug { "  ${name} = ${value}" }
                expression.setBinding(name, value)
            } else {
                val value = option.staticValue!!.evaluate(stepConfig)
                stepConfig.debug { "  ${name} = ${value}" }
                expression.setBinding(name, value)
            }
        }

        val eager = if (params.extensionAttr.containsKey(NsCx.eager)) {
            stepConfig.typeUtils.parseBoolean(params.extensionAttr[NsCx.eager]!!)
        } else {
            stepConfig.xmlCalabashConfig.eagerEvaluation
        }

        val canBeLazy = !eager
                && !expression.functionRefs.contains(Pair(NsP.iterationPosition,0))
                && !expression.functionRefs.contains(Pair(NsP.iterationSize,0))
                && !expression.functionRefs.contains(Pair(NsFn.collection,0))
                && !expression.functionRefs.contains(Pair(NsFn.collection,1))

        val lazy: () -> LazyDocumentValue = {
            try {
                val value = expression.evaluate(stepConfig)
                var properties: DocumentProperties = DocumentProperties()
                when (value) {
                    is XdmNode -> {
                        properties[Ns.baseUri] = value.baseURI
                        properties[Ns.contentType] = "application/xml"
                    }
                    is XdmMap, is XdmArray -> {
                        properties[Ns.contentType] = "application/json"
                    }
                    else -> Unit
                }
                for (context in contextItems) {
                    if (context.value.underlyingValue === value.underlyingValue) {
                        properties = DocumentProperties(context.properties)
                        break;
                    }
                }
                LazyDocumentValue(value, properties)
            } catch (ex: SaxonApiException) {
                if (ex.message != null && ex.message!!.contains("context item is absent")) {
                    if (contextItems.size > 1) {
                        throw stepConfig.exception(XProcError.xdSequenceForbidden())
                    } else {
                        throw stepConfig.exception(XProcError.xdEmptySequenceForbidden())
                    }
                }
                throw ex
            }
        }

        if (canBeLazy) {
            logger.debug { "  expression will be lazily evaluated" }
            receiver.output("result", XProcDocument(lazy, stepConfig))
        } else {
            logger.debug { "  expression=${lazy()}" }
            val lazyValue = lazy()
            receiver.output("result", XProcDocument(lazyValue.value, stepConfig, lazyValue.properties))
        }
    }

    override fun reset() {
        super.reset()
        contextItems.clear()
    }

    override fun toString(): String {
        val expr = when (expression) {
            is XProcSelectExpression -> "select=${expression.select}"
            is XProcAvtExpression -> "avt=${expression.avt}"
            is XProcConstantExpression -> "constant=${expression.staticValue}"
            else -> "???"
        }

        return "cx:expression: ${expr}"
    }
}