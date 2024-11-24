package com.xmlcalabash.ext.metadataextractor

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue

class MetadataExtractor(): AbstractAtomicStep() {
    companion object {
        private val _assertMetadata = QName("assert-metadata")
    }

    lateinit var document: XProcDocument
    lateinit var properties: Map<QName, XdmValue>
    var assertMetadata = false

    override fun input(port: String, doc: XProcDocument) {
        document = doc
    }

    override fun run() {
        super.run()

        properties = qnameMapBinding(Ns.properties)
        assertMetadata = booleanBinding(_assertMetadata) ?: false

        val impl = MetadataExtractorImpl(stepConfig, document, properties)

        try {
            val result = impl.extract()
            receiver.output("result", XProcDocument.ofXml(result, stepConfig))
        } catch (ex: Exception) {
            if (assertMetadata) {
                throw ex
            }
            receiver.output("result", XProcDocument.ofEmpty(stepConfig))
        }
    }

    override fun toString(): String = "cx:metadata-extractor"
}