package com.xmlcalabash.steps

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.MediaClassification

open class TextJoinStep(): AbstractTextStep() {
    override fun run() {
        super.run()

        val documents = queues["source"]!!
        val prefix = stringBinding(Ns.prefix)
        val suffix = stringBinding(Ns.suffix)
        val separator = stringBinding(Ns.separator)
        val contentType = try {
            MediaType.parse(stringBinding(Ns.overrideContentType)) ?: MediaType.TEXT
        } catch (ex: XProcException) {
            throw XProcException(ex.error.at(stepParams.location))
        }

        if (contentType.classification() != MediaClassification.TEXT) {
            throw stepConfig.exception(XProcError.xcInvalidContentType(contentType.toString()))
        }

        val sb = StringBuilder()
        if (prefix != null) {
            sb.append(prefix)
        }

        for (index in documents.indices) {
            if (index > 0 && separator != null) {
                sb.append(separator)
            }
            sb.append(text(documents[index]))
        }

        if (suffix != null) {
            sb.append(suffix)
        }

        val result = XProcDocument.ofText(sb.toString(), stepConfig, contentType, DocumentProperties())
        receiver.output("result", result)
    }

    override fun toString(): String = "p:text-join"
}