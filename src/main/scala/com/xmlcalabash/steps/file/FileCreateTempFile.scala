package com.xmlcalabash.steps.file

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType

import java.net.URI
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

class FileCreateTempFile() extends FileStep {
  private var failOnError = true
  private var deleteOnExit = false

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val href = uriBinding(XProcConstants._href)
    val prefix = optionalStringBinding(XProcConstants._prefix)
    val suffix = optionalStringBinding(XProcConstants._suffix)
    failOnError = booleanBinding(XProcConstants._fail_on_error).getOrElse(failOnError)
    deleteOnExit = booleanBinding(XProcConstants._delete_on_exit).getOrElse(deleteOnExit)
    var exception = Option.empty[Exception]
    var tempfile = Option.empty[Path]

    try {
      var dir: Path = null

      if (href.isDefined) {
        if (href.get.getScheme != "file") {
          throw XProcException.xcFileCreateTempFileBadScheme(href.get, location);
        }
        dir = Paths.get(href.get.getPath)
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
          throw XProcException.xdDoesNotExist(href.get.toString, location)
        }
      } else {
        val tdir = System.getProperty("java.io.tmpdir")
        if (tdir != null) {
          dir = Paths.get(tdir)
        } else {
          // I give up, use the cwd
          dir = Paths.get(".")
        }
      }

      tempfile = Some(Files.createTempFile(dir, prefix.orNull, suffix.orNull))
      if (deleteOnExit) {
        tempfile.get.toFile.deleteOnExit()
      }
    } catch {
      case ex: Exception =>
        if (failOnError) {
          throw ex
        }
        exception = Some(ex)
        logger.info("Failed to create temp file");
    }

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)

    if (exception.isDefined) {
      errorFromException(builder, exception.get)
    } else {
      builder.addStartElement(XProcConstants.c_result)
      builder.addText(tempfile.get.toString)
      builder.addEndElement()
    }

    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }
}
