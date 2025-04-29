package com.xmlcalabash.steps.file

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.value.DateTimeValue
import java.io.File
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime

class FileTouchStep(): FileStep(NsP.fileTouch) {
    override fun run() {
        super.run()

        val href = try {
            uriBinding(Ns.href)!!
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xdInvalidUri(options[Ns.href].toString()), ex)
        }

        if (href.scheme != "file") {
            throw stepConfig.exception(XProcError.xcUnsupportedFileTouchScheme(href.scheme))
        }

        val timestamp = if (hasBinding(Ns.timestamp)) {
            val dt = valueBinding(Ns.timestamp).value.underlyingValue as DateTimeValue
            dt.toZonedDateTime().withZoneSameInstant(ZoneId.of("UTC"))
        } else {
            ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
        }

        failOnError = booleanBinding(Ns.failOnError) != false

        val file = File(UriUtils.path(href))
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    maybeThrow(XProcError.xdDoesNotExist(UriUtils.path(href), "does not exist and failed to create"), href)
                    return
                }
            }

            val millis = timestamp.toInstant().toEpochMilli()
            file.setLastModified(millis)
        } catch (ex: IOException) {
            maybeThrow(XProcError.xdDoesNotExist(UriUtils.path(href), ex.message ?: "???").with(ex), href)
            return
        }

        val result = resultDocument(href)
        receiver.output("result", XProcDocument.ofXml(result, stepConfig))
    }
}