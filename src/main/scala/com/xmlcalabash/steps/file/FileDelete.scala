package com.xmlcalabash.steps.file

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{InternetProtocolRequest, MediaType}

import java.io.IOException
import java.net.URI
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, Paths, SimpleFileVisitor}
import scala.collection.mutable.ListBuffer

class FileDelete() extends FileStep {
  private var href: URI = _
  private var recursive = false
  private var failOnError = true

  private var staticContext: StaticContext = _
  private var exception = Option.empty[Exception]
  private val failures = ListBuffer.empty[URI]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    super.run(context)

    staticContext = context
    href = uriBinding(XProcConstants._href).get
    recursive = booleanBinding(XProcConstants._recursive).getOrElse(recursive)
    failOnError = booleanBinding(XProcConstants._fail_on_error).getOrElse(failOnError)

    try {
      if (href.getScheme == "file") {
        deleteFile(href)
      } else if (href.getScheme == "http" || href.getScheme == "https") {
        deleteHttp(href)
      } else {
        throw XProcException.xcFileDeleteBadScheme(href, location);
      }
    } catch {
      case ex: Exception =>
        if (failOnError) {
          throw ex
        }
        logger.info("Failed to delete " + href);
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

  private def deleteFile(href: URI): Unit = {
    val path = Paths.get(href.getPath)
    if (!Files.exists(path)) {
      return
    }

    if (Files.isDirectory(path)) {
      deleteDirectory(path)
    } else {
      Files.delete(path)
    }
  }

  private def deleteDirectory(path: Path): Unit = {
    if (Files.list(path).findAny.isPresent) {
      if (!recursive) {
        throw XProcException.xcFileDeleteNotRecursive(href, location)
      }
      Files.walkFileTree(path, new DeleteVisitor())
    } else {
      Files.delete(path)
    }
  }

  private def deleteHttp(href: URI): Unit = {
    val request = new InternetProtocolRequest(config, staticContext, href)
    val response = request.execute("DELETE")
    if (response.statusCode.getOrElse(204) >= 400) {
      throw new RuntimeException("Failed to delete " + href)
    }
  }

  override protected def errorDetail(builder: SaxonTreeBuilder): Unit = {
    if (failures.nonEmpty) {
      builder.addText("\nThe following failures occurred:\n")
      for (fail <- failures) {
        builder.addStartElement(XProcConstants._uri)
        builder.addText(fail.toString)
        builder.addEndElement()
        builder.addText("\n")
      }
    }
  }

  private class DeleteVisitor extends SimpleFileVisitor[Path] {
    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      try {
        Files.delete(file)
      } catch {
        case ex: Exception =>
          if (failOnError) {
            throw ex
          }
          logger.info("Failed to delete " + file)
          exception = Some(ex)
          failures += file.toUri
      }
      FileVisitResult.CONTINUE
    }
    override def postVisitDirectory(dir: Path, ex: IOException): FileVisitResult = {
      try {
        Files.delete(dir)
      } catch {
        case ex: Exception =>
          if (failOnError) {
            throw ex
          }
          logger.info("Failed to delete " + dir)
          exception = Some(ex)
          failures += dir.toUri
      }
      FileVisitResult.CONTINUE
    }
  }
}
