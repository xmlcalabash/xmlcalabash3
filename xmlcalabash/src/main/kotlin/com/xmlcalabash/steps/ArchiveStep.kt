package com.xmlcalabash.steps

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.steps.archives.ArInputArchive
import com.xmlcalabash.steps.archives.ArOutputArchive
import com.xmlcalabash.steps.archives.ArjInputArchive
import com.xmlcalabash.steps.archives.CpioInputArchive
import com.xmlcalabash.steps.archives.CpioOutputArchive
import com.xmlcalabash.steps.archives.InputArchive
import com.xmlcalabash.steps.archives.JarInputArchive
import com.xmlcalabash.steps.archives.JarOutputArchive
import com.xmlcalabash.steps.archives.OutputArchive
import com.xmlcalabash.steps.archives.SevenZInputArchive
import com.xmlcalabash.steps.archives.SevenZOutputArchive
import com.xmlcalabash.steps.archives.TarInputArchive
import com.xmlcalabash.steps.archives.TarOutputArchive
import com.xmlcalabash.steps.archives.XArchiveEntry
import com.xmlcalabash.steps.archives.ZipInputArchive
import com.xmlcalabash.steps.archives.ZipOutputArchive
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant

open class ArchiveStep(): AbstractArchiveStep() {
    private val archives = mutableListOf<InputArchive>()
    private val archiveMembers = mutableListOf<XProcDocument>()
    private var manifest: XProcDocument? = null
    private var command: String? = null
    protected var defaultMethod: String? = null
    private var defaultLevel: String? = null
    private var mergeDuplicates: String = "error"
    private val sourceMap = mutableMapOf<URI, XProcDocument>()
    private val manifestList = mutableListOf<ManifestEntry>()
    private val extraList = mutableListOf<ManifestEntry>()
    private val nameMap = mutableMapOf<String, ManifestEntry>()
    private var origArchiveFiles = mutableListOf<Path>()
    private lateinit var outputArchive: OutputArchive

    override fun run() {
        super.run()
        archiveMembers.addAll(queues["source"]!!)

        for (archive in queues["archive"]!!) {
            val format = selectFormat(archive.contentType ?: MediaType.OCTET_STREAM, Ns.zip)

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

            archiveInput.open()
            archives.add(archiveInput)
        }

        if (queues["manifest"]!!.size > 1) {
            throw stepConfig.exception(XProcError.xcMultipleManifests())
        }
        manifest = queues["manifest"]!!.firstOrNull()

        val format = qnameBinding(Ns.format) ?: archives.firstOrNull()?.archiveFormat ?: Ns.zip
        if (format == Ns.arj) {
            throw stepConfig.exception(XProcError.xcCannotCreateArjArchives())
        }

        val relativeTo = relativeTo()
        val parameters = qnameMapBinding(Ns.parameters)

        command = parameters[Ns.command]?.underlyingValue?.stringValue
        if (command == null) {
            if (archives.isEmpty()) {
                command = "create"
            } else {
                command = "update"
            }
        }

        if (command != "create" && archives.size > 1) {
            throw stepConfig.exception(XProcError.xcInvalidNumberOfArchives(archives.size))
        }

        if (command == "delete" && archives.size == 0) {
            throw stepConfig.exception(XProcError.xcInvalidNumberOfArchives(archives.size))
        }

        defaultMethod = parameters[Ns.method]?.underlyingValue?.stringValue
        if (defaultMethod != null) {
            when (format) {
                Ns.ar, Ns.cpio, Ns.tar -> {
                    throw stepConfig.exception(XProcError.xcInvalidParameter(Ns.method, defaultMethod!!))
                }
                Ns.sevenZ -> {
                    if (defaultMethod !in listOf("none", "lzma", "lzma2", "bzip2", "deflate", "deflate64")) {
                        throw stepConfig.exception(XProcError.xcInvalidParameter(Ns.method, defaultMethod!!))
                    }

                }
                Ns.zip, Ns.jar -> {
                    if (defaultMethod !in listOf("none", "deflated")) {
                        throw stepConfig.exception(XProcError.xcInvalidParameter(Ns.method, defaultMethod!!))
                    }
                }
            }
        }

        defaultLevel = parameters[Ns.level]?.underlyingValue?.stringValue
        if (defaultLevel != null) {
            when (format) {
                Ns.ar, Ns.cpio, Ns.tar, Ns.sevenZ -> {
                    throw stepConfig.exception(XProcError.xcInvalidParameter(Ns.level, defaultLevel!!))
                }
                Ns.zip, Ns.jar -> {
                    // FIXME: the level isn't actually used anywhere!
                    if (defaultLevel !in listOf("smallest", "fastest", "default", "huffman", "none")) {
                        throw stepConfig.exception(XProcError.xcInvalidParameter(Ns.level, defaultLevel!!))
                    }
                }
            }
        }

        mergeDuplicates = parameters[NsCx.mergeDuplicates]?.underlyingValue?.stringValue ?: "error"
        if (mergeDuplicates !in listOf("keep-first", "keep-last", "error")) {
            throw stepConfig.exception(XProcError.xcInvalidParameter(NsCx.mergeDuplicates, mergeDuplicates))
        }

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
                        throw stepConfig.exception(XProcError.xdDoesNotExist(entry.href.toString(), ex.message ?: "???"), ex)
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

            val entry = ManifestEntry(uri, mapOf(Ns.name to name))
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

        // Now work out what entries will go in the output archive...
        val outputEntries = when (command) {
            "create" -> createEntries()
            "update" -> updateEntries(addNew = true)
            "delete" -> deleteEntries()
            "freshen" -> updateEntries(addNew = false)
            else -> throw stepConfig.exception(XProcError.xcInvalidParameter(Ns.command, command!!))
        }

        outputArchive = when (format) {
            Ns.zip -> ZipOutputArchive(stepConfig)
            Ns.jar -> JarOutputArchive(stepConfig)
            Ns.tar -> TarOutputArchive(stepConfig)
            Ns.ar -> ArOutputArchive(stepConfig)
            Ns.cpio -> CpioOutputArchive(stepConfig)
            Ns.sevenZ -> SevenZOutputArchive(stepConfig)
            else -> throw stepConfig.exception(XProcError.xcInvalidArchiveFormat(format))
        }

        outputArchive.create()
        for (entry in outputEntries) {
            outputArchive.write(entry)
        }
        outputArchive.close()

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
        props[Ns.contentType] = formatMediaType(format)

        val doc = stepConfig.environment.documentManager.load(outputArchive.baseUri, stepConfig, props)
        receiver.output("result", doc)
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

            val mentry = ManifestEntry(muri, properties)
            entries.add(mentry)
        }

        return entries
    }

    private fun createEntries(): List<XArchiveEntry> {
        val outputEntries = mutableListOf<XArchiveEntry>()
        val map = mutableMapOf<String, ManifestEntry>()
        map.putAll(nameMap)

        for (entry in unduplicatedList()) {
            val manentry = nameMap[entry.name]
            if (manentry != null) {
                val newEntry = XArchiveEntry(stepConfig, entry.name, manentry.document!!)
                newEntry.properties.putAll(entry.properties)
                newEntry.properties.putAll(manentry.properties)
                outputEntries.add(newEntry)
                map.remove(entry.name)
            } else {
                outputEntries.add(entry)
            }
        }
        for ((name, entry) in map) {
            val newEntry = XArchiveEntry(stepConfig, name, entry.document!!)
            newEntry.properties.putAll(entry.properties)
            outputEntries.add(newEntry)
        }

        return outputEntries
    }

    private fun updateEntries(addNew: Boolean): List<XArchiveEntry> {
        val outputEntries = mutableListOf<XArchiveEntry>()
        val map = mutableMapOf<String, ManifestEntry>()
        map.putAll(nameMap)

        for (entry in unduplicatedList()) {
            val manentry = nameMap[entry.name]
            if (manentry != null) {
                val newEntry = XArchiveEntry(stepConfig, entry.name, manentry.document!!)
                newEntry.properties.putAll(entry.properties)
                newEntry.properties.putAll(manentry.properties)
                outputEntries.add(newEntry)
                map.remove(entry.name)
            } else {
                if (needsUpdating(entry)) {
                    val localUri = entry.archive?.baseUri?.resolve(entry.name)
                    val doc = stepConfig.environment.documentManager.load(localUri!!, stepConfig)
                    val newEntry = XArchiveEntry(stepConfig, entry.name, doc)
                    outputEntries.add(newEntry)
                } else {
                    outputEntries.add(entry)
                }
            }
        }

        if (addNew) {
            for ((name, entry) in map) {
                val newEntry = XArchiveEntry(stepConfig, name, entry.document!!)
                newEntry.properties.putAll(entry.properties)
                outputEntries.add(newEntry)
            }
        }

        return outputEntries
    }

    private fun unduplicatedList(): List<XArchiveEntry> {
        val outputEntries = mutableListOf<XArchiveEntry>()
        val seen = mutableMapOf<String, XArchiveEntry>()

        for (archive in archives) {
            for (entry in archive.entries) {
                if (seen.contains(entry.name)) {
                    if (mergeDuplicates == "error") {
                        throw stepConfig.exception(XProcError.xiMergeDuplicatesError(entry.name))
                    }
                    if (mergeDuplicates == "keep-last") {
                        outputEntries.remove(seen[entry.name]!!)
                        outputEntries.add(entry)
                    }
                } else {
                    outputEntries.add(entry)
                }
                seen[entry.name] = entry
            }
        }

        return outputEntries
    }

    private fun needsUpdating(entry: XArchiveEntry): Boolean {
        val lastModifiedProperty = entry.properties[NsCx.lastModified]
        val lastModified = if (lastModifiedProperty != null) {
            FileTime.from(Instant.parse(lastModifiedProperty))
        } else {
            FileTime.from(Instant.EPOCH)
        }

        val localUri = entry.archive?.baseUri?.resolve(entry.name)
        if (localUri?.scheme != "file") {
            if (localUri != null) {
                stepConfig.warn { "Ignoring update for non-file URI: ${localUri}" }
            }
            return false
        }

        val file = File(localUri.path)
        if (!file.exists()) {
            return false
        }

        val fileTime = FileTime.fromMillis(file.lastModified())
        return fileTime > lastModified
    }

    private fun deleteEntries(): List<XArchiveEntry> {
        val outputEntries = mutableListOf<XArchiveEntry>()
        for (archive in archives) {
            for (entry in archive.entries) {
                if (entry.name !in nameMap) {
                    outputEntries.add(entry)
                }
            }
        }

        return outputEntries
    }

    override fun toString(): String = "p:archive"

    internal class ManifestEntry(val href: URI, props: Map<QName,String>) {
        val properties = mutableMapOf<QName,String>()
        init {
            properties.putAll(props)
        }

        var document: XProcDocument? = null

        var contentType: MediaType?
            get() {
                val ctype = properties[Ns.contentType]
                if (ctype != null) {
                    return MediaType.parse(ctype)
                }
                return null
            }
            set(value) {
                properties.remove(Ns.contentType)
                value?.let { properties[Ns.contentType] = "${it}" }
            }
        val name: String
            get() = properties[Ns.name]!!
        var comment: String?
            get() = properties[Ns.comment]
            set(value) {
                properties.remove(Ns.comment)
                value?.let { properties[Ns.comment] = it }
            }
        var method: String?
            get() = properties[Ns.method]
            set(value) {
                properties.remove(Ns.method)
                value?.let { properties[Ns.method] = it }
            }
        var level: String?
            get() = properties[Ns.level]
            set(value) {
                properties.remove(Ns.level)
                value?.let { properties[Ns.level] = it }
            }
        override fun toString(): String {
            return "${name}: ${href}"
        }
    }
}