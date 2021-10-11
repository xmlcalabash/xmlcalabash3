package com.xmlcalabash.steps.file

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.{InternetProtocolRequest, MediaType, URIUtils}

import java.io.IOException
import java.net.URI
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, Paths, SimpleFileVisitor}

class FileMkdir() extends FileStep {
  private var href: URI = _
  private var failOnError = true

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    super.run(context)

    href = uriBinding(XProcConstants._href).get
    failOnError = booleanBinding(XProcConstants._fail_on_error).getOrElse(failOnError)
    var exception = Option.empty[Exception]

    try {
      if (href.getScheme == "file") {
        fileMkdir(Paths.get(href.getPath))
      } else {
        throw XProcException.xcFileMkdirBadScheme(href, location);
      }
    } catch {
      case ex: Exception =>
        if (failOnError) {
          throw ex
        }
        logger.info("Failed to mkdir " + href);
        exception = Some(ex)
    }

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)

    if (exception.isDefined) {
      errorFromException(builder, exception.get)
    } else {
      builder.addStartElement(XProcConstants.c_result)
      builder.addText(href.toString)
      builder.addEndElement()
    }

    builder.endDocument()

    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }

  private def fileMkdir(path: Path): Unit = {
    if (Files.exists(path)) {
      if (Files.isDirectory(path)) {
        return
      }
      throw XProcException.xcFileMkdirFail(href, location)
    }

    Files.createDirectories(path)
  }
}
