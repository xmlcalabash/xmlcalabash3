package com.xmlcalabash.steps

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.ValueUtils
import com.xmlcalabash.util.XProcCollectionFinder
import net.sf.saxon.event.PipelineConfiguration
import net.sf.saxon.event.Receiver
import net.sf.saxon.lib.SaxonOutputKeys
import net.sf.saxon.s9api.*
import net.sf.saxon.serialize.SerializationProperties
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.xml.transform.ErrorListener
import javax.xml.transform.TransformerException

open class XQueryStep(): AbstractAtomicStep() {
    val sources = mutableListOf<XProcDocument>()
    lateinit var query: XProcDocument

    val parameters = mutableMapOf<QName,XdmValue>()
    var version = "3.1"

    var goesBang: XProcError? = null

    private var primaryDestination: Destination? = null
    private var outputProperties = mutableMapOf<QName, XdmValue>()

    override fun run() {
        super.run()
        sources.addAll(queues["source"]!!)
        query = queues["query"]!!.first()

        parameters.putAll(qnameMapBinding(Ns.parameters))
        version = stringBinding(Ns.version) ?: "3.1"

        when (version) {
            "3.1" -> xquery31()
            "3.0" -> xquery30()
            else -> throw stepConfig.exception(XProcError.xcXQueryVersionNotAvailable(version))
        }
    }

    override fun reset() {
        super.reset()
        sources.clear()
        query = XProcDocument.ofEmpty(stepConfig)
    }

    private fun xquery30() {
        for (doc in sources) {
            val ctype = doc.contentType ?: MediaType.OCTET_STREAM
            if (ctype.classification() !in listOf(MediaClassification.XML, MediaClassification.XHTML,
                    MediaClassification.HTML, MediaClassification.TEXT)) {
                throw stepConfig.exception(XProcError.xcXQueryInputNot30Compatible(ctype))
            }
        }

        for ((name, value) in parameters) {
            when (value) {
                is XdmAtomicValue, is XdmNode -> Unit
                is XdmMap -> throw stepConfig.exception(XProcError.xcXQueryInvalidParameterType(name, "map"))
                is XdmArray -> throw stepConfig.exception(XProcError.xcXQueryInvalidParameterType(name, "array"))
                is XdmFunctionItem -> throw stepConfig.exception(XProcError.xcXQueryInvalidParameterType(name, "function"))
                else -> stepConfig.debug { "Unexpected parameter type: ${value} passed to p:xquery"}
            }
        }

        runXQueryProcessor()
    }

    private fun xquery31() {
        runXQueryProcessor()
    }

    private fun runXQueryProcessor() {
        val document = sources.firstOrNull()

        val processor = stepConfig.processor
        val underlyingConfig = processor.underlyingConfiguration
        // FIXME: runtime.getConfigurer().getSaxonConfigurer().configXQuery(config);

        val collectionFinder = underlyingConfig.collectionFinder
        val unparsedTextURIResolver = underlyingConfig.unparsedTextURIResolver

        underlyingConfig.setDefaultCollection(XProcCollectionFinder.DEFAULT)
        underlyingConfig.setCollectionFinder(XProcCollectionFinder(sources, collectionFinder))

        val compiler = processor.newXQueryCompiler()
        compiler.setSchemaAware(processor.isSchemaAware)
        compiler.setErrorListener(MyErrorListener(true))
        val exec = try {
            var xquery = query.value.underlyingValue.stringValue
            if (query.contentClassification == MediaClassification.XML) {
                val root = S9Api.documentElement(query.value as XdmNode)
                if (root.nodeName != NsC.query) {
                    val baos = ByteArrayOutputStream()
                    val writer = DocumentWriter(query, baos)
                    writer[Ns.encoding] = "UTF-8"
                    writer[Ns.omitXmlDeclaration] = true
                    writer.write()
                    xquery = baos.toString(StandardCharsets.UTF_8)
                }
            }

            compiler.compile(xquery)
        } catch (ex: Exception) {
            underlyingConfig.collectionFinder = collectionFinder
            if (goesBang != null) {
                throw goesBang!!.exception()
            }
            throw ex
        }
        val queryEval = exec.load()

        for ((param, value) in parameters) {
            queryEval.setExternalVariable(param, value)
        }

        if (document != null) {
            queryEval.setContextItem(document.value as XdmItem)
        }

        val result = MyDestination(outputProperties)
        queryEval.setDestination(result)

        queryEval.setSchemaValidationMode(ValidationMode.DEFAULT)
        queryEval.setErrorListener(MyErrorListener(false))

        try {
            queryEval.run()
            for (document in S9Api.makeDocuments(stepConfig, queryEval.evaluate())) {
                receiver.output("result", document)
            }
        } catch (ex: Throwable) {
            underlyingConfig.collectionFinder = collectionFinder
            throw stepConfig.exception(XProcError.xcXQueryEvalError(ex.message ?: "null"), ex)
        }

        underlyingConfig.collectionFinder = collectionFinder
    }

    inner class MyDestination(var map: MutableMap<QName,XdmValue>): RawDestination() {
        private var destination: Destination? = null
        private var destBase: URI? = null

        override fun setDestinationBaseURI(baseURI: URI) {
            destBase = baseURI
            if (destination != null) {
                destination!!.setDestinationBaseURI(baseURI)
            }
        }

        override fun getDestinationBaseURI(): URI? {
            return destBase
        }

        override fun getReceiver(pipe: PipelineConfiguration, params: SerializationProperties): Receiver {
            val tree = params.getProperty(SaxonOutputKeys.BUILD_TREE)

            val pnames = params.properties.propertyNames()
            while (pnames.hasMoreElements()) {
                val name: String = pnames.nextElement() as String
                val qname = if (name.startsWith("{")) {
                    ValueUtils.parseClarkName(name)
                } else {
                    QName(name)
                }
                val value = params.properties.getProperty(name) as String
                if (value == "yes" || value == "no") {
                    map.put(qname, XdmAtomicValue(value == "yes"))
                } else {
                    map.put(qname, XdmAtomicValue(value))
                }
            }

            val dest = if (tree == "yes") {
                XdmDestination()
            } else {
                RawDestination()
            }

            if (destBase != null) {
                dest.setDestinationBaseURI(destBase)
            }

            destination = dest
            primaryDestination = dest
            return dest.getReceiver(pipe, params)
        }

        override fun closeAndNotify() {
            if (destination != null) {
                destination!!.closeAndNotify()
            }
        }

        override fun close() {
            if (destination != null) {
                destination!!.close()
            }
        }
    }

    inner class MyErrorListener(val compileTime: Boolean): ErrorListener {
        override fun warning(e: TransformerException) {
            stepConfig.warn { e.messageAndLocation }
        }

        override fun error(e: TransformerException) {
            stepConfig.error { e.messageAndLocation }
            if (compileTime) {
                goesBang = XProcError.xcXQueryCompileError(e.message!!, e)
            }
        }

        override fun fatalError(e: TransformerException) {
            stepConfig.error { e.messageAndLocation }
            if (compileTime) {
                goesBang = XProcError.xcXQueryCompileError(e.message!!, e)
            }
        }
    }
}