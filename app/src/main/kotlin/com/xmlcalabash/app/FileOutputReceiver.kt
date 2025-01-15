package com.xmlcalabash.app

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.util.DefaultOutputReceiver
import net.sf.saxon.s9api.Processor
import org.apache.logging.log4j.kotlin.logger
import java.io.FileOutputStream

class FileOutputReceiver(xmlCalabash: XmlCalabash,
                         processor: Processor,
                         val files: Map<String,OutputFilename>,
                         val stdout: String?
): DefaultOutputReceiver(xmlCalabash, processor) {

    private val wroteTo = mutableSetOf<String>()

    init {
        if (stdout != null) {
            logger.debug { "Writing on ${stdout} to stdout" }
        }
    }

    override fun output(port: String, document: XProcDocument) {
        if (port in files) {
            val output = files[port]!!
            val fos = if (output.pattern == CommandLine.STDIO_NAME) {
                logger.debug { "Writing $port to stdout" }
                System.out
            } else {
                val outfile = files[port]!!.nextFile()
                outfile.parentFile.mkdirs()

                logger.debug { "Writing $port to ${outfile.absolutePath}" }

                if (wroteTo.contains(port)) {
                    if (!output.isSequential()) {
                        logger.warn { "Overwriting ${outfile.absolutePath}"}
                    }
                }
                wroteTo.add(port)

                FileOutputStream(outfile)
            }
            DocumentWriter(document, fos).write()
            fos.close()
        } else {
            if (stdout != null) {
                if (stdout in files && files[stdout]?.pattern == "-") {
                    logger.debug { "Discarding output to ${port}, ${stdout} writes to stdout" }
                } else {
                    logger.debug { "Discarding output to ${port}, implicit pipe output to ${stdout} is bound elsewhere" }
                }
            } else {
                logger.debug { "Sending output to ${port} to default output receiver" }
                super.output(port, document)
            }
        }
    }
}