package com.xmlcalabash.functions

import com.xmlcalabash.datamodel.*
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.util.BufferingReceiver
import com.xmlcalabash.util.MediaClassification
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.ExtensionFunctionCall
import net.sf.saxon.lib.ExtensionFunctionDefinition
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.ma.map.MapType
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.om.Sequence
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmMap
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.type.BuiltInAtomicType
import net.sf.saxon.value.Cardinality
import net.sf.saxon.value.EmptySequence
import net.sf.saxon.value.SequenceType

class PipelineFunction(private val decl: DeclareStepInstruction): ExtensionFunctionDefinition() {
    private val fname = StructuredQName(decl.type!!.prefix, decl.type!!.namespaceUri, decl.type!!.localName)
    private val inputs = decl.children.filterIsInstance<InputInstruction>()
    private val outputs = decl.children.filterIsInstance<OutputInstruction>()
    private val options = decl.children.filterIsInstance<OptionInstruction>()
    private var argumentTypes: Array<out SequenceType>? = null

    override fun getFunctionQName(): StructuredQName? {
        return fname
    }

    override fun getMinimumNumberOfArguments(): Int {
        return inputs.size
    }

    override fun getMaximumNumberOfArguments(): Int {
        return 1
    }

    override fun getArgumentTypes(): Array<out SequenceType?>? {
        if (argumentTypes != null) {
            return argumentTypes!!
        }

        val argumentList = mutableListOf<SequenceType>()
        for (input in inputs) {
            val seqtype = portSequenceType(input)
            argumentList.add(seqtype)
        }

        if (options.isNotEmpty()) {
            val maptype = MapType(BuiltInAtomicType.ANY_ATOMIC, SequenceType.ANY_SEQUENCE)
            argumentList.add(SequenceType(maptype, Cardinality.fromOccurrenceIndicator("?")))
        }

        argumentTypes = arrayOf(*argumentList.toTypedArray())
        return argumentTypes
    }

    override fun getResultType(sequenceTypes: Array<out SequenceType?>?): SequenceType? {
        return SequenceType.SINGLE_ITEM
    }

    override fun makeCallExpression(): ExtensionFunctionCall? {
        return FunctionCall()
    }

    private fun portSequenceType(binding: PortBindingContainer): SequenceType {
        val seqtype = if (binding.contentTypes.none { it.inclusive }) {
            SequenceType.SINGLE_ITEM
        } else {
            var onlyNodes = true
            for (ctype in binding.contentTypes.filter { it.inclusive }) {
                onlyNodes =
                    onlyNodes && (ctype.classification() in listOf(MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML, MediaClassification.TEXT))
            }
            if (onlyNodes) {
                SequenceType.SINGLE_NODE
            } else {
                SequenceType.SINGLE_ITEM
            }
        }

        if (binding.sequence == true) {
            if (seqtype == SequenceType.SINGLE_ITEM) {
                return SequenceType.ANY_SEQUENCE
            }
            return SequenceType.NODE_SEQUENCE
        }

        return seqtype
    }

    inner class FunctionCall(): ExtensionFunctionCall() {
        private var runtime: XProcRuntime? = null

        override fun call(context: XPathContext, sequences: Array<out Sequence?>?): Sequence? {
            if (runtime == null) {
                runtime = decl.runtime()
            }

            val exec = runtime!!.executable()
            val receiver = BufferingReceiver()
            exec.receiver = receiver

            if (sequences != null) {
                for ((index, input) in inputs.withIndex()) {
                    val items = sequences.elementAt(index)!!.materialize()
                    for (itemindex in 0..<items.length) {
                        val item = items.itemAt(itemindex)
                        if (item is NodeInfo) {
                            val node = XdmNode(item)
                            val doc = XProcDocument.ofXml(node, decl.stepConfig)
                            exec.input(input.port, doc)
                        } else {
                            throw IllegalArgumentException("Failed to configure input: ${item}")
                        }
                    }
                }

                if (sequences.size > inputs.size) {
                    val item = sequences.last()
                    if (item is MapItem) {
                        val map = decl.stepConfig.typeUtils.asMap(decl.stepConfig.typeUtils.forceQNameKeys(item))
                        for ((name, value) in map) {
                            exec.option(name, XProcDocument.ofValue(value, decl.stepConfig))
                        }
                    }
                }
            }

            exec.run()

            var map = XdmMap()
            for ((port, documents) in receiver.outputs) {
                var value: XdmValue = XdmEmptySequence.getInstance()
                for (document in documents) {
                    value = value.append(document.value)
                }
                map = map.put(XdmAtomicValue(port), value)
            }

            return map.underlyingValue
        }
    }
}