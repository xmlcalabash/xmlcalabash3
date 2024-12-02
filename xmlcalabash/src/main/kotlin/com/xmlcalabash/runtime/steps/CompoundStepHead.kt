package com.xmlcalabash.runtime.steps

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.runtime.RuntimeStepConfiguration
import com.xmlcalabash.runtime.model.HeadModel
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmFunctionItem
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import net.sf.saxon.s9api.XdmValue
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class CompoundStepHead(yconfig: RuntimeStepConfiguration, step: HeadModel): AbstractStep(yconfig, step) {
    override val params = RuntimeStepParameters(NsCx.foot, "!head",
        step.location, step.inputs, step.outputs, step.options)
    val defaultInputs = step.defaultInputs
    val openPorts = mutableSetOf<String>()
    internal val unboundInputs = mutableSetOf<String>()
    private var message: XdmValue? = null
    internal var showMessage = true
    private val _cache = mutableMapOf<String, MutableList<XProcDocument>>()
    private val _options = mutableMapOf<QName, MutableList<XProcDocument>>()
    private val inputErrors = mutableListOf<XProcError>()

    val cache: Map<String, List<XProcDocument>>
        get() = _cache

    val options: Map<QName, List<XProcDocument>>
        get() = _options

    init {
        // Inputs = step inputs that aren't passed on to the subpipeline; !source on p:for-each, for example
        for ((name, port) in params.inputs) {
            if (!port.weldedShut) {
                openPorts.add(name)
            }
        }
        // Outputs = step inputs that are passed on to the subpipeline, caches and current on p:for-each, for example
        for ((name, port) in params.outputs) {
            if (!port.weldedShut) {
                openPorts.add(name)
            }
        }
    }

    override val readyToRun: Boolean
        get() {
            for (port in openPorts) {
                if (!unboundInputs.contains(port)) {
                    return false
                }
            }
            return true
        }

    internal fun cacheClear() {
        _cache.clear()
        inputCount.clear()
    }

    private fun cacheInput(port: String, documents: List<XProcDocument>) {
        val count = (inputCount[port] ?: 0) + documents.size
        inputCount[port] = count
        val docList = _cache[port] ?: mutableListOf()

        docList.addAll(documents)
        _cache[port] = docList
    }

    internal fun cacheInputs(inputs: Map<String,List<XProcDocument>>) {
        for ((port, docs) in inputs) {
            cacheInput(port, docs)
        }
    }

    override fun input(port: String, doc: XProcDocument) {
        // N.B. inputs and outputs are swapped in the head
        if (port.startsWith("Q{")) {
            val name = stepConfig.parseQName(port)

            if ((type.namespaceUri == NsP.namespace && name == Ns.message)
                || (type.namespaceUri != NsP.namespace && name == NsP.message)) {
                message = doc.value
            } else {
                val olist = _options[name] ?: mutableListOf()
                olist.add(doc)
                _options[name] = olist
            }
            return
        }

        val error = if (port in params.inputs) {
            checkInputPort(port, doc, params.inputs[port])
        } else {
            checkInputPort(port, doc, params.outputs[port])
        }

        if (error == null) {
            val list = _cache[port] ?: mutableListOf()
            list.add(doc)
            _cache[port] = list
        } else {
            inputErrors.add(error)
        }
    }

    override fun output(port: String, document: XProcDocument) {
        throw UnsupportedOperationException("Never send an output to a compound head")
    }

    override fun close(port: String) {
        openPorts.remove(port)
    }

    override fun instantiate() {
        // nop
    }

    override fun run() {
        for ((name, details) in staticOptions) {
            if ((type.namespaceUri == NsP.namespace && name == Ns.message)
                || (type.namespaceUri != NsP.namespace && name == NsP.message)) {
                message = details.staticValue.evaluate()
            }
        }

        if (showMessage && message != null) {
            println(message)
            message = null
            showMessage = false
        }

        // Work out what should appear on each input...
        for ((port, output) in params.outputs) {
            if (output.weldedShut) {
                continue
            }

            if (port !in cache) {
                if (port in defaultInputs && port in unboundInputs) {
                    val default = defaultInputs[port]!!
                    for (binding in default.inputs) {
                        for (document in defaultBindingDocuments(binding)) {
                            if (default.select != null) {
                                default.select.contextItem = document
                                val selected = default.select.evaluate()
                                for (item in selected) {
                                    if (item is XdmNode && item.nodeKind == XdmNodeKind.ATTRIBUTE) {
                                        throw XProcError.xdInvalidSelection(item.nodeName).exception()
                                    }
                                    if (item is XdmFunctionItem) {
                                        throw XProcError.xdInvalidFunctionSelection().exception()
                                    }
                                    val itemdoc = XProcDocument.ofValue(
                                        item,
                                        document.context,
                                        document.contentType,
                                        document.properties
                                    )
                                    val error = checkInputPort(port, itemdoc, params.outputs[port])
                                    if (error == null) {
                                        val list = _cache[port] ?: mutableListOf()
                                        list.add(itemdoc)
                                        _cache[port] = list
                                    } else {
                                        inputErrors.add(error)
                                    }
                                }
                            } else {
                                val error = checkInputPort(port, document, params.outputs[port])
                                if (error == null) {
                                    val list = _cache[port] ?: mutableListOf()
                                    list.add(document)
                                    _cache[port] = list
                                } else {
                                    inputErrors.add(error)
                                }
                            }
                        }
                    }
                }
            }
        }

        for ((port, output) in params.outputs) {
            if (output.weldedShut) {
                continue
            }

            val documents = cache[port] ?: emptyList()

            val rpair = receiver[port]
            if (rpair == null) {
                if (stepConfig.staticContext.xmlCalabash.xmlCalabashConfig.debug) {
                    // Ultimately, I don't think these ever matter, but for debugging purposes...
                    if (((type == NsP.choose || type == NsP.`if`) && port == "!context")
                        || ((type == NsP.catch || type == NsP.finally) && port == "error")
                        || ((type == NsP.forEach || type == NsP.viewport) && port == "current")) {
                        // ignore
                    } else {
                        println("No receiver for ${port} from ${this} (in head)")
                    }
                }
            } else {
                val targetStep = rpair.first
                val targetPort = rpair.second

                for (doc in documents) {
                    targetStep.input(targetPort, doc)
                }
            }

            val error = checkInputPort(port, params.outputs[port]!!)
            if (error != null) {
                inputErrors.add(error)
            }
        }

        if (inputErrors.isNotEmpty()) {
            throw inputErrors.first().exception()
        }

        for ((_, rpair) in receiver) {
            rpair.first.close(rpair.second)
        }

        _cache.clear()
    }

    override fun reset() {
        super.reset()
        openPorts.clear()
        openPorts.addAll(params.outputs.keys)
        _cache.clear()
        inputCount.clear()
        showMessage = true
    }

    override fun toString(): String {
        return "(compound step head ${id})"
    }
}
