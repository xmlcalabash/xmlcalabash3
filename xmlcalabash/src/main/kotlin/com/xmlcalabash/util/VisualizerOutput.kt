package com.xmlcalabash.util

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsDescription
import net.sf.saxon.s9api.*
import org.xml.sax.InputSource
import java.io.File
import java.io.PrintStream
import javax.xml.transform.sax.SAXSource

class VisualizerOutput {
    companion object {
        fun xml(xmlCalabash: XmlCalabash, description: XdmNode, basename: String) {
            val stream = PrintStream(File(basename + ".xml"))
            val serial = XProcSerializer(xmlCalabash, description.processor)
            val props = mutableMapOf<QName, XdmValue>()
            props[Ns.method] = XdmAtomicValue("xml")
            props[Ns.indent] = XdmAtomicValue(true)
            serial.write(description, stream, props)
        }

        fun svg(description: XdmNode, basename: String, graphviz: String) {
            val root = S9Api.documentElement(description)
            var pipeCount = 0
            var graphCount = 0

            for (child in root.axisIterator(Axis.CHILD)) {
                if (child.nodeKind == XdmNodeKind.ELEMENT) {
                    when (child.nodeName) {
                        NsDescription.declareStep -> pipeCount++
                        NsDescription.graph -> graphCount++
                    }
                }
            }

            val dot = mutableListOf<String>()
            for (count in 1..pipeCount) {
                dot.add(toDot(root, "/com/xmlcalabash/pipeline2dot.xsl", count))
            }
            dotSvg(graphviz, dot, "${basename}.pipeline.svg")

            dot.clear()
            for (count in 1..pipeCount) {
                dot.add(toDot(root, "/com/xmlcalabash/graph2dot.xsl", count))
            }
            dotSvg(graphviz, dot, "${basename}.graph.svg")
        }

        private fun toDot(node: XdmNode, stylesheet: String, number: Int): String {
            val builder = SaxonTreeBuilder(node.processor)
            builder.startDocument(null)
            builder.addSubtree(node)
            builder.endDocument()
            val desc = builder.result

            var styleStream = VisualizerOutput::class.java.getResourceAsStream(stylesheet)
            var styleSource = SAXSource(InputSource(styleStream))
            var xsltCompiler = desc.processor.newXsltCompiler()
            var xsltExec = xsltCompiler.compile(styleSource)

            var transformer = xsltExec.load30()
            transformer.setStylesheetParameters(mapOf(Ns.number to XdmAtomicValue(number)))
            val xmlResult = XdmDestination()
            transformer.applyTemplates(desc.asSource(), xmlResult)
            val dotxml = xmlResult.xdmNode

            styleStream = VisualizerOutput::class.java.getResourceAsStream("/com/xmlcalabash/dot2txt.xsl")
            styleSource = SAXSource(InputSource(styleStream))
            xsltCompiler = desc.processor.newXsltCompiler()
            xsltExec = xsltCompiler.compile(styleSource)

            transformer = xsltExec.load30()
            val textResult = XdmDestination()
            transformer.applyTemplates(dotxml.asSource(), textResult)

            return textResult.xdmNode.underlyingNode.stringValue
        }

        private fun dotSvg(graphviz: String, dotFiles: List<String>, filename: String) {
            val tempFile = File.createTempFile("xmlcalabash-", ".dot")
            tempFile.deleteOnExit()

            val dot = PrintStream(tempFile)
            dot.println("digraph {")
            dot.println("compound=true")
            dot.println("rankdir=TB")
            dot.println()
            for (text in dotFiles) {
                dot.println(text)
            }
            dot.println("}")

            val rt = Runtime.getRuntime()
            val args = arrayOf(graphviz, "-Tsvg", tempFile.getAbsolutePath().toString(), "-o", filename)
            val process = rt.exec(args)
            process.waitFor()
            tempFile.delete()
        }
    }
}