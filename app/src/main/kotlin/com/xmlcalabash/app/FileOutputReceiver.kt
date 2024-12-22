package com.xmlcalabash.app

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.DefaultOutputReceiver
import net.sf.saxon.s9api.Processor
import org.apache.logging.log4j.kotlin.logger
import java.io.FileOutputStream
import java.io.PrintStream

class FileOutputReceiver(xmlCalabash: XmlCalabash, processor: Processor, val files: Map<String,OutputFilename>
): DefaultOutputReceiver(xmlCalabash, processor) {
    private val wroteTo = mutableSetOf<String>()
    override fun output(port: String, document: XProcDocument) {
        if (port in files) {
            val output = files[port]!!
            val outfile = files[port]!!.nextFile()

            if (wroteTo.contains(port)) {
                if (!output.isSequential()) {
                    logger.warn { "Overwriting ${outfile.absolutePath}"}
                }
            }
            wroteTo.add(port)

            val stream = PrintStream(FileOutputStream(outfile))
            val serializer = XProcSerializer(xmlCalabash, processor)
            //serializer.overrideProperties[Ns.omitXmlDeclaration] = "yes"
            //serializer.overrideProperties[Ns.indent] = "yes"
            serializer.write(document, stream)
            stream.close()
        } else {
            super.output(port, document)
        }
    }
}