package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import de.bottlecaps.markup.Blitz
import de.bottlecaps.markup.BlitzException
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.nineml.coffeefilter.InvisibleXml
import java.io.ByteArrayInputStream

class InvisibleXmlMarkupBlitz(stepConfig: XProcStepConfiguration): InvisibleXmlImpl(stepConfig, "markup-blitz") {
    companion object {
        private var invisibleXml: String? = null
    }
    override fun parse(grammar: String?, input: String, failOnError: Boolean, parameters: Map<QName, XdmValue>): XProcDocument {
        val ixmlGrammar = if (grammar == null) {
            if (invisibleXml == null) {
                val ixmlStream = InvisibleXmlMarkupBlitz::class.java.getResourceAsStream("/com/xmlcalabash/mb-ixml.ixml")
                invisibleXml = ixmlStream!!.readAllBytes().toString(Charsets.UTF_8)
            }
            invisibleXml!!
        } else {
            grammar
        }

        val blitzOptions = mutableListOf<Blitz.Option>()
        if (failOnError) {
            blitzOptions.add(Blitz.Option.FAIL_ON_ERROR)
        }

        for ((name, value) in parameters) {
            val bool = value.underlyingValue.effectiveBooleanValue()
            if (bool) {
                when (name) {
                    Ns.indent -> blitzOptions.add(Blitz.Option.INDENT)
                    Ns.trace -> blitzOptions.add(Blitz.Option.TRACE)
                    Ns.timing -> blitzOptions.add(Blitz.Option.TIMING)
                    Ns.verbose -> blitzOptions.add(Blitz.Option.VERBOSE)
                    else -> {
                        stepConfig.warn { "Ignoring unknown parameter: ${name}"}
                    }
                }
            }
        }

        val parser = try {
            Blitz.generate(ixmlGrammar, *blitzOptions.toTypedArray())
        } catch (ex: BlitzException) {
            throw stepConfig.exception(XProcError.xcInvalidIxmlGrammar(), ex)
        }

        val xml = try {
            parser.parse(input, *blitzOptions.toTypedArray())
        } catch (ex: BlitzException) {
            throw stepConfig.exception(XProcError.xcInvisibleXmlParseFailed(), ex)
        }

        val loader = DocumentLoader(stepConfig, null)
        return loader.load(ByteArrayInputStream(xml.toByteArray()), MediaType.XML)
    }
}