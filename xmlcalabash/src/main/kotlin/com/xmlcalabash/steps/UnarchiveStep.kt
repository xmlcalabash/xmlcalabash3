package com.xmlcalabash.steps

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.steps.archives.ArInputArchive
import com.xmlcalabash.steps.archives.ArjInputArchive
import com.xmlcalabash.steps.archives.CpioInputArchive
import com.xmlcalabash.steps.archives.JarInputArchive
import com.xmlcalabash.steps.archives.SevenZInputArchive
import com.xmlcalabash.steps.archives.TarInputArchive
import com.xmlcalabash.steps.archives.ZipInputArchive
import com.xmlcalabash.util.*
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.net.URI

open class UnarchiveStep(): AbstractArchiveStep() {
    override fun run() {
        super.run()

        val archive = queues["source"]!!.first()
        val format = qnameBinding(Ns.format) ?: Ns.zip

        if (archive !is XProcBinaryDocument) {
            // If it isn't binary, it definitely doesn't match the format...
            throw stepConfig.exception(XProcError.xcArchiveFormatIncorrect(format))
        }

        val archiveInput = when (format) {
            Ns.zip -> ZipInputArchive(stepConfig, archive)
            Ns.jar -> JarInputArchive(stepConfig, archive)
            Ns.tar -> TarInputArchive(stepConfig, archive)
            Ns.ar -> ArInputArchive(stepConfig, archive)
            Ns.arj -> ArjInputArchive(stepConfig, archive)
            Ns.cpio -> CpioInputArchive(stepConfig, archive)
            Ns.sevenZ -> SevenZInputArchive(stepConfig, archive)
            else -> throw stepConfig.exception(XProcError.xcInvalidArchiveFormat(format))
        }

        val relativeTo = relativeTo()

        archiveInput.open()

        for (entry in archiveInput.entries) {
            var include = true
            if (includeFilters.isNotEmpty()) {
                include = false
                for (filter in includeFilters) {
                    if (filter.toRegex().find(entry.name) != null) {
                        include = true
                        break
                    }
                }
            }
            if (excludeFilters.isNotEmpty()) {
                for (filter in excludeFilters) {
                    if (filter.toRegex().find(entry.name) != null) {
                        include = false
                        break
                    }
                }
            }

            if (!include) {
                continue
            }

            val baseUri = if (relativeTo == null) {
                if (archive.baseURI == null) {
                    throw stepConfig.exception(XProcError.xcNoUnarchiveBaseUri())
                }
                URI(Urify.urify(entry.name, archive.baseURI.toString() + "/"))
            } else {
                URI(Urify.urify(entry.name, relativeTo))
            }

            val contentType = contentType(entry.name)

            val loader = DocumentLoader(stepConfig, baseUri, DocumentProperties(), mapOf())
            val loadedDoc = loader.load(entry.inputStream!!, contentType)
            val doc = loadedDoc.with(loadedDoc.value, baseUri).with(contentType)

            receiver.output("result", doc)
        }

        archiveInput.close()
    }

    override fun toString(): String = "p:unarchive"
}