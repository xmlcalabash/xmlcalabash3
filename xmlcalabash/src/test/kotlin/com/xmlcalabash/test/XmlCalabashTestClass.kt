package com.xmlcalabash.test

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.datamodel.XProcInstruction
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.NsXs
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import javax.xml.transform.sax.SAXSource

open class XmlCalabashTestClass {
    fun type(name: String): String {
        return "Q{${NsXs.namespace}}${name}"
    }

    fun fromString(instruction: XProcInstruction, xml: String, properties: DocumentProperties = DocumentProperties()): XProcDocument {
        return fromString(instruction.stepConfig, xml, properties, emptyMap())
    }

    fun fromString(stepConfig: StepConfiguration, xml: String, properties: DocumentProperties = DocumentProperties()): XProcDocument {
        return fromString(stepConfig, xml, properties, emptyMap())
    }

    fun fromString(stepConfig: StepConfiguration, xml: String, properties: DocumentProperties, parameters: Map<QName, XdmValue>): XProcDocument {
        val builder = stepConfig.processor.newDocumentBuilder()
        builder.isLineNumbering = true
        val bais = ByteArrayInputStream(xml.toByteArray())
        val input = InputSource(bais)
        input.systemId = stepConfig.baseUri?.toString()
        val source = SAXSource(input)
        val xdm = builder.build(source)
        return XProcDocument.ofXml(xdm, stepConfig, MediaType.XML, properties)
    }
}