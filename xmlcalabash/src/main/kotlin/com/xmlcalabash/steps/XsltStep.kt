package com.xmlcalabash.steps

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.util.*
import net.sf.saxon.Configuration
import net.sf.saxon.event.PipelineConfiguration
import net.sf.saxon.event.Receiver
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.functions.ResolveURI
import net.sf.saxon.lib.ResultDocumentResolver
import net.sf.saxon.lib.SaxonOutputKeys
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.s9api.*
import net.sf.saxon.serialize.SerializationProperties
import net.sf.saxon.trans.XPathException
import net.sf.saxon.tree.wrapper.RebasedDocument
import java.net.URI

open class XsltStep(): AbstractAtomicStep() {
    companion object {
        val BUILD_TREE = ValueUtils.parseClarkName(SaxonOutputKeys.BUILD_TREE)
        val _terminate = QName("terminate")
        val _systemIdentifier = QName("system-identifier")
        val _publicIdentifier = QName("public-identifier")
    }

    val sources = mutableListOf<XProcDocument>()
    lateinit var stylesheet: XProcDocument
    lateinit var errorReporter: SaxonErrorReporter

    val parameters = mutableMapOf<QName,XdmValue>()
    val staticParameters = mutableMapOf<QName,XdmValue>()
    var globalContextItem: XProcDocument? = null
    var populateDefaultCollection = false
    var initialMode: QName? = null
    var templateName: QName? = null
    var outputBaseUri: URI? = null
    var version: String? = null

    var goesBang: XProcError? = null
    var terminationError: XProcError? = null
    var forceEmptyGlobalContextItem = false

    private var primaryDestination: Destination? = null
    private var primaryOutputProperties = mutableMapOf<QName, XdmValue>()

    override fun setup(stepConfig: XProcStepConfiguration, receiver: com.xmlcalabash.runtime.api.Receiver, stepParams: RuntimeStepParameters) {
        super.setup(stepConfig, receiver, stepParams)
        // FIXME: I expect this could be more centrally handled...
        errorReporter = SaxonErrorReporter(stepConfig)
        stepConfig.saxonConfig.configuration.setErrorReporterFactory { config -> errorReporter }
    }

    override fun extensionAttributes(attributes: Map<QName, String>) {
        super.extensionAttributes(attributes)
        val value = attributes[NsCx.emptyGlobalContext]
        if (value != null) {
            if (value == "true" || value == "false") {
                forceEmptyGlobalContextItem = value == "true"
            } else {
                stepConfig.debug { "Ignoring unexpected value for cx:empty-global-context: ${value}"}
            }
        }
    }

    override fun run() {
        super.run()
        sources.addAll(queues["source"]!!)
        stylesheet = queues["stylesheet"]!!.first()

        parameters.putAll(qnameMapBinding(Ns.parameters))
        staticParameters.putAll(qnameMapBinding(Ns.staticParameters))
        populateDefaultCollection = booleanBinding(Ns.populateDefaultCollection) ?: false
        initialMode = qnameBinding(Ns.initialMode)
        templateName = qnameBinding(Ns.templateName)
        outputBaseUri = uriBinding(Ns.outputBaseUri)
        version = stringBinding(Ns.version)

        val gcValue = options[Ns.globalContextItem]!!.value
        if (gcValue != XdmEmptySequence.getInstance()) {
            globalContextItem = XProcDocument.ofValue(gcValue,
                options[Ns.globalContextItem]!!.context, MediaType.OCTET_STREAM, DocumentProperties())
        }

        if (version == null) {
            val root = S9Api.firstElement(stylesheet.value as XdmNode)
            version = root?.getAttributeValue(Ns.version)
        }

        when (version) {
            "3.0" -> xslt30()
            "2.0" -> xslt20()
            "1.0" -> xslt10()
            else -> throw stepConfig.exception(XProcError.xcVersionNotAvailable(version ?: "null"))
        }
    }

    override fun reset() {
        super.reset()
        sources.clear()
        stylesheet = XProcDocument.ofEmpty(stepConfig)
    }

    private fun xslt30() {
        if (globalContextItem != null && forceEmptyGlobalContextItem) {
            stepConfig.warn { "cx:empty-global-context is ignored if an explicit global context item is specified" }
        }
        if (globalContextItem == null && sources.size == 1 && !forceEmptyGlobalContextItem) {
            globalContextItem = sources[0]
        }
        runXsltProcessor(sources.firstOrNull())
    }

    private fun xslt20() {
        if (globalContextItem != null) {
            stepConfig.info { "Global context item doesn't apply to XSLT 2.0"}
            globalContextItem = null
        }

        for (doc in sources) {
            if (doc.contentType == null) {
                throw stepConfig.exception(XProcError.xcXsltInputNot20Compatible())
            }
            if (doc.contentType!!.classification() !in listOf(MediaClassification.XML, MediaClassification.XHTML,
                    MediaClassification.HTML, MediaClassification.TEXT)) {
                throw stepConfig.exception(XProcError.xcXsltInputNot20Compatible(doc.contentType!!))
            }
        }

        for ((name, value) in parameters) {
            when (value) {
                is XdmAtomicValue, is XdmNode -> Unit
                is XdmMap -> throw stepConfig.exception(XProcError.xcXsltParameterNot20Compatible(name, "map"))
                is XdmArray -> throw stepConfig.exception(XProcError.xcXsltParameterNot20Compatible(name, "array"))
                is XdmFunctionItem -> throw stepConfig.exception(XProcError.xcXsltParameterNot20Compatible(name, "function"))
                else -> {
                    stepConfig.debug { "Unexpected parameter type: ${value} passed to p:xslt (2.0)"}
                }
            }
        }

        runXsltProcessor(sources.firstOrNull())
    }

    private fun runXsltProcessor(document: XProcDocument?) {
        val processor = stepConfig.processor
        val config = processor.underlyingConfiguration

        val collectionFinder = config.collectionFinder
        val unparsedTextURIResolver = config.unparsedTextURIResolver

        val compiler = processor.newXsltCompiler()
        compiler.isSchemaAware = processor.isSchemaAware
        compiler.errorReporter = errorReporter
        compiler.resourceResolver = stepConfig.environment.documentManager

        val exec = try {
            compiler.compile((stylesheet.value as XdmNode).asSource())
        } catch (sae: Exception) {
            // Compile time exceptions are caught
            if (goesBang != null) {
                throw goesBang!!.exception()
            }

            // Runtime ones are not
            var cause: QName? = null;
            if (sae.cause != null && sae.cause is XPathException) {
                val sname = (sae.cause as XPathException).errorCodeQName
                if (sname != null) {
                    cause = QName(sname)
                }
            }

            when (cause) {
                null -> Unit
                NsFn.errXTMM9000 -> throw stepConfig.exception(XProcError.xcXsltUserTermination(sae.message!!))
                NsFn.errXTDE0040 -> throw stepConfig.exception(XProcError.xcXsltNoTemplate(templateName!!))
                else -> throw stepConfig.exception(XProcError.xcXsltRuntimeError(sae.message!!))
            }

            throw stepConfig.exception(XProcError.xcXsltCompileError(sae.message!!, sae))
        }

        val transformer = exec.load30()
        transformer.resourceResolver = stepConfig.environment.documentManager
        transformer.setResultDocumentHandler(DocumentHandler())
        transformer.setStylesheetParameters(parameters)

        if (populateDefaultCollection) {
            transformer.underlyingController.setDefaultCollection(XProcCollectionFinder.DEFAULT)
            val docs = mutableListOf<XProcDocument>()
            for (doc in sources) {
                if (doc.value is XdmNode) {
                    docs.add(doc)
                }
            }
            transformer.underlyingController.setCollectionFinder(XProcCollectionFinder(docs, collectionFinder))
        }

        val inputSelection = if (document != null) {
            var sel: XdmValue = XdmEmptySequence.getInstance()
            for (doc in sources) {
                sel = sel.append(doc.value)
            }
            sel
        } else {
            XdmEmptySequence.getInstance()
        }

        transformer.setMessageHandler { message ->
            val extra = mutableMapOf<QName, String>()
            extra[Ns.code] = "Q{${message.errorCode.namespaceUri}}${message.errorCode.localName}"
            if (!stepParams.stepName.startsWith("!")) {
                extra[Ns.stepName] = stepParams.stepName
            }
            message.location.systemId?.let { extra[_systemIdentifier] = it }
            message.location.publicId?.let { extra[_publicIdentifier] = it }
            if (message.location.lineNumber > 0) {
                extra[Ns.lineNumber] = "${message.location.lineNumber}"
            }
            if (message.location.columnNumber > 0) {
                extra[Ns.columnNumber] = "${message.location.columnNumber}"
            }
            if (message.isTerminate) {
                extra[_terminate] = "true"
            }

            if (message.isTerminate) {
                stepConfig.environment.messageReporter.report(Verbosity.ERROR, extra, { message.toString() })
                terminationError = XProcError.xcXsltUserTermination(message.content.stringValue)
                    .at(stepParams.location).at(message.location)
            } else {
                stepConfig.environment.messageReporter.report(Verbosity.INFO, extra, { message.toString() })
            }
        }

        val documentResolver = MyResultDocumentResolver(processor.underlyingConfiguration)
        transformer.underlyingController.setResultDocumentResolver(documentResolver)

        primaryOutputProperties.putAll(S9Api.serializationPropertyMap(transformer.underlyingController.executable.primarySerializationProperties))
        var buildTree = false
        if (primaryOutputProperties.contains(BUILD_TREE)) {
            buildTree = ValueUtils.isTrue(primaryOutputProperties.get(BUILD_TREE))
        } else {
            val method = primaryOutputProperties.get(Ns.method)
            if (method != null) {
                buildTree = listOf("xml", "html", "xhtml", "text").contains(method.toString())
            } else {
                buildTree = true
            }
        }
        primaryDestination = if (buildTree) {
            XdmDestination()
        } else {
            RawDestination()
        }

        if (initialMode != null) {
            try {
                transformer.setInitialMode(initialMode!!)
            } catch (sae: SaxonApiException) {
                throw stepConfig.exception(XProcError.xcXsltNoMode(initialMode!!, sae.message!!))
            }
        }

        if (outputBaseUri != null) {
            if (stepConfig.baseUri != null) {
                transformer.setBaseOutputURI(stepConfig.baseUri!!.resolve(outputBaseUri!!).toString())
            } else {
                transformer.setBaseOutputURI(outputBaseUri!!.toString())
            }
        } else {
            if (document != null) {
                if (document.properties.baseURI != null) {
                    transformer.setBaseOutputURI(document.properties.baseURI.toString())
                } else if (document.value is XdmNode) {
                    val base = (document.value as XdmNode).baseURI
                    if (base != null) {
                        transformer.setBaseOutputURI(base.toString())
                    }
                }
            } else {
                if (stylesheet.baseURI != null) {
                    transformer.setBaseOutputURI(stylesheet.baseURI.toString())
                }
            }
        }

        transformer.setSchemaValidationMode(ValidationMode.DEFAULT)
        transformer.getUnderlyingController().setUnparsedTextURIResolver(unparsedTextURIResolver)

        if (globalContextItem != null && globalContextItem!!.value != XdmEmptySequence.getInstance()) {
            transformer.setGlobalContextItem(globalContextItem!!.value as XdmItem)
        }

        if (version != "3.0" && document != null && document.value != XdmEmptySequence.getInstance()) {
            transformer.setGlobalContextItem(document.value as XdmItem)
        }

        /*
        if (defaulted.contains(Ns.globalContextItem)) {
            if (sources.size == 1) {
                transformer.setGlobalContextItem(sources.first().value as XdmItem)
            }
        } else {
            if (globalContextItem!!.value != XdmEmptySequence.getInstance()) {
                transformer.setGlobalContextItem(globalContextItem!!.value as XdmItem)
            }
        }
         */

        try {
            if (templateName != null) {
                transformer.callTemplate(templateName!!, primaryDestination)
            } else {
                transformer.applyTemplates(inputSelection, primaryDestination)
            }
        } catch (ex: SaxonApiException) {
            when (ex.errorCode) {
                NsFn.errXTMM9000 -> {
                    if (terminationError != null) {
                        throw terminationError!!.exception(ex)
                    }
                    throw stepConfig.exception(XProcError.xcXsltUserTermination(ex.message ?: ""), ex)
                }
                NsFn.errXTDE0040 -> throw stepConfig.exception(XProcError.xcXsltNoTemplate(templateName!!), ex)
                else -> throw stepConfig.exception(XProcError.xcXsltRuntimeError(ex.message!!), ex)
            }
        }

        when (primaryDestination) {
            is RawDestination -> {
                val seq = mutableListOf<XdmValue>()
                val raw = primaryDestination as RawDestination
                val iter = raw.xdmValue.iterator()
                while (iter.hasNext()) {
                    seq.add(iter.next())
                }

                if (seq.size == 1 && seq.first() is XdmNode) {
                    val result = seq.first() as XdmNode
                    val props = DocumentProperties()
                    if (result.baseURI != null) {
                        props[Ns.baseUri] = result.baseURI
                    }
                    val doc = if (ValueUtils.contentClassification(result) == MediaType.TEXT) {
                        XProcDocument.ofText(result, stepConfig, MediaType.TEXT, props)
                    } else {
                        XProcDocument.ofXml(result, stepConfig, props)
                    }
                    receiver.output("result", doc)
                } else {
                    for (item in seq) {
                        val doc = XProcDocument.ofValue(item, stepConfig, MediaType.JSON, DocumentProperties())
                        receiver.output("result", doc)
                    }
                }
            }
            is XdmDestination -> {
                val tree = (primaryDestination as XdmDestination).xdmNode
                if (tree.baseURI != null) {
                    val props = DocumentProperties()
                    props[Ns.baseUri] = tree.baseURI
                    props[Ns.contentType] =  serializationContentType(primaryOutputProperties, ValueUtils.contentClassification(tree) ?: MediaType.XML)
                    if (primaryOutputProperties.isNotEmpty()) {
                        props[Ns.serialization] = serializationProperties(primaryOutputProperties)
                    }
                    val doc = XProcDocument.ofXml(tree, stepConfig, props)
                    receiver.output("result", doc)
                } else {
                    receiver.output("result", XProcDocument.ofXml(tree, stepConfig))
                }
            }
        }

        for ((uri, result) in documentResolver.results) {
            val destination = result.first
            val serprops = result.second

            when (destination) {
                is RawDestination -> {
                    val iter = destination.xdmValue.iterator()
                    val items = mutableListOf<XdmItem>()
                    while (iter.hasNext()) {
                        items.add(iter.next())
                    }

                    if (items.isEmpty()) {
                        val props = DocumentProperties()
                        if (serprops.isNotEmpty()) {
                            props[Ns.serialization] = stepConfig.asXdmMap(serprops)
                        }
                        receiver.output("secondary", XProcDocument.ofText(XdmEmptySequence.getInstance(), stepConfig, MediaType.XML, props))
                    } else {
                        consumeSecondary(items, uri, serprops)
                    }
                }
                is XdmDestination -> {
                    consumeSecondary(listOf(destination.xdmNode), uri, serprops)
                }
            }
        }
    }

    private fun consumeSecondary(results: List<XdmItem>, uri: URI, serprops: Map<QName,XdmValue>) {
        val prop = DocumentProperties()
        prop[Ns.baseUri] = uri
        if (primaryOutputProperties.isNotEmpty()) {
            prop[Ns.serialization] = serializationProperties(primaryOutputProperties)
        }

        if (results.get(0) is XdmNode) {
            // Sigh. Secondary output documents don't have the correct intrinsict base URI,
            // so we rebuild documents around them where we can with the correct URI.
            // Also make sure they're always documents, not just elements.
            val builder = SaxonTreeBuilder(stepConfig)
            builder.startDocument(uri)
            for (item in results) {
                if (item is XdmNode) {
                    builder.addSubtree(S9Api.adjustBaseUri(item, uri))
                } else {
                    builder.addSubtree(item)
                }
            }
            builder.endDocument()
            val doc = builder.result

            prop[Ns.contentType] =  serializationContentType(primaryOutputProperties, ValueUtils.contentClassification(doc) ?: MediaType.XML)

            if (ValueUtils.contentClassification(doc) == MediaType.TEXT) {
                receiver.output("secondary", XProcDocument.ofText(doc, stepConfig, MediaType.TEXT, prop))
            } else {
                receiver.output("secondary", XProcDocument.ofXml(doc, stepConfig, prop))
            }
        } else {
            for (item in results) {
                val doc = XProcDocument.ofValue(item, stepConfig, MediaType.JSON, prop)
                receiver.output("secondary", doc)
            }
        }
    }

    private fun serializationProperties(props: Map<QName,XdmValue>): XdmMap {
        var serprop = XdmMap()
        for ((name, value) in primaryOutputProperties) {
            val strval = value.underlyingValue.stringValue
            if (strval == "yes" || strval == "no") {
                // Hack; we should check if the property is boolean...but what about extension properties?
                serprop = serprop.put(XdmAtomicValue(name), XdmAtomicValue(strval == "yes"))
            } else {
                serprop = serprop.put(XdmAtomicValue(name), value)
            }
        }
        return serprop
    }

    private fun serializationContentType(props: Map<QName,XdmValue>, default: MediaType): MediaType {
        if (!props.containsKey(Ns.method)) {
            return default
        }

        val method = props[Ns.method]!!.underlyingValue.stringValue
        when (method) {
            "xml" -> return MediaType.XML
            "html" -> return MediaType.HTML
            "xhtml" -> return MediaType.XHTML
            "text" -> return MediaType.TEXT
            "json" -> return MediaType.JSON
            else -> return default
        }
    }

    private fun xslt10() {
        throw stepConfig.exception(XProcError.xcVersionNotAvailable(version ?: "null"))
    }

    override fun toString(): String = "p:xslt"

    inner class DocumentHandler(): (URI) -> Destination {
        override fun invoke(uri: URI): Destination {
            val xdmResult = XdmDestination()
            xdmResult.setBaseURI(uri)
            xdmResult.onClose(DocumentCloseAction(uri, xdmResult))
            return xdmResult
        }
    }

    inner class BaseURIMapper(val origBase: String?): (NodeInfo) -> String {
        override fun invoke(node: NodeInfo): String {
            var base = node.baseURI
            if (origBase != null && (base == null || base == "")) {
                base = origBase
            }
            return base
        }
    }

    inner class SystemIdMapper(): (NodeInfo) -> String {
        // This is a nop for now
        override fun invoke(node: NodeInfo): String {
            return node.systemId
        }
    }

    inner class DocumentCloseAction(val uri: URI, val destination: XdmDestination): Action {
        override fun act() {
            var doc = destination.xdmNode
            val bmapper = BaseURIMapper(doc.baseURI.toASCIIString())
            val smapper = SystemIdMapper()
            val treeinfo = doc.underlyingNode.treeInfo
            val rebaser = RebasedDocument(treeinfo, bmapper, smapper)
            val xfixbase = rebaser.wrap(doc.underlyingNode)
            doc = XdmNode(xfixbase)

            // FIXME: what should the properties be?
            receiver.output("secondary", XProcDocument.ofXml(doc, stepConfig))
        }
    }

    inner class MyResultDocumentResolver(val sconfig: Configuration): ResultDocumentResolver {
        val results = mutableMapOf<URI, Pair<Destination, MutableMap<QName, XdmValue>>>()

        override fun resolve(context: XPathContext, href: String, baseUri: String, properties: SerializationProperties): Receiver {
            synchronized(results) {
                val tree = properties.getProperty(SaxonOutputKeys.BUILD_TREE)
                val uri = ResolveURI.makeAbsolute(href, baseUri)
                val destination = if (tree == "yes") {
                    XdmDestination()
                } else {
                    RawDestination()
                }

                val xprocProps = mutableMapOf<QName, XdmValue>()
                // FIXME: copy the serialization properties?

                results[uri] = Pair(destination, xprocProps)

                val pc = PipelineConfiguration(sconfig)
                return destination.getReceiver(pc, properties);
            }
        }
    }
}