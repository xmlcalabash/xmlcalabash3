package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import de.bottlecaps.convert.Convert
import net.sf.saxon.s9api.QName
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class EbnfConvertStep(): AbstractAtomicStep() {
    companion object {
        private val _factoring = QName("factoring")
        private val _removeRecursion = QName("remove-recursion")
        private val _inlineTerminals = QName("inline-terminals")
        private val _epsilonReferences = QName("epsilon-references")
        private val _xml = QName("xml")
    }
    override fun run() {
        super.run()

        val grammarDoc = queues["source"]!!.first()
        val grammar = grammarDoc.value.underlyingValue.stringValue
        val factoring = stringBinding(_factoring)!!
        val recursionRemoval = stringBinding(_removeRecursion)!!
        val inline = booleanBinding(_inlineTerminals)!!
        val epsilon = booleanBinding(_epsilonReferences)!!
        val toXML = booleanBinding(_xml)!!
        val notation = stringBinding(Ns.notation)
        val verbose = booleanBinding(Ns.verbose)!!
        val timestamp = booleanBinding(Ns.timestamp)!!

        val parserImplementation = Convert.ParserImplementation.JAVA

        val ebnf = try {
            Convert.convert(notation, grammar, 0, toXML, recursionRemoval, factoring, inline, epsilon, parserImplementation, !timestamp, verbose)
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcxGrammarConversionFailed(notation ?: "unspecified"))
        }

        if (toXML) {
            val loader = DocumentLoader(stepConfig, grammarDoc.baseURI)
            val xml = loader.load(ByteArrayInputStream(ebnf.toByteArray(StandardCharsets.UTF_8)), MediaType.XML)
            receiver.output("result", xml)
            return
        }

        receiver.output("result", XProcDocument.ofText(ebnf, stepConfig))
    }

    override fun toString(): String = "cx:ebnf-convert"
}