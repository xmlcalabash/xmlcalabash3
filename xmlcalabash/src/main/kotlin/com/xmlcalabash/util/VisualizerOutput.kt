package com.xmlcalabash.util

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsDescription
import com.xmlcalabash.runtime.XProcDescription
import net.sf.saxon.lib.ResourceRequest
import net.sf.saxon.lib.ResourceResolver
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmDestination
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XsltExecutable
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.InputSource
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintStream
import javax.xml.transform.Source
import javax.xml.transform.sax.SAXSource

class VisualizerOutput(val xmlCalabash: XmlCalabash, val description: XProcDescription, val outputDirectory: String) {
    companion object {
        val resolverPrefix = "https://xmlcalabash.com/xsl/"
        val defaultStyle = "/com/xmlcalabash/graphstyle.xsl"
        val graphDot = "/com/xmlcalabash/graph2dot.xsl"
        val dotText = "/com/xmlcalabash/dot2text.xsl"
        val graphvizIndex = "/com/xmlcalabash/graphviz.xsl"
        val graphvizSvg = "/com/xmlcalabash/graphsvg.xsl"
    }

    private var cleanupPerformed = false
    private val debug = xmlCalabash.config.debug

    fun xml() {
        try {
            cleanupOutputDirectory()
            writeNode("${outputDirectory}pipeline.xml", description.xml())
        } catch (ex: Exception) {
            logger.warn("SVG generation failed: ${ex.message}")
        }
    }

    private fun writeNode(filename: String, node: XdmNode) {
        val stream = PrintStream(File(filename))
        val tempDoc = XProcDocument.ofXml(node, description.stepConfig)
        val writer = DocumentWriter(tempDoc, stream)

        if (S9Api.isTextDocument(node)) {
            writer[Ns.method] = "text"
        } else {
            if (S9Api.documentElement(node).nodeName.localName == "html") {
                writer[Ns.method] = "html"
                writer[Ns.htmlVersion] = "5"
                writer[Ns.indent] = true
            } else {
                writer[Ns.method] = "xml"
                writer[Ns.indent] = true
            }
        }

        writer.write()
    }

    fun svg() {
        try {
            do_svg()
        } catch (ex: Exception) {
            logger.warn("SVG generation failed: ${ex.message}")
        }
    }

    private fun do_svg() {
        cleanupOutputDirectory()

        // This has to be first because the description is mutable.
        // Slightly funny code smell, but I'm not going to fuss about
        // it in the visualizer output...
        graphvizIndex()

        styleDescription()
        dotDescription()
        graphvizDescription()
    }

    private fun styleDescription() {
        val styleStream = VisualizerOutput::class.java.getResourceAsStream(defaultStyle)
        transformDescription(SAXSource(InputSource(styleStream)), ".xml",
            params = mapOf(QName("show-thread-groups") to XdmAtomicValue(xmlCalabash.config.maxThreadCount > 1)))

        if (debug) {
            val builder = SaxonTreeBuilder(xmlCalabash.config.saxonConfiguration.processor)
            builder.startDocument(null)
            builder.addStartElement(NsDescription.g("description"))
            for (pipeline in description.pipelines) {
                builder.addSubtree(pipeline)
            }
            for (graph in description.graphs) {
                builder.addSubtree(graph)
            }
            builder.addEndElement()
            builder.endDocument()
            writeNode("${outputDirectory}pipeline.xml", builder.result)
        }

        if (xmlCalabash.config.graphStyle != null) {
            val source = SAXSource(InputSource(xmlCalabash.config.graphStyle!!.toString()))
            transformDescription(source, ".xml")
        }
    }

    private fun dotDescription() {
        var styleStream = VisualizerOutput::class.java.getResourceAsStream(graphDot)
        transformDescription(SAXSource(InputSource(styleStream)), ".dot.xml")

        styleStream = VisualizerOutput::class.java.getResourceAsStream(dotText)
        transformDescription(SAXSource(InputSource(styleStream)), ".dot", store=true)
    }

    private fun transformDescription(stylesheet: SAXSource, ext: String, params: Map<QName,XdmAtomicValue> = emptyMap(), store: Boolean = debug) {
        val xsltCompiler = description.stepConfig.processor.newXsltCompiler()
        xsltCompiler.isSchemaAware = description.stepConfig.processor.isSchemaAware
        xsltCompiler.resourceResolver = VisualizerResourceResolver()
        val xsltExec = xsltCompiler.compile(stylesheet)

        val styledPipelines = mutableListOf<XdmNode>()
        val styledGraphs = mutableListOf<XdmNode>()

        for (pipeline in description.pipelines) {
            val result = transform(xsltExec, pipeline,
                filename = if (store) "${outputDirectory}pipelines/${description.pipelineName(pipeline)}${ext}" else null,
                params = params)
            styledPipelines.add(result)
        }

        for (graph in description.graphs) {
            val result = transform(xsltExec, graph,
                filename = if (store) "${outputDirectory}graphs/${description.graphName(graph)}${ext}" else null,
                params = params)
            styledGraphs.add(result)
        }

        description.replacePipelines(styledPipelines)
        description.replaceGraphs(styledGraphs)
    }

    private fun graphvizIndex() {
        var styleStream = VisualizerOutput::class.java.getResourceAsStream(graphvizIndex)
        val styleSource = SAXSource(InputSource(styleStream))

        var xsltCompiler = description.stepConfig.processor.newXsltCompiler()
        xsltCompiler.isSchemaAware = description.stepConfig.processor.isSchemaAware
        xsltCompiler.resourceResolver = VisualizerResourceResolver()
        val xsltExec = xsltCompiler.compile(styleSource)
        transform(xsltExec, description.xml(), "${outputDirectory}/index.html",
            mapOf(Ns.version to XdmAtomicValue(description.stepConfig.environment.productVersion)))
    }

    private fun transform(xsltExec: XsltExecutable, source: XdmNode, filename: String?, params: Map<QName, XdmAtomicValue> = emptyMap()): XdmNode {
        val transformer = xsltExec.load30()
        transformer.setStylesheetParameters(params)
        val xmlResult = XdmDestination()
        transformer.globalContextItem = source
        transformer.applyTemplates(source, xmlResult)
        val result = xmlResult.xdmNode
        filename?.let { writeNode(it, result) }
        return result
    }

    private fun graphvizDescription() {
        for (pipeline in description.pipelines) {
            graphviz("${outputDirectory}pipelines/", description.pipelineName(pipeline))
        }
        for (graph in description.graphs) {
            graphviz("${outputDirectory}graphs/", description.graphName(graph))
        }
    }

    private fun graphviz(path: String, basename: String) {
        val rt = Runtime.getRuntime()
        val graphviz = xmlCalabash.config.graphviz!!.absolutePath

        val dotFile = File("${path}${basename}.dot")
        val svgFile = File("${path}${basename}.svg")

        val args = arrayOf(graphviz, "-Tsvg", dotFile.absolutePath, "-o", svgFile.absolutePath)
        val process = rt.exec(args)
        process.waitFor()
        if (process.exitValue() != 0) {
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            var line = errorReader.readLine()
            while (line != null) {
                logger.debug("ERR: $line")
                line = errorReader.readLine()
            }

            val outputReader = BufferedReader(InputStreamReader(process.inputStream))
            line = outputReader.readLine()
            while (line != null) {
                logger.debug("OUT: $line")
                line = outputReader.readLine()
            }

            logger.warn { "Graph generation failed for $basename" }
        }

        var styleStream = VisualizerOutput::class.java.getResourceAsStream(graphvizSvg)
        val styleSource = SAXSource(InputSource(styleStream))

        var xsltCompiler = description.stepConfig.processor.newXsltCompiler()
        xsltCompiler.isSchemaAware = description.stepConfig.processor.isSchemaAware
        xsltCompiler.resourceResolver = VisualizerResourceResolver()
        val xsltExec = xsltCompiler.compile(styleSource)

        val source = SAXSource(InputSource(svgFile.absolutePath))
        val builder = description.stepConfig.processor.newDocumentBuilder()
        val xml = builder.build(source)

        transform(xsltExec, xml, "${path}${basename}.html",
            mapOf(
                Ns.version to XdmAtomicValue(description.stepConfig.environment.productVersion),
                QName("basename") to XdmAtomicValue(basename),
                QName("output") to XdmAtomicValue(if (path.contains("/pipelines/")) "pipeline" else "graph")
            ))

        if (!debug) {
            dotFile.delete()
        }
    }

    private fun cleanupOutputDirectory() {
        if (cleanupPerformed) {
            return
        }

        if (!outputDirectory.endsWith("/")) {
            throw IllegalStateException("Output directory does not end with /: ${outputDirectory}")
        }

        val root = File(outputDirectory)
        val index = root.resolve("index.html")
        if (index.exists() && !index.delete()) {
            throw IllegalStateException("Failed to erase ${index}")
        }
        val pipeline = root.resolve("pipeline.xml")
        if (pipeline.exists() && !pipeline.delete()) {
            throw IllegalStateException("Failed to erase ${pipeline}")
        }

        val pipelinesDir = File("${outputDirectory}pipelines")
        if (pipelinesDir.exists() && !pipelinesDir.deleteRecursively()) {
            throw IllegalStateException("Failed to erase ${pipelinesDir}")
        }
        if (!pipelinesDir.mkdirs()) {
            throw IllegalStateException("Failed to create ${pipelinesDir}")
        }

        val graphsDir = File("${outputDirectory}/graphs")
        if (graphsDir.exists() && !graphsDir.deleteRecursively()) {
            throw IllegalStateException("Failed to delete ${graphsDir}")
        }
        if (!graphsDir.mkdirs()) {
            throw IllegalStateException("Failed to create ${graphsDir}")
        }
        if (!graphsDir.isDirectory) {
            throw IllegalStateException("Not a directory: ${graphsDir}")
        }
    }

    private class VisualizerResourceResolver : ResourceResolver {
        override fun resolve(request: ResourceRequest?): Source? {
            if (request == null) {
                return null
            }

            if (!request.uri.startsWith(resolverPrefix)) {
                return null;
            }

            val style = request.uri.substring(resolverPrefix.length)
            if (style.contains("/")) {
                throw XProcError.xiImpossible("Request for graphviz unexpected stylesheet: ${request.uri}")
                    .exception()
            }

            val resource = "/com/xmlcalabash/${style}"
            var styleStream = VisualizerOutput::class.java.getResourceAsStream(resource)
            return SAXSource(InputSource(styleStream))
        }
    }
}
