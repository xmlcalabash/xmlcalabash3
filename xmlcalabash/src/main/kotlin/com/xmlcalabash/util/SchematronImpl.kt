package com.xmlcalabash.util

import com.xmlcalabash.XmlCalabashBuildConfig
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import javax.xml.transform.stream.StreamSource

class SchematronImpl(val stepConfig: XProcStepConfiguration) {
    companion object {
        val _phase = QName(NamespaceUri.of("http://dmaus.name/ns/2023/schxslt"), "schxslt:phase")
        val transpilerResource = "/com/xmlcalabash/schxslt2-${XmlCalabashBuildConfig.SCHXSLT2}/transpile.xsl"
        var transpilerExec: XsltExecutable? = null
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

        val xschema = if (schemaXml.nodeKind == XdmNodeKind.DOCUMENT) {
            schemaXml
        } else {
            val patch = SaxonTreeBuilder(stepConfig)
            patch.startDocument(schemaRoot.baseURI)
            patch.addSubtree(schemaXml)
            patch.endDocument()
            patch.result
        }

        val schema = S9Api.adjustBaseUri(xschema, schemaXml.baseURI)
        val schemaAware = stepConfig.processor.isSchemaAware

        val transpilerExec = loadExecutable()
        val transpiler = transpilerExec.load()
        if (phase != null) {
            transpiler.setParameter(_phase, XdmAtomicValue(phase))
        }

        for ((name, value) in params) {
            transpiler.setParameter(name, value)
        }

        var destination = XdmDestination()
        transpiler.initialContextNode = schema
        transpiler.destination = destination
        if (schema.baseURI != null) {
            transpiler.baseOutputURI = schema.baseURI.toString()
        }

        transpiler.transform()

        val compiledSchema = S9Api.adjustBaseUri(destination.xdmNode, schema.baseURI)
        val compiler = stepConfig.processor.newXsltCompiler()
        compiler.isSchemaAware = stepConfig.processor.isSchemaAware
        destination = XdmDestination()

        compiler.isSchemaAware = schemaAware

        val exec = compiler.compile(compiledSchema.asSource())
        val transformer = exec.load30()

        transformer.setStylesheetParameters(params)

        transformer.globalContextItem = sourceXml
        if (sourceXml is XdmNode && sourceXml.baseURI != null) {
            transformer.setBaseOutputURI(sourceXml.baseURI.toString())
        }

        transformer.applyTemplates(sourceXml, destination)
        return destination.xdmNode
    }

    fun failedAssertions(node: XdmNode): List<XdmNode> {
        val nsBindings = mapOf("svrl" to "http://purl.oclc.org/dsdl/svrl")
        val xpath = "//svrl:failed-assert|//svrl:successful-report"

        val xcomp = stepConfig.newXPathCompiler()
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

    private fun loadExecutable(): XsltExecutable {
        if (transpilerExec == null) {
            val stream = SchematronImpl::class.java.getResourceAsStream(transpilerResource)
                ?: throw XProcError.xiCannotLoadResource(transpilerResource).exception()
            val source = StreamSource(stream)
            val compiler = stepConfig.processor.newXsltCompiler()
            compiler.isSchemaAware = stepConfig.processor.isSchemaAware
            transpilerExec = compiler.compile(source)
        }

        return transpilerExec!!
    }
}