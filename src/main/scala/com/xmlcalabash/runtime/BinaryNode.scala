package com.xmlcalabash.runtime

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.util.SaxonTreeBuilder
import net.sf.saxon.s9api.XdmNode
import org.apache.http.util.ByteArrayBuffer
import org.slf4j.{Logger, LoggerFactory}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileInputStream, FileOutputStream, InputStream}

// Because the XmlStep API doesn't expose messages, the BinaryNode has to keep track
// of its XdmNode counterpart. (That travels in the message, but we have to be able
// to tunnel it through XmlStep and I don't want to break the node/binary association
// by manufacturing another one later.)

class BinaryNode(config: XMLCalabashConfig, private val rawValue: Any) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var cacheBytes = Option.empty[Array[Byte]]
  private var cacheFile = Option.empty[File]
  private val xdmNodeValue = SaxonTreeBuilder.emptyTree(config)

  def this(runtime: XMLCalabashRuntime, rawValue: Any) = {
    this(runtime.config, rawValue)
  }

  rawValue match {
    case stream: ByteArrayOutputStream =>
      val bytes = stream.toByteArray
      if (bytes.length > 1024000) {
        makeFile(new ByteArrayInputStream(bytes))
      } else {
        logger.trace(s"Storing ${bytes.length} bytes)")
        cacheBytes = Some(bytes)
      }
    case bytes: Array[Byte] =>
      if (bytes.length > 1024000) {
        makeFile(new ByteArrayInputStream(bytes))
      } else {
        logger.trace(s"Storing ${bytes.length} bytes)")
        cacheBytes = Some(bytes)
      }
    case stream: InputStream =>
      // Make a copy of it, we don't know if this one's reusable or not
      makeFile(stream)
    case _ =>
      logger.trace(s"Caching binary (${rawValue.getClass.getName}")
  }

  def value: Any = rawValue
  def node: XdmNode = xdmNodeValue

  def bytes: Array[Byte] = {
    if (cacheBytes.isDefined) {
      cacheBytes.get
    } else if (cacheFile.isDefined) {
      val stream = new FileInputStream(cacheFile.get)
      val pagesize = 4096
      val buffer = new ByteArrayBuffer(pagesize)
      val tmp = new Array[Byte](4096)
      var length = 0
      length = stream.read(tmp)
      while (length >= 0) {
        buffer.append(tmp, 0, length)
        length = stream.read(tmp)
      }
      stream.close()
      buffer.toByteArray
    } else {
      throw new RuntimeException(s"No bytes support for BinaryNode raw value: ${rawValue.getClass.getName}")
    }
  }

  def stream: InputStream = {
    if (cacheBytes.isDefined) {
      new ByteArrayInputStream(cacheBytes.get)
    } else if (cacheFile.isDefined) {
      new FileInputStream(cacheFile.get)
    } else {
      throw new RuntimeException(s"No InputStream support for BinaryNode raw value: ${rawValue.getClass.getName}")
    }
  }

  // If you attempt to access a file, we force the binary node into one
  def file: File = {
    if (cacheFile.isDefined) {
      cacheFile.get
    } else {
      makeFile(stream)
      cacheFile.get
    }
  }

  private def makeFile(stream: InputStream): Unit = {
    val tempFile = File.createTempFile("xmlcalabash-", ".bin")
    tempFile.deleteOnExit()
    val fos = new FileOutputStream(tempFile)
    var totBytes = 0L
    val pagesize = 4096
    val tmp = new Array[Byte](4096)
    var length = 0
    length = stream.read(tmp)
    while (length >= 0) {
      fos.write(tmp, 0, length)
      totBytes += length
      length = stream.read(tmp)
    }
    fos.close()
    stream.close()
    logger.trace(s"Storing $totBytes bytes bytes in $tempFile")
    cacheBytes = None
    cacheFile = Some(tempFile)
  }
}
