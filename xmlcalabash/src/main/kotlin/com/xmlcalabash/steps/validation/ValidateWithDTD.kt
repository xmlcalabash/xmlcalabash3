package com.xmlcalabash.steps.validation

import com.xmlcalabash.config.XProcStepConfiguration
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.lib.ErrorReporter
import net.sf.saxon.s9api.*
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.transform.sax.SAXSource

class ValidateWithDTD(): AbstractAtomicStep() {
    lateinit var source: XProcDocument
    val errors = mutableListOf<XmlProcessingError>()

    override fun input(port: String, doc: XProcDocument) {
        source = doc
    }

    override fun run() {
        super.run()

        val assertValid = booleanBinding(Ns.assertValid) ?: true

        val stepSerialization = if (options[Ns.serialization] != null) {
            val map = options[Ns.serialization]!!
            if (map.value == XdmEmptySequence.getInstance()) {
                mapOf()
            } else {
                stepConfig.asMap(map.value as XdmMap)
            }
        } else {
            mapOf()
        }

        val documentSerialization = stepConfig.asMap(
            stepConfig.forceQNameKeys(
                (source.properties[Ns.serialization] ?: XdmMap()) as XdmMap
            )
        )

        val serialization = mutableMapOf<QName, XdmValue>()
        serialization.putAll(stepSerialization)
        serialization.putAll(documentSerialization)

        val docText = serializeSource(serialization)

        var result = source.value as XdmNode
        try {
            val parser = stepConfig.processor.newDocumentBuilder()
            parser.isDTDValidation = true
            parser.isLineNumbering = true

            val sconfig = stepConfig.saxonConfig.processor.underlyingConfiguration
            val options = sconfig.parseOptions.withErrorReporter(MyErrorReporter())
            sconfig.parseOptions = options

            val destination = XdmDestination()
            val bais = ByteArrayInputStream(docText.toByteArray(StandardCharsets.UTF_8))
            val vsource = SAXSource(InputSource(bais))
            vsource.systemId = source.baseURI?.toString()
            parser.parse(vsource, destination)
            result = destination.xdmNode

            val props = DocumentProperties()
            props.set(Ns.baseUri, source.properties.baseURI)
            if (source.properties.get(Ns.serialization) != null) {
                props.set(Ns.serialization, source.properties.get(Ns.serialization))
            }
            xvrlReport(null)
            receiver.output("result", XProcDocument.ofXml(result, stepConfig, MediaType.XML, props))
            return
        } catch (ex: Exception) {
            if (assertValid) {
                throw XProcError.xcDtdValidationFailed().exception()
            }

            xvrlReport(ex)
            receiver.output("result", source)
        }
    }

    private fun serializeSource(serialization: Map<QName,XdmValue>): String {
        if (source.contentType!!.xmlContentType()) {
            val baos = ByteArrayOutputStream()
            val serializer = XProcSerializer(stepConfig)
            serializer.write(source.value as XdmNode, baos, serialization)
            return baos.toString(StandardCharsets.UTF_8)
        } else {
            return source.value.underlyingValue.stringValue
        }
    }

    private fun xvrlReport(ex: Exception?) {
        val xvrl = SaxonTreeBuilder(stepConfig)
        xvrl.startDocument(null)
        xvrl.addStartElement(NsXvrl.report)
        xvrl.addStartElement(NsXvrl.metadata)

        xvrl.addStartElement(NsXvrl.timestamp)
        xvrl.addText(ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
        xvrl.addEndElement()

        xvrl.addStartElement(NsXvrl.creator, stepConfig.attributeMap(mapOf(
            Ns.name to stepConfig.saxonConfig.productName,
            Ns.version to stepConfig.saxonConfig.version,
        )))
        xvrl.addEndElement()

        xvrl.addStartElement(NsXvrl.document, stepConfig.attributeMap(mapOf(
            Ns.href to (source.baseURI?.toString() ?: "")
        )))
        xvrl.addEndElement()
        xvrl.addEndElement()

        xvrl.addStartElement(NsXvrl.digest, stepConfig.attributeMap(mapOf(
            Ns.valid to "false",
            QName("error-count") to errors.size.toString(),
            QName("worst") to "error")
        ))
        xvrl.addEndElement()

        for (error in errors) {
            val severity = "error"
            val code = error.errorCode.eqName

            xvrl.addStartElement(NsXvrl.detection, stepConfig.attributeMap(mapOf(
                QName("severity") to severity,
                QName("code") to code,
            )))
            xvrl.addStartElement(NsXvrl.summary)
            xvrl.addText(error.message)
            xvrl.addEndElement()
            if (error.location != null && error.location.lineNumber > 0) {
                xvrl.addStartElement(NsXvrl.location, stepConfig.attributeMap(mapOf(
                    QName("line") to error.location.lineNumber.toString(),
                    QName("column") to error.location.columnNumber.toString())
                ))
                xvrl.addEndElement()
            }
            xvrl.addEndElement()
        }

        if (ex != null) {
            xvrl.addStartElement(NsXvrl.detection, stepConfig.attributeMap(mapOf(
                QName("severity") to "error"
            )))
            xvrl.addStartElement(NsXvrl.summary)
            xvrl.addText(ex.message ?: "")
            xvrl.addEndElement()
            xvrl.addEndElement()
        }

        xvrl.addEndElement()
        xvrl.endDocument()

        val report = xvrl.result

        receiver.output("report", XProcDocument.ofXml(report, stepConfig, MediaType.XML, DocumentProperties()))

    }

    inner class MyErrorReporter : ErrorReporter {
        override fun report(error: XmlProcessingError?) {
            if (error != null) {
                errors.add(error)
            }
        }
    }
}