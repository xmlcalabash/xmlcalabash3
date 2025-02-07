package com.xmlcalabash.datamodel

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.XProcStepConfigurationImpl
import net.sf.saxon.expr.parser.XPathParser
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.sxpath.IndependentContext
import net.sf.saxon.trans.XPathException
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.net.URI
import javax.xml.transform.sax.SAXSource

class InstructionConfigurationImpl private constructor(
    private val stepConfig: XProcStepConfiguration,
): XProcStepConfiguration by stepConfig, InstructionConfiguration
{
    companion object {
        fun newInstance(builder: PipelineBuilder): InstructionConfiguration {
            val environment =  builder.pipelineContext //PipelineCompilerContext(builder.saxonConfig.xmlCalabash)
            val xprocStepConfig = XProcStepConfigurationImpl(environment, builder.saxonConfig, Location.NULL)
            val iconfig = InstructionConfigurationImpl(xprocStepConfig)
            return iconfig
        }
    }

    private val _inscopeStepNames = mutableMapOf<String, StepDeclaration>()
    private val _inscopeVariables = mutableMapOf<QName, VariableBindingContainer>()
    private val _staticBindings = mutableMapOf<QName, XdmValue>()

    private var _nextId: String? = null
    override val nextId: String
        get() {
            if (_nextId == null) {
                _nextId = environment.nextId
            }
            return _nextId!!
        }

    override val inscopeStepNames: Map<String, StepDeclaration>
        get() = _inscopeStepNames
    override val inscopeVariables: Map<QName, VariableBindingContainer>
        get() = _inscopeVariables
    override val staticBindings: Map<QName, XdmValue>
        get() = _staticBindings

    override val qnameMapType: SequenceType
        get() {
            return parseXsSequenceType("map(xs:QName,item()*)")
        }

    override var drp: PortBindingContainer? = null

    override fun copy(): InstructionConfiguration {
        return copy(stepConfig.copy())
    }

    override fun copyNew(): InstructionConfiguration {
        return copy(stepConfig.copyNew())
    }

    override fun copy(config: XProcStepConfiguration): InstructionConfiguration {
        val copy = InstructionConfigurationImpl(config)
        copy._inscopeStepNames.putAll(_inscopeStepNames)
        copy._inscopeVariables.putAll(_inscopeVariables)
        copy._staticBindings.putAll(_staticBindings)
        copy.drp = drp
        return copy
    }

    override fun addVisibleStepName(decl: StepDeclaration) {
        val name = decl.name
        val current = _inscopeStepNames[name]
        if (current != null && current !== decl) {
             throw stepConfig.exception(XProcError.xsDuplicateStepName(name))
        }
        _inscopeStepNames[name] = decl
    }

    override fun addVisibleStepType(decl: DeclareStepInstruction) {
        val name = decl.type!!
        val current = inscopeStepTypes[name]
        if (current != null && current !== decl) {
            throw stepConfig.exception(XProcError.xsDuplicateStepType(name))
        }
        stepConfig.putStepType(name, decl)
    }

    override fun addVariable(binding: VariableBindingContainer) {
        _inscopeVariables[binding.name] = binding
    }

    override fun addStaticBinding(name: QName, value: XdmValue) {
        _staticBindings[name] = value
    }

    override fun with(location: Location): InstructionConfiguration {
        val copy = copy() as InstructionConfigurationImpl
        copy.stepConfig.setLocation(location)
        return copy
    }

    override fun with(prefix: String, uri: NamespaceUri): InstructionConfiguration {
        val copy = copy() as InstructionConfigurationImpl
        copy.putNamespace(prefix, uri)
        return copy
    }

    override fun with(namespaces: Map<String, NamespaceUri>): InstructionConfiguration {
        val copy = copy() as InstructionConfigurationImpl
        copy.stepConfig.putAllNamespaces(namespaces)
        return copy
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
            stepConfig.setLocation(Location(node))
        } catch (ex: IllegalStateException) {
            val uri = node.getAttributeValue(NsXml.base) ?: ""
            throw stepConfig.exception(XProcError.xdInvalidUri(uri))
        }

        stepConfig.putAllNamespaces(nsmap)
    }

    override fun updateWith(baseUri: URI) {
        stepConfig.setLocation(location)
    }

    override fun putNamespace(prefix: String, uri: NamespaceUri) {
        stepConfig.putNamespace(prefix, uri)
    }

    override fun fromUri(
        href: URI,
        properties: DocumentProperties,
        parameters: Map<QName, XdmValue>
    ): XProcDocument {
        return environment.documentManager.load(href, this, properties, parameters)
    }

    override fun fromString(
        xml: String,
        properties: DocumentProperties,
        parameters: Map<QName, XdmValue>
    ): XProcDocument {
        val builder = processor.newDocumentBuilder()
        builder.isLineNumbering = true
        val bais = ByteArrayInputStream(xml.toByteArray())
        val input = InputSource(bais)
        input.systemId = baseUri.toString()
        val source = SAXSource(input)
        val xdm = builder.build(source)
        return XProcDocument.ofXml(xdm, this, MediaType.XML, properties)
    }

    override fun parseVisibility(visible: String): Visibility {
        when (visible) {
            "private" -> return Visibility.PRIVATE
            "public" -> return Visibility.PUBLIC
            else -> throw stepConfig.exception(XProcError.xsValueDoesNotSatisfyType(visible, "Visibility"))
        }
    }

    override fun parseContentTypes(text: String): List<MediaType> {
        try {
            val alist = ArrayList<MediaType>()
            for (mt in MediaType.parseList(text)) {
                alist.add(mt)
            }
            return alist
        } catch (ex: XProcException) {
            throw ex.error.asStatic().exception()
        }
    }

    override fun parseExcludeInlinePrefixes(prefixes: String): Set<NamespaceUri> {
        if (prefixes.trim() == "") {
            throw stepConfig.exception(XProcError.xsInvalidExcludePrefix())
        }

        val uriSet = mutableSetOf<NamespaceUri>()
        uriSet.add(NsP.namespace)
        for (token in prefixes.split("\\s+".toRegex())) {
            when (token) {
                "#all" -> {
                    for ((_, uri) in inscopeNamespaces) {
                        uriSet.add(uri)
                    }
                }
                "#default" -> {
                    if (inscopeNamespaces[""] == null) {
                        throw stepConfig.exception(XProcError.xsNoDefaultNamespace())
                    } else {
                        uriSet.add(inscopeNamespaces[""]!!)
                    }
                }
                else -> {
                    val uri = inscopeNamespaces[token]
                    if (uri == null) {
                        throw stepConfig.exception(XProcError.xsInvalidExcludePrefix(token))
                    } else {
                        uriSet.add(uri)
                    }
                }
            }
        }

        return uriSet
    }

    override fun parseValues(text: String): List<XdmAtomicValue> {
        val values = mutableListOf<XdmAtomicValue>()
        val compiler = newXPathCompiler()
        val selector = compiler.compile(text).load()
        //selector.resourceResolver = context.pipelineConfig.documentManager
        for (value in selector.evaluate().iterator()) {
            when (value) {
                is XdmAtomicValue -> values.add(value)
                else -> throw stepConfig.exception(XProcError.xsInvalidValues(value.toString()))
            }
        }

        return values
    }

    override fun parseSequenceType(asExpr: String): SequenceType {
        val config = processor.getUnderlyingConfiguration()
        val icontext = IndependentContext(config)
        icontext.clearAllNamespaces() // We get no defaults
        for ((prefix, uri) in inscopeNamespaces) {
            icontext.declareNamespace(prefix, uri)
        }

        try {
            icontext.setXPathLanguageLevel(31)
            val parser = XPathParser(icontext)
            val st = parser.parseSequenceType(asExpr, icontext)
            return SequenceType.fromUnderlyingSequenceType(processor, st)
        } catch (ex: XPathException) {
            throw stepConfig.exception(XProcError.xsInvalidSequenceType(asExpr))
        }
    }

    override fun parseSpecialType(type: String): SpecialType {
        when (type) {
            "XPathExpression" -> return SpecialType.XPATH_EXPRESSION
            "XSLTSelectionPattern" -> return SpecialType.XSLT_SELECTION_PATTERN
            "RegularExpression" -> return SpecialType.REGULAR_EXPRESSION
            "EQNameList" -> return SpecialType.EQNAME_LIST
            else -> {
                throw stepConfig.exception(XProcError.xiNotASpecialType(type))
            }
        }
    }
}