package com.xmlcalabash.visualizers

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.CompoundStep
import com.xmlcalabash.runtime.steps.PipelineStep
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import org.apache.logging.log4j.kotlin.logger

class Detail(printer: MessagePrinter, options: Map<String,String>): Plain(printer, emptyMap()) {
    private var showsteps = true
    private var showdocs = false
    private val t_start: String
    private val t_end: String
    private val t_indent: String
    private val t_tee: String
    private val t_dots: String
    private val t_arrow: String
    private val t_leftg: String
    private val t_rightg: String

    init {
        for ((key, value) in options) {
            when (key) {
                "steps" -> showsteps = boolean(key, value)
                "docs", "documents" -> showdocs = boolean(key, value)
                else -> logger.warn("Unknown detail visualizer option: $key")
            }
        }

        if (printer.encoding.lowercase().startsWith("utf")) {
            t_start = "┌─ "
            t_end = "└─ "
            t_indent = "│  "
            t_tee = "├─ "
            t_dots = "┄"
            t_arrow = "⟶"
            t_leftg = "«"
            t_rightg = "»"
        } else {
            t_start = "+- "
            t_end = "+- "
            t_indent = "|  "
            t_tee = "+- "
            t_dots = "---"
            t_arrow = "-->"
            t_leftg = "<<"
            t_rightg = ">>"
        }

    }

    override fun showStart(step: AbstractStep, depth: Int) {
        if (!showsteps) {
            return
        }

        if (depth == 1) {
            printer.print(t_start)
        } else {
            printer.print(t_indent.repeat(depth-1))
            printer.print(t_tee)
        }
        printer.println("${name(step)}${extra(step)}")
    }

    override fun showEnd(step: AbstractStep, depth: Int) {
        if (!showsteps || step !is CompoundStep) {
            return
        }

        if (depth == 1) {
            printer.print(t_end)
        } else {
            printer.print(t_indent.repeat(depth-1))
            printer.print(t_tee)
        }

        printer.println("${name(step)}${extra(step)} (ends)")
    }

    override fun showDocument(step: AbstractStep, port: String, depth: Int, document: XProcDocument) {
        if (!showdocs) {
            return
        }

        val value = document.value

        val type = if (document is XProcBinaryDocument) {
            "${t_leftg}binary${t_rightg}"
        } else {
            when (value) {
                is XdmNode -> {
                    when (value.nodeKind) {
                        XdmNodeKind.DOCUMENT -> {
                            val root = S9Api.firstElement(value)?.nodeName
                            if (root == null) {
                                "${t_leftg}empty document${t_rightg}"
                            } else {
                                "<${root} ...>"
                            }
                        }
                        XdmNodeKind.ELEMENT -> "<${value.nodeName} ...>"
                        XdmNodeKind.TEXT -> {
                            val str = value.underlyingNode.stringValue
                            if (str.length > 30) {
                                "\"${str.substring(0, 30)}...\""
                            } else {
                                "\"${str}\""
                            }
                        }
                        else -> "${t_leftg}${value.nodeKind.toString().lowercase()}${t_rightg}"
                    }
                }
                is XdmMap -> "map"
                is XdmArray -> "array"
                is XdmAtomicValue -> "${value}"
                else -> "unknown"
            }
        }

        printer.print(t_indent.repeat(depth-1))
        printer.print(t_indent)
        printer.println("${t_dots} ${name(step)}.${port} ${t_arrow} ${type}")
    }

    override fun name(step: AbstractStep): String {
        val type = if (step is PipelineStep && step.stepType != null) {
            "${step.stepType}"
        } else {
            "${step.type}"
        }
        return "${type}/${step.id}"
    }
}