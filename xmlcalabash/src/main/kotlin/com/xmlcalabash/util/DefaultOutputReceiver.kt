package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.api.Receiver
import net.sf.saxon.s9api.Processor

open class DefaultOutputReceiver(val processor: Processor): Receiver {
    private var port = ""
    private var totals = mutableMapOf<String, Int>()
    // This is a crude check, if you really care, use --output
    private val writingToTerminal = System.console() != null

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
        val serializer = XProcSerializer(processor)
        serializer.overrideProperties[Ns.omitXmlDeclaration] = "yes"
        serializer.overrideProperties[Ns.indent] = "yes"
        serializer.write(document, System.out)
        if (writingToTerminal) {
            println("".padEnd(header.length, '='))
        }
    }
}