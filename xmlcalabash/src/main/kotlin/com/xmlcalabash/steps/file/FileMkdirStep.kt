package com.xmlcalabash.steps.file

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.*
import com.xmlcalabash.util.UriUtils
import java.io.File
import java.io.IOException

class FileMkdirStep(): FileStep(NsP.fileDelete) {
    override fun run() {
        super.run()

        val href = try {
            uriBinding(Ns.href)!!
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdInvalidUri(options[Ns.href].toString()), ex)
        }

        if (href.scheme != "file") {
            throw stepConfig.exception(XProcError.xcUnsupportedFileMkdirScheme(href.scheme))
        }

        failOnError = booleanBinding(Ns.failOnError) != false

        val file = File(UriUtils.path(href))

        if (file.exists() && file.isDirectory) {
            val result = resultDocument(href)
            receiver.output("result", XProcDocument.ofXml(result, stepConfig))
            return
        }

        if (file.exists()) {
            maybeThrow(XProcError.xcMkdirFailed(file.path), href)
            return
        }

        try {
            val success = file.mkdirs()
            if (!success) {
                maybeThrow(XProcError.xcMkdirFailed(UriUtils.path(href)), href)
                return
            }
        } catch (ex: IOException) {
            maybeThrow(XProcError.xcMkdirFailed(UriUtils.path(href)), href)
            return
        }

        val result = resultDocument(href)
        receiver.output("result", XProcDocument.ofXml(result, stepConfig))
    }
}