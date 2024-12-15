package com.xmlcalabash.steps.file

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.*
import com.xmlcalabash.util.SaxonTreeBuilder
import java.io.File
import java.nio.file.Files

class FileInfoStep(): FileStep(NsP.fileInfo) {
    override fun input(port: String, doc: XProcDocument) {
        // never called
    }

    override fun run() {
        super.run()

        val href = try {
            uriBinding(Ns.href)!!
        } catch (ex: Exception) {
            throw XProcError.xdInvalidUri(options[Ns.href].toString()).exception(ex)
        }

        if (href.scheme != "file") {
            throw XProcError.xcUnsupportedFileInfoScheme(href.scheme).exception()
        }

        failOnError = booleanBinding(Ns.failOnError) ?: true

        val file = File(href.path)
        if (!file.exists()) {
            maybeThrow(XProcError.xdDoesNotExist(href.path), href)
            return
        }

        var override = false
        var contentType = MediaType.ANY
        for (pair in overrideContentTypes) {
            if (pair.first.toRegex().find(href.path) != null) {
                contentType = pair.second
                override = true
                break
            }
        }
        if (!override) {
            contentType = MediaType.parse(stepConfig.environment.mimeTypes.getContentType(file))
        }

        val atts = if (file.isDirectory) {
            fileAttributes(file, true,null, null)
        } else {
            fileAttributes(file, true, contentType, null)
        }

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(href)

        if (Files.isRegularFile(file.toPath())) {
            builder.addStartElement(NsC.file, atts)
        } else {
            if (file.isDirectory()) {
                builder.addStartElement(NsC.directory, atts)
            } else {
                builder.addStartElement(NsC.other, atts)
            }
        }

        builder.addEndElement()
        builder.endDocument()
        val list = builder.result

        receiver.output("result", XProcDocument.ofXml(list, stepConfig))
    }
}