package com.xmlcalabash.steps.file

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{InternetProtocolRequest, MediaType, TypeUtils, URIUtils}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap}
import net.sf.saxon.s9api.QName

import java.net.URI
import java.nio.file.attribute.{FileTime, PosixFilePermission}
import java.nio.file.{Files, Path, Paths}
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}

class FileInfo() extends FileStep {
  private var href: URI = _
  private var failOnError = true

  private var staticContext: StaticContext = _
  private var builder: SaxonTreeBuilder = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    super.run(context)

    staticContext = context
    href = uriBinding(XProcConstants._href).get
    failOnError = booleanBinding(XProcConstants._fail_on_error).getOrElse(failOnError)
    var exception = Option.empty[Exception]

    builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)

    try {
      if (href.getScheme == "file") {
        fileInfo(href)
      } else if (href.getScheme == "http" || href.getScheme == "https") {
        httpInfo(href)
      } else {
        throw XProcException.xcFileInfoBadScheme(href, location);
      }
    } catch {
      case ex: Exception =>
        if (failOnError) {
          throw ex
        }
        logger.info("Failed to get info for " + href);
        exception = Some(ex)
    }

    builder.endDocument()

    if (exception.isDefined) {
      // Discard the things we might have built...
      builder = new SaxonTreeBuilder(config)
      builder.startDocument(None)
      errorFromException(builder, exception.get)
      builder.endDocument()
    }

    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }

  private def fileInfo(href: URI): Unit = {
    val path = Paths.get(href.getPath)
    if (!Files.exists(path)) {
      throw XProcException.xdDoesNotExist(href.toString, location)
    }

    val attrs = pathInfo(path)

    if (Files.isSymbolicLink(path)) {
      builder.addStartElement(XProcConstants.c_symbolic_link, attrs)
      builder.addEndElement()
    } else if (Files.isDirectory(path)) {
      builder.addStartElement(XProcConstants.c_directory, attrs)
      builder.addEndElement()
    } else if (Files.isRegularFile(path)) {
      builder.addStartElement(XProcConstants.c_file, attrs)
      builder.addEndElement()
    } else {
      builder.addStartElement(XProcConstants.c_other, attrs)
      builder.addEndElement()
    }
  }

  private def pathInfo(path: Path): AttributeMap = {
    var amap: AttributeMap = EmptyAttributeMap.getInstance()

    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, href.toString))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._readable, Files.isReadable(path).toString))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._writable, Files.isWritable(path).toString))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._hidden, Files.isHidden(path).toString))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._last_modified, instantUTC(Files.getLastModifiedTime(path))))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._size, Files.size(path).toString))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._content_type, URIUtils.guessContentType(href).toString))

    val attr = Files.readAttributes(path, "*")
    if (attr.containsKey("lastAccessTime")) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants.cx_last_accessed, instantUTC(attr.get("lastAccessTime").asInstanceOf[FileTime])))
    }
    if (attr.containsKey("creationTime")) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants.cx_creation_time, instantUTC(attr.get("creationTime").asInstanceOf[FileTime])))
    }

    if (Files.getOwner(path) != null) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants.cx_owner, Files.getOwner(path).toString))
    }

    val posixPerm = Files.getPosixFilePermissions(path)
    for (perm <- PosixFilePermission.values()) {
      val localname = perm.toString.toLowerCase().replace("_", "-")
      val aname = new QName("cx", XProcConstants.ns_cx, localname)
      amap = amap.put(TypeUtils.attributeInfo(aname, posixPerm.contains(perm).toString))
    }

    amap
  }

  private def instantUTC(dt: FileTime): String = {
    val zdt = ZonedDateTime.ofInstant(dt.toInstant, ZoneId.systemDefault())
    zdt.format(DateTimeFormatter.ISO_INSTANT)
  }

  private def httpInfo(href: URI): Unit = {
    val request = new InternetProtocolRequest(config, staticContext, href)
    val response = request.execute("HEAD")
    if (response.statusCode.getOrElse(404) != 200) {
      throw XProcException.xdDoesNotExist(href.toString, location)
    }

    if (response.report.isEmpty) {
      throw new RuntimeException("InternetProtocolResponse has no report?")
    }

    val report = TypeUtils.castAsScala(response.report.get).asInstanceOf[Map[Any,Any]]
    if (!report.contains("headers")) {
      throw new RuntimeException("InternetProtocolResponse report has no headers?")
    }

    var amap: AttributeMap = EmptyAttributeMap.getInstance()

    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, href.toString))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._readable, "true"))
    amap = amap.put(TypeUtils.attributeInfo(XProcConstants.cx_multipart, response.multipart.toString))

    val headermap = report("headers").asInstanceOf[Map[Any,Any]]
    for (header <- headermap.keySet) {
      val key = header.toString
      val value = headermap(header).toString

      key match {
        case "last-modified" =>
          val zdt = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
          amap = amap.put(TypeUtils.attributeInfo(XProcConstants._last_modified, zdt.format(DateTimeFormatter.ISO_INSTANT)))
        case "content-length" =>
          amap = amap.put(TypeUtils.attributeInfo(XProcConstants._size, value))
        case "content-type" =>
          amap = amap.put(TypeUtils.attributeInfo(XProcConstants._content_type, value))
        case "name" =>
          logger.debug("Cannot add 'name' (" + value + ") property to URI info for " + href)
        case "readable" =>
          logger.debug("Cannot add 'readable' (" + value + ") property to URI info for " + href)
        case _ =>
          val qname = new QName("cx", XProcConstants.ns_cx, key)
          amap = amap.put(TypeUtils.attributeInfo(qname, value))
      }
    }

    builder.addStartElement(XProcConstants.c_uri, amap)
    builder.addEndElement()
  }
}
