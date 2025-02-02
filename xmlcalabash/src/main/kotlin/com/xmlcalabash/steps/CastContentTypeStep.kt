package com.xmlcalabash.steps

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentConverter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue

open class CastContentTypeStep(): AbstractAtomicStep() {
    lateinit var document: XProcDocument
    var docContentType = MediaType.ANY
    var contentType = MediaType.ANY
    var parameters = mapOf<QName,XdmValue>()

    override fun run() {
        super.run()
        document = queues["source"]!!.first()

        docContentType = document.contentType ?: MediaType.OCTET_STREAM
        contentType = mediaTypeBinding(Ns.contentType)
        parameters = qnameMapBinding(Ns.parameters)

        if (contentType == docContentType) {
            // c:data is a special case...
            var passThrough = true
            if (docContentType.classification() == MediaClassification.XML) {
                val element = S9Api.documentElement(document.value as XdmNode)
                passThrough = element.nodeName != NsC.data
            }

            if (passThrough) {
                receiver.output("result", document)
                return
            }
        }

        val converter = DocumentConverter(stepConfig, document, contentType, parameters)
        val result = converter.convert()
        receiver.output("result", result)
    }
    override fun toString(): String = "p:cast-content-type"
}