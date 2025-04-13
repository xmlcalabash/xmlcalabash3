package com.xmlcalabash.steps.file

import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.*
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.XAttributeMap
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

abstract class FileStep(val stepType: QName): AbstractAtomicStep() {
    protected var overrideContentTypes = mutableListOf<Pair<String,MediaType>>()
    protected var failOnError = true
    protected var overwrite = false

    override fun run() {
        super.run()

        if (hasBinding(Ns.overrideContentTypes)) {
            overrideContentTypes.clear()
            overrideContentTypes.addAll(overrideContentTypes(valueBinding(Ns.overrideContentTypes).value))
        }
    }

    protected fun fileAttributes(file: File, detailed: Boolean, contentType: MediaType?, parent: File?): AttributeMap {
        val amap = XAttributeMap()

        if (parent == null) {
            amap[NsXml.base] = file.toURI().toString()
        } else {
            val base = file.toURI().toString()
            val pbase = parent.toURI().toString()
            amap[NsXml.base] = base.substring(pbase.length)
        }

        amap[Ns.name] = file.name

        if (detailed) {
            if (contentType != null) {
                amap[Ns.contentType] = contentType.toString()
            }
            try {
                // This fails on some platforms, for example Windows. I wonder if
                // it's even worth doing.
                val attr = Files.getPosixFilePermissions(file.toPath())
                amap[Ns.readable] = attr.contains(PosixFilePermission.OWNER_READ).toString()
                amap[Ns.writable] = attr.contains(PosixFilePermission.OWNER_WRITE).toString()
                amap[NsCx.executable] = attr.contains(PosixFilePermission.OWNER_EXECUTE).toString()
                amap[NsCx.groupReadable] = attr.contains(PosixFilePermission.GROUP_READ).toString()
                amap[NsCx.groupWritable] = attr.contains(PosixFilePermission.GROUP_WRITE).toString()
                amap[NsCx.groupExecutable] = attr.contains(PosixFilePermission.GROUP_EXECUTE).toString()
                amap[NsCx.otherReadable] = attr.contains(PosixFilePermission.OTHERS_READ).toString()
                amap[NsCx.otherWritable] = attr.contains(PosixFilePermission.OTHERS_WRITE).toString()
                amap[NsCx.otherExecutable] = attr.contains(PosixFilePermission.OTHERS_EXECUTE).toString()
            } catch (_: UnsupportedOperationException) {
                amap[Ns.readable] = file.canRead().toString()
                amap[Ns.writable] = file.canWrite().toString()
                amap[NsCx.executable] = file.canExecute().toString()
            }
            amap[Ns.hidden] = file.isHidden.toString()
            amap[Ns.size] = file.length().toString()
            val stamp = LocalDateTime.ofInstant(Files.getLastModifiedTime(file.toPath()).toInstant(), ZoneId.of("UTC"))
            val dt = ZonedDateTime.of(stamp, ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            amap[Ns.lastModified] = dt
            if (Files.isSymbolicLink(file.toPath())) {
                amap[NsCx.symbolicLink] = file.canonicalPath
            }
        }

        return amap.attributes
    }

    protected fun errorDocument(source: URI?, errorCode: QName, target: URI? = null): XdmNode {
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(source)
        builder.addStartElement(NsC.error, stepConfig.typeUtils.attributeMap(mapOf(Ns.code to "{${errorCode.namespaceUri}}${errorCode.localName}")))
        if (source == null) {
            builder.addText("I/O error")
        } else {
            builder.addText("Error accessing ${source.path}")
        }
        if (target != null) {
            builder.addText(". Target: ${target.path}")
        }
        builder.addEndElement()
        builder.endDocument()
        return builder.result
    }

    protected fun resultDocument(href: URI): XdmNode {
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(href)
        builder.addStartElement(NsC.result)
        builder.addText(href.toString())
        builder.addEndElement()
        builder.endDocument()
        return builder.result
    }

    protected fun errorFromException(builder: SaxonTreeBuilder, errorCode: QName, exception: Exception) {
        val amap = mutableMapOf<QName, String?>(
            Ns.type to stepType.eqName.toString(),
            Ns.code to "{${errorCode.namespaceUri}}${errorCode.localName}"
        )
        if (!stepParams.stepName.startsWith("!")) {
            amap[Ns.name] = stepParams.stepName
        }

        val body = exception.message ?: "???"

        builder.addStartElement(NsC.error, stepConfig.typeUtils.attributeMap(amap))
        builder.addText(body)
        builder.addEndElement()
    }

    protected fun maybeThrow(error: XProcError, href: URI) {
        if (failOnError) {
            throw error.exception()
        } else {
            val err = errorDocument(href, error.code)
            receiver.output("result", XProcDocument.ofXml(err, stepConfig))
        }
    }

    protected fun maybeThrow(error: XProcError, source: URI, target: URI) {
        if (failOnError) {
            throw error.exception()
        } else {
            val err = errorDocument(source, error.code, target)
            receiver.output("result", XProcDocument.ofXml(err, stepConfig))
        }
    }

    override fun toString(): String = "${stepType}"
}