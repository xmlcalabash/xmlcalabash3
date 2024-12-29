package com.xmlcalabash.visualizers

import com.xmlcalabash.datamodel.XProcConstantExpression
import com.xmlcalabash.datamodel.XProcSelectExpression
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.AtomicOptionStep
import com.xmlcalabash.runtime.steps.AtomicStep
import com.xmlcalabash.runtime.steps.PipelineStep
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.steps.internal.ExpressionStep
import com.xmlcalabash.steps.internal.GuardStep

open class Plain(options: Map<String,String>): AbstractVisualizer(options) {
    val width = 0.coerceAtLeast(options["indent"]?.toInt() ?: 1)

    override fun showStart(step: AbstractStep, depth: Int) {
        if (step.type == NsCx.head || step.type == NsCx.foot
            || step.type == NsCx.splitter || step.type == NsCx.joiner || step.type == NsCx.select) {
            return
        }

        if (depth == 1 || width == 0) {
            println("Running ${name(step)}${extra(step)}")
        } else {
            println("Running ${"".padEnd((depth-1)*width, '.')} ${name(step)}${extra(step)}")
        }
    }

    override fun showEnd(step: AbstractStep, depth: Int) {
        // nop
    }

    override fun showDocument(step: AbstractStep, port: String, depth: Int, document: XProcDocument) {
        // nop
    }

    protected open fun name(step: AbstractStep): String {
        val type = if (step is PipelineStep && step.stepType != null) {
            "${step.stepType}"
        } else {
            "${step.type}"
        }

        if (step.name.startsWith("!")) {
            return type
        } else {
            return "${type}/${step.name}"
        }
    }

    protected open fun extra(step: AbstractStep): String {
        val extra = if (step.type == NsCx.document) {
            val impl = (step as AtomicStep).implementation as AbstractAtomicStep
            " (${impl._options[Ns.href]?.value ?: "(NULL?)"})"
        } else if (step.type == NsCx.guard) {
            val impl = (step as AtomicStep).implementation as GuardStep
            val value = impl.queues["source"]!!.first()
            " (${value.value.underlyingValue.effectiveBooleanValue()})"
        } else {
            if (step is AtomicOptionStep) {
                if (step.externalValue == null) {
                    return "" // ignore this one
                }
                " (${step.externalName} = ${step.externalValue?.value})"
            } else if (step.type == NsCx.expression) {
                val expr = ((step as AtomicStep).implementation as ExpressionStep).expression
                when (expr) {
                    is XProcSelectExpression -> " (${expr.select})"
                    is XProcConstantExpression -> " (${expr.staticValue ?: "(NULL?)"})"
                    else -> " (???)"
                }
            } else {
                ""
            }
        }

        return extra
    }
}