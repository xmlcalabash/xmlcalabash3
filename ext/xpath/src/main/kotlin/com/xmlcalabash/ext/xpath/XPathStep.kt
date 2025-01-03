package com.xmlcalabash.ext.xpath

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonErrorReporter
import com.xmlcalabash.util.XProcCollectionFinder
import net.sf.saxon.s9api.XdmItem

class XPathStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val xpath = queues["xpath"]!!.first().value.toString()
        val sources = mutableListOf<XProcDocument>()
        sources.addAll(queues["source"] ?: emptyList())
        val document = if (sources.size == 1) {
            sources.removeFirst()
        } else {
            null
        }

        val params = qnameMapBinding(Ns.parameters)
        val version = stringBinding(Ns.version)

        if (version != null && version != "3.1") {
            throw stepConfig.exception(XProcError.xiXPathVersionNotSupported(version))
        }

        val underlyingConfig = stepConfig.processor.underlyingConfiguration
        val collectionFinder = underlyingConfig.collectionFinder

        underlyingConfig.defaultCollection = XProcCollectionFinder.DEFAULT
        underlyingConfig.collectionFinder = XProcCollectionFinder(sources, collectionFinder)

        val compiler = stepConfig.processor.newXPathCompiler()
        compiler.isSchemaAware = stepConfig.processor.isSchemaAware
        compiler.setWarningHandler(SaxonErrorReporter(stepConfig))
        for ((prefix, ns) in stepConfig.inscopeNamespaces) {
            compiler.declareNamespace(prefix, "${ns}")
        }
        for ((name, _) in params) {
            compiler.declareVariable(name)
        }

        val exec = try {
            compiler.compile(xpath)
        } catch (ex: Exception) {
            underlyingConfig.collectionFinder = collectionFinder
            throw ex
        }

        val selector = exec.load()
        for ((name, value) in params) {
            selector.setVariable(name, value)
        }

        if (document != null) {
            selector.contextItem = document.value as XdmItem
        }

        try {
            for (document in S9Api.makeDocuments(stepConfig, selector.evaluate())) {
                receiver.output("result", document)
            }
        } catch (ex: Exception) {
            underlyingConfig.collectionFinder = collectionFinder
            throw ex
        }

        underlyingConfig.collectionFinder = collectionFinder
    }
}