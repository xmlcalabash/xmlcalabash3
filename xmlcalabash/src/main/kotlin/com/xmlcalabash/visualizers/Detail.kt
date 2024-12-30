package com.xmlcalabash.visualizers

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.NsXs.boolean
import com.xmlcalabash.runtime.steps.AbstractStep
import com.xmlcalabash.runtime.steps.AtomicStep
import com.xmlcalabash.runtime.steps.CompoundStep
import com.xmlcalabash.runtime.steps.PipelineStep
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import org.apache.logging.log4j.kotlin.logger

class Detail(options: Map<String,String>): Plain(emptyMap()) {
    private var showsteps = true
    private var showdocs = false

    init {
        for ((key, value) in options) {
            when (key) {
                "steps" -> showsteps = boolean(key, value)
                "docs", "documents" -> showdocs = boolean(key, value)
                else -> logger.warn("Unknown detail visualizer option: $key")
            }
        }
    }

    override fun showStart(step: AbstractStep, depth: Int) {
        if (!showsteps) {
            return
        }

        if (depth == 1) {
            print("┌─ ")
        } else {
            for (index in 1 ..< depth) {
                print("│  ")
            }
            print("├─ ")
        }
        println("${name(step)}${extra(step)}")
    }

    override fun showEnd(step: AbstractStep, depth: Int) {
        if (!showsteps || step !is CompoundStep) {
            return
        }

        if (depth == 1) {
            print("└─ ")
        } else {
            for (index in 1 ..< depth) {
                print("│  ")
            }
            print("├─ ")
        }

        println("${name(step)}${extra(step)} (ends)")
    }

    override fun showDocument(step: AbstractStep, port: String, depth: Int, document: XProcDocument) {
        if (!showdocs) {
            return
        }

        val value = document.value

        val type = if (document is XProcBinaryDocument) {
            "«binary»"
        } else {
            when (value) {
                is XdmNode -> {
                    when (value.nodeKind) {
                        XdmNodeKind.DOCUMENT -> {
                            val root = S9Api.firstElement(value)?.nodeName
                            if (root == null) {
                                "«empty document»"
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
                        else -> "«${value.nodeKind.toString().lowercase()}»"
                    }
                }
                is XdmMap -> "map"
                is XdmArray -> "array"
                is XdmAtomicValue -> "${value}"
                else -> "unknown"
            }
        }

        for (index in 1 ..< depth) {
            print("│  ")
        }
        print("│  ")

        println("┄ ${name(step)}.${port} ⟶ ${type}")
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