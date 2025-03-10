package com.xmlcalabash.steps

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsFn
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.lib.ErrorReporter
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XmlProcessingError

open class CompareStep(): AbstractAtomicStep() {
    lateinit var source: XProcDocument
    lateinit var alternate: XProcDocument
    var failIfNotEqual = false
    var differences = mutableListOf<String>()

    override fun run() {
        super.run()
        source = queues["source"]!!.first()
        alternate = queues["alternate"]!!.first()

        val method = qnameBinding(Ns.method) ?: Ns.deepEqual
        failIfNotEqual = booleanBinding(Ns.failIfNotEqual) ?: false

        if (method == Ns.deepEqual || method == NsFn.deepEqual) {
            deepEqualCompare()
        } else {
            throw stepConfig.exception(XProcError.xcComparisonMethodNotSupported(method))
        }
    }

    private fun report(theSame: Boolean) {
        if (failIfNotEqual && !theSame) {
            throw stepConfig.exception(XProcError.xcComparisonFailed())
        }

        if (differences.isNotEmpty()) {
            val builder = SaxonTreeBuilder(stepConfig)
            builder.startDocument(stepConfig.baseUri)
            builder.addStartElement(NsCx.report)
            for (message in differences) {
                builder.addStartElement(NsCx.difference)
                builder.addText(message)
                builder.addEndElement()
            }
            builder.addEndElement()
            builder.endDocument()
            receiver.output("differences", XProcDocument.ofXml(builder.result, stepConfig))
        }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(stepConfig.baseUri)
        builder.addStartElement(NsC.result)
        builder.addText("${theSame}")
        builder.addEndElement()
        builder.endDocument()
        receiver.output("result", XProcDocument.ofXml(builder.result, stepConfig))
    }

    private fun deepEqualCompare() {
        val sourceCt = source.contentType ?: MediaType.XML
        val alternateCt = alternate.contentType ?: MediaType.XML

        val sourceClass = sourceCt.classification()
        val alternateClass = alternateCt.classification()

        if (sourceClass == MediaClassification.TEXT && alternateClass == MediaClassification.TEXT) {
            deepEqualText()
            return
        }

        if (sourceClass in listOf(MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML)
            && alternateClass in listOf(MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML)) {
            deepEqualXml()
            return
        }

        if (source is XProcBinaryDocument && alternate is XProcBinaryDocument) {
            val sourceBytes = (source as XProcBinaryDocument).binaryValue
            val alternateBytes = (alternate as XProcBinaryDocument).binaryValue
            report(sourceBytes == alternateBytes)
            return
        }

        throw stepConfig.exception(XProcError.xcComparisonNotPossible(sourceCt, alternateCt))
    }

    private fun deepEqualText() {
        val scontent = S9Api.textContent(source)
        val acontent = S9Api.textContent(alternate)
        report(scontent == acontent)
    }

    private fun deepEqualXml() {
        val compiler = stepConfig.newXPathCompiler()
        compiler.declareVariable(QName("a"))
        compiler.declareVariable(QName("b"))
        // In Saxon 12+, we have access to the 4.0 version of deep-equal...
        val selector = compiler.compile("deep-equal(\$a, \$b, default-collation(), map { 'debug': true() })").load()
        selector.resourceResolver = stepConfig.environment.documentManager
        selector.setErrorReporter(CompareReporter(this))
        selector.setVariable(QName("a"), source.value)
        selector.setVariable(QName("b"), alternate.value)
        val theSame = selector.effectiveBooleanValue()
        report(theSame)
    }

    override fun toString(): String = "p:compare"

    class CompareReporter(val compare: CompareStep): ErrorReporter {
        override fun report(error: XmlProcessingError?) {
            if (error != null && error.message != null) {
                compare.differences.add(error.message!!)
            }
        }
    }
}