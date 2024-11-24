package com.xmlcalabash.steps

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.util.*
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.net.URI

open class UnarchiveStep(): AbstractArchiveStep() {
    override fun input(port: String, doc: XProcDocument) {
        archives.add(doc)
    }

    override fun run() {
        super.run()

        val archive = archives.first()
        val format = qnameBinding(Ns.format) ?: Ns.zip
        val relativeTo = relativeTo()

        if (format != Ns.zip) {
            throw XProcError.xcInvalidArchiveFormat(format).exception()
        }

        if (format != Ns.zip) {
            throw XProcError.xcUnsupportedArchiveFormat(format).exception()
        }

        // Setup access to the ZIP data...
        val channel = SeekableInMemoryByteChannel(archiveBytes(archive, format))
        val zipBuilder = ZipFile.Builder()
        zipBuilder.setSeekableByteChannel(channel)
        val zipFile = zipBuilder.get()

        for (entry in zipFile.entries) {
            if (entry.isDirectory) {
                continue
            }

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
                    throw XProcError.xcNoUnarchiveBaseUri().exception()
                }
                URI(Urify.urify(entry.name, archive.baseURI.toString() + "/"))
            } else {
                URI(Urify.urify(entry.name, relativeTo))
            }

            val contentType = contentType(entry.name)

            val loader = DocumentLoader(stepConfig, baseUri, DocumentProperties(), mapOf())
            val loadedDoc = loader.load(baseUri, zipFile.getInputStream(entry), contentType)
            val doc = loadedDoc.with(loadedDoc.value, baseUri).with(contentType)

            receiver.output("result", doc)
        }

        zipFile.close()

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(stepConfig.baseUri)
        builder.addStartElement(NsC.archive)
        builder.addEndElement()
        builder.endDocument()

        //receiver.output("result", XProcDocument.ofBinary(baos.toByteArray(), context))
    }

    override fun toString(): String = "p:unarchive"
}