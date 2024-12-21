package com.xmlcalabash.steps.internal

import com.xmlcalabash.datamodel.XProcAvtExpression
import com.xmlcalabash.datamodel.XProcConstantExpression
import com.xmlcalabash.datamodel.XProcSelectExpression
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.parameters.ExpressionStepParameters
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.expr.parser.RoleDiagnostic
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.XdmValue
import java.util.function.Supplier

open class ExpressionStep(val params: ExpressionStepParameters): AbstractAtomicStep() {
    val contextItems = mutableListOf<XProcDocument>()
    var override: XProcDocument? = null
    val expression = params.expression
    val collection = params.collection

    override fun run() {
        super.run()
        // Expression steps are unusual in that source may not exist
        contextItems.addAll(queues["source"] ?: emptyList())

        if (override != null) {
            receiver.output("result", override!!)
            override = null
            return
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

        // Some options may have been passed to us
        for ((name, doc) in options) {
            expression.setBinding(name, doc.value)
        }

        // Any options that haven't been passed, may have statically computed values
        for ((name, option) in params.options) {
            if (name !in options) {
                expression.setBinding(name, option.staticValue!!.evaluate(stepConfig))
            }
        }

        val eager = if (params.extensionAttr.containsKey(NsCx.eager)) {
            stepConfig.parseBoolean(params.extensionAttr[NsCx.eager]!!)
        } else {
            stepConfig.environment.commonEnvironment.eagerEvaluation
        }

        val canBeLazy = !eager
                && !expression.functionRefs.contains(Pair(NsP.iterationPosition,0))
                && !expression.functionRefs.contains(Pair(NsP.iterationSize,0))
                && !expression.functionRefs.contains(Pair(NsFn.collection,0))
                && !expression.functionRefs.contains(Pair(NsFn.collection,1))

        val lazy: () -> XdmValue = {
            try {
                expression.evaluate(stepConfig)
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
            receiver.output("result", XProcDocument(lazy, stepConfig))
        } else {
            receiver.output("result", XProcDocument(lazy(), stepConfig))
        }
    }

    override fun reset() {
        super.reset()
        contextItems.clear()
    }

    override fun toString(): String {
        val expr = if (override != null) {
            "value=${override!!.value}"
        } else {
            when (expression) {
                is XProcSelectExpression -> "select=${expression.select}"
                is XProcAvtExpression -> "avt=${expression.avt}"
                    is XProcConstantExpression -> "constant=${expression.staticValue}"
                else -> "???"
            }
        }

        return "cx:expression: ${expr}"
    }
}