package com.xmlcalabash.steps

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import java.net.{URI, URLConnection}
import java.util.regex.{Pattern, PatternSyntaxException}
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, NameValueBinding, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, URIUtils, Urify}
import net.sf.saxon.s9api.{QName, XdmArray, XdmValue}
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.IOUtils

import scala.collection.mutable.ListBuffer

class Unarchive extends DefaultXmlStep {
  private val _zip = new QName("", "zip")

  private val _relativeTo = new QName("", "relative-to")

  private var source: Any = _
  private var smeta: XProcMetadata = _

  private var format = Option.empty[QName]
  private var parameters = Map.empty[QName, XdmValue]
  private var relativeTo: URI = _
  private var overrideContentTypes = List.empty[Tuple2[Pattern,MediaType]]
  private var includeFilter = ListBuffer.empty[String]
  private var excludeFilter = ListBuffer.empty[String]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

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
      uriBinding(_relativeTo).get
    } else {
      if (smeta.baseURI.isEmpty) {
        throw XProcException.xcArchiveNoBaseURI(location)
      }
      new URI(smeta.baseURI.get.toString + "/")
    }

    if (bindings.contains(XProcConstants._include_filter)) {
      val value = bindings(XProcConstants._include_filter).value
      val iter = value.iterator()
      while (iter.hasNext) {
        includeFilter += iter.next().getStringValue
      }
    }

    if (bindings.contains(XProcConstants._exclude_filter)) {
      val value = bindings(XProcConstants._exclude_filter).value
      val iter = value.iterator()
      while (iter.hasNext) {
        excludeFilter += iter.next().getStringValue
      }
    }

    format.get match {
      case `_zip` => unzip(context)
      case _ => ()
    }
  }

  private def unzip(context: StaticContext): Unit = {
    // ZIP requires random access: https://commons.apache.org/proper/commons-compress/zip.html
    source match {
      case bn: BinaryNode =>
        unzipFile(context, bn.file)
      case _ =>
        throw XProcException.xcArchiveFormatError(format.get, location)
    }
  }

  private def unzipFile(context: StaticContext, zfile: File): Unit = {
    val zipIn = new ZipFile(zfile)
    val enum = zipIn.getEntries
    while (enum.hasMoreElements) {
      val entry = enum.nextElement()

      if (!entry.isDirectory) {
        var matches = includeFilter.isEmpty
        for (regex <- includeFilter) {
          val patn = Pattern.compile(regex)
          matches = matches || patn.matcher(entry.getName).matches()
        }
        for (regex <- excludeFilter) {
          val patn = Pattern.compile(regex)
          matches = matches && !patn.matcher(entry.getName).matches()
        }

        if (matches) {
          if (zipIn.canReadEntryData(entry)) {
            val baos = new ByteArrayOutputStream()
            IOUtils.copy(zipIn.getInputStream(entry), baos)
            val bais = new ByteArrayInputStream(baos.toByteArray)

            val href = URI.create(new Urify(relativeTo).resolve(entry.getName))

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

            val request = new DocumentRequest(href, contentType.get)
            request.baseURI = href
            val response = config.documentManager.parse(request, bais)
            consumer.get.receive("result", response.value, new XProcMetadata(response.contentType, response.props))
          } else {
            logger.info(s"Cannot read {$entry.getName} from ZIP")
          }
        }
      }
    }
    zipIn.close()
  }

  private def inferredFormat(): Option[QName] = {
    val ctype = smeta.contentType.mediaType + "/" + smeta.contentType.mediaSubtype
    ctype match {
      case "application/zip" => Some(_zip)
      case _ => None
    }
  }
}