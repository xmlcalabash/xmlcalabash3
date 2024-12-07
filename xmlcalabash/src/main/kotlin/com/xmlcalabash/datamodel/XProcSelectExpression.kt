package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.util.XProcCollectionFinder
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.type.BuiltInAtomicType

class XProcSelectExpression private constructor(stepConfig: StepConfiguration, val select: String, asType: SequenceType, collection: Boolean, values: List<XdmAtomicValue>): XProcExpression(stepConfig, asType, collection, values) {
    companion object {
        fun newInstance(stepConfig: StepConfiguration, select: String, asType: SequenceType, collection: Boolean, values: List<XdmAtomicValue> = emptyList()): XProcSelectExpression {
            return XProcSelectExpression(stepConfig, select, asType, collection, values)
        }
    }

    override fun cast(asType: SequenceType, values: List<XdmAtomicValue>): XProcExpression {
        return select(stepConfig, select, asType, collection, values)
    }

    override fun xevaluate(): () -> XdmValue {
        return { evaluate() }
    }

    override fun evaluate(): XdmValue {
        val compiler = stepConfig.processor.newXPathCompiler()
        if (stepConfig.processor.isSchemaAware) {
            compiler.isSchemaAware = true
        }

        // Hack
        val uri = stepConfig.baseUri
        if (uri != null && !uri.toString().startsWith("?uniqueid")) {
            compiler.baseURI = uri
        }

        for ((prefix, uri) in stepConfig.inscopeNamespaces) {
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

        val config = stepConfig.saxonConfig.configuration
        val defaultCollectionUri = config.defaultCollection
        val collectionFinder = XProcCollectionFinder(defaultCollection, selector.underlyingXPathContext.collectionFinder)
        selector.underlyingXPathContext.collectionFinder = collectionFinder
        selector.resourceResolver = stepConfig.documentManager
        config.defaultCollection = XProcCollectionFinder.DEFAULT

        for (name in variableRefs) {
            if (name in staticVariableBindings) {
                selector.setVariable(name, staticVariableBindings[name]!!.evaluate())
            } else if (name in variableBindings) {
                selector.setVariable(name, variableBindings[name]!!)
            }
        }

        setupExecutionContext(selector)

        val result = try {
            selector.evaluate()
        } catch (ex: Throwable) {
            selector.underlyingXPathContext.collectionFinder = collectionFinder.chain
            throw ex
        }

        selector.underlyingXPathContext.collectionFinder = collectionFinder.chain
        config.defaultCollection = defaultCollectionUri

        teardownExecutionContext()

        if (asType !== SequenceType.ANY || values.isNotEmpty()) {
            try {
                return stepConfig.checkType(null, result, asType, values)
            } catch (ex: XProcException) {
                if (ex.error.code == NsErr.xd(36) && asType.underlyingSequenceType.primaryType == BuiltInAtomicType.QNAME) {
                    throw ex.error.with(NsErr.xd(61)).exception()
                }
                throw ex
            }
        }

        return result
    }

    override fun toString(): String {
        return select
    }
}