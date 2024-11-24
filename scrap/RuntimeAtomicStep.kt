package com.xmlcalabash.runtime

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.datamodel.*
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.graph.Model
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.runtime.api.OptionFlangeInfo
import com.xmlcalabash.runtime.api.PortFlangeInfo
import com.xmlcalabash.runtime.parameters.*
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

open class RuntimeAtomicStep(runtime: XProcRuntime): RuntimeStep(runtime) {
    constructor(runtime: XProcRuntime, impl: XProcStep): this(runtime) {
        implementation = impl
    }

    internal lateinit var implementation: XProcStep
    private val staticOptions = mutableMapOf<QName, XProcDocument>()

    override fun setup(model: Model) {
        super.setup(model)

        for ((name, value) in model.step.staticOptions) {
            staticOptions[name] = XProcDocument.ofValue(value, model.step.stepConfig, null, DocumentProperties())
        }

        val provider = when (tag) {
            NsCx.empty -> runtime.stepProvider(EmptyStepParameters())
            NsCx.inline -> {
                val step = model.step as AtomicInlineStepInstruction
                val param = InlineStepParameters(step.filter, step.contentType, step.encoding, null)
                runtime.stepProvider(param)
            }
            NsCx.document -> {
                val step = model.step as AtomicDocumentStepInstruction
                val param = DocumentStepParameters(model.step.contentType)
                runtime.stepProvider(param)
            }
            NsCx.select -> {
                val step = model.step as AtomicSelectStepInstruction
                val param = SelectStepParameters(step.select)
                runtime.stepProvider(param)
            }
            NsCx.expression -> {
                val step = model.step as AtomicExpressionStepInstruction
                // FIXME: what about extension attributes? (And collection)
                val param = ExpressionStepParameters(step.expression, step.expression.asType, step.expression.values, false, mapOf())
                runtime.stepProvider(param)
            }
            else -> {
                if ((model.step as AtomicStepInstruction).userDefinedStep != null) {
                    { UserDefinedStep(model.step as AtomicStepInstruction) }
                } else {
                    runtime.stepProvider(StepParameters(tag, model.step.name))
                }
            }
        }
        implementation = provider()

        val inputs = mutableListOf<PortFlange>()
        for ((name, port) in inputManifold) {
            inputs.add(PortFlange(name, PortFlangeInfo(port)))
        }

        val outputs = mutableListOf<PortFlange>()
        for ((name, port) in outputManifold) {
            outputs.add(PortFlange(name, PortFlangeInfo(port)))
        }

        val options = mutableListOf<OptionFlange>()
        for ((name, option) in optionManifold) {
            options.add(OptionFlange(name, OptionFlangeInfo(option)))
        }

        implementation.setup(stepConfig, receiver, 0, inputs, outputs, options)
    }

    override fun runStep() {
        for ((name, doc) in staticOptions) {
            implementation.option(name, doc, false)
        }

        for ((portName, port) in inputManifold) {
            val documents = inputDocuments[portName] ?: emptyList()
            if (portName.startsWith("Q{")) {
                val name = stepConfig.parseQName(portName, NamespaceUri.NULL)
                implementation.option(name, documents.first(), false)
            } else {
                if (documents.size != 1 && !port.sequence) {
                    throw XProcError.xdInputSequenceForbidden(portName).exception()
                }
                for (document in documents) {
                    if (port.contentTypes.isNotEmpty()) {
                        val ctype = document.contentType ?: MediaType.OCTET_STREAM
                        if (!ctype.allowed(port.contentTypes)) {
                            throw XProcError.xdBadInputContentType(ctype.toString()).exception()
                        }
                    }

                    implementation.input(portName, document)
                }
            }
            inputDocuments.remove(portName)
        }

        for ((portName, documents) in inputDocuments) {
            throw IllegalArgumentException("Unexpected input port: ${portName}")
        }

        implementation.run()
    }

    override fun reset() {
        super.reset()
        implementation.reset()
    }
}