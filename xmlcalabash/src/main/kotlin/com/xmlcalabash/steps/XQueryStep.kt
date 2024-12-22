package com.xmlcalabash.steps

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.ValueUtils
import com.xmlcalabash.util.XProcCollectionFinder
import net.sf.saxon.event.PipelineConfiguration
import net.sf.saxon.event.Receiver
import net.sf.saxon.lib.SaxonOutputKeys
import net.sf.saxon.s9api.*
import net.sf.saxon.serialize.SerializationProperties
import org.apache.logging.log4j.kotlin.logger
import java.io.ByteArrayOutputStream
import java.net.URI
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
            if (!ctype.xmlContentType() && !ctype.htmlContentType() && !ctype.textContentType()) {
                throw stepConfig.exception(XProcError.xcXQueryInputNot30Compatible(ctype))
            }
        }

        for ((name, value) in parameters) {
            when (value) {
                is XdmAtomicValue, is XdmNode -> Unit
                is XdmMap -> throw stepConfig.exception(XProcError.xcXQueryInvalidParameterType(name, "map"))
                is XdmArray -> throw stepConfig.exception(XProcError.xcXQueryInvalidParameterType(name, "array"))
                is XdmFunctionItem -> throw stepConfig.exception(XProcError.xcXQueryInvalidParameterType(name, "function"))
                else -> logger.debug { "Unexpected parameter type: ${value} passed to p:xquery"}
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
            if (query.contentType != null && query.contentType!!.xmlContentType()) {
                val root = S9Api.documentElement(query.value as XdmNode)
                if (root.nodeName != NsC.query) {
                    val baos = ByteArrayOutputStream()

                    val xserializer = XProcSerializer(stepConfig)
                    xserializer.setOverrideProperty(MediaType.XML, Ns.encoding, "UTF-8")
                    xserializer.setOverrideProperty(MediaType.XML, Ns.omitXmlDeclaration, "true")
                    xserializer.write(query, baos, MediaType.XML)

                    xquery = baos.toString("utf-8")
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
        } catch (ex: Throwable) {
            throw stepConfig.exception(XProcError.xcXQueryEvalError(ex.message ?: "null"), ex)
        }

        try {
            val seq = mutableListOf<XdmValue>()
            val iter = queryEval.iterator()
            while (iter.hasNext()) {
                seq.add(iter.next())
            }

            for (item in seq) {
                when (item) {
                    is XdmNode -> {
                        val props = DocumentProperties()
                        if (item.baseURI != null) {
                            props[Ns.baseUri] = item.baseURI
                        }
                        val doc = if (ValueUtils.contentClassification(item) == MediaType.TEXT) {
                            XProcDocument.ofText(item, stepConfig, MediaType.TEXT, props)
                        } else {
                            XProcDocument.ofXml(item, stepConfig, props)
                        }
                        receiver.output("result", doc)
                    }
                    else -> {
                        val doc = XProcDocument.ofValue(item, stepConfig, MediaType.JSON, DocumentProperties())
                        receiver.output("result", doc)
                    }
                }
            }
        } catch (ex: Exception) {
            if (goesBang != null) {
                throw goesBang!!.exception()
            }
            throw ex
        }

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
            // nop
        }

        override fun error(e: TransformerException) {
            goesBang = XProcError.xcXQueryCompileError(e.message!!, e)
        }

        override fun fatalError(e: TransformerException) {
            goesBang = XProcError.xcXQueryCompileError(e.message!!, e)
        }
    }
}