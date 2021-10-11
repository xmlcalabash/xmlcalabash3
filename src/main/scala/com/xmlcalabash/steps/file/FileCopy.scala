package com.xmlcalabash.steps.file

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.stores.{DataInfo, DataReader, DataWriter}
import com.xmlcalabash.util.{InternetProtocolRequest, MediaType}
import net.sf.saxon.s9api.{QName, XdmAtomicValue}

import java.io.{InputStream, OutputStream}
import java.net.URI
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{Files, LinkOption, Path, Paths, StandardCopyOption}
import scala.collection.mutable.ListBuffer

class FileCopy() extends FileStep {
  private val cx_copyLinks = new QName("cx", XProcConstants.ns_cx, "copy-links")
  private val cx_copyAttributes = new QName("cx", XProcConstants.ns_cx, "copy-attributes")
  private val _bufsize = 8192

  private var overwrite = true
  private var failOnError = true
  private var copyLinks = false
  private var copyAttributes = false

  private var staticContext: StaticContext = _
  private var href: URI = _
  private var target: URI = _
  private var exception = Option.empty[Exception]
  private val failures = ListBuffer.empty[URI]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(context: StaticContext): Unit = {
    super.run(context)

    staticContext = context
    href = uriBinding(XProcConstants._href).get
    target = uriBinding(XProcConstants._target).get

    overwrite = booleanBinding(XProcConstants._overwrite).getOrElse(overwrite);
    failOnError = booleanBinding(XProcConstants._fail_on_error).getOrElse(failOnError)
    copyLinks = booleanBinding(cx_copyLinks).getOrElse(copyLinks)
    copyAttributes = booleanBinding(cx_copyAttributes).getOrElse(copyAttributes)

    try {
      // Let's do some error checking
      if (href.getScheme != "file" && href.getScheme != "http" && href.getScheme != "https") {
        throw XProcException.xcFileCopyBadScheme(href, location);
      }

      if (target.getScheme != "file" && target.getScheme != "http" && target.getScheme != "https") {
        throw XProcException.xcFileCopyBadScheme(target, location);
      }

      if (href.getScheme == "file") {
        copyFromFile()
      } else {
        copyFromHttp()
      }
    } catch {
      case ex: Exception =>
        if (failOnError) {
          throw ex
        }
        exception = Some(ex)
    }

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)

    if (exception.isDefined) {
      errorFromException(builder, exception.get)
    } else {
      builder.addStartElement(XProcConstants.c_result)
      builder.addText(target.toString)
      builder.addEndElement()
    }

    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }

  private def copyFromFile(): Unit = {
    val srcpath = Paths.get(href.getPath)

    if (!Files.exists(srcpath)) {
      throw XProcException.xdDoesNotExist(href.toString, location)
    }

    if (Files.isDirectory(srcpath)) {
      if (target.getScheme == "file") {
        // If the source is a directory, "/path/from", and the target is "/path/to",
        // we want to copy the files in /path/from/* to /path/to/from/
        copyFileDirectoryToFile(srcpath, Paths.get(target.getPath).resolve(srcpath.getFileName))
      } else {
        if (target.getPath.endsWith("/")) {
          // Okay, we'll try this
          copyFileDirectoryToHttp(srcpath)
        } else {
          throw XProcException.xcFileCopyDirToFile(href, target, location);
        }
      }
    }
  }

  private def copyFromHttp(): Unit = {
    if (href.getPath.endsWith("/")) {
      copyFromHttpDirectory()
    } else {
      copyFromHttpFile()
    }
  }

  def copyFromHttpFile(): Unit = {
    if (target.getScheme == "file") {
      var destination = Paths.get(target.getPath)
      if (!Files.exists(destination)) {
        Files.createDirectories(destination)
      }
      if (!Files.exists(destination)) {
        throw XProcException.xcCannotStore(target, location)
      }
      if (Files.isDirectory(destination)) {
        var name = href.getPath
        val pos = name.lastIndexOf("/")
        if (pos >= 0) {
          name = name.substring(pos+1)
        }
        destination = destination.resolve(name)
      }

      config.datastore.readEntry(href.toString, href, "*/*", None, new CopyReader(destination.toUri))
    } else {
      config.datastore.readEntry(href.toString, href, "*/*", None, new CopyReader(target))
    }
  }

  def copyFromHttpDirectory(): Unit = {
    if (target.getScheme == "file") {
      val destination = Paths.get(target.getPath)
      if (!Files.exists(destination)) {
        Files.createDirectories(destination)
      }
      if (!Files.exists(destination)) {
        throw XProcException.xcCannotStore(target, location)
      }
      if (!Files.isDirectory(destination)) {
        throw XProcException.xcFileCopyDirToFile(href, target, location);
      }

      if (!target.getPath.endsWith("/")) {
        target = new URI("file://" + target.getPath + "/")
      }

      config.datastore.listEachEntry(href.toString, href, "*/*", new CopyListReader(target))
    } else {
      if (target.getPath.endsWith("/")) {
        // Okay, we'll try this
        config.datastore.listEachEntry(href.toString, href, "*/*", new CopyListReader(target))
      } else {
        throw XProcException.xcFileCopyDirToFile(href, target, location);
      }
    }
  }

  def copyFileDirectoryToFile(source: Path, target: Path): Unit = {
    if (!Files.exists(target)) {
      val permissions = Files.getPosixFilePermissions(source)
      val fileattr = PosixFilePermissions.asFileAttribute(permissions)
      Files.createDirectories(target, fileattr)
    }

    if (!Files.exists(target)) {
      throw new RuntimeException("failed to create " + target);
    }

    if (!Files.isDirectory(target)) {
      throw XProcException.xcFileCopyDirToFile(source.toUri, target.toUri, location);
    }

    Files.walk(source).forEach(copyItemToFile(source, target, _))
  }

  private def copyItemToFile(source: Path, target: Path, item: Path): Unit = {
    if (source == item) {
      // .walk passes the source as the first item, ignore this
      return
    }

    val relsrc = source.relativize(item)
    val output = target.resolve(relsrc)
    if (copyLinks && Files.isSymbolicLink(item)) {
      try {
        if (Files.exists(output, LinkOption.NOFOLLOW_LINKS) && overwrite) {
          Files.delete(output);
        }
        Files.createSymbolicLink(output, Files.readSymbolicLink(item))
      } catch {
        case ex: Exception =>
          if (failOnError) {
            throw ex;
          } else {
            logger.info("Failed to create symlink: " + ex.getMessage)
            failures += output.toUri
            exception = Some(ex)
          }
      }
    } else if (Files.isDirectory(item)) {
      if (!Files.exists(output)) {
        val permissions = Files.getPosixFilePermissions(item)
        val fileattr = PosixFilePermissions.asFileAttribute(permissions)
        Files.createDirectories(output, fileattr)
      }
    } else if (Files.isRegularFile(item) || (Files.isSymbolicLink(item) && !copyLinks)) {
      try {
        if (overwrite) {
          if (copyAttributes) {
            Files.copy(item, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
          } else {
            Files.copy(item, output, StandardCopyOption.REPLACE_EXISTING)
          }
        } else {
          if (copyAttributes) {
            Files.copy(item, output, StandardCopyOption.COPY_ATTRIBUTES)
          } else {
            Files.copy(item, output)
          }
        }
      } catch {
        case ex: Exception =>
          if (failOnError) {
            throw ex;
          } else {
            logger.info("Failed to copy file: " + ex.getMessage)
            failures += item.toUri
            exception = Some(ex)
          }
      }
    } else {
      logger.info("Ignoring special file: " + item);
    }
  }

  def copyFileDirectoryToHttp(source: Path): Unit = {
    Files.walk(source).forEach(copyItemToHttp(source, target, _))
  }

  private def copyItemToHttp(source: Path, target: URI, item: Path): Unit = {
    if (source == item) {
      // .walk passes the source as the first item, ignore this
      return
    }

    if (Files.isDirectory(item)) {
      // nevermind
    } else {
      try {
        val relsrc = source.relativize(item)
        val output = target.resolve(relsrc.toString)
        config.datastore.readEntry(item.toUri.toString, item.toUri, "*/*", None, new CopyReader(output))
      } catch {
        case ex: Exception =>
          if (failOnError) {
            throw ex
          }
          logger.info("Failed to PUT " + item)
          failures += item.toUri
          exception = Some(ex)
      }
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

  private class CopyReader(target: URI) extends DataReader {
    override def load(id: URI, media: String, content: InputStream, len: Option[Long]): Unit = {
      try {
        config.datastore.writeEntry(target.toString, target, "application/octet-stream", new CopyWriter(content))
      } catch {
        case ex: Exception =>
          if (failOnError) {
            throw ex
          }
          logger.info(ex.getMessage)
          failures += target
          exception = Some(ex)
      }
    }
  }

  private class CopyListReader(target: URI) extends DataInfo {
    override def list(id: URI, props: Map[String, XdmAtomicValue]): Unit = {
      try {
        val hrefstr = href.toString
        val destination = id.toString
        if (!destination.startsWith(hrefstr)) {
          throw XProcException.xcCannotStore(id, location)
        }

        val request = new InternetProtocolRequest(config, staticContext, id)
        val result = request.execute("GET")
        if (result.statusCode.getOrElse(404) == 200) {
          if (result.multipart) {
            throw XProcException.xcCannotStore(id, location)
          }
          config.datastore.writeEntry(destination.substring(hrefstr.length), target, "application/octet-stream", new CopyWriter(result.response.head))
        } else {
          throw XProcException.xdDoesNotExist(id.toString, location)
        }
      } catch {
        case ex: Exception =>
          if (failOnError) {
            throw ex
          }
          logger.info(ex.getMessage)
          failures += target
          exception = Some(ex)
      }
    }
  }

  private class CopyWriter(source: InputStream) extends DataWriter {
    override def store(content: OutputStream): Unit = {
      var buf = source.readNBytes(_bufsize)
      while (buf.nonEmpty) {
        content.write(buf);
        buf = source.readNBytes(_bufsize)
      }
    }
  }
}
