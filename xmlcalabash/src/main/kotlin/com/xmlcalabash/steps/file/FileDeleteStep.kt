package com.xmlcalabash.steps.file

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.*
import java.io.File
import java.io.IOException

class FileDeleteStep(): FileStep(NsP.fileDelete) {
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
            throw XProcError.xcUnsupportedFileDeleteScheme(href.scheme).exception()
        }

        val recursive = booleanBinding(Ns.recursive) ?: false
        failOnError = booleanBinding(Ns.failOnError) ?: true

        val file = File(href.path)
        if (!file.exists()) {
            val result = resultDocument(href)
            receiver.output("result", XProcDocument.ofXml(result, stepConfig))
            return
        }

        try {
            val success = if (recursive) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            if (!success) {
                maybeThrow(XProcError.xcDeleteFailed(href.path), href)
                return
            }
        } catch (ex: IOException) {
            maybeThrow(XProcError.xcDeleteFailed(href.path), href)
            return
        }

        val result = resultDocument(href)
        receiver.output("result", XProcDocument.ofXml(result, stepConfig))
    }
}