package com.xmlcalabash.util

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import net.sf.saxon.s9api.Processor
import org.apache.logging.log4j.kotlin.logger

open class DefaultOutputReceiver(val xmlCalabash: XmlCalabash, val processor: Processor): Receiver {
    constructor(config: XProcStepConfiguration): this(config.xmlCalabash, config.processor)

    companion object {
        // This is a crude check, if you really care, use --output
        private val writingToTerminal = System.console() != null
        private var debugOutputShown = false // man, this is persnickty
    }

    private var port = ""
    private var totals = mutableMapOf<String, Int>()

    init {
        if (!debugOutputShown && writingToTerminal) {
            logger.debug { "Output is going to the terminal" }
        }
        debugOutputShown = true
    }

    override fun output(port: String, document: XProcDocument) {
        this.port = port
        totals[port] = totals.getOrDefault(port, 0) + 1
        write(port, document, totals[port]!!, -1)
    }

    fun write(port: String, document: XProcDocument, position: Int, total: Int) {
        val header = if (total > 0) {
            "=== ${port} :: ${position}/${total} :: ${document.baseURI} ===".padEnd(72, '=')
        } else {
            "=== ${port} :: ${position} :: ${document.baseURI} ===".padEnd(72, '=')
        }
        if (writingToTerminal) {
            println(header)
        }
        val contentType = document.contentType
        val writer = DocumentWriter(document, System.out)

        if (writingToTerminal && contentType != null) {
            if (contentType.classification() in listOf(MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML)) {
                writer[Ns.omitXmlDeclaration] = true
            }
            writer[Ns.indent] = true
        }

        writer.write()

        if (writingToTerminal) {
            println("".padEnd(header.length, '='))
        }
    }
}