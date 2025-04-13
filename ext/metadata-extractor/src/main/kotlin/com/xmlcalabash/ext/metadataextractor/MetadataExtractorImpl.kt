package com.xmlcalabash.ext.metadataextractor

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.Metadata
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsC
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.xmpbox.type.*
import org.apache.xmpbox.xml.DomXmpParser
import java.awt.Image
import java.awt.Toolkit
import java.awt.image.ImageObserver
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MetadataExtractorImpl(private val config: XProcStepConfiguration, private val document: XProcDocument, private val properties: Map<QName, XdmValue>) {
    companion object {
        private val cTag = QName(NsC.namespace, "c:tag")
        private val _dir = QName("dir")
        private val _type = QName("type")
        private val _name = QName("name")
        private val _password = QName("password")
        private val rdfAlt = QName("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "Alt")
        private val rdfSeq = QName("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "Seq")
        private val rdfLi = QName("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "li")

        private const val HEADLESS = "java.awt.headless"

        private val controls = listOf(
            "0000", "0001", "0002", "0003", "0004", "0005", "0006", "0007",
            "0008",                 "000b", "000c",         "000e", "000f",
            "0010", "0011", "0012", "0013", "0014", "0015", "0016", "0017",
            "0018", "0019", "001a", "001b", "001c", "001d", "001e", "001f",
            "007c")

        private val APPLICATION_PDF = MediaType.parse("application/pdf")

        private val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

        var bytes = ByteArray(0)
    }

    init {
        df.timeZone = TimeZone.getTimeZone("UTC")
    }

    lateinit var builder: SaxonTreeBuilder

    fun extract(): XdmNode {
        if (document !is XProcBinaryDocument) {
            throw IllegalArgumentException("Document is not binary")
        }

        builder = SaxonTreeBuilder(config.processor)
        builder.startDocument(document.baseURI)

        try {
            if (document.contentType == APPLICATION_PDF) {
                extractPdf()
            } else {
                val metadata = ImageMetadataReader.readMetadata(ByteArrayInputStream(document.binaryValue))
                extractMetadata(metadata)
            }
        } catch (ex: Exception) {
            when (ex) {
                is ImageProcessingException -> {
                    extractIntrinsics()
                }
                else -> throw ex
            }
        }

        builder.endDocument()
        return builder.result
    }

    private fun extractPdf() {
        // There's a lot more metadata that could be in a PDF file...this is just a cursory skim
        val _pages = QName("pages")
        val _width = QName("width")
        val _height = QName("height")
        val _units = QName("units")

        val password = if (properties[_password] != null) {
            properties[_password]!!.underlyingValue.stringValue
        } else {
            ""
        }

        val pdf = try {
            PDDocument.load((document as XProcBinaryDocument).binaryValue, password)
        } catch (ex: Exception) {
            when (ex) {
                is InvalidPasswordException -> throw ex
                else -> throw ex
            }
        }

        val firstPage = if (pdf.numberOfPages > 0) {
            pdf.getPage(0)
        } else {
            null
        }

        val pfxMap = mutableMapOf<String,String>()
        val nsMap = mutableMapOf<String,String>()

        val catalog = pdf.documentCatalog
        val meta = catalog.metadata
        if (meta != null) {
            val xmpParser = DomXmpParser()
            val xmpMeta = xmpParser.parse(meta.createInputStream())
            for (schema in xmpMeta.allSchemas) {
                for (prop in schema.allProperties) {
                    val prefix = computePrefix(prop.prefix, prop.namespace, pfxMap, nsMap)
                    pfxMap[prefix] = prop.namespace
                    nsMap[prop.namespace] = prefix
                }
            }
        }

        var nsmap = NamespaceMap.emptyMap()
        for ((pfx, ns) in pfxMap) {
            nsmap = nsmap.put(pfx, NamespaceUri.of(ns))
        }

        val amap = mutableMapOf<QName, String?>(
            Ns.contentType to document.contentType.toString(),
            Ns.baseUri to document.baseURI?.toString(),
            _pages to pdf.numberOfPages.toString()
        )

        if (firstPage != null) {
            amap[_height] = firstPage.mediaBox.height.toString()
            amap[_width] = firstPage.mediaBox.width.toString()
            amap[_units] = "pt"
        }

        builder.addStartElement(NsC.result, config.typeUtils.attributeMap(amap), nsmap)

        try {
            if (meta != null) {
                val xmpParser = DomXmpParser()
                val metadata = xmpParser.parse(meta.createInputStream())
                for (schema in metadata.allSchemas) {
                    for (prop in schema.allProperties) {
                        val pfx = nsMap[prop.namespace]
                        builder.addStartElement(QName(pfx, prop.namespace, prop.propertyName))
                        when (prop) {
                            is ArrayProperty -> {
                                val outer = if (prop.arrayType == Cardinality.Alt) {
                                    rdfAlt
                                } else {
                                    rdfSeq
                                }
                                val inner = rdfLi

                                builder.addStartElement(outer)
                                for (value in prop.elementsAsString) {
                                    builder.addStartElement(inner)
                                    builder.addText(value)
                                    builder.addEndElement()
                                }
                                builder.addEndElement()
                            }

                            is MIMEType -> builder.addText(prop.stringValue)
                            is TextType -> builder.addText(prop.stringValue)
                            is DateType -> {
                                val value = prop.value
                                builder.addText(df.format(value.time))
                            }
                            else -> {
                                println("cx:metadata-extractor: unknown property type: ${prop}")
                                builder.addText(prop.toString())
                            }
                        }
                        builder.addEndElement()
                    }
                }
            }
        } finally {
            pdf.close()
        }

        builder.addEndElement()
    }

    private fun computePrefix(pfx: String, ns: String, pfxMap: MutableMap<String,String>, nsMap: MutableMap<String,String>): String {
        if (nsMap.contains(ns)) {
            return nsMap[ns]!!
        } else if (!pfxMap.contains(pfx)) {
            return pfx
        } else {
            val cpfx = "ns_"
            var count = 1
            while (pfxMap.contains("$cpfx$count")) {
                count += 1
            }
            return "$cpfx$count"
        }
    }

    private fun extractMetadata(metadata: Metadata) {
        builder.addStartElement(NsC.result)

        for (directory in metadata.directories) {
            val dir = directory.name
            for (tag in directory.tags) {
                val attr = mapOf<QName,String?>(
                    _dir to dir,
                    _type to tag.tagTypeHex,
                    _name to tag.tagName
                )
                builder.addStartElement(cTag, config.typeUtils.attributeMap(attr))

                var value = tag.description

                // Laboriously escape all the control characters with \\uxxxx, but first replace
                // \\uxxxx with \\u005cuxxxx so we don't inadvertantly change the meaning of a string
                value = value.replace("\\\\u([0-9a-fA-F]{4}+)".toRegex(), "\\\\u005cu$1")
                for (control in controls) {
                    val rematch = "^.*\\\\u${control}.*$$".toRegex()
                    if (value.matches(rematch)) {
                        value = value.replace("[\\\\u${control}]", "\\\\u${control}")
                    }
                }

                // Bah humbug. I don't see any way to tell if it's a date/time
                if (value.matches("^\\d\\d\\d\\d:\\d\\d:\\d\\d \\d\\d:\\d\\d:\\d\\d$".toRegex())) {
                    val iso = "${value.substring(0,4)}-${value.substring(5,7)}-${value.substring(8,10)}T${value.substring(11,19)}"
                    value = iso
                }

                builder.addText(value)
                builder.addEndElement()
            }
        }

        builder.addEndElement()
    }

    private fun extractIntrinsics() {
        val headless = System.getProperty(HEADLESS)
        System.setProperty(HEADLESS, "true")

        val temp = File.createTempFile("xmlcalabash-",".bin")
        temp.deleteOnExit()
        val writer = FileOutputStream(temp)
        writer.write((document as XProcBinaryDocument).binaryValue)
        writer.close()

        builder.addStartElement(NsC.result)

        val intrinsics = ImageIntrinsics()
        intrinsics.run(temp)

        builder.addEndElement()

        temp.delete()

        if (headless == null) {
            System.clearProperty(HEADLESS)
        } else {
            System.setProperty(HEADLESS, headless)
        }
    }

    inner class ImageIntrinsics: ImageObserver {
        private var imageFailed = false
        private var width = -1
        private var height = -1

        fun run(data: File) {
            val image = Toolkit.getDefaultToolkit().getImage(data.absolutePath)
            while (!imageFailed && (width < 0 || height < 0)) {
                try {
                    Thread.sleep(50)
                } catch (ex: Exception) {
                    // nop
                }
                // Do something to get the image loading
                image.getWidth(this)
            }
            image.flush()

            if ((width < 0 || height < 0) && imageFailed) {
                // Maybe it's an EPS or a PDF? Do a crude search for the size
                val ir: BufferedReader?
                try {
                    var limit = 100
                    ir = BufferedReader(InputStreamReader(ByteArrayInputStream((document as XProcBinaryDocument).binaryValue)))
                    var line = ir.readLine()

                    if (line != null && line.startsWith("%PDF-")) { // We have a PDF!
                        while (limit > 0 && line != null) {
                            limit -= 1
                            if (line.startsWith("/CropBox [")) {
                                line = line.substring(10)
                                if (line.indexOf("]") >= 0) {
                                    line = line.substring(0, line.indexOf("]"))
                                }
                                parseBox(line)
                                limit = 0
                            }
                            line = ir.readLine()
                        }
                    } else if (line != null && line.startsWith("%!") && line.contains(" EPSF-")) { // We've got an EPS!
                        while (limit > 0 && line != null) {
                            limit -= 1
                            if (line.startsWith("%%BoundingBox: ")) {
                                line = line.substring(15)
                                parseBox(line)
                                limit = 0
                            }
                            line = ir.readLine()
                        }
                    } else {
                        throw RuntimeException("Failed to interpret image: ${document.baseURI ?: "(unknown base URI)"}")
                    }
                } catch (ex: Exception) {
                    throw RuntimeException("Failed to load image: ${document.baseURI ?: "(unknown base URI)"}")
                }

                try {
                    ir.close()
                } catch (ex: Exception) {
                    // nop
                }
            }

            if (width > 0) {
                builder.addStartElement(cTag, config.typeUtils.attributeMap(mapOf(
                    _dir to "Exif",
                    _type to "0x9000",
                    _name to "Exif Version"
                )))
                builder.addText("0")
                builder.addEndElement()

                builder.addStartElement(cTag, config.typeUtils.attributeMap(mapOf(
                    _dir to "Jpeg",
                    _type to "0x0001",
                    _name to "Image Height"
                )))
                builder.addText("${height} pixels")
                builder.addEndElement()

                builder.addStartElement(cTag, config.typeUtils.attributeMap(mapOf(
                    _dir to "Jpeg",
                    _type to "0x0003",
                    _name to "Image Width"
                )))
                builder.addText("${width} pixels")
                builder.addEndElement()
            } else {
                throw RuntimeException("Failed to read image intrinsics=: ${document.baseURI ?: "(unknown base URI)"}")
            }
        }

        fun parseBox(line: String) {
            val corners = intArrayOf(0,0,0,0)
            var count = 0
            var fail = false
            val st = StringTokenizer(line)
            while (!fail && count < 4 && st.hasMoreTokens()) {
                try {
                    corners[count] = st.nextToken().toInt()
                    count += 1
                } catch (ex: Exception) {
                    fail = true
                }
            }

            if (!fail) {
                width = corners[2] - corners[0]
                height = corners[3] - corners[1]
            }
        }

        override fun imageUpdate(img: Image?, infoflags: Int, x: Int, y: Int, width: Int, height: Int): Boolean {
            val error = (infoflags and ImageObserver.ERROR) == ImageObserver.ERROR
            val abort = (infoflags and ImageObserver.ABORT) == ImageObserver.ABORT
            if (error || abort) {
                imageFailed = true
                return false
            }

            if ((infoflags and ImageObserver.WIDTH) == ImageObserver.WIDTH) {
                this.width = width
            }

            if ((infoflags and ImageObserver.HEIGHT) == ImageObserver.HEIGHT) {
                this.height = height
            }

            // I really only care about the width and height, but if I return false as
            // soon as those are available, the BufferedInputStream behind the loader
            // gets closed too early.
            val allbits = (infoflags and ImageObserver.ALLBITS) == ImageObserver.ALLBITS
            return !allbits
        }
    }
}