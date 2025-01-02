package com.xmlcalabash.ext.epubcheck

import com.adobe.epubcheck.api.EPUBLocation
import com.adobe.epubcheck.api.EpubCheck
import com.adobe.epubcheck.messages.MessageId
import com.adobe.epubcheck.messages.Severity
import com.adobe.epubcheck.util.DefaultReportImpl
import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.xvrl.XvrlDetection
import com.xmlcalabash.xvrl.XvrlReport
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import java.io.ByteArrayInputStream

class EPubCheckStep(): AbstractAtomicStep() {
    lateinit var reporter: MessageReporter
    lateinit var report: XvrlReport

    companion object {
        val epubNamespace = NamespaceUri.of("http://xmlcalabash.com/ns/epubcheck")
        val epub_path = QName(epubNamespace, "epub:path")
    }

    override fun run() {
        super.run()

        report = XvrlReport.newInstance(stepConfig)
        reporter = stepConfig.environment.messageReporter

        val assertValid = booleanBinding(Ns.assertValid) ?: true

        val epub = queues["source"]!!.first() as XProcBinaryDocument
        if (epub.baseURI != null && epub.baseURI.toString().isNotEmpty()) {
            report.metadata.document(epub.baseURI!!)
        }

        val bais = ByteArrayInputStream(epub.binaryValue)
        val epubReport = EpubReport(epub.baseURI.toString())
        val epubCheck = EpubCheck(bais, epubReport, epub.baseURI.toString())
        epubCheck.check()

        val node = report.asXml()
        if (report.digest.errorCount > 0 || report.digest.fatalErrorCount > 0) {
            if (assertValid) {
                throw stepConfig.exception(XProcError.xdStepFailed("EPUBCheck reported errors"))
            }
        }

        receiver.output("result", epub)
        receiver.output("report", XProcDocument.ofXml(node, stepConfig, MediaType.XML, DocumentProperties()))
    }

    override fun toString(): String = "cx:epubcheck"

    inner class EpubReport(epubName: String): DefaultReportImpl(epubName, null, true) {
        override fun message(id: MessageId?, location: EPUBLocation?, vararg args: Any?) {
            super.message(id, location, *args)
            val message = getDictionary().getMessage(id)
            val text = message.getMessage(*args)

            val attr = mutableMapOf<QName, String?>()
            attr[epub_path] = location?.path?.toString()

            var detection: XvrlDetection? = null
            when (message.severity) {
                Severity.SUPPRESSED -> reporter.trace { text }
                Severity.USAGE -> {
                    reporter.debug { text }
                    detection = report.detection("error", "${id}", text, attr)
                }
                Severity.ERROR -> {
                    reporter.error { text }
                    detection = report.detection("error", "${id}", text, attr)
                }
                Severity.WARNING -> {
                    reporter.warn { text }
                    detection = report.detection("warning", "${id}", text, attr)
                }
                Severity.INFO -> {
                    reporter.info { text }
                    detection = report.detection("info", "${id}", text, attr)
                }
                Severity.FATAL -> {
                    reporter.error { text }
                    detection = report.detection("fatal-error", "${id}", text, attr)
                }
            }

            if (detection != null && location != null) {
                detection.location(location.url?.toJavaURI(), location.line, location.column)
                if (location.context.isPresent) {
                    detection.context(location.context.get())
                }
            }
        }
    }
}