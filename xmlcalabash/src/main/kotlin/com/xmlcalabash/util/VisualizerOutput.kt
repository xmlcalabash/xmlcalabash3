package com.xmlcalabash.util

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
        fun xml(description: XdmNode, basename: String) {
            val stream = PrintStream(File(basename + ".xml"))
            val serial = XProcSerializer(description.processor)
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

            if (pipeCount == 1) {
                toSvg(graphviz, root, "${basename}.pipeline.svg", "/com/xmlcalabash/pipeline2dot.xsl", 1)
            } else {
                for (count in 1..pipeCount) {
                    toSvg(graphviz, root, "${basename}.pipeline.${count}.svg", "/com/xmlcalabash/pipeline2dot.xsl", count)
                }
            }

            if (graphCount == 1) {
                toSvg(graphviz, root, "${basename}.graph.svg", "/com/xmlcalabash/graph2dot.xsl", 1)
            } else {
                for (count in 1..pipeCount) {
                    toSvg(graphviz, root, "${basename}.graph.${count}.svg", "/com/xmlcalabash/graph2dot.xsl", count)
                }
            }
        }

        private fun toSvg(graphviz: String, node: XdmNode, filename: String, stylesheet: String, number: Int) {
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
            val textResult = RawDestination()
            transformer.applyTemplates(dotxml.asSource(), textResult)

            val tempFile = File.createTempFile("xmlcalabash-", ".dot")
            tempFile.deleteOnExit()

            val dot = PrintStream(tempFile)
            val iter = textResult.xdmValue.iterator()
            while (iter.hasNext()) {
                dot.print(iter.next().stringValue)
            }
            dot.close()

            val rt = Runtime.getRuntime()
            val args = arrayOf(graphviz, "-Tsvg", tempFile.getAbsolutePath().toString(), "-o", filename)
            val process = rt.exec(args)
            process.waitFor()
            tempFile.delete()
        }
    }
}