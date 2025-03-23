package com.xmlcalabash.ext.trang

import com.thaiopensource.relaxng.output.LocalOutputDirectory
import com.thaiopensource.resolver.AbstractResolver
import com.thaiopensource.resolver.Identifier
import com.thaiopensource.resolver.Input
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsRng
import com.xmlcalabash.namespace.NsXs
import com.xmlcalabash.util.MediaClassification
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.XdmNode
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.deleteIfExists

class TrangStep(): AbstractTrangStep() {
    val sources = mutableListOf<File>()
    val inputUris = mutableListOf<URI>()
    lateinit var outputBaseUri: URI
    lateinit var outputDirectory: URI

    val baseUriToFile = mutableMapOf<String, String>()
    val fileToBaseUri = mutableMapOf<String, String>()
    val includeToFile = mutableMapOf<String, String>()
    val rootUriMap = mutableMapOf<String, String>()

    override fun run() {
        super.run()

        if (queues["source"]!!.isEmpty()) {
            throw stepConfig.exception(XProcError.xcxTrangSourceRequired())
        }

        sourceFormat = stringBinding(_sourceFormat) ?: inferredFormat()
        if (sourceFormat == "xsd") {
            throw stepConfig.exception(XProcError.xcxTrangNoXsdSource())
        }

        resultFormat = stringBinding(_resultFormat)!!

        if (sourceFormat == resultFormat) {
            throw stepConfig.exception(XProcError.xcxTrangIdenticalFormats(sourceFormat!!))
        }

        outputBaseUri = uriBinding(_outputBaseUri)!!

        parseNamespaces()
        parseParameters()

        val tempDirectory = Files.createTempDirectory("trang")
        outputDirectory = tempDirectory.toUri()

        for (doc in queues["source"]!!) {
            val tempFile = File.createTempFile("cx-trang", ".${sourceFormat}")
            val stream = FileOutputStream(tempFile)
            val writer = DocumentWriter(doc, stream)
            writer.write()
            sources.add(tempFile)

            if (doc.baseURI == null) {
                if (queues["source"]!!.size > 1) {
                    throw stepConfig.exception(XProcError.xcxTrangBaseUriRequired())
                }
                baseUriToFile[magicBaseUri.toString()] = tempFile.toURI().toString()
                fileToBaseUri[tempFile.toURI().toString()] = magicBaseUri.toString()
                inputUris.add(magicBaseUri)
            } else {
                if (doc.baseURI!!.scheme != "file") {
                    throw stepConfig.exception(XProcError.xcxTrangFileUriRequired())
                }
                baseUriToFile[doc.baseURI!!.toString()] = tempFile.toURI().toString()
                fileToBaseUri[tempFile.toURI().toString()] = doc.baseURI.toString()
                inputUris.add(doc.baseURI!!)
            }
        }

        translate()

        Files.walkFileTree(tempDirectory, Visitor())

        tempDirectory.deleteIfExists()
    }

    private fun translate() {
        val trangInputFormat = trangInputFormat()
        val trangOutputFormat = trangOutputFormat()

        val inputArray = inputOptions.toTypedArray()
        val outputArray = outputOptions.toTypedArray()

        val resolver = TrangResolver()
        val errorHandler = TrangErrorHandler()

        var primaryInput = inputUris.first().toString().replace("\\", "/")
        val underlyingInput = baseUriToFile[primaryInput]!!

        includeToFile[primaryInput] = underlyingInput

        // There's extra magic here. Trang doesn't put the initial schema through the resolver,
        // so we have to pass the file in the temporary directory. But we also have to be able
        // to "undo" that if there are relative URI references against it; those have to be
        // against the "real" base URI.
        includeToFile[underlyingInput] = underlyingInput
        rootUriMap[underlyingInput] = primaryInput
        val sc = trangInputFormat.load(underlyingInput, inputArray, sourceFormat, errorHandler, resolver)

        val inputFile = File(inputUris.first().path)
        val primaryOutputFile = "${inputFile.nameWithoutExtension}.${resultFormat}"
        val primaryOutput = outputDirectory.resolve(primaryOutputFile).path

        val od = LocalOutputDirectory(baseUriToFile[primaryInput]!!, File(primaryOutput), ".${resultFormat}", outputEncoding, lineLength, indent)

        try {
            trangOutputFormat.output(sc, od, outputArray, sourceFormat, errorHandler)
        } catch (ex: Throwable) {
            throw stepConfig.exception(XProcError.xdStepFailed(ex.message ?: "unknown error occurred"), ex)
        }
    }

    private fun inferredFormat(): String {
        val first = queues["source"]!!.first()

        when (first.contentType) {
            null -> throw stepConfig.exception(XProcError.xcxTrangCannotDetermineFormat())
            MediaType.RNC -> return "rnc"
            MediaType.DTD -> return "dtd"
            else -> Unit
        }

        if (first.contentType!!.classification() == MediaClassification.XML) {
            val root = S9Api.documentElement(first.value as XdmNode)
            when (root.nodeName.namespaceUri) {
                NsXs.namespace -> return "xsd"
                NsRng.namespace -> return "rng"
                else -> throw stepConfig.exception(XProcError.xcxTrangUnsupportedXmlFormat())
            }
        }

        if (first.contentType!!.classification() == MediaClassification.TEXT) {
            val text = S9Api.textContent(first)
            if (text.trim().startsWith("<")) {
                return "dtd"
            }
            return "rnc"
        }

        throw stepConfig.exception(XProcError.xcxTrangCannotDetermineFormat())
    }

    override fun toString(): String = "cx:trang"

    inner class TrangResolver(): AbstractResolver() {
        override fun resolve(id: Identifier?, input: Input?) {
            if (id == null || input == null) {
                return
            }

            val baseUri = if (rootUriMap.containsKey(id.base)) {
                URI(rootUriMap[id.base]!!)
            } else {
                URI(id.base)
            }

            val resolved = baseUri.resolve(id.uriReference).toString()

            if (resolved !in baseUriToFile) {
                throw stepConfig.exception(XProcError.xcxTrangUnresolvedInput(resolved))
            }

            val mapped = baseUriToFile[resolved]!!
            includeToFile[resolved] = mapped
            fileToBaseUri[resolved] = mapped

            stepConfig.debug { "Trang: ${id.uriReference} (${id.base}) = ${resolved}" }

            input.uri = resolved
        }

        override fun open(input: Input?) {
            if (input == null) {
                return
            }

            if (input.uri !in includeToFile) {
                throw stepConfig.exception(XProcError.xcxTrangUnresolvedInput(input.uri))
            }

            val uri = URI(includeToFile[input.uri]!!)
            val file = File(uri.path)
            input.byteStream = file.inputStream()
        }
    }

    inner class Visitor(): SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            val props = DocumentProperties()
            when (resultFormat) {
                "rnc" -> props[Ns.contentType] = MediaType.RNC.toString()
                "dtd" -> props[Ns.contentType] = MediaType.DTD.toString()
                else -> props[Ns.contentType] = MediaType.XML.toString()
            }
            val loader = DocumentLoader(stepConfig, file.toUri(), props)
            val doc = loader.load()

            val path = doc.baseURI!!.path
            val pos = path.lastIndexOf('/')
            val newbase = outputBaseUri.resolve(path.substring(pos + 1))

            val newdoc = doc.with(newbase)

            receiver.output("result", newdoc)
            file.deleteIfExists()
            return FileVisitResult.CONTINUE
        }
    }
}