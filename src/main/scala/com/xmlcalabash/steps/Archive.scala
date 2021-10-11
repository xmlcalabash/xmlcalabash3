package com.xmlcalabash.steps

import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.zip.{ZipEntry, ZipException}
import com.jafpl.steps.PortCardinality
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, NameValueBinding, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, TypeUtils}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap}
import net.sf.saxon.s9api.{Axis, QName, XdmAtomicValue, XdmNode, XdmNodeKind, XdmValue}
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipArchiveOutputStream, ZipFile}
import org.apache.commons.compress.utils.IOUtils

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Archive extends DefaultXmlStep {
  private val _zip = new QName("", "zip")
  private val _command = new QName("", "command")
  private val _relativeTo = new QName("", "relative-to")

  private val zipCommands = List("update", "create", "freshen", "delete")
  private val zipMethods = List("none", "deflated")
  private val zipLevels = List("default", "smallest", "fastest", "huffman", "none")
  private var defaultMethod = "deflated"
  private var defaultLevel = "default"

  private var context: StaticContext = _

  private var sources = ListBuffer.empty[DocumentWrapper]

  private var manifest_source = Option.empty[Any]
  private var manmeta = Option.empty[XProcMetadata]

  private var archive_source = Option.empty[Any]
  private var arcmeta = Option.empty[XProcMetadata]

  private var format = Option.empty[QName]
  private var parameters = Map.empty[QName, XdmValue]
  private var relativeTo: URI = _
  private var command = ""
  private val manifest = new Manifest()

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.ZERO_OR_MORE,
      "manifest" -> PortCardinality.ZERO_OR_MORE,
      "archive" -> PortCardinality.ZERO_OR_MORE),
    Map("source" -> List("*/*"),
      "manifest" -> List("application/xml"),
      "archive" -> List("*/*"))
  )

  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.EXACTLY_ONE,
      "report" -> PortCardinality.EXACTLY_ONE),
    Map("result" -> List("*/*"),
      "report" -> List("application/xml"))
  )

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    port match {
      case "source" =>
        sources += new DocumentWrapper(item, metadata)
      case "manifest" =>
        if (manifest_source.isDefined) {
          throw XProcException.xcArchiveTooManyManifests(location)
        }
        manifest_source = Some(item)
        manmeta = Some(metadata)
      case "archive" =>
        if (archive_source.isDefined) {
          throw XProcException.xcArchiveTooManyArchives(location)
        }
        archive_source = Some(item)
        arcmeta = Some(metadata)
      case _ => throw new RuntimeException("Unexpected port:" + port)
    }
  }

  override def receiveBinding(variable: NameValueBinding): Unit = {
    super.receiveBinding(variable)
    if (variable.name == XProcConstants._parameters && variable.value.size() > 0) {
      parameters = ValueParser.parseParameters(variable.value, variable.context)
    }
  }

  override def run(context: StaticContext): Unit = {
    this.context = context

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

    val result = processZip()
    val props = mutable.HashMap.empty[QName,XdmValue]
    if (arcmeta.isDefined && arcmeta.get.baseURI.isDefined) {
      props.put(XProcConstants._base_uri, new XdmAtomicValue(arcmeta.get.baseURI.get))
    } else {
      props.put(XProcConstants._base_uri, new XdmAtomicValue(context.baseURI.get))
    }
    consumer.get.receive("result", result, new XProcMetadata(MediaType.ZIP, props.toMap))

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)
    builder.addStartElement(XProcConstants.c_archive)
    for (entry <- manifest.toList) {
      entry.makeEntry(builder)
    }
    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("report", builder.result, new XProcMetadata(MediaType.XML))
  }

  private def processZip(): BinaryNode = {
    command = if (parameters.contains(_command)) {
      parameters(_command).getUnderlyingValue.getStringValue
    } else {
      "update"
    }
    if (!zipCommands.contains(command)) {
      throw XProcException.xcArchiveInvalidParameterValue("command", command, location)
    }

    if (command == "delete" && archive_source.isEmpty) {
      throw XProcException.xcArchiveTooFewArchives(location)
    }

    if (parameters.contains(XProcConstants._level)) {
      defaultLevel = parameters(XProcConstants._level).getUnderlyingValue.getStringValue
      if (!zipLevels.contains(defaultLevel)) {
        throw XProcException.xcArchiveInvalidParameterValue("level", defaultLevel, location)
      }
    }

    if (parameters.contains(XProcConstants._method)) {
      defaultMethod = parameters(XProcConstants._method).getUnderlyingValue.getStringValue
      if (!zipMethods.contains(defaultMethod)) {
        throw XProcException.xcArchiveInvalidParameterValue("method", defaultMethod, location)
      }
    }

    relativeTo = if (definedBinding(_relativeTo)) {
      uriBinding(_relativeTo).get
    } else {
      if (archive_source.isDefined) {
        arcmeta.get.baseURI.get
      } else {
        context.baseURI.get
      }
    }

    if (manifest_source.isDefined) {
      manifest_source.get match {
        case node: XdmNode => constructManifest(Some(node))
        case _ => throw new RuntimeException("Unexpected document on manifest port")
      }
    } else {
      constructManifest(None)
    }

    // ZIP requires random access: https://commons.apache.org/proper/commons-compress/zip.html
    var zis: ZipFile = null
    val entries = ListBuffer.empty[ZipArchiveEntry]
    if (archive_source.isDefined) {
      if (!MediaType.ZIP.matches(arcmeta.get.contentType)) {
        throw XProcException.xcArchiveFormatError(format.get, location)
      }
      // We can't update the ZIP file in place, no matter what update means because
      // it could result in multiple threads doing updates at the same time.
      archive_source.get match {
        case bn: BinaryNode =>
          try {
            zis = new ZipFile(bn.file)
            val enum = zis.getEntries
            while (enum.hasMoreElements) {
              entries += enum.nextElement()
            }
          } catch {
            case _: ZipException =>
              throw XProcException.xcArchiveFormatError(format.get, location)
            case ex: Exception =>
              throw ex
          }
        case _ =>
          throw XProcException.xcArchiveFormatError(format.get, location)
      }
    }

    val arcnode = new BinaryNode(config, Array.empty[Byte])
    val zos = new ZipArchiveOutputStream(arcnode.file)
    val processed = mutable.HashSet.empty[String]

    // Process the current entries first so that order is preserved
    for (entry <- entries) {
      val mentry = manifest.getName(entry.getName)
      if (mentry.isDefined) {
        if (command == "delete") {
          // nop
        } else {
          mentry.get.method match {
            case "none" => entry.setMethod(ZipEntry.STORED)
            case "deflated" => entry.setMethod(ZipEntry.DEFLATED)
            case _ => throw XProcException.xcArchiveInvalidParameterValue("method", mentry.get.method, location)
          }
          // FIXME: levels? List("smallest", "fastest", "default", "huffman", "none")
          if (mentry.get.comment != "") {
            entry.setComment(mentry.get.comment)
          }

          zos.putArchiveEntry(entry)
          writeEntry(mentry.get, zos)
          zos.closeArchiveEntry()
        }
      } else {
        zos.putArchiveEntry(entry)

        if (zis.canReadEntryData(entry)) {
          val baos = new ByteArrayOutputStream()
          IOUtils.copy(zis.getInputStream(entry), baos)
          zos.write(baos.toByteArray)
        } else {
          logger.info(s"Cannot read {$entry.getName} from ZIP")
        }

        zos.closeArchiveEntry()
      }

      processed += entry.getName
    }

    if (command != "delete" && command != "freshen") {
      for (mentry <- manifest.toList) {
        if (!processed.contains(mentry.name)) {
          val zentry = new ZipArchiveEntry(mentry.name)
          mentry.method match {
            case "none" => zentry.setMethod(ZipEntry.STORED)
            case "deflated" => zentry.setMethod(ZipEntry.DEFLATED)
            case _ => throw XProcException.xcArchiveInvalidParameterValue("method", mentry.method, location)
          }
          // FIXME: levels? List("smallest", "fastest", "default", "huffman", "none")
          if (mentry.comment != "") {
            zentry.setComment(mentry.comment)
          }

          zos.putArchiveEntry(zentry)
          writeEntry(mentry, zos)
          zos.closeArchiveEntry()
        }
      }
    }

    if (Option(zis).isDefined) {
      zis.close()
    }
    zos.close()
    arcnode
  }

  private def inferredFormat(): Option[QName] = {
    if (arcmeta.isDefined) {
      val ctype = arcmeta.get.contentType.mediaType + "/" + arcmeta.get.contentType.mediaSubtype
      ctype match {
        case "application/zip" => Some(_zip)
        case _ => None
      }
    } else {
      Some(_zip)
    }
  }

  private def writeEntry(entry: ManifestEntry, zip: ArchiveOutputStream): Unit = {
    var source = Option.empty[Any]
    var meta = Option.empty[XProcMetadata]
    for (doc <- sources) {
      if (doc.meta.baseURI.get == entry.href) {
        source = Some(doc.doc)
        meta = Some(doc.meta)
      }
    }

    val baos = new ByteArrayOutputStream()
    if (source.isDefined) {
      serialize(context, source.get, meta.get, baos)
    } else {
      val request = new DocumentRequest(entry.href)
      val response = config.documentManager.parse(request)
      serialize(context, response.value, new XProcMetadata(response.contentType), baos)
    }

    zip.write(baos.toByteArray)
  }

  private def constructManifest(optSource: Option[XdmNode]): Unit = {
    if (optSource.isDefined) {
      val source = optSource.get
      val root = S9Api.documentElement(source).get
      if (root.getNodeName != XProcConstants.c_archive) {
        throw XProcException.xcArchiveBadManifest(location)
        throw new RuntimeException("Archive isn't a c:archive")
      }
      for (entry <- S9Api.axis(root, Axis.CHILD)) {
        entry.getNodeKind match {
          case XdmNodeKind.ELEMENT =>
            if (entry.getNodeName != XProcConstants.c_entry) {
              throw XProcException.xcArchiveBadManifest(location)
            }
            var attr = Option(entry.getAttributeValue(XProcConstants._name))
            val name = if (attr.isDefined) {
              attr.get
            } else {
              throw XProcException.xcArchiveBadManifest(location)
            }

            attr = Option(entry.getAttributeValue(XProcConstants._href))
            val href = if (attr.isDefined) {
              resolveURI(entry.getBaseURI, attr.get)
            } else {
              throw XProcException.xcArchiveBadManifest(location)
            }

            attr = Option(entry.getAttributeValue(XProcConstants._comment))
            val comment = if (attr.isDefined) {
              attr.get
            } else {
              ""
            }

            attr = Option(entry.getAttributeValue(XProcConstants._method))
            val method = if (attr.isDefined) {
              attr.get
            } else {
              defaultMethod
            }

            attr = Option(entry.getAttributeValue(XProcConstants._level))
            val level = if (attr.isDefined) {
              attr.get
            } else {
              defaultLevel
            }

            manifest.add(new ManifestEntry(name, href, comment, method, level))

          case XdmNodeKind.TEXT =>
            if (entry.getStringValue.trim != "") {
              throw XProcException.xcArchiveBadManifest(location)
            }
          case XdmNodeKind.PROCESSING_INSTRUCTION => ()
          case XdmNodeKind.COMMENT => ()
          case XdmNodeKind.DOCUMENT => () // impossible
          case XdmNodeKind.ATTRIBUTE => () // impossible
          case XdmNodeKind.NAMESPACE => () // impossible
        }
      }
    }

    val seenUris = new mutable.HashSet[URI]
    for (source <- sources) {
      val uri = source.meta.baseURI
      if (uri.isDefined) {
        if (seenUris.contains(uri.get)) {
          throw XProcException.xcArchiveBadURI(uri.get, location)
        } else {
          seenUris += uri.get
        }
      } else {
        throw XProcException.xcArchiveBadURI(location)
      }

      if (manifest.getURI(uri.get).isEmpty) {
        val baseStr = uri.get.toASCIIString
        val relStr = relativeTo.toASCIIString
        val name= if (baseStr.startsWith(relStr)) {
          baseStr.substring(relStr.length)
        } else {
          uri.get.getPath.substring(1) // Skip the leading "/"
        }

        manifest.add(new ManifestEntry(name, uri.get, "", defaultMethod, defaultLevel))
      }
    }
  }

  class DocumentWrapper(val doc: Any, val meta: XProcMetadata) {
    // nop
  }

  class Manifest() {
    private val uris = mutable.HashMap.empty[URI, ManifestEntry]
    private val names = mutable.HashMap.empty[String, ManifestEntry]
    private val entries = ListBuffer.empty[ManifestEntry]

    def add(entry: ManifestEntry): Unit = {
      if (uris.contains(entry.href)) {
        throw new RuntimeException("Duplicate URI in manifest")
      }
      if (names.contains(entry.name)) {
        throw new RuntimeException("Duplicate name in manifest")
      }
      uris.put(entry.href, entry)
      names.put(entry.name, entry)
      entries += entry
    }

    def getURI(href: URI): Option[ManifestEntry] = {
      uris.get(href)
    }

    def getName(name: String): Option[ManifestEntry] = {
      names.get(name)
    }

    def toList: List[ManifestEntry] = {
      entries.toList
    }
  }

  class ManifestEntry(val name: String, val href: URI, val comment: String, val method: String, val level: String) {
    def makeEntry(builder: SaxonTreeBuilder): Unit = {
      var amap: AttributeMap = EmptyAttributeMap.getInstance()

      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._name, name))
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._href, href.toASCIIString))
      if (comment != "") {
        amap = amap.put(TypeUtils.attributeInfo(XProcConstants._comment, comment))
      }
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._method, method))
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._level, level))

      builder.addStartElement(XProcConstants.c_entry, amap)
      builder.addEndElement()
    }
  }
}