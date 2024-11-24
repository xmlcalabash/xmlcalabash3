package com.xmlcalabash.runtime

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.datamodel.StepConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.runtime.steps.RuntimeStepStaticContext
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.net.URI

open class RuntimeStepStaticContextImpl(
    override val saxonConfig: SaxonConfiguration,
    val rteContext: RuntimeExecutionContext,
    val standardSteps: () -> Map<QName, DeclareStepInstruction>
): RuntimeStepStaticContext {
    constructor(config: StepConfiguration) : this(config.saxonConfig.newConfiguration(), config.rteContext, { config.inscopeStepTypes }) {
        _location = config.location
        _inscopeNamespaces.putAll(config.inscopeNamespaces)
        _inscopeStepTypes.putAll(config.inscopeStepTypes)
    }

    override val xmlCalabash: XmlCalabash
        get() = saxonConfig.xmlCalabash
    override val processor: Processor
        get() = saxonConfig.processor

    protected var _location: Location = Location.NULL
    protected val _inscopeNamespaces = mutableMapOf<String, NamespaceUri>()
    protected val _inscopeStepTypes = mutableMapOf<QName, DeclareStepInstruction>()

    override val inscopeNamespaces: Map<String, NamespaceUri>
        get() = _inscopeNamespaces
    override val inscopeStepTypes: Map<QName, DeclareStepInstruction>
        get() = _inscopeStepTypes

    override val location: Location
        get() = _location
    override val baseUri: URI?
        get() = _location.baseURI

    open fun copy(): RuntimeStepStaticContextImpl {
        return copyNew(saxonConfig)
    }

    open fun copyNew(): RuntimeStepStaticContextImpl {
        return copyNew(saxonConfig.newConfiguration())
    }

    private fun copyNew(config: SaxonConfiguration): RuntimeStepStaticContextImpl {
        val stepConfig = RuntimeStepStaticContextImpl(config, config.rteContext, standardSteps)
        stepConfig._location = location
        stepConfig._inscopeStepTypes.putAll(_inscopeStepTypes)
        stepConfig._inscopeNamespaces.putAll(_inscopeNamespaces)
        return stepConfig
    }

    fun with(location: Location): RuntimeStepStaticContextImpl {
        val newContext = copyNew(saxonConfig)
        newContext._location = location
        return newContext
    }

    fun with(prefix: String, uri: NamespaceUri): RuntimeStepStaticContextImpl {
        val stepConfig = copyNew(saxonConfig)
        stepConfig._inscopeNamespaces[prefix] = uri
        return stepConfig
    }

    fun with(inscopeNamespaces: Map<String, NamespaceUri>): RuntimeStepStaticContextImpl {
        val stepConfig = copyNew(saxonConfig)
        stepConfig._inscopeNamespaces.clear()
        stepConfig._inscopeNamespaces.putAll(inscopeNamespaces)
        return stepConfig
    }

    override fun addVisibleStepType(decl: DeclareStepInstruction) {
        val name = decl.type!!
        val current = _inscopeStepTypes[name]
        if (current != null && current !== decl) {
            throw XProcError.xsDuplicateStepType(name).exception()
        }
        _inscopeStepTypes[name] = decl
    }

    override fun stepDeclaration(name: QName): DeclareStepInstruction? {
        return _inscopeStepTypes[name] ?: standardSteps()[name]
    }

    override fun stepAvailable(name: QName): Boolean {
        val decl = stepDeclaration(name)
        if (decl == null || decl.isAtomic) {
            //println("${name}: ${decl?.isAtomic}: ${rteContext.atomicStepAvailable(name)}")
            return rteContext.atomicStepAvailable(name)
        }
        //println("${name} is available")
        return true
    }

    override fun updateWith(node: XdmNode) {
        val nsmap = mutableMapOf<String,NamespaceUri>()
        for (ns in node.axisIterator(Axis.NAMESPACE)) {
            if (node.nodeName.localName != "xml") {
                if (ns.nodeName == null) {
                    nsmap[""] = NamespaceUri.of(ns.stringValue)
                } else {
                    nsmap[ns.nodeName.localName] = NamespaceUri.of(ns.stringValue)
                }
            }
        }

        try {
            _location = Location(node)
        } catch (ex: IllegalStateException) {
            val uri = node.getAttributeValue(NsXml.base) ?: ""
            throw XProcError.xdInvalidUri(uri).exception()
        }

        _inscopeNamespaces.putAll(nsmap)
    }

    override fun updateWith(baseUri: URI) {
        _location = Location(baseUri)
    }

    override fun updateNamespaces(nsmap: Map<String, NamespaceUri>) {
        _inscopeNamespaces.clear()
        _inscopeNamespaces.putAll(nsmap)
    }

    override fun addNamespace(prefix: String, uri: NamespaceUri) {
        _inscopeNamespaces[prefix] = uri
    }

    override fun resolve(href: String): URI {
        if (baseUri == null) {
            return URI(href)
        }
        return baseUri!!.resolve(href)
    }

    override fun exception(error: XProcError): XProcException {
        // TODO: add more context to the error
        return error.exception()
    }

    override fun exception(error: XProcError, cause: Throwable): XProcException {
        return XProcException(error.at(location), cause)
    }
}