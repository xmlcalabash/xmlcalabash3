package com.xmlcalabash.steps

import com.xmlcalabash.documents.DocumentContext
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.Item
import net.sf.saxon.s9api.*
import net.sf.saxon.tree.iter.ManualIterator
import java.net.URI

open class WrapSequenceStep(): AbstractAtomicStep() {
    private val documents = mutableListOf<XProcDocument>()
    private lateinit var wrapperName: QName
    private lateinit var attributeMap: AttributeMap
    private var groupAdjacent: String? = null
    private var groupAdjacentContext: DocumentContext? = null
    private var baseUri: URI? = null
    private var index = 0

    override fun run() {
        super.run()

        baseUri = stepConfig.baseUri
        documents.addAll(queues["source"]!!)
        wrapperName = qnameBinding(Ns.wrapper)!!
        groupAdjacent = stringBinding(Ns.groupAdjacent)

        val attributeSet = mutableMapOf<QName,String?>()
        val attrMap = qnameMapBinding(Ns.attributes)
        for ((key, value) in attrMap) {
            forbidNamespaceAttribute(key)
            attributeSet[key] = (value as XdmAtomicValue).underlyingValue.stringValue
            if (key == NsXml.base) {
                baseUri = URI(value.underlyingValue.stringValue)
            }
        }
        attributeMap = stepConfig.attributeMap(attributeSet)

        if (groupAdjacent == null) {
            runSimple()
        } else {
            runAdjacent()
        }
    }

    override fun reset() {
        super.reset()
        documents.clear()
        index = 0
    }

    private fun runSimple() {
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(baseUri)
        builder.addStartElement(wrapperName, attributeMap)
        for (doc in documents) {
            if (doc.value is XdmItem) {
                builder.addSubtree(doc.value)
            } else {
                throw RuntimeException("Attempt to wrap non item?")
            }
        }
        builder.addEndElement()
        builder.endDocument()

        receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig))
    }

    private fun runAdjacent() {
        var inGroup = false
        var lastValue: XdmValue? = null
        var builder: SaxonTreeBuilder? = null
        groupAdjacentContext = options[Ns.groupAdjacent]!!.context

        for (doc in documents) {
            index++
            val thisValue = adjacentValue(doc)
            var equal = false

            if (lastValue != null) {
                equal = stepConfig.xpathDeepEqual(lastValue, thisValue)
                if (equal) {
                    builder!!.addSubtree(doc.value)
                } else {
                    if (inGroup) {
                        inGroup = false
                        builder!!.addEndElement()
                        builder.endDocument()
                        receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig))
                    }
                }
            }

            if (lastValue == null || !equal) {
                lastValue = thisValue
                inGroup = true
                builder = SaxonTreeBuilder(stepConfig)
                builder.startDocument(stepConfig.environment.uniqueUri(baseUri.toString()))
                builder.addStartElement(wrapperName, attributeMap)
                builder.addSubtree(doc.value)
            }
        }

        if (inGroup) {
            builder!!.addEndElement()
            builder.endDocument()
            receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig))
        }
    }

    private fun adjacentValue(doc: XProcDocument): XdmValue {
        val compiler = stepConfig.newXPathCompiler()
        compiler.baseURI = groupAdjacentContext!!.baseUri
        for ((prefix, uri) in groupAdjacentContext!!.inscopeNamespaces) {
            compiler.declareNamespace(prefix, uri.toString())
        }
        val exec = compiler.compile(groupAdjacent!!)
        val expr = exec.underlyingExpression

        val dyncontext = expr.createDynamicContext()
        val context = dyncontext.xPathContextObject

        val fakeIterator = ManualIterator(doc.value.underlyingValue as Item, index)
        fakeIterator.setLengthFinder { -> documents.size }
        context.currentIterator = fakeIterator

        var result: XdmValue = XdmEmptySequence.getInstance()
        val iter = expr.iterate(dyncontext)
        var item = iter.next()
        while (item != null) {
            result = result.append(XdmValue.wrap(item))
            item = iter.next()
        }

        return result
    }

    override fun toString(): String = "p:wrap-sequence"


}