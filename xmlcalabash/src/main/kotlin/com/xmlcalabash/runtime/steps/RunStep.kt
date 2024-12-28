package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.model.CompoundStepModel
import com.xmlcalabash.runtime.parameters.RunStepStepParameters
import net.sf.saxon.ma.map.MapType
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.type.BuiltInAtomicType

open class RunStep(config: XProcStepConfiguration, compound: CompoundStepModel): CompoundStep(config, compound) {
    val runParams = compound.params as RunStepStepParameters

    override fun run() {
        // This isn't *really* a compound step...
        val cache = mutableMapOf<String, List<XProcDocument>>()
        cache.putAll(head.cache)
        head.cacheClear()

        stepConfig.environment.newExecutionContext(stepConfig)

        val parser = stepConfig.xmlCalabash.newXProcParser()
        for ((name, option) in runParams.options) {
            if (option.static) {
                parser.builder.staticOptionsManager.compileTimeValue(name, option.staticValue!!)
            }
        }

        val decl = try {
            parser.parse(cache["!source"]!!.first().value as XdmNode)
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcNotAPipeline(), ex)
        }

        val pipeline = try {
            decl.getExecutable()
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcNotAPipeline(), ex)
        }

        var okPrimary: String? = null
        for (input in decl.getInputs()) {
            if (input.primary == true) {
                okPrimary = input.port
                if (runParams.primaryInput != null && input.port != runParams.primaryInput) {
                    throw stepConfig.exception(XProcError.xcRunInputPrimaryMismatch(input.port, runParams.primaryInput))
                }
            }
            val documents = cache[input.port] ?: emptyList()
            for (doc in documents) {
                pipeline.input(input.port, doc)
            }
        }
        if (okPrimary != runParams.primaryInput) {
            throw stepConfig.exception(XProcError.xcRunInputPrimaryMismatch(okPrimary ?: runParams.primaryInput!!))
        }

        okPrimary = null
        for (output in decl.getOutputs()) {
            if (output.primary == true) {
                okPrimary = output.port
                if (runParams.primaryOutput != null && output.port != runParams.primaryOutput) {
                    throw stepConfig.exception(XProcError.xcRunOutputPrimaryMismatch(output.port, runParams.primaryOutput))
                }
            }
        }

        if (okPrimary != runParams.primaryOutput) {
            throw stepConfig.exception(XProcError.xcRunOutputPrimaryMismatch(okPrimary ?: runParams.primaryOutput!!))
        }

        for (option in decl.options) {
            var value = if (staticOptions[option.name] != null) {
                val sopt = staticOptions[option.name]!!
                sopt.staticValue.evaluate(stepConfig)
            } else if (head.options[option.name] != null) {
                listToValue(head.options[option.name] ?: emptyList())
            } else if (option.static) {
                continue
            } else {
                var defaultApplies = false
                val portName = "Q{{${option.name.namespaceUri}}${option.name.localName}"
                if (portName !in cache) {
                    val runnables = pipeline.runnable.runnables
                    for (runnable in runnables) {
                        if (runnable is AtomicOptionStep && runnable.externalName == option.name) {
                            defaultApplies = true
                            break
                        }
                    }
                }

                if (defaultApplies) {
                    continue
                }

                listToValue(cache[portName] ?: emptyList())
            }

            // Apply the QName magic to maps...
            if (option.asType?.underlyingSequenceType?.primaryType is MapType) {
                val mtype = option.asType?.underlyingSequenceType?.primaryType as MapType
                if (mtype.keyType.primitiveItemType == BuiltInAtomicType.QNAME) {
                    if (value is XdmMap) {
                        value = stepConfig.forceQNameKeys(value)
                    } else {
                        throw stepConfig.exception(XProcError.xsXPathStaticError("Value is not a map"))
                    }
                }
            }

            if (staticOptions[option.name] != null) {
                val sopt = staticOptions[option.name]!!
                pipeline.option(
                    option.name,
                    XProcDocument.ofValue(value, sopt.stepConfig, null, DocumentProperties())
                )
            } else {
                pipeline.option(
                    option.name,
                    XProcDocument.ofValue(value, stepConfig, null, DocumentProperties()))
            }
        }

        pipeline.receiver = PassthroughReceiver()

        pipeline.run()

        foot.run()

        stepConfig.environment.releaseExecutionContext()
    }

    private fun listToValue(documents: List<XProcDocument>): XdmValue {
        var value: XdmValue = XdmEmptySequence.getInstance()
        for (doc in documents) {
            value = value.append(doc.value)
        }
        return value
    }

    inner class PassthroughReceiver(): Receiver {
        override fun output(port: String, document: XProcDocument) {
            if (port in foot.receiver) {
                foot.input(port, document)
            }
        }
    }
}