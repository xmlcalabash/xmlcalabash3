package com.xmlcalabash.ext.rr

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsHtml
import com.xmlcalabash.namespace.NsSvg
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.SaxonTreeBuilder
import de.bottlecaps.railroad.RailroadGenerator
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmArray
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmDestination
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import org.xml.sax.InputSource
import java.awt.Color
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.swing.text.Document
import javax.xml.transform.sax.SAXSource
import kotlin.collections.iterator

class RailroadStep(): AbstractAtomicStep() {
    companion object {
        val _nonterminal = QName("nonterminal")
        val _color = QName("color")
        val _colorOffset = QName("color-offset")
        val _padding = QName("padding")
        val _strokeWidth = QName("stroke-width")
        val _width = QName("width")
        val _eliminateRecursion = QName("eliminate-recursion")
        val _factoring = QName("factoring")
        val _inlineLiterals = QName("inline-literals")
        val _keepEpsilonNonterminals = QName("keep-epsilon-nonterminals")
    }

    lateinit var source: XProcDocument
    var nonterminal: String? = null
    var found = false

    override fun run() {
        super.run()

        source = queues["source"]!!.first()
        val ebnf = source.value.underlyingValue.stringValue

        nonterminal = stringBinding(_nonterminal)
        val pcolor = (stringBinding(_color) ?: "#FFDB4D").trim()
        var colorOffset = integerBinding(_colorOffset) ?: 0
        val width = integerBinding(_width) ?: 992
        val recursion = booleanBinding(_eliminateRecursion) ?: true
        val factoring = booleanBinding(_factoring) ?: true
        val inlineLiterals = booleanBinding(_inlineLiterals) ?: true
        val keepEpsilons = booleanBinding(_keepEpsilonNonterminals) ?: true

        if (width <= 0) {
            throw stepConfig.exception(XProcError.xcxInvalidWidth(width))
        }

        if (colorOffset < 0 || colorOffset > 359) {
            throw stepConfig.exception(XProcError.xcxOffsetOutOfRange(colorOffset))
        }

        try {
            val generator = RailroadGenerator()
            val baos = ByteArrayOutputStream()
            generator.setOutput(baos)
            generator.setEmbedded(true)

            val baseColor = when(pcolor) {
                "white" -> Color.white
                "lightGray" -> Color.lightGray
                "gray" -> Color.gray
                "darkGray" -> Color.darkGray
                "black" -> Color.black
                "red" -> Color.red
                "pink" -> Color.pink
                "orange" -> Color.orange
                "yellow" -> Color.yellow
                "green" -> Color.green
                "magenta" -> Color.magenta
                "cyan" -> Color.cyan
                "blue" -> Color.blue
                else -> {
                    if (pcolor.startsWith("#")) {
                        parseHex(pcolor)
                    } else {
                        throw stepConfig.exception(XProcError.xcxInvalidHexColor(pcolor))
                    }
                }
            }

            generator.setWidth(width)
            generator.setBaseColor(baseColor)
            generator.setColorOffset(colorOffset)
            generator.setRecursionElimination(recursion)
            generator.setFactoring(factoring)
            generator.setInlineLiterals(inlineLiterals)
            generator.setKeepEpsilon(keepEpsilons)

            val colorOffset = integerBinding(_colorOffset)
            if (colorOffset != null) {
                generator.setColorOffset(colorOffset)
            }

            val padding = integerBinding(_padding)
            if (padding != null) {
                generator.setPadding(padding)
            }

            val strokeWidth = integerBinding(_strokeWidth)
            if (strokeWidth != null) {
                generator.setStrokeWidth(strokeWidth)
            }

            generator.generate(ebnf)

            val bais = ByteArrayInputStream(baos.toByteArray())
            val docbuilder = stepConfig.processor.newDocumentBuilder()
            val destination = XdmDestination()
            val svgsource = SAXSource(InputSource(bais))
            docbuilder.parse(svgsource, destination)
            val html = destination.xdmNode
            receiver.output("html", XProcDocument.ofXml(html, stepConfig, MediaType.HTML, DocumentProperties()))

            val nodes = getSvg(html)
            if (nodes.size < 3) {
                stepConfig.warn { "Unexpected SVG output, ${nodes.size} SVG elements" }
                return
            }

            val defs = getSvgDefs(nodes[0])
            outputSvg(defs, nodes.subList(1, nodes.size - 1))

            if (!found && nonterminal != null) {
                throw stepConfig.exception(XProcError.xcxNonterminalNotFound(nonterminal!!))
            }
        } catch (ex: Exception) {
            if (ex is XProcException) {
                throw ex
            }
            throw stepConfig.exception(XProcError.xdStepFailed(ex.message ?: "???"), ex)
        }
    }

    private fun outputSvg(defs: XdmNode, diagrams: List<XdmNode>) {
        for (node in diagrams) {
            val builder = SaxonTreeBuilder(stepConfig)
            builder.startDocument(source.baseURI)
            builder.addStartElement(node)
            builder.addSubtree(defs)
            for (node in node.axisIterator(Axis.CHILD)) {
                builder.addSubtree(node)
            }
            builder.addEndElement()
            builder.endDocument()

            val props = getProperties(node)
            val docprops = DocumentProperties()
            for ((name, value) in props) {
                docprops.set(QName(NsCx.namespace, "cx:${name}"), value)
            }

            if (nonterminal == null || nonterminal == props["nonterminal"]?.underlyingValue?.stringValue) {
                found = true
                val result = builder.result
                receiver.output("result", XProcDocument.ofXml(result, stepConfig, MediaType.SVG, docprops))
            }
        }
    }

    private fun getSvg(html: XdmNode): List<XdmNode> {
        val compiler = stepConfig.newXPathCompiler()
        compiler.declareNamespace("svg", NsSvg.namespace.toString())
        val exec = compiler.compile("//svg:svg")
        val selector = exec.load()
        selector.contextItem = html

        val nodes = mutableListOf<XdmNode>()
        val iter = selector.evaluate().iterator()
        while (iter.hasNext()) {
            nodes.add(iter.next() as XdmNode)
        }

        return nodes
    }

    private fun getSvgDefs(svg: XdmNode): XdmNode {
        val compiler = stepConfig.newXPathCompiler()
        compiler.declareNamespace("svg", NsSvg.namespace.toString())
        val exec = compiler.compile("svg:defs")
        val selector = exec.load()
        selector.contextItem = svg

        val iter = selector.evaluate().iterator()
        while (iter.hasNext()) {
            return iter.next() as XdmNode
        }

        throw stepConfig.exception(XProcError.xcxNoStyleDefinitions())
    }

    private fun parseHex(hex: String): Color {
        val array = hex.toCharArray()
        val digits = if (hex.length == 4) {
            "${array[1]}${array[1]}${array[2]}${array[2]}${array[3]}${array[3]}".uppercase()
        } else if (hex.length == 7) {
            hex.substring(1).uppercase()
        } else {
            throw stepConfig.exception(XProcError.xcxInvalidHexColor(hex))
        }

        val r = hexToInt(digits.substring(0, 2))
        val g = hexToInt(digits.substring(2, 4))
        val b = hexToInt(digits.substring(4, 6))
        return Color(r, g, b)
    }

    private fun hexToInt(hex: String): Int {
        val array = hex.toCharArray()
        val alphaBase = 'A' - 10
        var power = 1
        var decimal = 0
        for (index in hex.length - 1 downTo 0) {
            val digit = array[index]
            if (digit in '0'..'9') {
                decimal += (digit - '0') * power
            } else if (digit in 'A'..'F') {
                decimal += (digit - alphaBase) * power
            } else {
                throw stepConfig.exception(XProcError.xcxInvalidHexColor(hex))
            }
            power = power * 16
        }

        return decimal
    }

    private fun getProperties(svg: XdmNode): Map<String, XdmValue> {
        val properties = mutableMapOf<String, XdmValue>()

        val compiler = stepConfig.newXPathCompiler()
        compiler.declareNamespace("svg", NsSvg.namespace.toString())
        compiler.declareNamespace("h", NsHtml.namespace.toString())
        var exec = compiler.compile("preceding-sibling::h:p[1]/h:a")
        var selector = exec.load()
        selector.contextItem = svg

        var iter = selector.evaluate().iterator()
        if (iter.hasNext()) {
            val anchor = iter.next()
            val label = anchor.underlyingValue.stringValue
            if (label.endsWith(":")) {
                properties["nonterminal"] = XdmAtomicValue(label.substring(0, label.length - 1))
            } else {
                if (label != "") {
                    properties["nonterminal"] = XdmAtomicValue(label)
                }
            }
        }

        exec = compiler.compile("following-sibling::h:p[1]/h:div")
        selector = exec.load()
        selector.contextItem = svg
        iter = selector.evaluate().iterator()
        if (iter.hasNext()) {
            val div = iter.next()
            properties["ebnf"] = div
        }

        exec = compiler.compile("following-sibling::h:p[2]//h:li/h:a")
        selector = exec.load()
        selector.contextItem = svg
        iter = selector.evaluate().iterator()
        if (iter.hasNext()) {
            var refs = XdmArray()
            while (iter.hasNext()) {
                val anchor = iter.next()
                refs = refs.addMember(XdmAtomicValue(anchor.underlyingValue.stringValue))
            }
            if (!refs.isEmpty) {
                properties["referenced-by"] = refs
            }
        }

        return properties
    }

    override fun toString(): String = "cx:railroad"
}