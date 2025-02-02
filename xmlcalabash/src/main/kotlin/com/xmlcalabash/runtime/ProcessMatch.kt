package com.xmlcalabash.runtime

import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.NameOfNode
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.serialize.SerializationProperties
import java.net.URI
import javax.xml.xpath.XPathException

class ProcessMatch(val stepConfig: XProcStepConfiguration,
                   val nodeProcessor: ProcessMatchingNodes,
                   val inScopeNamespaces: Map<String,NamespaceUri>,
                   val bindings: Map<QName,XdmValue> = mapOf()): SaxonTreeBuilder(stepConfig) {

    companion object {
        private val SAW_ELEMENT = 1
        private val SAW_WHITESPACE = 2
        private val SAW_TEXT = 4
        private val SAW_PI = 8
        private val SAW_COMMENT = 16
    }

    var _selector: XPathSelector? = null
    val selector: XPathSelector
        get() = _selector ?: throw RuntimeException("Configuration error: selector is null")
    var nodeCount: Int = 0
    private var saw = 0

    fun process(doc: XdmNode, pattern: String) {
        _selector = compilePattern(pattern)
        destination = XdmDestination()
        val pipe = controller.makePipelineConfiguration()
        receiver = destination.getReceiver(pipe, SerializationProperties())

        receiver.setPipelineConfiguration(pipe)
        receiver.setSystemId(doc.baseURI!!.toString())
        receiver.open()

        // If we start a match at an element, fake a document wrapper
        if (doc.nodeKind != XdmNodeKind.DOCUMENT) {
            startDocument(doc.baseURI)
        }

        traverse(doc)

        if (doc.nodeKind != XdmNodeKind.DOCUMENT) {
            endDocument()
        }

        receiver.close()
    }

    // We've already done a bunch of setup, don't do it again
    override fun startDocument(baseURI: URI?) {
        inDocument = true
        seenRoot = false
        receiver.startDocument(0)
    }

    fun count(doc: XdmNode, pattern: String, deep: Boolean): Int {
        _selector = compilePattern(pattern)
        nodeCount = 0
        traverse(doc, deep)
        return nodeCount
    }

    fun matches(node: XdmNode): Boolean {
        try {
            selector.setContextItem(node)
            return selector.effectiveBooleanValue()
        } catch (ex: XPathException) {
            return false
        }
    }

    private fun traverse(node: XdmNode) {
        val nmatch = matches(node)
        var processChildren = false

        if (!nmatch) {
            when (node.nodeKind) {
                XdmNodeKind.ELEMENT -> saw = saw or SAW_ELEMENT
                XdmNodeKind.TEXT -> {
                    if (node.stringValue.trim() == "") {
                        saw = saw or SAW_WHITESPACE
                    } else {
                        saw = saw or SAW_TEXT
                    }
                }
                XdmNodeKind.COMMENT -> saw = saw or SAW_COMMENT
                XdmNodeKind.PROCESSING_INSTRUCTION -> saw = saw or SAW_PI
                else -> Unit
            }
        }

        when (node.nodeKind) {
            XdmNodeKind.DOCUMENT -> {
                if (nmatch) {
                    processChildren = nodeProcessor.startDocument(node)
                    saw = 0
                } else {
                    startDocument(node.baseURI)
                }

                if (!nmatch || processChildren) {
                    traverseChildren(node)
                }

                if (nmatch) {
                    nodeProcessor.endDocument(node)
                } else {
                    endDocument()
                }
            }

            XdmNodeKind.ELEMENT -> {
                var allAttributes = node.underlyingNode.attributes()
                var matchingAttributes: AttributeMap = EmptyAttributeMap.getInstance()
                var nonMatchingAttributes: AttributeMap = EmptyAttributeMap.getInstance()

                for (child in node.axisIterator(Axis.ATTRIBUTE)) {
                    val name = NameOfNode.makeName(child.underlyingNode)
                    val attr = allAttributes.get(name)
                    if (matches(child)) {
                        matchingAttributes = matchingAttributes.put(attr)
                    } else {
                        nonMatchingAttributes = nonMatchingAttributes.put(attr)
                    }
                }

                if (matchingAttributes.size() > 0) {
                    val processed = nodeProcessor.attributes(node, matchingAttributes, nonMatchingAttributes)
                    if (processed != null) {
                        allAttributes = processed
                    }
                }

                if (nmatch) {
                    processChildren = nodeProcessor.startElement(node, allAttributes)
                    saw = 0
                } else {
                    addStartElement(node, allAttributes)
                }


                if (!nmatch || processChildren) {
                    traverseChildren(node)
                }

                if (nmatch) {
                    nodeProcessor.endElement(node)
                } else {
                    addEndElement()
                }
            }

            XdmNodeKind.COMMENT -> {
                if (nmatch) {
                    nodeProcessor.comment(node)
                    saw = 0
                } else {
                    addComment(node.stringValue)
                }
            }

            XdmNodeKind.TEXT -> {
                if (nmatch) {
                    nodeProcessor.text(node)
                    saw = 0
                } else {
                    addText(node.stringValue)
                }
            }

            XdmNodeKind.PROCESSING_INSTRUCTION -> {
                if (nmatch) {
                    nodeProcessor.pi(node)
                    saw = 0
                } else {
                    addPI(node.nodeName.localName, node.stringValue)
                }
            }

            else -> throw UnsupportedOperationException("Unexpected node type: ${node}")
        }
    }

    private fun traverse(node: XdmNode, deep: Boolean) {
        val nmatch = matches(node)

        if (nmatch) {
            nodeCount += 1
        }

        when (node.nodeKind) {
            XdmNodeKind.DOCUMENT -> {
                if (!nmatch || deep) {
                    traverseDeepChildren(node, deep, Axis.CHILD)
                }
            }
            XdmNodeKind.ELEMENT -> {
                if (!nmatch || deep) {
                    traverseDeepChildren(node, deep, Axis.ATTRIBUTE)
                    traverseDeepChildren(node, deep, Axis.CHILD)
                }
            }
            else -> Unit
        }
    }

    private fun traverseChildren(node: XdmNode) {
        for (child in node.axisIterator(Axis.CHILD)) {
            traverse(child)
        }
    }

    private fun traverseDeepChildren(node: XdmNode, deep: Boolean, axis: Axis) {
        for (child in node.axisIterator(axis)) {
            traverse(child, deep)
        }
    }

    /**
     * Compiles a match pattern.
     *
     * This method is public so that the pattern provided to, for example p:viewport, can be
     * checked statically.
     */
    fun compilePattern(pattern: String): XPathSelector {
        val xcomp = stepConfig.newXPathCompiler()

        for ((name, _) in bindings) {
            xcomp.declareVariable(name)
        }

        for ((prefix, uri) in inScopeNamespaces) {
            xcomp.declareNamespace(prefix, uri.toString())
        }

        try {
            val matcher = xcomp.compilePattern(pattern)
            val selector = matcher.load()
            selector.resourceResolver = stepConfig.environment.documentManager

            for ((name, value) in bindings) {
                selector.setVariable(name, value)
            }

            return selector
        } catch (sae: SaxonApiException) {
            val xpe = sae.cause
            when (xpe) {
                is XPathException -> {
                    if (xpe.message!!.contains("Undeclared variable")
                        || xpe.message!!.contains("Cannot find")
                    ) {
                        throw RuntimeException("107: ${pattern}, ${xpe.message}")
                    }
                    throw sae
                }
                else -> throw sae
            }
        }
    }
}

