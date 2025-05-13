package com.xmlcalabash.util

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.api.RuntimePort
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.type.StringConverter
import org.apache.logging.log4j.kotlin.logger

open class DefaultOutputReceiver(val xmlCalabash: XmlCalabash,
                                 val processor: Processor,
                                 val outputManifold: Map<String, RuntimePort>,
                                 val ports: Set<String> = outputManifold.keys): Receiver {
    constructor(config: XProcStepConfiguration, outputManifold: Map<String, RuntimePort>): this(config.xmlCalabash, config.processor, outputManifold)

    companion object {
        // This is a crude check, if you really care, use --output
        private val writingToTerminal = true // System.console() != null
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
        val contentType = document.contentType

        val externalSerialization = mutableMapOf<QName, XdmValue>()
        val configProps = xmlCalabash.config.serialization[contentType] ?: emptyMap()
        for ((name, value) in configProps) {
            val untypedValue = StringConverter.StringToUntypedAtomic().convert(XdmAtomicValue(value).underlyingValue)
            externalSerialization[name] = XdmAtomicValue.wrap(untypedValue)
        }
        val writer = DocumentWriter(document, System.out, externalSerialization)

        val runtimePort = outputManifold[port]
        val decorate = writingToTerminal && (runtimePort?.sequence == true || ports.size > 1)

        if (xmlCalabash.config.pipe) {
            writer.write()
            return
        }

        val header = if (total > 0) {
            "=== ${port} :: ${position}/${total} :: ${document.baseURI} ===".padEnd(72, '=')
        } else {
            "=== ${port} :: ${position} :: ${document.baseURI} ===".padEnd(72, '=')
        }

        if (decorate) {
            println(header)
        }

        if (decorate && contentType != null) {
            if (contentType.classification() in listOf(MediaClassification.XML, MediaClassification.XHTML, MediaClassification.HTML)) {
                writer[Ns.encoding] = xmlCalabash.config.consoleEncoding
                if (xmlCalabash.config.consoleEncoding.lowercase() == "utf-8") {
                    writer[Ns.omitXmlDeclaration] = true
                }
            }
            writer[Ns.indent] = true
        }

        writer.write()

        if (decorate) {
            println("".padEnd(header.length, '='))
        }
    }
}