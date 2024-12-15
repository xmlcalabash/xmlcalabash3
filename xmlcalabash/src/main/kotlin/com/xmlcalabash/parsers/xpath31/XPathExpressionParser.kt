package com.xmlcalabash.parsers.xpath31

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.parsers.XPath31
import com.xmlcalabash.parsers.XPath31.EventHandler
import com.xmlcalabash.parsers.XPathExpressionDetails
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmDestination
import net.sf.saxon.s9api.Xslt30Transformer
import org.xml.sax.InputSource
import javax.xml.transform.sax.SAXSource

class XPathExpressionParser(val stepConfig: XProcStepConfiguration) {
    companion object {
        private val TRACE = false
        private val DEBUG = false
        private val nsNt = NamespaceUri.of("https://xmlcalabash.com/ns/nonterminal")
        private val nsT = NamespaceUri.of("https://xmlcalabash.com/ns/terminal")

        // FIXME: what's the right list here?
        val contextDependentFunctions = setOf(
            NsFn.collection, NsFn.baseUri)

        val alwaysDynamicFunctions = setOf(
            NsP.systemProperty, NsP.iterationPosition, NsP.iterationSize, NsP.documentProperties,
            NsP.documentPropertiesDocument, NsP.documentProperty,
            NsFn.currentDate, NsFn.currentDateTime, NsFn.currentTime,
            NsFn.doc, NsFn.docAvailable, NsFn.document, NsFn.unparsedText,
            NsFn.unparsedTextAvailable)
    }

    private val transformer = stepConfig.saxonConfig.xpathTransformer

    fun parse(expr: String): XPathExpressionDetails {
        var error: Exception? = null
        val handler = FindRefs(TRACE)
        handler.initialize()

        val variables = mutableSetOf<QName>()
        var usesContext = false
        var alwaysDynamic = false
        val functions = mutableSetOf<Pair<QName,Int>>()

        val parser = XPath31(expr, handler)
        try {
            if (DEBUG) {
                println(expr)
            }

            parser.parse_XPath()
            val lines = handler.walkTree().split("\n")
            for (line in lines) {
                val pos = line.indexOf("}")
                val sppos = line.indexOf(" ")

                if (line.startsWith("f")) {
                    val ns = if (pos == 3) NsFn.namespace else NamespaceUri.of(line.substring(3, pos))
                    val eqname = line.substring(pos+1, sppos)

                    val hashpos = eqname.indexOf("#")
                    val arity = eqname.substring(hashpos+1).toInt(10)
                    val local = eqname.substring(0, hashpos)
                    val qname = QName(ns, local)

                    usesContext = usesContext || contextDependentFunctions.contains(qname)
                    alwaysDynamic = alwaysDynamic || alwaysDynamicFunctions.contains(qname)
                    functions.add(Pair(qname, arity))
                    if (DEBUG) {
                        println("  ${qname}#${arity}()")
                    }
                } else if (line.startsWith("v")) {
                    val local = line.substring(pos+1, sppos)
                    val ns = NamespaceUri.of(line.substring(3, pos))
                    val qname = QName(ns, local)
                    variables.add(qname)
                    if (DEBUG) {
                        println("  \$${qname}")
                    }
                } else if (line.startsWith(".")) {
                    usesContext = usesContext || (line == ".true")
                }
            }
            if (DEBUG) {
                println("  ${if (usesContext) "Uses context" else "Does not use the context"}")
            }
        } catch (ex: Exception) {
            if (DEBUG) {
                println("  Exception: ${ex.message}")
            }
            error = ex
        }

        if (TRACE) {
            println("parsed expression")
        }

        return XPathExpressionDetails(error, variables, functions, usesContext, alwaysDynamic)
    }

    inner class FindRefs(val trace: Boolean): EventHandler {
        private var input: String = ""
        private val root = Root()
        private var node: Tree = root
        var nsmap: NamespaceMap = NamespaceMap.emptyMap()

        init {
            nsmap = nsmap.put("nt", nsNt)
            nsmap = nsmap.put("t", nsT)
        }

        fun initialize() {
            input = ""
        }

        override fun reset(string: CharSequence?) {
            initialize()
            if (trace) {
                println("Parser reset: ${string}")
            }
            input = string?.toString() ?: ""
        }

        override fun startNonterminal(name: String?, begin: Int) {
            if (trace) {
                println("+NT: ${name}")
            }
            val child = Nonterminal(name!!, node)
            node.children.add(child)
            node = child
        }

        override fun endNonterminal(name: String?, end: Int) {
            if (trace) {
                println("-NT: ${name}")
            }

            if (name == "Literal") {
                node.children.clear()
            } else {
                if (node.squash) {
                    val child = node.children[0]
                    node.parent!!.children.removeLast()
                    node.parent!!.children.add(child)
                    child.parent = node.parent!!
                }
            }

            if (name == "FunctionName" || name == "FunctionEQName") {
                // The nonterminal FunctionName is an artifact of the REx grammar
                val child = node.children[0]
                node.parent!!.children.removeLast()
                node.parent!!.children.add(child)
                child.parent = node.parent!!
            }

            node = node.parent!!
        }

        override fun terminal(name: String?, begin: Int, end: Int) {
            val token = characters(begin, end)
            if (trace) {
                println("  T: ${name} (${token} at ${begin})")
            }

            if (node is Nonterminal && ((node as Nonterminal).name == "QName" || (node as Nonterminal).name == "FunctionName")) {
                // The grammar treats language keywords in this context as terminals;
                // turn them back into QNames because they aren't terminals.
                val child = Terminal("QName", token, node)
                node.children.add(child)
                return
            }

            // Bit of a hack...
            var isName = false
            if (node.parent is Nonterminal) {
                val parent = node.parent as Nonterminal
                if (parent.parent is Nonterminal) {
                    val gparent = parent.parent as Nonterminal
                    isName = parent.name == "EQName"
                }
            }

            if (!name!!.startsWith("'") || isName) {
                val child = Terminal(name, token, node)
                node.children.add(child)
            }
        }

        override fun whitespace(begin: Int, end: Int) {
            // nop
        }

        private fun characters(begin: Int, end: Int): String =
            if (begin < end) {
                input.substring(begin, end)
            } else {
                ""
            }

        fun walkTree(): String {
            val builder = SaxonTreeBuilder(stepConfig)
            builder.startDocument(null)
            walk(root.children.first(), builder)
            builder.endDocument()

            if (DEBUG) {
                println(builder.result.toString())
            }

            val xmlResult = XdmDestination()
            transformer.applyTemplates(builder.result, xmlResult)
            return xmlResult.xdmNode.toString()
        }

        private fun walk(node: Tree, builder: SaxonTreeBuilder) {
            if (node is Nonterminal) {
                builder.addStartElement(QName(nsNt, "nt:" + node.name), EmptyAttributeMap.getInstance(), nsmap)
                for (child in node.children) {
                    walk(child, builder)
                }
                builder.addEndElement()
            } else {
                val terminal = node as Terminal
                when (terminal.name) {
                    "QName" -> {
                        val pos = terminal.token.indexOf(":")
                        val prefix = if (pos > 0) terminal.token.substring(0, pos) else ""
                        val localName = if (pos > 0) terminal.token.substring(pos+1) else terminal.token
                        val ns = if (prefix == "") "" else stepConfig.inscopeNamespaces[prefix]!!.toString()
                        val atts = stepConfig.stringAttributeMap(mapOf("name" to "Q{${encodeForUri(ns)}}${localName}"))
                        builder.addStartElement(QName(nsT, "t:" + terminal.name), atts, nsmap)
                        builder.addText(terminal.token)
                        builder.addEndElement()
                    }
                    "URIQualifiedName" -> {
                        val pos = terminal.token.indexOf("}")
                        val ns = terminal.token.substring(2, pos)
                        val localName = terminal.token.substring(pos+1)
                        val atts = stepConfig.stringAttributeMap(mapOf("name" to "Q{${encodeForUri(ns)}}${localName}"))
                        builder.addStartElement(QName(nsT, "t:QName"), atts, nsmap)
                        builder.addText(localName)
                        builder.addEndElement()
                    }
                    else -> {
                        val atts = stepConfig.stringAttributeMap(mapOf("token" to terminal.token))
                        builder.addStartElement(QName(nsT, "t:" + terminal.name), atts, nsmap)
                        builder.addEndElement()
                    }
                }
            }
        }

        private fun encodeForUri(ns: String): String {
            return ns.replace(" ", "%20")
        }
    }

    private abstract class Tree {
        abstract val squash: Boolean
        var parent: Tree? = null
        val children = mutableListOf<Tree>()
    }

    private class Root(): Tree() {
        override val squash = false
        override fun toString(): String {
            return "ROOT"
        }
    }

    private class Nonterminal(val name: String, parent: Tree): Tree() {
        override val squash: Boolean
            get() {
                return (name != "VarRef" && name != "ArgumentList" && name != "ParamList" && name != "UnaryLookup"
                        && children.size == 1 && children.first() is Nonterminal)
                        || name == "QName" || name == "EQName"
            }

        init {
            this.parent = parent
        }
        override fun toString(): String {
            return name
        }
    }

    private class Terminal(val name: String, val token: String, parent: Tree): Tree() {
        override val squash = false
        init {
            this.parent = parent
        }
        override fun toString(): String {
            return "${name}/${token}"
        }
    }
}