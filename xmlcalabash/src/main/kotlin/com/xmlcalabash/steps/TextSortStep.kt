package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXslt
import com.xmlcalabash.runtime.parameters.StepParameters
import com.xmlcalabash.util.SaxonErrorReporter
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.TransformationNamespaces
import net.sf.saxon.s9api.*
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import javax.xml.transform.sax.SAXSource

open class TextSortStep(): AbstractTextStep() {
    override fun run() {
        super.run()

        val source = queues["source"]!!.first()
        val sortKey = stringBinding(Ns.sortKey) ?: "."
        val order = stringBinding(Ns.order)
        val caseOrder = stringBinding(Ns.caseOrder)
        val stable = booleanBinding(Ns.stable)
        var collation = stringBinding(Ns.collation)

        val lang = try {
            stringBinding(Ns.lang)
        } catch (ex: SaxonApiException) {
            throw stepConfig.exception(XProcError.xdBadType("Invalid language"), ex)
        }

        // We accidentally published collations starting with https://www.w3.org/ in the specs :-(
        if (collation != null && collation.startsWith("https://www.w3.org/")) {
            stepConfig.info { "Fixing invalid collation: ${collation}" }
            collation = "http:${collation.substring(6)}"
        }

        // Make sure we do line handling...
        val text = textLines(source)
        val sb = StringBuilder()
        for (index in text.indices) {
            if (index > 0) {
                sb.append("\n")
            }
            sb.append(text[index])
        }

        val tns = TransformationNamespaces(source.inScopeNamespaces)

        val builder = SaxonTreeBuilder(stepConfig.processor)
        val stylesheet = builder.xslt(
            ns = tns.namespaces.toMap(),
            vocabularyPrefix = tns.xslPrefix) {
            output(method="text", encoding="utf-8") { }
            param(name="text-lines", `as`="${tns.xsPrefix}:string*", required=true) { }
            template(name="${tns.xslPrefix}:initial-template") {
                variable(name="lines", `as`="${tns.xsPrefix}:string*") {
                    forEach(select="tokenize(\$text-lines, '\n')") {
                        sort(select=sortKey, order=order, caseOrder=caseOrder, lang=lang, collation=collation, stable="${stable}") { }
                        sequence(select=".") { }
                    }
                }
                // The extra '' assures that we get a newline at the end
                valueOf(select="(\$lines, '')", separator="\n") { }
            }
        }

        // Reading a source directly from an XdmNode doesn't seem to do the right thing with namespace
        // bindings...so serialize first :-(
        val stream = ByteArrayOutputStream()
        val serializer = stepConfig.processor.newSerializer(stream)
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml")
        serializer.setOutputProperty(Serializer.Property.INDENT, "yes")
        serializer.serialize(stylesheet.asSource())
        val xsl = stream.toString(StandardCharsets.UTF_8)

        val bais = ByteArrayInputStream(xsl.toByteArray(StandardCharsets.UTF_8))

        try {
            val errorReporter = SaxonErrorReporter(stepConfig)
            val compiler = stepConfig.processor.newXsltCompiler()
            compiler.resourceResolver = stepConfig.environment.documentManager
            compiler.setErrorReporter(errorReporter)

            val destination = XdmDestination()
            val exec = compiler.compile(SAXSource(InputSource(bais)))
            //val exec = compiler.compile(stylesheet.asSource())
            val transformer = exec.load30()

            transformer.setErrorReporter(errorReporter)
            transformer.setStylesheetParameters(mapOf(Ns.textLines to XdmAtomicValue(sb.toString())))
            transformer.callTemplate(QName(NsXslt.namespace, "${tns.xslPrefix}:initial-template"), destination)
            val sorted = destination.xdmNode
            val result = source.with(sorted)
            receiver.output("result", result)
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdStepFailed("could not sort with requested options"), ex)
        }
    }

    override fun toString(): String = "p:text-count"
}