package com.xmlcalabash.datamodel

import com.xmlcalabash.config.ExecutionContext
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.*
import com.xmlcalabash.runtime.RuntimeExecutionContext
import com.xmlcalabash.runtime.RuntimeStepStaticContextImpl
import com.xmlcalabash.runtime.ValueConverter
import com.xmlcalabash.config.XProcStepConfiguration
import com.xmlcalabash.util.SaxonValueConverter
import net.sf.saxon.expr.parser.XPathParser
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.*
import net.sf.saxon.s9api.*
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.sxpath.IndependentContext
import net.sf.saxon.trans.XPathException
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.net.URI
import javax.xml.transform.sax.SAXSource

class StepConfiguration private constructor(
    val staticContext: InstructionStaticContextImpl,
    val rteContext: RuntimeExecutionContext,
    val converter: SaxonValueConverter
): XProcStepConfiguration, InstructionStaticContext by staticContext, ExecutionContext by rteContext, ValueConverter by converter {
    companion object {
        private var _id = 0L

        internal fun newInstance(builder: PipelineBuilder): StepConfiguration {
            val rteContext = RuntimeExecutionContext(builder.saxonConfig.xmlCalabash)
            val rContext = RuntimeStepStaticContextImpl(builder.saxonConfig, rteContext, { builder.standardLibrary.exportedSteps })
            val iContext = InstructionStaticContextImpl(rContext)
            val converter = SaxonValueConverter(builder.saxonConfig.processor)
            val stepConfig = StepConfiguration(iContext, rteContext, converter)
            stepConfig.addNamespace("p", NsP.namespace)
            return stepConfig
        }
    }

    private var _stepName: String? = null

    fun copy(): StepConfiguration {
        return copyNew(staticContext.copy(), converter)
    }

    fun copyNew(): StepConfiguration {
        val context = staticContext.copyNew()
        return copyNew(context, SaxonValueConverter(context.processor))
    }

    internal fun copyNew(newContext: InstructionStaticContextImpl, newConverter: SaxonValueConverter): StepConfiguration {
        val stepConfig = StepConfiguration(newContext, rteContext, newConverter)
        stepConfig._stepName = _stepName
        return stepConfig
    }

    fun with(location: Location): StepConfiguration {
        val stepConfig = StepConfiguration(staticContext.with(location), rteContext, converter)
        stepConfig._stepName = _stepName
        return stepConfig
    }

    fun with(prefix: String, uri: NamespaceUri): StepConfiguration {
        val stepConfig = StepConfiguration(staticContext.with(prefix, uri), rteContext, converter)
        stepConfig._stepName = _stepName
        return stepConfig
    }

    fun with(inscopeNamespaces: Map<String, NamespaceUri>): StepConfiguration {
        val stepConfig = StepConfiguration(staticContext.with(inscopeNamespaces), rteContext, converter)
        stepConfig._stepName = _stepName
        return stepConfig
    }

    private var _itemTypeFactory: ItemTypeFactory? = null
    private val itemTypeFactory: ItemTypeFactory
        get() {
            if (_itemTypeFactory == null) {
                _itemTypeFactory = ItemTypeFactory(processor)
            }
            return _itemTypeFactory!!
        }

    val configuration = saxonConfig.configuration

    fun newXPathCompiler(): XPathCompiler {
        val compiler = processor.newXPathCompiler()
        for ((prefix, value) in inscopeNamespaces) {
            compiler.declareNamespace(prefix, value.toString())
        }
        return compiler
    }

    internal val nextId: Long
        get() {
            synchronized(Companion) {
                return ++_id
            }
        }

    var stepName: String
        get() {
            return _stepName!!
        }
        internal set(value) {
            _stepName = value
        }

    fun fromUri(href: URI, properties: DocumentProperties = DocumentProperties(), parameters: Map<QName, XdmValue> = emptyMap()): XProcDocument {
        return documentManager.load(href, this, properties, parameters)
    }

    fun fromString(xml: String, properties: DocumentProperties = DocumentProperties()): XProcDocument {
        val builder = processor.newDocumentBuilder()
        builder.isLineNumbering = true
        val bais = ByteArrayInputStream(xml.toByteArray())
        val input = InputSource(bais)
        input.systemId = baseUri.toString()
        val source = SAXSource(input)
        val xdm = builder.build(source)
        return XProcDocument.ofXml(xdm, this, properties)
    }

    fun parseVisibility(visible: String): Visibility {
        when (visible) {
            "private" -> return Visibility.PRIVATE
            "public" -> return Visibility.PUBLIC
            else -> throw XProcError.xsValueDoesNotSatisfyType(visible, "Visibility").exception()
        }
    }

    fun parseContentTypes(text: String): MutableList<MediaType> {
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

    fun parseExcludeInlinePrefixes(prefixes: String): Set<NamespaceUri> {
        if (prefixes.trim() == "") {
            throw XProcError.xsInvalidExcludePrefix().exception()
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
                        throw XProcError.xsNoDefaultNamespace().exception()
                    } else {
                        uriSet.add(inscopeNamespaces[""]!!)
                    }
                }
                else -> {
                    val uri = inscopeNamespaces[token]
                    if (uri == null) {
                        throw XProcError.xsInvalidExcludePrefix(token).exception()
                    } else {
                        uriSet.add(uri)
                    }
                }
            }
        }

        return uriSet
    }

    fun parseValues(text: String): List<XdmAtomicValue> {
        val values = mutableListOf<XdmAtomicValue>()
        val compiler = newXPathCompiler()
        val selector = compiler.compile(text).load()
        //selector.resourceResolver = context.pipelineConfig.documentManager
        for (value in selector.evaluate().iterator()) {
            when (value) {
                is XdmAtomicValue -> values.add(value)
                else -> throw XProcError.xsInvalidValues(value.toString()).exception()
            }
        }

        return values
    }

    internal val qnameMapType: SequenceType
        get() {
            return parseXsSequenceType("map(xs:QName,item()*)")
        }

    fun parseSequenceType(asExpr: String): SequenceType {
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
            throw XProcError.xsInvalidSequenceType(asExpr).exception()
        }
    }

    fun parseSpecialType(type: String): SpecialType {
        when (type) {
            "XPathExpression" -> return SpecialType.XPATH_EXPRESSION
            "XSLTSelectionPattern" -> return SpecialType.XSLT_SELECTION_PATTERN
            "RegularExpression" -> return SpecialType.REGULAR_EXPRESSION
            "EQNameList" -> return SpecialType.EQNAME_LIST
            else -> {
                throw XProcError.xiNotASpecialType(type).exception()
            }
        }
    }

    override fun parseQName(name: String): QName {
        return converter.parseQName(name, inscopeNamespaces)
    }

    override fun checkType(
        varName: QName?,
        value: XdmValue,
        sequenceType: SequenceType?,
        values: List<XdmAtomicValue>
    ): XdmValue {
        return converter.checkType(varName, value, sequenceType, inscopeNamespaces, values)
    }

    override fun forceQNameKeys(inputMap: MapItem): XdmMap {
        return converter.forceQNameKeys(inputMap, inscopeNamespaces)
    }

    override fun forceQNameKeys(inputMap: XdmMap): XdmMap {
        return converter.forceQNameKeys(inputMap, inscopeNamespaces)
    }

    /*
    override fun evaluateExpression(expression: String, bindings: Map<QName, XdmValue>): XdmValue {
        val compiler = processor.newXPathCompiler()
        for ((prefix, uri) in inscopeNamespaces) {
            compiler.declareNamespace(prefix, uri.toString())
        }
        for ((name, _) in bindings) {
            compiler.declareVariable(name)
        }
        val exec = compiler.compile(expression)
        val select = exec.load()
        for ((name, value) in bindings) {
            select.setVariable(name, value)
        }
        return select.evaluate()
    }
     */
}