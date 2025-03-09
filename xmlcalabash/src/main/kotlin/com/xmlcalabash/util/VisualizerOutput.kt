package com.xmlcalabash.util

import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.config.XmlCalabashConfiguration
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsDescription
import net.sf.saxon.lib.ResourceRequest
import net.sf.saxon.lib.ResourceResolver
import net.sf.saxon.s9api.*
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.InputSource
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintStream
import javax.xml.transform.Source
import javax.xml.transform.sax.SAXSource
import kotlin.io.path.toPath

class VisualizerOutput {
    companion object {
        fun xml(xmlCalabash: XmlCalabash, description: XdmNode, basename: String) {
            val stream = PrintStream(File(basename + ".xml"))
            val builder = xmlCalabash.newPipelineBuilder()
            val tempDoc = XProcDocument.ofXml(description, builder.stepConfig)
            val writer = DocumentWriter(tempDoc, stream)
            writer[Ns.method] = "xml"
            writer[Ns.indent] = true
            writer.write()
        }

        fun svg(node: XdmNode, outdir: String, xmlcalabashConfig: XmlCalabashConfiguration, debug: Boolean = false) {
            val graphviz = xmlcalabashConfig.graphviz!!.absolutePath

            var xsltCompiler = node.processor.newXsltCompiler()

            var source = if (xmlcalabashConfig.graphStyle == null) {
                node
            } else {
                val graphStyleSource = SAXSource(InputSource(xmlcalabashConfig.graphStyle!!.toString()))
                xsltCompiler.isSchemaAware = node.processor.isSchemaAware
                xsltCompiler.resourceResolver = VisualizerResourceResolver()
                val xsltExec = xsltCompiler.compile(graphStyleSource)
                var transformer = xsltExec.load30()
                transformer.globalContextItem = node
                val xmlResult = XdmDestination()
                transformer.applyTemplates(node.asSource(), xmlResult)
                xmlResult.xdmNode
            }

            val stylesheet = "/com/xmlcalabash/graphviz.xsl"
            var styleStream = VisualizerOutput::class.java.getResourceAsStream(stylesheet)
            var styleSource = SAXSource(InputSource(styleStream))
            styleSource.systemId = "/com/xmlcalabash/graphviz"
            xsltCompiler.isSchemaAware = source.processor.isSchemaAware
            xsltCompiler.resourceResolver = VisualizerResourceResolver()
            var xsltExec = xsltCompiler.compile(styleSource)

            val outputBaseURI = UriUtils.cwdAsUri().resolve(outdir)

            var transformer = xsltExec.load30()
            transformer.setStylesheetParameters(mapOf(Ns.version to XdmAtomicValue(XmlCalabashBuildConfig.VERSION)))
            if (debug) {
                transformer.setStylesheetParameters(mapOf(Ns.debug to XdmAtomicValue("1")))
            }

            transformer.globalContextItem = source
            transformer.baseOutputURI = "${outputBaseURI}"
            val xmlResult = XdmDestination()
            transformer.applyTemplates(source.asSource(), xmlResult)
            val dotxml = xmlResult.xdmNode
            val dotFiles = dotxml.underlyingNode.stringValue.trim().split('\n')

            val rt = Runtime.getRuntime()
            for (filename in dotFiles) {
                val dotUri = outputBaseURI.resolve(filename)
                val dotFile = File("${dotUri.toPath()}.dot")
                val svgFile = File("${dotUri.toPath()}.svg")
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
                    logger.warn { "Graph generation failed for $filename" }
                }
                if (!debug) {
                    dotFile.delete()
                }
            }
        }
    }

    private class VisualizerResourceResolver : ResourceResolver {
        override fun resolve(request: ResourceRequest?): Source? {
            if (request == null) {
                return null
            }

            val pos = request.uri.lastIndexOf('/')
            if (pos < 0) {
                throw XProcError.xiImpossible("Request for graphviz unexpected stylesheet: ${request.uri}").exception()
            }

            val resource= "/com/xmlcalabash/${request.uri.substring(pos+1)}"
            var styleStream = VisualizerOutput::class.java.getResourceAsStream(resource)
            return SAXSource(InputSource(styleStream))
        }
    }

}