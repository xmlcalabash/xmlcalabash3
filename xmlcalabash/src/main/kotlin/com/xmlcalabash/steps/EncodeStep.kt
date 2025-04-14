package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.*

class EncodeStep(): AbstractAtomicStep() {
    override fun run() {
        super.run()

        val document = queues["source"]!!.first()
        val encoding = stringBinding(Ns.encoding) ?: "base64"
        val serialization = qnameMapBinding(Ns.serialization)

        if (encoding != "base64") {
            throw stepConfig.exception(XProcError.xcInvalidEncoding(encoding))
        }

        val bytes = if (document is XProcBinaryDocument) {
            document.binaryValue
        } else {
            val baos = ByteArrayOutputStream()
            DocumentWriter(document, baos, serialization).write()
            baos.toByteArray()
        }

        val encoder = Base64.getMimeEncoder()
        val encoded = encoder.encode(bytes).toString(StandardCharsets.UTF_8).replace("\r", "")

        var attr = mutableMapOf<QName, String?>()
        attr[Ns.encoding] = encoding
        attr[Ns.contentType] = document.contentType.toString()
        serialization[Ns.encoding]?.let { attr[Ns.charset] = it.underlyingValue.stringValue }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(stepConfig.baseUri)
        builder.addStartElement(NsC.data, stepConfig.typeUtils.attributeMap(attr))
        builder.addText(encoded)
        builder.addEndElement()
        builder.endDocument()

        receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig))
    }

    override fun toString(): String = "p:encode"
}