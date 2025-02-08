package com.xmlcalabash.test

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.SchematronImpl
import com.xmlcalabash.xvrl.XvrlReport
import com.xmlcalabash.xvrl.XvrlReports
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmDestination
import net.sf.saxon.s9api.XdmNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.io.File
import java.net.URI

@TestInstance(PER_CLASS)
class XvrlReportTest {
    lateinit var stepConfig: XProcStepConfiguration

    @BeforeAll
    fun init() {
        val xmlCalabash = XmlCalabash.newInstance()
        val builder = xmlCalabash.newPipelineBuilder()
        stepConfig = builder.stepConfig
    }

    @Test
    fun metadataTest() {
        val report = XvrlReport.newInstance(stepConfig)
        report.metadata.timestamp()
        report.metadata.validator(stepConfig.environment.productName, stepConfig.environment.productVersion, "content")
        report.metadata.creator(stepConfig.environment.vendor, mapOf(NsXml.id to "test"))
        report.metadata.document(URI.create("https://xmlcalabash.com/"))

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(null)
        builder.addStartElement(QName("h1"))
        builder.addText("Some Title")
        builder.addEndElement()
        builder.endDocument()

        report.metadata.title(builder.result)
        report.metadata.summary("What summary?")
        report.metadata.category("test", "https://nwalsh.com/ns/vocabulary")
        report.metadata.schema(URI.create("file://tmp/schema.xml"), NsXs.namespace, "3.4", mapOf(NsXml.id to "test2"))
        report.metadata.supplemental("Wat?")

        val node = report.asXml()
        Assertions.assertNotNull(node)
    }

    @Test
    fun detectionTest() {
        val report = XvrlReport.newInstance(stepConfig)

        val detect = report.detection("info", "42")
        detect.location(URI.create("file:/tmp/out.xml"))
        detect.provenance().location(URI.create("file://tmp/out.xml"), 3, 4)

        val location = detect.provenance().location(URI.create("file://tmp/out.xml"))
        location.xpath = "/path/to/thing"

        detect.title("Title")
        detect.title("French Title", mapOf(NsXml.lang to "fr"))
        detect.summary("Summary")
        detect.category("test", "https://nwalsh.com/ns/vocabulary")
        detect.let(QName("prime"), "17")

        detect.message("text message")
        val message = detect.message()
        message.message("text message")
        message.message(QName("mixed"), "")

        val context = detect.context()
        context.location(URI.create("file://tmp/out.xml"))

        val node = report.asXml()
        Assertions.assertNotNull(node)
    }

    fun load(filename: String): XdmNode {
        val builder = stepConfig.processor.newDocumentBuilder()
        val destination = XdmDestination()
        builder.parse(File("src/test/resources/schematron/${filename}"), destination)
        return destination.xdmNode
    }

    @Test
    fun fromSvrl1() {
        val schema = load("schema-001.xml")
        val doc = load("doc-001.xml")
        val schReport = SchematronImpl(stepConfig).report(doc, schema)
        val reports = XvrlReport.fromSvrl(stepConfig, schReport)
        val node = reports.asXml()
        Assertions.assertNotNull(node)
    }

}