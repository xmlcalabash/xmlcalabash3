package com.xmlcalabash.steps.os

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, SaxonApiException, XdmNode}

import java.io.{ByteArrayInputStream, File, IOException, InputStream, InputStreamReader}
import java.nio.file.{Files, Paths}
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.jdk.CollectionConverters.SeqHasAsJava

class OsExec extends DefaultXmlStep {
  private val _command = new QName("", "command")
  private val _args = new QName("", "args")
  private val _cwd = new QName("", "cwd")
  private val _result_content_type = new QName("", "result-content-type")
  private val _error_content_type = new QName("", "error-content-type")
  private val _path_separator = new QName("", "path-separator")
  private val _failure_threshold = new QName("", "failure-threshold")

  private var command = ""
  private var args = List.empty[String]
  private var cwd = Option.empty[String]
  private var resultContentType = MediaType.TEXT
  private var errorContentType = MediaType.TEXT
  private var pathSeparator = Option.empty[String]
  private var failureThreshold = Option.empty[Integer]

  var source = Option.empty[Any]
  var sourceMeta: XProcMetadata = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.ZERO_OR_MORE, "error" -> PortCardinality.EXACTLY_ONE, "exit-status" -> PortCardinality.EXACTLY_ONE),
    Map("result" -> List("*"), "error" -> List("*"), "exit-status" -> List("application/xml")))

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (source.isDefined) {
      throw XProcException.xdInputSequenceNotAllowed(port, location)
    }
    source = Some(item)
    sourceMeta = metadata
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    command = stringBinding(_command)
    args = listOfStringBinding(_args)
    cwd = optionalStringBinding(_cwd)
    resultContentType = MediaType.parse(stringBinding(_result_content_type, "text/plain"))
    errorContentType = MediaType.parse(stringBinding(_error_content_type, "text/plain"))
    pathSeparator = optionalStringBinding(_path_separator)
    failureThreshold = integerBinding(_failure_threshold)

    val slash = System.getProperty("file.separator")
    if (pathSeparator.isDefined) {
      val sep = pathSeparator.get
      if (sep.length != 1) {
        throw XProcException.xcOsExecBadSeparator(sep, location)
      }

      command = command.replace(sep, slash)
      if (cwd.isDefined) {
        cwd = Some(cwd.get.replace(sep, slash))
      }
      val newlist = ListBuffer.empty[String]
      for (arg <- args) {
        newlist += arg.replace(sep, slash)
      }
      args = newlist.toList
    }

    if (cwd.isDefined) {
      val path = Paths.get(cwd.get)
      if (!Files.exists(path) || !Files.isDirectory(path)) {
        throw XProcException.xcOsExecBadCwd(cwd.get, location)
      }
    }

    val stdout = ArrayBuffer.empty[Byte]
    val stderr = ArrayBuffer.empty[Byte]
    var rc = 0

    try {
      val showCmd = new StringBuilder()
      val commandLine = ListBuffer.empty[String]
      commandLine += command
      showCmd.append(command)
      for (arg <- args) {
        commandLine += arg
        showCmd.append(" ").append(arg)
      }
      val builder = new ProcessBuilder(commandLine.toList.asJava)
      if (cwd.isDefined) {
        builder.directory(new File(cwd.get))
      }

      logger.debug("Exec: " + showCmd.toString())

      val process = builder.start()

      val stdoutReader = new ProcessOutputReader(process.getInputStream, stdout)
      val stderrReader = new ProcessOutputReader(process.getErrorStream, stderr)

      val stdoutThread = new Thread(stdoutReader)
      val stderrThread = new Thread(stderrReader)

      stdoutThread.start()
      stderrThread.start()

      try {
        val os = process.getOutputStream
        if (source.isDefined) {
          // If the process doesn't care about input and finishes before we finish serializing, this
          // can cause an error.
          serialize(context, source.get, sourceMeta, os)
        }
        os.close()
      } catch {
        case ex: SaxonApiException =>
          var ignore = false
          if (ex.getCause != null) {
            val cause = ex.getCause.getCause
            if (cause != null) {
              cause match {
                case iex: IOException =>
                  ignore = iex.getMessage.contains("Stream closed")
              }
            }
          }
          if (!ignore) {
            throw ex
          }
      }

      rc = process.waitFor()
      stdoutThread.join()
      stderrThread.join()
    } catch {
      case ex: Exception =>
        throw ex
    }

    if (failureThreshold.isDefined && rc > failureThreshold.get) {
      throw XProcException.xcOsExecFailed(rc, location)
    }

    val outreq = new DocumentRequest(None, Some(resultContentType), location)
    val outres = config.documentManager.parse(outreq, new ByteArrayInputStream(stdout.toArray))

    if (outres.shadow.isDefined) {
      val node = new BinaryNode(config, outres.shadow.get)
      consumer.get.receive("result", node, new XProcMetadata(resultContentType))
    } else {
      consumer.get.receive("result", outres.value, new XProcMetadata(resultContentType))
    }

    val errreq = new DocumentRequest(None, Some(errorContentType), location)
    val errres = config.documentManager.parse(errreq, new ByteArrayInputStream(stderr.toArray))

    if (errres.shadow.isDefined) {
      val node = new BinaryNode(config, errres.shadow.get)
      consumer.get.receive("error", node, new XProcMetadata(errorContentType))
    } else {
      consumer.get.receive("error", errres.value, new XProcMetadata(errorContentType))
    }

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)
    builder.addStartElement(XProcConstants.c_result)
    builder.addText(rc.toString)
    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("exit-status", builder.result, new XProcMetadata(MediaType.XML))
  }

  private class ProcessOutputReader(val is: InputStream, val buffer: ArrayBuffer[Byte]) extends Runnable {
    private val tree = new SaxonTreeBuilder(config)

    override def run(): Unit = {
      tree.startDocument(None)
      tree.addStartElement(XProcConstants.c_result)
      val reader = new InputStreamReader(is)
      val buf: Array[Char] = new Array(4096)
      var len = reader.read(buf, 0, buf.length)
      while (len >= 0) {
        if (len == 0) {
          Thread.sleep(250)
        } else {
          // This seems horifically crude...
          for (pos <- 0 to len) {
            buffer.append(buf(pos).toByte)
          }
        }
        len = reader.read(buf, 0, buf.length)
      }
    }
  }


}
