package com.xmlcalabash.steps

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmArray
import java.net.URI
import java.net.URISyntaxException

abstract class AbstractArchiveStep(): AbstractAtomicStep() {
    protected var archives = mutableListOf<XProcDocument>()
    protected var overrideContentTypes = mutableListOf<Pair<String,MediaType>>()
    protected var includeFilters = mutableListOf<String>()
    protected var excludeFilters = mutableListOf<String>()

    override fun run() {
        super.run()

        if (hasBinding(Ns.overrideContentTypes)) {
            overrideContentTypes.clear()
            overrideContentTypes.addAll(overrideContentTypes(valueBinding(Ns.overrideContentTypes).value))
        }

        if (options.containsKey(Ns.includeFilter)) {
            val include = valueBinding(Ns.includeFilter)
            for (item in include.value.iterator()) {
                includeFilters.add(item.stringValue)
            }
        }

        if (options.containsKey(Ns.excludeFilter)) {
            val exclude = valueBinding(Ns.excludeFilter)
            for (item in exclude.value.iterator()) {
                excludeFilters.add(item.stringValue)
            }
        }
    }

    protected fun relativeTo(): URI? {
        try {
            return uriBinding(Ns.relativeTo)
        } catch (ex: Exception) {
            when (ex) {
                is URISyntaxException, is IllegalArgumentException -> {
                    throw XProcError.xdInvalidRelativeTo(stringBinding(Ns.relativeTo)!!).exception()
                }
                else -> throw ex
            }
        }
    }

    protected fun archiveBytes(archive: XProcDocument, format: QName): ByteArray {
        if (archive is XProcBinaryDocument) {
            return archive.binaryValue
        }
        throw XProcError.xcUnsupportedArchiveFormat(format).exception()
    }

    protected fun contentType(name: String): MediaType {
        for (pair in overrideContentTypes) {
            if (pair.first.toRegex().find(name) != null) {
                return pair.second
                break
            }
        }
        return MediaType.parse(stepConfig.mimeTypes.getContentType(name))
    }
}