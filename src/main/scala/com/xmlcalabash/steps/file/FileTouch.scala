package com.xmlcalabash.steps.file

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType

import java.io.FileOutputStream
import java.nio.file.{Files, Path, Paths}
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.util.{Calendar, Date}

class FileTouch() extends FileStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val href = uriBinding(XProcConstants._href).get
    val failOnError = booleanBinding(XProcConstants._fail_on_error).getOrElse(true)
    val timestamp = optionalStringBinding(XProcConstants._timestamp)
    var exception = Option.empty[Exception]

    var lastModified = Calendar.getInstance().getTime
    if (timestamp.isDefined) {
      try {
        val zdt = ZonedDateTime.parse(timestamp.get, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        lastModified = Date.from(zdt.toInstant)
      } catch {
        case _: DateTimeParseException =>
          val zdt = LocalDateTime.parse(timestamp.get, DateTimeFormatter.ISO_DATE_TIME)
          lastModified = Date.from(zdt.toInstant(ZoneOffset.UTC))
        case ex: Exception =>
          throw ex
      }
    }

    try {
      if (href.getScheme != "file") {
        throw XProcException.xcFileTouchBadScheme(href, location);
      }

      val path = Paths.get(href.getPath)
      if (!Files.exists(path)) {
        new FileOutputStream(path.toFile).close()
      }

      if (!Files.exists(path)) {
        throw XProcException.xdDoesNotExist(href.toString, location)
      }

      path.toFile.setLastModified(lastModified.getTime)
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
      builder.addText(href.toString)
      builder.addEndElement()
    }

    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }
}
