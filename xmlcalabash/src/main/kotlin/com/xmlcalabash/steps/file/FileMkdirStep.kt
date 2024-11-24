package com.xmlcalabash.steps.file

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.*
import java.io.File
import java.io.IOException

class FileMkdirStep(): FileStep(NsP.fileDelete) {
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
            throw XProcError.xcUnsupportedFileMkdirScheme(href.scheme).exception()
        }

        failOnError = booleanBinding(Ns.failOnError) ?: true

        val file = File(href.path)

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
                maybeThrow(XProcError.xcMkdirFailed(href.path), href)
                return
            }
        } catch (ex: IOException) {
            maybeThrow(XProcError.xcMkdirFailed(href.path), href)
            return
        }

        val result = resultDocument(href)
        receiver.output("result", XProcDocument.ofXml(result, stepConfig))
    }
}