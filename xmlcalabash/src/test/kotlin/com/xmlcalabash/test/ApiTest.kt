package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.io.MessagePrinter
import com.xmlcalabash.util.BufferingReceiver
import com.xmlcalabash.util.Report
import com.xmlcalabash.util.Verbosity
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.xml.sax.InputSource
import java.io.*
import java.nio.charset.StandardCharsets
import javax.xml.transform.sax.SAXSource


class ApiTest {
    private lateinit var processor: Processor

    fun parseString(xml: String): XdmNode {
        val stream = ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8))
        return parseStream(stream)
    }

    fun parseStream(stream: InputStream): XdmNode {
        val builder = processor.newDocumentBuilder()
        val input = InputSource(stream)
        input.systemId = "http://example.com/"
        val doc = builder.build(SAXSource(input))
        return doc
    }

    fun anIdentityPipeline(): XdmNode {
        val stream = FileInputStream(File("src/test/resources/identity.xpl"))
        return parseStream(stream)
    }

    fun anXsltPipeline(): XdmNode {
        val stream = FileInputStream(File("src/test/resources/xslt.xpl"))
        return parseStream(stream)
    }

    fun setupXmlCalabash(): XmlCalabash {
        val xmlcalabashBuilder = XmlCalabashBuilder()
        val xmlcalabash = xmlcalabashBuilder.build()
        processor = xmlcalabash.saxonConfiguration.processor
        return xmlcalabash
    }

    @Test
    fun runIdentity() {
        val xmlcalabash = setupXmlCalabash()

        val parser = xmlcalabash.newXProcParser()
        val declareStep = parser.parse(anIdentityPipeline())
        val runtime = declareStep.runtime()
        val pipeline = runtime.executable()

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver

        pipeline.input("source", XProcDocument.ofXml(parseString("<doc/>"), pipeline.config, MediaType.XML))

        pipeline.run()

        val result = receiver.outputs["result"]!!.first().value
        Assertions.assertEquals("<doc/>", result.toString())
    }

    @Test
    fun runXslt() {
        val xmlcalabash = setupXmlCalabash()

        val parser = xmlcalabash.newXProcParser()
        val declareStep = parser.parse(anXsltPipeline())
        val runtime = declareStep.runtime()
        val pipeline = runtime.executable()

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver

        pipeline.input("source", XProcDocument.ofXml(parseString("<doc/>"), pipeline.config, MediaType.XML))

        pipeline.run()

        val result = receiver.outputs["result"]!!.first().value
        Assertions.assertEquals("<doc>You got some output</doc>", result.toString())
    }

    @Test
    fun runXsltWithAMessage() {
        val xmlcalabash = setupXmlCalabash()

        val parser = xmlcalabash.newXProcParser()
        val declareStep = parser.parse(anXsltPipeline())
        val runtime = declareStep.runtime()
        val pipeline = runtime.executable()

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver

        pipeline.input("source", XProcDocument.ofXml(parseString("<doc/>"), pipeline.config, MediaType.XML))
        pipeline.option(QName("message"), XdmAtomicValue("Hello, world."))
        pipeline.run()

        val result = receiver.outputs["result"]!!.first().value
        Assertions.assertEquals("<doc>You got some output</doc>", result.toString())
    }

    @Test
    fun runXsltCaptureMessage() {
        val printer = MyMessagePrinter()
        val reporter = MyMessageReporter(printer)

        val xmlcalabashBuilder = XmlCalabashBuilder()
        xmlcalabashBuilder.setMessagePrinter(printer)
        xmlcalabashBuilder.setMessageReporter(reporter)

        val xmlcalabash = xmlcalabashBuilder.build()
        processor = xmlcalabash.saxonConfiguration.processor

        val parser = xmlcalabash.newXProcParser()
        val declareStep = parser.parse(anXsltPipeline())
        val runtime = declareStep.runtime()
        val pipeline = runtime.executable()

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver

        pipeline.input("source", XProcDocument.ofXml(parseString("<doc/>"), pipeline.config, MediaType.XML))
        pipeline.option(QName("message"), XdmAtomicValue("Hello, world."))
        pipeline.run()

        val result = receiver.outputs["result"]!!.first().value
        Assertions.assertEquals("<doc>You got some output</doc>", result.toString())
    }

    @Test
    fun runXsltCaptureError() {
        val printer = MyMessagePrinter()
        val reporter = MyMessageReporter(printer)

        val xmlcalabashBuilder = XmlCalabashBuilder()
        xmlcalabashBuilder.setMessagePrinter(printer)
        xmlcalabashBuilder.setMessageReporter(reporter)

        val xmlcalabash = xmlcalabashBuilder.build()
        processor = xmlcalabash.saxonConfiguration.processor

        val parser = xmlcalabash.newXProcParser()
        val declareStep = parser.parse(anXsltPipeline())
        val runtime = declareStep.runtime()
        val pipeline = runtime.executable()

        val receiver = BufferingReceiver()
        pipeline.receiver = receiver

        pipeline.input("source", XProcDocument.ofXml(parseString("<doc/>"), pipeline.config, MediaType.XML))
        pipeline.option(QName("message"), XdmAtomicValue("Ruh roh."))
        pipeline.option(QName("fail"), XdmAtomicValue(true))

        try {
            pipeline.run()
            fail()
        } catch (ex: Exception) {
            Assertions.assertNotNull(ex)
        }
    }

    class MyMessagePrinter(): MessagePrinter {
        private var _encoding = "UTF-8"
        override val encoding: String
            get() = _encoding

        override fun setEncoding(encoding: String) {
            _encoding = encoding
        }

        override fun setPrintStream(stream: PrintStream) {
            // nop
        }

        override fun print(message: String) {
            // nop
        }

        override fun println(message: String) {
            // nop
        }
    }

    class MyMessageReporter(messagePrinter: MessagePrinter): MessageReporter {
        private var _messagePrinter = messagePrinter
        private var _threshold = Verbosity.INFO

        override val messagePrinter: MessagePrinter
            get() = _messagePrinter

        override var threshold: Verbosity
            get() = _threshold
            set(value) {
                _threshold = value
            }

        override fun setMessagePrinter(messagePrinter: MessagePrinter) {
            _messagePrinter = messagePrinter
        }

        override fun error(report: () -> Report) {
            report(Verbosity.ERROR, report)
        }

        override fun warn(report: () -> Report) {
            report(Verbosity.WARN, report)
        }

        override fun info(report: () -> Report) {
            report(Verbosity.INFO, report)
        }

        override fun debug(report: () -> Report) {
            report(Verbosity.DEBUG, report)
        }

        override fun trace(report: () -> Report) {
            report(Verbosity.TRACE, report)
        }

        override fun report(severity: Verbosity, report: () -> Report) {
            if (severity >= threshold) {
                println(report().message)
            }
        }
    }
}