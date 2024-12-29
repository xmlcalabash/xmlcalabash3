package com.xmlcalabash.steps

import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.XProcSerializer
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry

open class ArchiveStep(): AbstractArchiveStep() {
    private val archiveMembers = mutableListOf<XProcDocument>()
    private var manifest: XProcDocument? = null
    protected var defaultMethod: String? = null
    private var command: String? = null
    private var defaultLevel: String? = null
    private val sourceMap = mutableMapOf<URI, XProcDocument>()
    private val manifestList = mutableListOf<ManifestEntry>()
    private val extraList = mutableListOf<ManifestEntry>()
    private val nameMap = mutableMapOf<String, ManifestEntry>()
    private var origArchiveFiles = mutableListOf<Path>()
    private var archiveFile: Path? = null

    override fun run() {
        super.run()
        archiveMembers.addAll(queues["source"]!!)
        archives.addAll(queues["archive"]!!)
        if (queues["manifest"]!!.size > 1) {
            throw stepConfig.exception(XProcError.xcMultipleManifests())
        }
        manifest = queues["manifest"]!!.firstOrNull()

        val format = qnameBinding(Ns.format) ?: Ns.zip
        val relativeTo = relativeTo()

        if (format != Ns.zip) {
            throw stepConfig.exception(XProcError.xcUnsupportedArchiveFormat(format))
        }

        for (archive in archives) {
            archiveBytes(archive, format)
        }

        val parameters = qnameMapBinding(Ns.parameters)

        command = parameters[Ns.command]?.underlyingValue?.stringValue
        if (command == null) {
            if (archives.isEmpty()) {
                command = "create"
            } else {
                command = "update"
            }
        }

        defaultMethod = parameters[Ns.method]?.underlyingValue?.stringValue
        defaultLevel = parameters[Ns.level]?.underlyingValue?.stringValue

        for (source in archiveMembers) {
            val uri = source.baseURI
            if (uri == null) {
                throw stepConfig.exception(XProcError.xcNoArchiveSourceUri())
            } else {
                if (sourceMap.containsKey(uri)) {
                    throw stepConfig.exception(XProcError.xcDuplicateArchiveSourceUri(uri))
                }
            }
            sourceMap[uri] = source
        }

        if (manifest != null) {
            manifestList.addAll(parseManifest(manifest!!))
        }

        // Find the documents for the entries in the manifest.
        if (command != "delete") {
            for (entry in manifestList) {
                if (sourceMap.containsKey(entry.href)) {
                    entry.document = sourceMap[entry.href]!!
                    sourceMap.remove(entry.href)
                } else {
                    val manager = stepConfig.environment.documentManager
                    try {
                        entry.document = manager.load(entry.href, stepConfig)
                    } catch (ex: XProcException) {
                        throw ex
                    } catch (ex: Exception) {
                        throw stepConfig.exception(XProcError.xdDoesNotExist(entry.href.toString()))
                    }
                }
            }
        }

        // Construct a manifest for the other documents on the input port
        for (source in archiveMembers) {
            val uri = source.baseURI!!
            if (!sourceMap.containsKey(uri)) {
                continue
            }

            val relStr = relativeTo?.toString()
            val hStr = uri.toString()
            val path = if (relStr != null && hStr.startsWith(relStr)) {
                hStr.substring(relStr.length)
            } else {
                uri.path
            }
            val name = if (path.startsWith("/")) {
                path.substring(1)
            } else {
                path
            }

            val entry = ManifestEntry(name, uri)
            entry.document = source
            if (defaultMethod != null) {
                entry.method = defaultMethod!!
            }

            extraList.add(entry)
        }

        for (entry in manifestList + extraList) {
            if (nameMap.containsKey(entry.name)) {
                throw stepConfig.exception(XProcError.xcDuplicateArchiveSourceUri(entry.name))
            }
            nameMap[entry.name] = entry
        }

        // Archives can be large. We probably don't need them in memory and the
        // XProcDocument class should have the ability to manage them on disk.
        // Various aspects of creating an archive file are easier if the file is
        // being written to disk, so let's just do that for now. (In particular,
        // the ZIP archiver is very fussy if the output isn't going to a file.)
        val tdir = if (System.getProperty("java.io.tmpdir") == null) {
            Paths.get(".")
        } else {
            Paths.get(System.getProperty("java.io.tmpdir"))
        }
        archiveFile = Files.createTempFile(tdir, null, ".zip")
        archiveFile!!.toFile().deleteOnExit()

        for (archive in archives) {
            val ofile =  Files.createTempFile(tdir, null, ".zip")
            ofile.toFile().deleteOnExit()
            val stream = FileOutputStream(ofile.toFile())
            stream.write(archiveBytes(archive, format))
            stream.close()
            origArchiveFiles.add(ofile)
        }

        val archiver = ZipArchiver()

        try {
            archiver.processArchive()
        } catch (ex: XProcException) {
            throw ex
        } catch (ex: Exception) {
            throw stepConfig.exception(XProcError.xcInvalidArchiveFormat(format), ex)
        }

        if (archiveFile!!.toFile().length() > Integer.MAX_VALUE) {
            throw stepConfig.exception(XProcError.xcArchiveTooLarge(archiveFile!!.toFile().length()))
        }

        val bytes = ByteArray(archiveFile!!.toFile().length().toInt())
        val stream = FileInputStream(archiveFile!!.toFile())
        stream.read(bytes)
        stream.close()

        archiveFile!!.toFile().delete()

        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(stepConfig.baseUri)
        builder.addStartElement(NsC.archive)

        for (entry in manifestList + extraList) {
            val amap = mutableMapOf(
                Ns.name to entry.name,
                Ns.href to entry.href.toString(),
                Ns.contentType to contentType(entry.name).toString(),
                Ns.method to entry.method
            )

            builder.addStartElement(NsC.entry, stepConfig.attributeMap(amap))
            builder.addEndElement()
        }

        builder.addEndElement()
        builder.endDocument()

        receiver.output("report", XProcDocument.ofXml(builder.result, stepConfig))

        val props = DocumentProperties()
        props[Ns.baseUri] = XdmEmptySequence.getInstance()

        receiver.output("result", XProcDocument.ofBinary(bytes, stepConfig, props))
    }

    private fun parseManifest(manifest: XProcDocument): MutableList<ManifestEntry> {
        val entries = mutableListOf<ManifestEntry>()
        val root = S9Api.documentElement(manifest.value as XdmNode)
        if (root.nodeName != NsC.archive) {
            throw stepConfig.exception(XProcError.xcInvalidManifest())
        }
        for (entry in root.axisIterator(Axis.CHILD)) {
            if (entry.nodeKind != XdmNodeKind.ELEMENT) {
                continue
            }

            if (entry.nodeName.namespaceUri != NsC.namespace) {
                continue
            }

            if (entry.nodeName != NsC.entry) {
                throw stepConfig.exception(XProcError.xcInvalidManifest(entry.nodeName))
            }

            val properties = mutableMapOf<QName,String>()

            if (defaultMethod != null) {
                properties[Ns.method] = defaultMethod!!
            }

            for (attr in entry.axisIterator(Axis.ATTRIBUTE)) {
                if (attr.nodeName == Ns.name || attr.nodeName == Ns.href || attr.nodeName == Ns.comment
                    || attr.nodeName == Ns.method || attr.nodeName == Ns.level || attr.nodeName == Ns.contentType) {
                    properties[attr.nodeName] = attr.stringValue
                } else {
                    if (attr.nodeName.namespaceUri != NamespaceUri.NULL) {
                        throw stepConfig.exception(XProcError.xcInvalidManifestEntry(attr.nodeName))
                    }
                }
            }

            if (!properties.containsKey(Ns.name)) {
                throw stepConfig.exception(XProcError.xcInvalidManifestEntryName())
            }

            if (!properties.containsKey(Ns.href)) {
                throw stepConfig.exception(XProcError.xcInvalidManifestEntryHref())
            }

            val muri = try {
                entry.baseURI.resolve(properties[Ns.href]!!)
            } catch (ex: IllegalArgumentException) {
                throw stepConfig.exception(XProcError.xdInvalidUri(properties[Ns.href]!!.toString()))
            }

            val mentry = ManifestEntry(properties[Ns.name]!!, muri)
            mentry.comment = properties[Ns.comment]
            mentry.method = properties[Ns.method]
            mentry.level = properties[Ns.level]
            mentry.contentType = MediaType.parse(properties[Ns.contentType])
            entries.add(mentry)
        }

        return entries
    }

    override fun toString(): String = "p:archive"

    internal class ManifestEntry(val name: String, val href: URI) {
        var document: XProcDocument? = null
        var comment: String? = null
        var method: String? = null
        var level: String? = null
        var contentType: MediaType? = null
        override fun toString(): String {
            return "${name}: ${href}"
        }
    }

    inner class ZipArchiver() {
        var origZipFile: ZipFile? = null
        var zipResultStream: ZipArchiveOutputStream? = null

        fun processArchive() {
            when (command) {
                null -> Unit
                "update", "create", "freshen", "delete" -> Unit
                else -> throw stepConfig.exception(XProcError.xcInvalidParameter(Ns.command, command!!))
            }
            when (defaultLevel) {
                null -> Unit
                "smallest", "fastest", "default", "huffman", "none" -> Unit
                else -> throw stepConfig.exception(XProcError.xcInvalidParameter(Ns.level, defaultLevel!!))
            }
            when (defaultMethod) {
                null -> Unit
                "deflated", "none" -> Unit
                else -> throw stepConfig.exception(XProcError.xcInvalidParameter(Ns.method, defaultMethod!!))
            }

            if (archives.size > 1) {
                throw stepConfig.exception(XProcError.xcInvalidNumberOfArchives(archives.size))
            } else if (command == "delete" && archives.size != 1) {
                throw stepConfig.exception(XProcError.xcInvalidNumberOfArchives(archives.size))
            }

            if (origArchiveFiles.isNotEmpty()) {
                val zipBuilder = ZipFile.Builder()
                zipBuilder.setFile(origArchiveFiles.first().toFile())
                origZipFile = zipBuilder.get()
            }

            zipResultStream = ZipArchiveOutputStream(archiveFile)

            when (command) {
                "delete" -> archiveDelete()
                else -> archiveUpdate()
            }

            zipResultStream!!.close()
        }

        fun archiveUpdate() {
            // If there's an original ZIP file, keep all the entries in the same order...
            if (origZipFile != null) {
                for (oentry in origZipFile!!.entries) {
                    var copyEntry = true
                    if (nameMap.containsKey(oentry.name)) {
                        addToArchive(nameMap[oentry.name]!!)
                        nameMap.remove(oentry.name)
                        copyEntry = false
                    }
                    if (copyEntry) {
                        addToArchive(oentry)
                    }
                }
            }

            if (command != "freshen") {
                for (entry in manifestList) {
                    if (!nameMap.containsKey(entry.name)) {
                        continue // we already did this one
                    }
                    addToArchive(entry)
                }
                for (entry in extraList) {
                    if (!nameMap.containsKey(entry.name)) {
                        continue // we already did this one
                    }
                    addToArchive(entry)
                }
            }
        }

        fun archiveDelete() {
            if (origZipFile != null) {
                for (oentry in origZipFile!!.entries) {
                    var copyEntry = true
                    if (nameMap.containsKey(oentry.name)) {
                        copyEntry = false
                    }
                    if (copyEntry) {
                        addToArchive(oentry)
                    }
                }
            }
        }

        private fun addToArchive(oentry: ZipArchiveEntry) {
            val zipEntry = ZipArchiveEntry(oentry.name)
            zipEntry.comment = oentry.comment
            zipEntry.method = oentry.method
            zipResultStream!!.putArchiveEntry(zipEntry)
            val inputStream = origZipFile!!.getInputStream(oentry)
            val copybuf = ByteArray(4096)
            var length = inputStream.read(copybuf)
            while (length >= 0) {
                zipResultStream!!.write(copybuf, 0, length)
                length = inputStream.read(copybuf)
            }
            inputStream.close()
            zipResultStream!!.closeArchiveEntry()
        }

        private fun addToArchive(entry: ManifestEntry) {
            val zipEntry = ZipArchiveEntry(entry.name)
            zipEntry.comment = entry.comment
            if (entry.method != null) {
                when (entry.method) {
                    "none" -> zipEntry.method = ZipEntry.STORED
                    "deflated" -> zipEntry.method = ZipEntry.DEFLATED
                    else -> Unit
                }
            }
            zipResultStream!!.putArchiveEntry(zipEntry)

            val entryBaos = ByteArrayOutputStream()
            val serializer = XProcSerializer(stepConfig)
            serializer.write(entry.document!!, entryBaos, "archive")
            zipResultStream!!.write(entryBaos.toByteArray())
            zipResultStream!!.closeArchiveEntry()
        }
    }
}