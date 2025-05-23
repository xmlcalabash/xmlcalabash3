package com.xmlcalabash.steps.file

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.*
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.UriUtils
import java.io.File
import java.nio.file.Files

class FileInfoStep(): FileStep(NsP.fileInfo) {
    override fun run() {
        super.run()

        val href = try {
            uriBinding(Ns.href)!!
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdInvalidUri(options[Ns.href].toString()), ex)
        }

        if (href.scheme != "file") {
            throw stepConfig.exception(XProcError.xcUnsupportedFileInfoScheme(href.scheme))
        }

        failOnError = booleanBinding(Ns.failOnError) != false

        val file = File(UriUtils.path(href))
        if (!file.exists()) {
            maybeThrow(XProcError.xdDoesNotExist(UriUtils.path(href), "path does not exist"), href)
            return
        }

        var override = false
        var contentType = MediaType.ANY
        for (pair in overrideContentTypes) {
            if (pair.first.toRegex().find(UriUtils.path(href)) != null) {
                contentType = pair.second
                override = true
                break
            }
        }
        if (!override) {
            contentType = MediaType.parse(stepConfig.documentManager.mimetypesFileTypeMap.getContentType(file))
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