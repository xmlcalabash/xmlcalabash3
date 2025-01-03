package com.xmlcalabash.app

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.util.DefaultOutputReceiver
import net.sf.saxon.s9api.Processor
import org.apache.logging.log4j.kotlin.logger
import java.io.FileOutputStream

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

            val fos = FileOutputStream(outfile)
            DocumentWriter(document, fos).write()
            fos.close()
        } else {
            super.output(port, document)
        }
    }
}