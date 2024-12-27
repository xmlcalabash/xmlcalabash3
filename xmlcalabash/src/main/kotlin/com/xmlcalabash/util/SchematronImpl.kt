package com.xmlcalabash.util

import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.lib.ResourceRequest
import net.sf.saxon.lib.ResourceResolver
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.*
import org.xml.sax.InputSource
import java.net.URI
import javax.xml.transform.Source
import javax.xml.transform.sax.SAXSource

class SchematronImpl(val stepConfig: XProcStepConfiguration) {
    companion object {
        val s_schema = QName(NamespaceUri.of("http://purl.oclc.org/dsdl/schematron"), "schema")
        val _queryBinding = QName("queryBinding")
        val fakeBaseUri = "https://sch.xmlcalabash.com"
        val _phase = QName("phase")
        val _untyped = StructuredQName("xs", NsXs.namespace, "untyped")
        val INSTANCE = QName("INSTANCE")
    }

    val params = mutableMapOf<QName, XdmValue>()

    fun test(sourceXml: XdmNode, schemaXml: XdmNode, phase: String? = null): List<XdmNode> {
        return failedAssertions(report(sourceXml, schemaXml, phase))
    }

    fun test(sourceValue: XdmValue, schemaXml: XdmNode, phase: String? = null): List<XdmNode> {
        val failures = mutableListOf<XdmNode>()
        val iter = sourceValue.iterator()
        while (iter.hasNext()) {
            val item = iter.next()
            failures.addAll(failedAssertions(report(item, schemaXml, phase)))
        }
        return failures
    }

    fun report(sourceXml: XdmItem, schemaXml: XdmNode, phase: String? = null): XdmNode {
        val schemaRoot = when (schemaXml.nodeKind) {
            XdmNodeKind.ELEMENT -> schemaXml
            else -> S9Api.documentElement(schemaXml)
        }
        val xschema = if (schemaRoot.nodeName == s_schema && schemaRoot.getAttributeValue(_queryBinding) == null) {
            // The schema doesn't specify a query binding. That'll cause SchXslt to fail. Patch it.
            val patch = SaxonTreeBuilder(stepConfig)
            patch.startDocument(schemaRoot.baseURI)
            val amap = mutableMapOf<QName, String?>()
            amap.putAll(stepConfig.attributeMap(schemaRoot.underlyingNode.attributes()))
            amap[_queryBinding] = "xslt3"
            patch.addStartElement(s_schema, stepConfig.attributeMap(amap), schemaRoot.underlyingNode.allNamespaces)
            for (child in schemaRoot.axisIterator(Axis.CHILD)) {
                patch.addSubtree(child)
            }
            patch.addEndElement()
            patch.endDocument()
            patch.result
        } else {
            if (schemaXml.nodeKind == XdmNodeKind.DOCUMENT) {
                schemaXml
            } else {
                val patch = SaxonTreeBuilder(stepConfig)
                patch.startDocument(schemaRoot.baseURI)
                patch.addSubtree(schemaXml)
                patch.endDocument()
                patch.result
            }
        }

        val schema = S9Api.adjustBaseUri(xschema, schemaXml.baseURI)
        val schemaAware = stepConfig.processor.isSchemaAware

        var compiler = stepConfig.processor.newXsltCompiler()
        var xResolver = UResourceResolver(compiler.resourceResolver)
        compiler.resourceResolver = xResolver
        var destination = XdmDestination()

        compiler.isSchemaAware = schemaAware

        val rreq = ResourceRequest()
        rreq.baseUri = fakeBaseUri
        rreq.uri = "/xslt/2.0/pipeline-for-svrl.xsl"
        val schpipeline = xResolver.resolve(rreq)

        var exec = compiler.compile(schpipeline)
        val schemaCompiler = exec.load()

        if (phase != null) {
            schemaCompiler.setParameter(_phase, XdmAtomicValue(phase))
        }

        // FIXME: We pass parameters to both the compiler and the validator, is that right?
        for ((name, value) in params) {
            schemaCompiler.setParameter(name, value)
        }

        schemaCompiler.initialContextNode = schema
        schemaCompiler.destination = destination
        if (schema.baseURI != null) {
            schemaCompiler.baseOutputURI = schema.baseURI.toString()
        }

        schemaCompiler.transform()

        val compiledSchema = S9Api.adjustBaseUri(destination.xdmNode, schema.baseURI)
        compiler = stepConfig.processor.newXsltCompiler()
        xResolver = UResourceResolver(compiler.resourceResolver)
        compiler.resourceResolver = xResolver
        destination = XdmDestination()

        compiler.isSchemaAware = schemaAware

        exec = compiler.compile(compiledSchema.asSource())
        val transformer = exec.load30()

        transformer.setStylesheetParameters(params)

        transformer.resourceResolver = xResolver
        transformer.globalContextItem = sourceXml

        if (sourceXml is XdmNode) {
            if (sourceXml.baseURI != null) {
                transformer.setBaseOutputURI(sourceXml.baseURI.toString())
            }
        }

        transformer.applyTemplates(sourceXml, destination)
        return destination.xdmNode
    }

    fun failedAssertions(node: XdmNode): List<XdmNode> {
        val nsBindings = mapOf("svrl" to "http://purl.oclc.org/dsdl/svrl")
        val xpath = "//svrl:failed-assert|//svrl:successful-report"

        val xcomp = stepConfig.processor.newXPathCompiler()
        xcomp.baseURI = URI(fakeBaseUri)
        for ((prefix, value) in nsBindings) {
            xcomp.declareNamespace(prefix, value)
        }

        val xexec = xcomp.compile(xpath)
        val selector = xexec.load()

        selector.contextItem = node

        val results = mutableListOf<XdmNode>()
        for (value in selector.iterator()) {
            results.add(value as XdmNode)
        }

        return results
    }

    inner class UResourceResolver(val next: ResourceResolver?): ResourceResolver {
        override fun resolve(req: ResourceRequest?): Source? {
            if (req == null) {
                return null
            }

            if (req.baseUri != null && req.baseUri.startsWith(fakeBaseUri)) {
                val uri = URI(req.baseUri).resolve(req.uri)
                val fn = uri.toString().substring(fakeBaseUri.length)
                val skeleton = SchematronImpl::class.java.getResourceAsStream(fn)
                    ?: throw RuntimeException("Configuration error: cannot resolve schematron skeleton")
                val source = InputSource(skeleton)
                source.systemId = uri.toString()
                return SAXSource(source)
            }

            if (next != null) {
                return next.resolve(req)
            }
            return null
        }
    }
}