package com.xmlcalabash.steps

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.QName
import java.net.URI
import java.net.URISyntaxException

abstract class AbstractArchiveStep(): AbstractAtomicStep() {
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
                    throw stepConfig.exception(XProcError.xdInvalidRelativeTo(stringBinding(Ns.relativeTo)!!))
                }
                else -> throw ex
            }
        }
    }

    protected fun archiveBytes(archive: XProcDocument, format: QName): ByteArray {
        if (archive is XProcBinaryDocument) {
            return archive.binaryValue
        }
        throw stepConfig.exception(XProcError.xcArchiveFormatIncorrect(format))
    }

    protected fun contentType(name: String): MediaType {
        for (pair in overrideContentTypes) {
            if (pair.first.toRegex().find(name) != null) {
                return pair.second
            }
        }
        return MediaType.parse(stepConfig.documentManager.mimetypesFileTypeMap.getContentType(name))
    }

    protected fun selectFormat(contentType: MediaType, defaultFormat: QName?): QName {
        return when (contentType) {
            MediaType.JAR -> Ns.jar
            MediaType.TAR -> Ns.tar
            MediaType.AR -> Ns.ar
            MediaType.ARJ -> Ns.arj
            MediaType.CPIO -> Ns.cpio
            MediaType.SEVENZ -> Ns.sevenZ
            else -> defaultFormat
                ?: throw stepConfig.exception(XProcError.xcUnrecognizedArchiveFormat(contentType))
        }
    }

    protected fun formatMediaType(format: QName): MediaType {
        return when (format) {
            Ns.zip -> MediaType.ZIP
            Ns.jar -> MediaType.JAR
            Ns.tar -> MediaType.TAR
            Ns.ar -> MediaType.AR
            Ns.arj -> MediaType.ARJ
            Ns.cpio -> MediaType.CPIO
            Ns.sevenZ -> MediaType.SEVENZ
            else -> MediaType.OCTET_STREAM
        }
    }

}