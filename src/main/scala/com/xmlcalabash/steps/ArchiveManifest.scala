package com.xmlcalabash.steps

import java.io.{File, InputStream}
import java.net.URI
import java.util.zip.ZipEntry
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, NameValueBinding, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, TypeUtils, URIUtils}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap}
import net.sf.saxon.s9api.{QName, XdmArray, XdmValue}
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipFile}

import java.util.regex.Pattern
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ArchiveManifest extends DefaultXmlStep {
  private val _zip = new QName("", "zip")
  private val _jar = new QName("", "jar")

  private val _relativeTo = new QName("", "relative-to")

  private var source: Any = _
  private var smeta: XProcMetadata = _

  private var format = Option.empty[QName]
  private var parameters = Map.empty[QName, XdmValue]
  private var relativeTo = Option.empty[URI]
  private var overrideContentTypes = List.empty[Tuple2[Pattern,MediaType]]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYXML

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item
    smeta = metadata
  }

  override def receiveBinding(variable: NameValueBinding): Unit = {
    super.receiveBinding(variable)
    if (variable.name == XProcConstants._parameters && variable.value.size() > 0) {
      parameters = ValueParser.parseParameters(variable.value, variable.context)
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    format = if (qnameBinding(XProcConstants._format).isDefined) {
      qnameBinding(XProcConstants._format)
    } else {
      inferredFormat()
    }

    if (format.isEmpty) {
      throw XProcException.xcUnrecognizedArchiveFormat(location)
    }

    if (format.get != _zip) {
      throw XProcException.xcUnknownArchiveFormat(format.get, location)
    }

    overrideContentTypes = if (definedBinding(XProcConstants._override_content_types)) {
      parseOverrideContentTypes(bindings(XProcConstants._override_content_types).value)
    } else {
      List.empty[Tuple2[Pattern,MediaType]]
    }

    relativeTo = if (uriBinding(_relativeTo).isDefined) {
      uriBinding(_relativeTo)
    } else if (smeta.baseURI.isDefined) {
      smeta.baseURI
    } else {
      throw XProcException.xcArchiveNoBaseURI(location)
    }

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(smeta.baseURI)
    builder.addStartElement(XProcConstants.c_archive)

    format.get match {
      case `_zip` => zipArchive(context, builder)
      case _ => ()
    }

    builder.addEndElement()
    builder.endDocument()
    val result = builder.result

    consumer.get.receive("result", result, new XProcMetadata(MediaType.XML))
  }

  private def zipArchive(context: StaticContext, builder: SaxonTreeBuilder): Unit = {
    // ZIP requires random access: https://commons.apache.org/proper/commons-compress/zip.html
    source match {
      case bn: BinaryNode =>
        zipArchiveFile(context, builder, bn.file)
      case _ =>
        throw XProcException.xcArchiveFormatError(format.get, location)
    }
  }

  private def zipArchiveFile(context: StaticContext, builder: SaxonTreeBuilder, zfile: File): Unit = {
    val zipIn = new ZipFile(zfile)
    val enum = zipIn.getEntries
    while (enum.hasMoreElements) {
      val entry = enum.nextElement()

      if (!entry.isDirectory) {
        var amap: AttributeMap = EmptyAttributeMap.getInstance()
        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, entry.getName))

        val href = if (relativeTo.isDefined) {
          relativeTo.get.resolve(entry.getName)
        } else {
          if (context.baseURI.isDefined) {
            context.baseURI.get.resolve(entry.getName)
          } else {
            // I wouldn't expect this to succeed, as the name is unlikely to be an absolute
            // URI, but I've run out of options.
            new URI(entry.getName)
          }
        }

        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._href, href.toASCIIString))
        amap = archiveAttribute(amap, XProcConstants._comment, Option(entry.getComment))
        amap = entry.getMethod match {
          // ZipArchiveEntry inherites from ZipEntry, but ZipArchiveEntry.STORED doesn't resolve???
          case ZipEntry.STORED => archiveAttribute(amap, XProcConstants._method, Some("none"))
          case ZipEntry.DEFLATED => archiveAttribute(amap, XProcConstants._method, Some("deflated"))
          case _ => archiveAttribute(amap, XProcConstants._method, Some("unknown"))
        }
        if (Option(entry.getComment).isDefined) {
          amap = archiveAttribute(amap, XProcConstants._comment, Some(entry.getComment))
        }
        if (entry.getCompressedSize >= 0) {
          amap = archiveAttribute(amap, XProcConstants._compressed_size, Some(entry.getCompressedSize.toString))
        }
        if (entry.getSize >= 0) {
          amap = archiveAttribute(amap, XProcConstants._size, Some(entry.getSize.toString))
        }

        var contentType = Option.empty[MediaType]
        for (over <- overrideContentTypes) {
          val patn = over._1
          val ctype = over._2
          if (contentType.isEmpty) {
            val rmatch = patn.matcher(entry.getName)
            if (rmatch.matches()) {
              contentType = Some(ctype)
            }
          }
        }

        if (contentType.isEmpty) {
          contentType = Some(URIUtils.guessContentType(href))
        }

        amap = archiveAttribute(amap, XProcConstants._content_type, Some(contentType.get.toString))

        builder.addStartElement(XProcConstants.c_entry, amap)
        builder.addEndElement()
        builder.addText("\n")
      }
    }
    zipIn.close()
  }

  private def archiveAttribute(amap: AttributeMap, name: QName, value: Option[String]): AttributeMap = {
    if (value.isDefined) {
      amap.put(TypeUtils.attributeInfo(name, value.get))
    } else {
      amap
    }
  }

  private def inferredFormat(): Option[QName] = {
    val ctype = smeta.contentType.mediaType + "/" + smeta.contentType.mediaSubtype
    ctype match {
      case "application/zip" => Some(_zip)
      case "application/jar+archive" => Some(_jar)
      case _ => None
    }
  }
}