package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabashBuilder
import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.DocumentConverter
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.*
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.io.*
import java.net.URI
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists

@TestInstance(PER_CLASS)
class MarshallTest {
    lateinit var stepConfig: StepConfiguration
    lateinit var cwd: URI
    val tempdir = if (System.getProperty("java.io.tmpdir") == null) {
        Paths.get(".")
    } else {
        Paths.get(System.getProperty("java.io.tmpdir"))
    }

    @BeforeAll
    fun init() {
        val xmlCalabash = XmlCalabashBuilder().build()
        val builder = xmlCalabash.newPipelineBuilder()
        stepConfig = builder.stepConfig
        cwd = UriUtils.cwdAsUri()
    }

    fun propertyMap(props: Map<String,String>): MutableMap<QName, XdmValue> {
        val properties = mutableMapOf<QName, XdmValue>()
        for ((name, value) in props) {
            val pname = stepConfig.typeUtils.parseQName(name)
            if (value == "true" || value == "false") {
                properties[pname] = XdmAtomicValue(value == "true")
            } else {
                properties[pname] = XdmAtomicValue(value)
            }
        }
        return properties
    }

    fun openStream(uri: URI): InputStream {
        val file = File(uri.path)
        return FileInputStream(file)
    }

    fun temporaryFile(ext: String? = ".bin"): Path {
        val file = Files.createTempFile(tempdir, null, ext)
        file.toFile().deleteOnExit()
        return file
    }

    fun textContent(file: Path, encoding: String = "UTF-8"): String {
        if (!Charset.isSupported(encoding)) {
            throw stepConfig.exception(XProcError.xdUnsupportedDocumentCharset(encoding))
        }
        val charset = Charset.forName(encoding)
        val reader = InputStreamReader(FileInputStream(file.toFile()), charset)
        val sb = StringBuilder()
        val buf = CharArray(4096)
        var len = reader.read(buf)
        while (len >= 0) {
            sb.appendRange(buf, 0, len)
            len = reader.read(buf)
        }
        return sb.toString()
    }

    @Test
    fun readXml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.xml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.XML)
        Assertions.assertEquals(MediaType.XML, doc.contentType)
    }

    @Test
    fun writeXml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.xml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.XML)

        val temp = temporaryFile(".xml")
        val writer = DocumentWriter(doc, FileOutputStream(temp.toFile()))
        writer.write()
        val text = textContent(temp)
        Assertions.assertTrue(text.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        temp.deleteIfExists()
    }

    @Test
    fun writeXmlOmitDecl() {
        val uri = cwd.resolve("src/test/resources/marshall/input.xml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.XML)

        val temp = temporaryFile(".xml")
        val writer = DocumentWriter(doc, FileOutputStream(temp.toFile()))
        writer[Ns.omitXmlDeclaration] = true
        writer.write()

        val text = textContent(temp)
        Assertions.assertTrue(text.startsWith("<doc>"))
        temp.deleteIfExists()
    }

    @Test
    fun writeXmlEncoded() {
        val uri = cwd.resolve("src/test/resources/marshall/input.xml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.XML)

        val temp = temporaryFile(".xml")
        val writer = DocumentWriter(doc, FileOutputStream(temp.toFile()))
        writer[Ns.encoding] = "iso-8859-1"
        writer.write()

        val text = textContent(temp)
        Assertions.assertTrue(text.startsWith("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>"))
        Assertions.assertTrue(text.contains("&#x201c;"))
        temp.deleteIfExists()
    }

    @Test
    fun readBrokenXml() {
        val uri = cwd.resolve("src/test/resources/marshall/input-broken.xml")
        val loader = DocumentLoader(stepConfig, uri)
        try {
            val doc = loader.load(openStream(uri), MediaType.XML)
            fail()
        } catch (ex: XProcException) {
            Assertions.assertEquals(ex.error.code, NsErr.xd(49))
        }
    }

    @Test
    fun readXHtml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.xhtml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.XHTML)
        Assertions.assertEquals(MediaType.XHTML, doc.contentType)
    }

    @Test
    fun writeXhtml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.xhtml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.XHTML)

        val temp = temporaryFile(".xhtml")
        val writer = DocumentWriter(doc, FileOutputStream(temp.toFile()))
        writer.write()

        val text = textContent(temp)
        Assertions.assertTrue(text.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        Assertions.assertTrue(text.contains("<meta "))
        temp.deleteIfExists()
    }

    @Test
    fun readBrokenXhtml() {
        val uri = cwd.resolve("src/test/resources/marshall/input-broken.xhtml")
        val loader = DocumentLoader(stepConfig, uri)
        try {
            val doc = loader.load(openStream(uri), MediaType.XHTML)
            fail()
        } catch (ex: XProcException) {
            Assertions.assertEquals(ex.error.code, NsErr.xd(49))
        }
    }

    @Test
    fun readHtml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.html")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.HTML)
        Assertions.assertEquals(MediaType.HTML, doc.contentType)
    }

    @Test
    fun writeHtml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.html")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.HTML)

        val temp = temporaryFile(".html")
        val writer = DocumentWriter(doc, FileOutputStream(temp.toFile()))
        writer.write()

        val text = textContent(temp)
        Assertions.assertTrue(text.startsWith("<!DOCTYPE HTML>"))
        Assertions.assertTrue(text.contains("<meta "))
        temp.deleteIfExists()
    }

    @Test
    fun writeHtml5() {
        val uri = cwd.resolve("src/test/resources/marshall/input.html")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.HTML)

        val temp = temporaryFile(".html")
        val writer = DocumentWriter(doc, FileOutputStream(temp.toFile()))
        writer[Ns.htmlVersion] = "5"
        writer.write()

        val text = textContent(temp)
        Assertions.assertTrue(text.startsWith("<!DOCTYPE HTML>"))
        Assertions.assertTrue(text.contains("<meta "))
        temp.deleteIfExists()
    }

    @Test
    fun readBrokenHtml() {
        val uri = cwd.resolve("src/test/resources/marshall/input-broken.html")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.HTML)
        Assertions.assertEquals(MediaType.HTML, doc.contentType)
    }

    @Test
    fun readJsonMap() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-map.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.JSON)
        Assertions.assertEquals(MediaType.JSON, doc.contentType)
    }

    @Test
    fun readJsonMapDupForbid() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-map-dup-keys.json")
        val loader = DocumentLoader(stepConfig, uri, DocumentProperties(), mapOf(QName("duplicates") to XdmAtomicValue("reject")))
        try {
            val doc = loader.load(openStream(uri), MediaType.JSON)
            fail()
        } catch (ex: XProcException) {
            Assertions.assertEquals(ex.error.code, NsErr.xd(58))
        }
    }

    @Test
    fun readJsonArray() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-array.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.JSON)
        Assertions.assertEquals(MediaType.JSON, doc.contentType)
    }

    @Test
    fun readJsonNumber() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-number.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.JSON)
        Assertions.assertEquals(MediaType.JSON, doc.contentType)
    }

    @Test
    fun readJsonString() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-string.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.JSON)
        Assertions.assertEquals(MediaType.JSON, doc.contentType)
    }

    @Test
    fun writeJson() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-map.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.JSON)

        val temp = temporaryFile(".json")
        val writer = DocumentWriter(doc, FileOutputStream(temp.toFile()))
        writer.write()

        val text = textContent(temp)
        Assertions.assertTrue(text.startsWith("{"))
        Assertions.assertTrue(text.contains("\"number\":17"))
        temp.deleteIfExists()
    }

    @Test
    fun readYaml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.yaml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.YAML)
        Assertions.assertEquals(MediaType.YAML, doc.contentType)
        Assertions.assertTrue(doc.value is XdmMap)
    }

    @Test
    fun writeYaml() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-map.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.YAML)

        val temp = temporaryFile(".yaml")
        val writer = DocumentWriter(doc, FileOutputStream(temp.toFile()))
        writer.write()

        val text = textContent(temp)
        Assertions.assertFalse(text.startsWith("{"))
        Assertions.assertTrue(text.contains("number: 17"))
        temp.deleteIfExists()
    }

    @Test
    fun writeYamlOverrideMethod() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-map.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.JSON)

        val temp = temporaryFile(".yaml")
        val writer = DocumentWriter(doc, FileOutputStream(temp.toFile()))
        writer[Ns.method] = "Q{${NsCx.namespace}}yaml"
        writer.write()

        val text = textContent(temp)
        Assertions.assertFalse(text.startsWith("{"))
        Assertions.assertTrue(text.contains("number: 17"))
        temp.deleteIfExists()
    }

    @Test
    fun readToml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.toml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.TOML)
        Assertions.assertEquals(MediaType.TOML, doc.contentType)
        Assertions.assertTrue(doc.value is XdmMap)
    }

    @Test
    fun writeToml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.toml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.TOML)

        val temp = temporaryFile(".toml")
        val writer = DocumentWriter(doc, FileOutputStream(temp.toFile()))
        writer.write()

        val text = textContent(temp)
        Assertions.assertFalse(text.startsWith("{"))
        Assertions.assertTrue(text.contains("database.enabled = true"))
        temp.deleteIfExists()
    }

    @Test
    fun writeTomlOverrideMethod() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-map.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.JSON)

        val temp = temporaryFile(".toml")
        val writer = DocumentWriter(doc, FileOutputStream(temp.toFile()))
        writer[Ns.method] = "Q{${NsCx.namespace}}toml"
        writer.write()

        val text = textContent(temp)
        Assertions.assertFalse(text.startsWith("{"))
        Assertions.assertTrue(text.contains("number = 17"))
        temp.deleteIfExists()
    }

    @Test
    fun readText() {
        val uri = cwd.resolve("src/test/resources/marshall/input.txt")
        val mediaType = MediaType.parse("text/arbitrary")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), mediaType)

        Assertions.assertEquals(mediaType, doc.contentType)
        Assertions.assertTrue(doc.value.toString().contains("emergency"))
    }

    @Test
    fun writeText() {
        val uri = cwd.resolve("src/test/resources/marshall/input.txt")
        val mediaType = MediaType.parse("text/arbitrary")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), mediaType)

        val temp = temporaryFile(".txt")
        val writer = DocumentWriter(doc, FileOutputStream(temp.toFile()))
        writer.write()

        val text = textContent(temp)
        Assertions.assertTrue(text.startsWith("This"))
        Assertions.assertTrue(text.contains("emergency"))
        temp.deleteIfExists()
    }

    @Test
    fun readBinary() {
        val uri = cwd.resolve("src/test/resources/marshall/input.jpg")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.JPEG)

        Assertions.assertEquals(MediaType.JPEG, doc.contentType)
        Assertions.assertTrue(doc is XProcBinaryDocument)
    }

    @Test
    fun writeBinary() {
        val uri = cwd.resolve("src/test/resources/marshall/input.zip")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.ZIP)

        val temp = temporaryFile(".bin")
        val writer = DocumentWriter(doc, FileOutputStream(temp.toFile()))
        writer.write()

        val stream = FileInputStream(temp.toFile())
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        var len = stream.read(buf)
        while (len >= 0) {
            baos.write(buf, 0, len)
            len = stream.read(buf)
        }
        val bytes = baos.toByteArray()

        Assertions.assertEquals('P'.code.toByte(), bytes[0])
        Assertions.assertEquals('K'.code.toByte(), bytes[1])
        temp.deleteIfExists()
    }

    // xml to xml
    @Test
    fun convertXmltoXml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.xml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.XML)

        val props = propertyMap(mapOf(
            "method" to "xml",
            "omit-xml-declaration" to "true"
        ))
        doc.properties.setSerialization(stepConfig.typeUtils.asXdmMap(props))
        doc.properties.set(NsCx.link, XdmAtomicValue("false"))

        val converter = DocumentConverter(stepConfig, doc, MediaType.XSLT)
        val converted = converter.convert()

        Assertions.assertEquals(MediaType.XSLT, converted.contentType)
        Assertions.assertTrue(converted.properties.getSerialization().keySet().size == 2)
        Assertions.assertEquals(XdmAtomicValue("false"), doc.properties.get(NsCx.link))
    }

    // xml to html (removes serialization)
    @Test
    fun convertXmltoHtml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.xml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.XML)

        val props = propertyMap(mapOf(
            "method" to "xml",
            "omit-xml-declaration" to "true"
        ))
        doc.properties.setSerialization(stepConfig.typeUtils.asXdmMap(props))
        doc.properties.set(NsCx.link, XdmAtomicValue("false"))

        val converter = DocumentConverter(stepConfig, doc, MediaType.HTML)
        val converted = converter.convert()

        Assertions.assertEquals(MediaType.HTML, converted.contentType)
        Assertions.assertTrue(converted.properties.getSerialization().isEmpty)
        Assertions.assertEquals(XdmAtomicValue("false"), doc.properties.get(NsCx.link))
    }

    // fn:map xml to json
    @Test
    fun convertFnJsonXmltoJson() {
        val stream = ByteArrayInputStream("{\"test\": 17}".toByteArray(StandardCharsets.UTF_8))
        val loader = DocumentLoader(stepConfig, null)
        val originalJson = loader.load(stream, MediaType.JSON)

        val xmlConverter = DocumentConverter(stepConfig, originalJson, MediaType.XML)
        val xmlDoc = xmlConverter.convert()

        val root = S9Api.documentElement(xmlDoc.value as XdmNode)
        Assertions.assertEquals(NsFn.map, root.nodeName)

        val jsonConverter = DocumentConverter(stepConfig, xmlDoc, MediaType.JSON)
        val jsonDoc = jsonConverter.convert()

        Assertions.assertTrue(jsonDoc.value is XdmMap)
    }

    // c:param-set xml to json
    @Test
    fun convertParamSetXmltoJson() {
        val xml = "<c:param-set xmlns:c='${NsC.namespace}'>" +
                "<c:param name='string' value='Some string value'/>" +
                "<c:param name='number' value='17'/>" +
                "<c:param name='other' namespace='http://example.com/' value='Some string value'/>" +
                "</c:param-set>"
        val stream = ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8))
        val loader = DocumentLoader(stepConfig, null)
        val xmlDoc = loader.load(stream, MediaType.XML)

        val converter = DocumentConverter(stepConfig, xmlDoc, MediaType.JSON)
        val jsonDoc = converter.convert()

        Assertions.assertTrue(jsonDoc.value is XdmMap)
        val map = jsonDoc.value as XdmMap
        Assertions.assertEquals(XdmAtomicValue("Some string value"), map.get(XdmAtomicValue(QName("string"))))
    }

    // other xml to json
    @Test
    fun convertOtherXmltoJson() {
        val xml = "<test><this/></test>"
        val stream = ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8))
        val loader = DocumentLoader(stepConfig, null)
        val xmlDoc = loader.load(stream, MediaType.XML)

        val converter = DocumentConverter(stepConfig, xmlDoc, MediaType.JSON)
        try {
            val jsonDoc = converter.convert()
            fail()
        } catch (ex: Exception) {
            // pass
        }
    }

    // xml to text
    @Test
    fun convertXmlToText() {
        val uri = cwd.resolve("src/test/resources/marshall/input.xml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.XML)

        val converter = DocumentConverter(stepConfig, doc, MediaType.TEXT)
        converter[Ns.encoding] = "iso-8859-1"
        val textDoc = converter.convert()

        val text = textDoc.value.underlyingValue.stringValue
        Assertions.assertTrue(text.startsWith("<doc>"))
        Assertions.assertTrue(textDoc.properties.getSerialization().isEmpty)
    }

    // xml to binary
    @Test
    fun convertXmlToBinary() {
        val uri = cwd.resolve("src/test/resources/marshall/input.xml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.XML)

        val converter = DocumentConverter(stepConfig, doc, MediaType.JPEG)
        try {
            val bindoc = converter.convert()
            fail()
        } catch (ex: Exception) {
            // pass
        }
    }

    // xml to text
    @Test
    fun convertDataXmlToBinary() {
        val uri = cwd.resolve("src/test/resources/marshall/input-base64.xml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.XML)

        val converter = DocumentConverter(stepConfig, doc, MediaType.ZIP)
        val binDoc = converter.convert() as XProcBinaryDocument

        Assertions.assertEquals('P'.code.toByte(), binDoc.binaryValue[0])
        Assertions.assertEquals('K'.code.toByte(), binDoc.binaryValue[1])
    }

    // json to xml
    @Test
    fun convertJsonToXml() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-map.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.JSON)

        val converter = DocumentConverter(stepConfig, doc, MediaType.XML)
        converter[Ns.indent] = true

        val xmlDoc = converter.convert()
        Assertions.assertEquals(MediaType.XML, xmlDoc.contentType)
        Assertions.assertTrue(xmlDoc.properties.getSerialization().isEmpty)

        val root = S9Api.documentElement(xmlDoc.value as XdmNode)
        Assertions.assertEquals(NsFn.map, root.nodeName)
    }

    // json to html
    @Test
    fun convertJsonToHtml() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-map.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.JSON)

        val converter = DocumentConverter(stepConfig, doc, MediaType.HTML)
        try {
            val xmlDoc = converter.convert()
            fail()
        } catch (ex: Exception) {
            // pass
        }
    }

    // json to yaml
    @Test
    fun convertJsonToYaml() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-map.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.JSON)

        val converted = DocumentConverter(stepConfig, doc, MediaType.YAML).convert()
        Assertions.assertEquals(MediaType.YAML, converted.contentType)
    }

    // json to text
    @Test
    fun convertJsonToText() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-map.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.JSON)

        val converted = DocumentConverter(stepConfig, doc, MediaType.TEXT).convert()
        val text = converted.value.underlyingValue.stringValue
        Assertions.assertTrue(text.startsWith("{"))
        Assertions.assertTrue(text.contains("\"number\":17"))
    }

    // yaml to text
    @Test
    fun convertYamlToText() {
        val uri = cwd.resolve("src/test/resources/marshall/input.yaml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.YAML)

        val converted = DocumentConverter(stepConfig, doc, MediaType.TEXT).convert()
        val text = converted.value.underlyingValue.stringValue
        Assertions.assertTrue(text.startsWith("jobs:"))
        Assertions.assertTrue(text.contains("fetch-depth: 0"))
    }

    // toml to text
    @Test
    fun convertTomlToText() {
        val uri = cwd.resolve("src/test/resources/marshall/input.toml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.TOML)

        val converted = DocumentConverter(stepConfig, doc, MediaType.TEXT).convert()
        val text = converted.value.underlyingValue.stringValue
        Assertions.assertTrue(text.contains("owner.dob ="))
    }

    // json to binary
    @Test
    fun convertJsonToBinary() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-map.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.JSON)

        val converter = DocumentConverter(stepConfig, doc, MediaType.JPEG)
        try {
            val bindoc = converter.convert()
            fail()
        } catch (ex: Exception) {
            // pass
        }
    }

    // text to xml
    @Test
    fun convertTextToXml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.xml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.TEXT)

        val props = propertyMap(mapOf(
            "method" to "xml",
            "omit-xml-declaration" to "true"
        ))
        doc.properties.setSerialization(stepConfig.typeUtils.asXdmMap(props))
        doc.properties.set(NsCx.link, XdmAtomicValue("false"))

        val converted = DocumentConverter(stepConfig, doc, MediaType.XSLT).convert()
        Assertions.assertTrue(converted.value is XdmNode)
        val root = S9Api.documentElement(converted.value as XdmNode)
        Assertions.assertTrue(root.nodeName == QName("doc"))
        Assertions.assertEquals(0, converted.properties.getSerialization().mapSize())
        Assertions.assertEquals(XdmAtomicValue("false"), converted.properties[NsCx.link])
    }

    // text to xml
    @Test
    fun convertTextToHtml() {
        val uri = cwd.resolve("src/test/resources/marshall/input-broken.html")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.TEXT)

        val props = propertyMap(mapOf(
            "method" to "xml",
            "omit-xml-declaration" to "true"
        ))
        doc.properties.setSerialization(stepConfig.typeUtils.asXdmMap(props))
        doc.properties.set(NsCx.link, XdmAtomicValue("false"))

        val converted = DocumentConverter(stepConfig, doc, MediaType.HTML).convert()
        Assertions.assertTrue(converted.value is XdmNode)
        Assertions.assertEquals(0, converted.properties.getSerialization().mapSize())
        val root = S9Api.documentElement(converted.value as XdmNode)
        Assertions.assertEquals(root.nodeName, QName(NamespaceUri.of("http://www.w3.org/1999/xhtml"), "html"))
    }

    // text to json
    @Test
    fun convertTextToJson() {
        val uri = cwd.resolve("src/test/resources/marshall/input-json-array.json")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.TEXT)

        val props = propertyMap(mapOf(
            "method" to "xml",
            "omit-xml-declaration" to "true"
        ))
        doc.properties.setSerialization(stepConfig.typeUtils.asXdmMap(props))
        doc.properties.set(NsCx.link, XdmAtomicValue("false"))

        val converted = DocumentConverter(stepConfig, doc, MediaType.JSON).convert()
        Assertions.assertTrue(converted.value is XdmArray)
        Assertions.assertEquals(0, converted.properties.getSerialization().mapSize())
    }

    // text to yaml
    @Test
    fun convertTextToYaml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.yaml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.TEXT)

        val props = propertyMap(mapOf(
            "method" to "xml",
            "omit-xml-declaration" to "true"
        ))
        doc.properties.setSerialization(stepConfig.typeUtils.asXdmMap(props))
        doc.properties.set(NsCx.link, XdmAtomicValue("false"))

        val converted = DocumentConverter(stepConfig, doc, MediaType.YAML).convert()
        Assertions.assertTrue(converted.value is XdmMap)
        Assertions.assertEquals(0, converted.properties.getSerialization().mapSize())
    }

    // text to toml
    @Test
    fun convertTextToToml() {
        val uri = cwd.resolve("src/test/resources/marshall/input.toml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.TEXT)

        val props = propertyMap(mapOf(
            "method" to "xml",
            "omit-xml-declaration" to "true"
        ))
        doc.properties.setSerialization(stepConfig.typeUtils.asXdmMap(props))
        doc.properties.set(NsCx.link, XdmAtomicValue("false"))

        val converted = DocumentConverter(stepConfig, doc, MediaType.TOML).convert()
        Assertions.assertTrue(converted.value is XdmMap)
        Assertions.assertEquals(0, converted.properties.getSerialization().mapSize())
    }

    // text to text
    @Test
    fun convertTextToText() {
        val uri = cwd.resolve("src/test/resources/marshall/input.toml")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.TEXT)

        val props = propertyMap(mapOf(
            "method" to "xml",
            "omit-xml-declaration" to "true"
        ))
        doc.properties.setSerialization(stepConfig.typeUtils.asXdmMap(props))
        doc.properties.set(NsCx.link, XdmAtomicValue("false"))

        val converted = DocumentConverter(stepConfig, doc, MediaType.parse("text/arbitrary")).convert()
        Assertions.assertTrue(converted.value is XdmNode)
        Assertions.assertEquals(2, converted.properties.getSerialization().mapSize())
    }

    // text to binary
    @Test
    fun convertTextToBinary() {
        val uri = cwd.resolve("src/test/resources/marshall/input.txt")
        val loader = DocumentLoader(stepConfig, uri)
        val doc = loader.load(openStream(uri), MediaType.TEXT)

        val converter = DocumentConverter(stepConfig, doc, MediaType.JPEG)
        try {
            val bindoc = converter.convert()
            fail()
        } catch (ex: Exception) {
            // pass
        }
    }
}
