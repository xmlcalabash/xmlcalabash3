package com.xmlcalabash.datamodel

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.util.XProcCollectionFinder
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.type.BuiltInAtomicType

class XProcSelectExpression private constructor(stepConfig: XProcStepConfiguration, val select: String, asType: SequenceType, collection: Boolean, values: List<XdmAtomicValue>): XProcExpression(stepConfig, asType, collection, values) {
    companion object {
        fun newInstance(stepConfig: XProcStepConfiguration, select: String, asType: SequenceType, collection: Boolean, values: List<XdmAtomicValue> = emptyList()): XProcSelectExpression {
            return XProcSelectExpression(stepConfig, select, asType, collection, values)
        }
    }

    override fun cast(asType: SequenceType, values: List<XdmAtomicValue>): XProcExpression {
        return select(stepConfig, select, asType, collection, values)
    }

    override fun xevaluate(config: XProcStepConfiguration): () -> XdmValue {
        return { evaluate(config) }
    }

    override fun evaluate(config: XProcStepConfiguration): XdmValue {
        val compiler = config.processor.newXPathCompiler()
        if (config.processor.isSchemaAware) {
            compiler.isSchemaAware = true
        }

        // Hack
        val uri = config.baseUri
        if (uri != null && !uri.toString().startsWith("?uniqueid")) {
            compiler.baseURI = uri
        }

        for ((prefix, uri) in config.inscopeNamespaces) {
            compiler.declareNamespace(prefix, uri.toString())
        }
        for (name in variableRefs) {
            compiler.declareVariable(name)
        }
        val selector = try {
            compiler.compile(select).load()
        } catch (ex: SaxonApiException) {
            // This is a bit of a hack; the problem appears in a test case for p:set-attributes but
            // I suppose it's possible that it could occur elsewhere...still, hopefully the message
            // is clear enough.
            if (ex.message != null && ex.message!!.contains("cannot be used as a namespace URI")) {
                throw XProcError.xcCannotSetNamespaces().exception()
            }
            throw ex
        }

        val sconfig = config.saxonConfig.configuration
        val defaultCollectionUri = sconfig.defaultCollection
        val collectionFinder = XProcCollectionFinder(defaultCollection, selector.underlyingXPathContext.collectionFinder)
        selector.underlyingXPathContext.collectionFinder = collectionFinder
        selector.resourceResolver = config.environment.documentManager
        sconfig.defaultCollection = XProcCollectionFinder.DEFAULT

        for (name in variableRefs) {
            if (name in staticVariableBindings) {
                selector.setVariable(name, staticVariableBindings[name]!!.evaluate(config))
            } else if (name in variableBindings) {
                selector.setVariable(name, variableBindings[name]!!)
            }
        }

        setupExecutionContext(config, selector)

        val result = try {
            selector.evaluate()
        } catch (ex: Throwable) {
            selector.underlyingXPathContext.collectionFinder = collectionFinder.chain
            throw ex
        }

        selector.underlyingXPathContext.collectionFinder = collectionFinder.chain
        sconfig.defaultCollection = defaultCollectionUri

        teardownExecutionContext()

        if (asType !== SequenceType.ANY || values.isNotEmpty()) {
            try {
                return config.checkType(null, result, asType, values)
            } catch (ex: XProcException) {
                if (ex.error.code == NsErr.xd(36) && asType.underlyingSequenceType.primaryType == BuiltInAtomicType.QNAME) {
                    throw ex.error.with(NsErr.xd(61)).exception()
                }
                throw ex
            }
        }

        //println("${select} = ${result}")

        return result
    }

    override fun toString(): String {
        return select
    }
}