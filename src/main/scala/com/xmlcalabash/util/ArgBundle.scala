package com.xmlcalabash.util

import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.config.{DocumentRequest, XMLCalabashDebugOptions}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.model.xml.XMLContext
import com.xmlcalabash.runtime.{XProcMetadata, XProcXPathExpression}
import net.sf.saxon.lib.NamespaceConstant
import net.sf.saxon.s9api.{Axis, ItemTypeFactory, Processor, QName, XdmAtomicValue, XdmNode, XdmNodeKind, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import java.io.File
import java.net.URI
import java.util.Properties
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ArgBundle() {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val _key = new QName("key")
  private val _value = new QName("value")
  private val _type = new QName("type")

  private val _parameters = ListBuffer.empty[PipelineParameter]
  private var _pipeline = Option.empty[PipelineDocument]
  private var _help = false

  def help: Boolean = _help
  def pipeline: Option[PipelineDocument] = _pipeline
  def parameters: List[PipelineParameter] = _parameters.toList
  def parameters_=(params: List[PipelineParameter]): Unit = {
    _parameters.clear()
    _parameters ++= params
    _pipeline = None
    _help = false
  }

  def environmentOptions(name: QName): List[PipelineEnvironmentOption] = {
    _parameters.toList collect { case p: PipelineEnvironmentOption => p } filter { _.eqname == name.getEQName }
  }

  def namespace(prefix: String, namespace: String): Unit = {
    _parameters += new PipelineNamespace(prefix, namespace)
  }

  def pipeline(xpl: String): Unit = {
    _pipeline = Some(new PipelineFilenameDocument(xpl))
  }

  def pipeline(xpl: URI): Unit = {
    _pipeline = Some(new PipelineURIDocument(xpl))
  }

  def pipeline(xpl: File): Unit = {
    _pipeline = Some(new PipelineFileDocument(xpl, MediaType.XML))
  }

  def pipeline(xpl: XdmNode): Unit = {
    _pipeline = Some(new PipelineXdmDocument(xpl))
  }

  def pipeline(text: String, contentType: MediaType): Unit = {
    _pipeline = Some(new PipelineTextDocument(text, contentType))
  }

  def input(port: String, document: String): Unit = {
    _parameters += new PipelineInputFilename(port, document)
  }

  def input(port: String, document: URI): Unit = {
    _parameters += new PipelineInputURI(port, document)
  }

  def input(port: String, document: File, mediaType: MediaType): Unit = {
    _parameters += new PipelineInputFile(port, document, mediaType)
  }

  def input(port: String, document: File, contentType: String): Unit = {
    input(port, document, MediaType.parse(contentType))
  }

  def input(port: String, document: XdmNode): Unit = {
    _parameters += new PipelineInputXdm(port, document)
  }

  def input(port: String, text: String, contentType: MediaType): Unit = {
    _parameters += new PipelineInputText(port, text, contentType)
  }

  def output(port: String, document: String): Unit = {
    _parameters += new PipelineOutputFilename(port, document)
  }

  def output(port: String, document: URI): Unit = {
    _parameters += new PipelineOutputURI(port, document)
  }

  def output(port: String, consumer: DataConsumer): Unit = {
    _parameters += new PipelineOutputConsumer(port, consumer)
  }

  def option(eqname: String, value: String): Unit = {
    _parameters += new PipelineUntypedOption(eqname, value)
  }

  def option(eqname: String, value: Integer): Unit = {
    _parameters += new PipelineIntegerOption(eqname, value)
  }

  def option(eqname: String, value: Boolean): Unit = {
    _parameters += new PipelineBooleanOption(eqname, value)
  }

  def option(eqname: String, value: Double): Unit = {
    _parameters += new PipelineDoubleOption(eqname, value)
  }

  def option(eqname: String, value: URI): Unit = {
    _parameters += new PipelineUriOption(eqname, value)
  }

  def option(eqname: String, value: XdmValue): Unit = {
    _parameters += new PipelineXdmValueOption(eqname, value)
  }

  def optionDocument(eqname: String, document: String): Unit = {
    _parameters += new PipelineDocumentOption(eqname, new PipelineFilenameDocument(document))
  }

  def optionDocument(eqname: String, document: URI): Unit = {
    _parameters += new PipelineDocumentOption(eqname, new PipelineURIDocument(document))
  }

  def optionDocument(eqname: String, document: File, mediaType: MediaType): Unit = {
    _parameters += new PipelineDocumentOption(eqname, new PipelineFileDocument(document, mediaType))
  }

  def optionDocument(eqname: String, document: File, contentType: String): Unit = {
    optionDocument(eqname, document, MediaType.parse(contentType))
  }

  def optionExpression(eqname: String, value: String): Unit = {
    _parameters += new PipelineExpressionOption(eqname, value)
  }

  def parse(args: List[String]): Unit = {
    // -iport=input       | --input port=input
    // -oport=output      | --output port=output
    // -bprefix=namespace | --bind prefix=namespace
    // -jinjectable       | --inject injectable
    //                    | --norun
    // -d                 | --debug
    // -v                 | --verbose
    //
    // param=string value
    // +param=file value
    // ?param=xpath expression value

    val longPortRegex   = "--(input|output)".r
    val paramRegex      = "([+?])?([^-]\\S+)=(\\S+)".r
    val pipelineRegex   = "([^-])(.*)".r
    val shortOptRegex   = "-(.+)".r
    val longOptRegex    = "--(.+)".r
    var pos = 0
    while (pos < args.length) {
      val opt = args(pos)
      opt match {
        case longPortRegex(kind) =>
          kind match {
            case "input" =>
              val tuple = parsePort(args(pos+1))
              _parameters += new PipelineInputFilename(tuple._1, tuple._2)
            case "output" =>
              val tuple = parsePort(args(pos+1))
              _parameters += new PipelineOutputFilename(tuple._1, tuple._2)
          }
          pos += 2

        case paramRegex(kind, name, value) =>
          kind match {
            case "+" =>
              _parameters += new PipelineDocumentOption(name, new PipelineFilenameDocument(value))
            case "?" =>
              _parameters += new PipelineExpressionOption(name, value)
            case null =>
              _parameters += new PipelineUntypedOption(name, value)
            case _ =>
              throw XProcException.xiArgBundlePfxChar(kind)
          }
          pos += 1

        case longOptRegex(optname) =>
          try {
            optname match {
              case "help" => _help = true
              case "verbose" =>
                _parameters += new PipelineEnvironmentOptionString(XProcConstants.cc_verbose, "true")
              case "norun" =>
                _parameters += new PipelineEnvironmentOptionString(XProcConstants.cc_run, "false")
              case "graph" =>
                parseGraphOptions(args(pos+1))
                pos += 1
              case "config" =>
                _parameters += new PipelineConfigurationFile(new PipelineFilenameDocument(args(pos+1)))
                pos += 1
              case "inject" =>
                _parameters += new PipelineInjectable(args(pos+1))
                pos += 1
              case "bind" =>
                val rest = args(pos + 1)
                val eqpos = rest.indexOf("=")
                if (eqpos > 0) {
                  val prefix = rest.substring(0, eqpos)
                  val uri = rest.substring(eqpos+1)
                  _parameters += new PipelineNamespace(prefix, uri)
                } else {
                  throw XProcException.xiArgBundleCannotParseNamespace(rest)
                }
                pos += 1
              case "stacktrace" =>
                _parameters += new PipelineEnvironmentOptionString(XProcConstants.cc_stacktrace, "true")
              case "info" =>
                _parameters += new PipelineEnvironmentOptionString(XProcConstants.cc_loglevel, "info")
              case "debug" =>
                _parameters += new PipelineEnvironmentOptionString(XProcConstants.cc_loglevel, "debug")
              case _ => throw XProcException.xiArgBundleUnexpectedOption(optname)
            }
          } catch {
            case _: IndexOutOfBoundsException =>
              throw XProcException.xiArgBundleIndexOOB(optname)
            case t: Throwable => throw t
          }
          pos += 1
        case shortOptRegex(chars) =>
          var optname = ""
          try {
            var skip = false
            var chpos = 0
            for (ch <- chars) {
              optname = ch.toString
              if (!skip) {
                ch match {
                  case 'v' =>
                    _parameters += new PipelineEnvironmentOptionString(XProcConstants.cc_verbose, "true")
                  case 'h' => _help = true
                  case 'i' =>
                    var rest = chars.substring(chpos + 1)
                    if (rest == "" && pos < args.length) {
                      pos += 1
                      rest = args(pos)
                    }

                    val eqpos = rest.indexOf("=")
                    if (eqpos > 0) {
                      val port = rest.substring(0, eqpos)
                      val value = rest.substring(eqpos+1)
                      _parameters += new PipelineInputFilename(port, value)
                    } else {
                      throw XProcException.xiArgBundleCannotParseInput(s"-i$rest")
                    }
                    skip = true

                  case 'o' =>
                    var rest = chars.substring(chpos + 1)
                    if (rest == "" && pos < args.length) {
                      pos += 1
                      rest = args(pos)
                    }

                    val eqpos = rest.indexOf("=")
                    if (eqpos > 0) {
                      val port = rest.substring(0, eqpos)
                      val value = rest.substring(eqpos+1)
                      _parameters += new PipelineOutputFilename(port, value)
                    } else {
                      throw XProcException.xiArgBundleCannotParseOutput(s"-o$rest")
                    }
                    skip = true

                  case 'j' =>
                    val rest = chars.substring(chpos + 1)
                    _parameters += new PipelineInjectable(rest)
                    skip = true

                  case 'b' =>
                    var rest = ""
                    if (chpos + 1 == chars.length) {
                      rest = args(pos + 1)
                      pos += 1
                    } else {
                      rest = chars.substring(chpos + 1)
                      skip = true
                    }
                    val eqpos = rest.indexOf("=")
                    if (eqpos > 0) {
                      val prefix = rest.substring(0, eqpos)
                      val uri = rest.substring(eqpos+1)
                      _parameters += new PipelineNamespace(prefix, uri)
                    } else {
                      throw XProcException.xiArgBundleCannotParseNamespace(rest)
                    }
                  case 'D' =>
                    val rest = chars.substring(chpos + 1)
                    val eqpos = rest.indexOf("=")
                    if (eqpos > 0) {
                      val prop = rest.substring(0, eqpos)
                      val value = rest.substring(eqpos+1)
                      _parameters += new PipelineSystemProperty(prop, value)
                    } else {
                      throw XProcException.xiArgBundleCannotParseProperty(s"-D$rest")
                    }
                    skip = true

                  case _ =>
                    throw XProcException.xiArgBundleUnexpectedOption(ch.toString)
                }
              }
              chpos += 1
            }
          } catch {
            case _: IndexOutOfBoundsException =>
              throw XProcException.xiArgBundleIndexOOB(optname)
            case t: Throwable => throw t
          }
          pos += 1
        case pipelineRegex(pfx,rest) =>
          if (_pipeline.isEmpty) {
            _pipeline = Some(new PipelineFilenameDocument(pfx + rest))
          } else {
            throw XProcException.xiArgBundleMultiplePipelines(_pipeline.get.toString, pfx+rest)
          }
          pos += 1
        case _ =>
          throw XProcException.xiArgBundleUnexpectedOption(opt)
      }
    }
  }

  private def parsePort(binding: String): Tuple2[String,String] = {
    val pos = binding.indexOf("=")
    if (pos < 1) {
      throw XProcException.xiArgBundleInvalidPortSpec(binding)
    }
    val port = binding.substring(0, pos)
    val fn = binding.substring(pos+1)
    (port,fn)
  }

  private def parseGraphOptions(opts: String): Unit = {
    val validKeys = List(XMLCalabashDebugOptions.TREE, XMLCalabashDebugOptions.PIPELINE,
      XMLCalabashDebugOptions.GRAPH, XMLCalabashDebugOptions.OPENGRAPH)

    val options = opts.split("\\s*,\\s*")
    for (opt <- options) {
      val token = opt

      var key = Option.empty[String]
      for (valid <- validKeys) {
        if (valid.startsWith(token)) {
          if (key.isDefined) {
            throw XProcException.xiArgBundleAmbiguousGraphKey(token)
          }
          key = Some(valid)
        }
      }

      if (key.isDefined) {
        _parameters += new PipelineEnvironmentOptionString(XProcConstants.cc_graph, key.get)
      } else {
        throw XProcException.xiArgBundleInvalidGraphKey(token)
      }
    }
  }

  def load(cfg: URI, required: Boolean): Unit = {
    if (cfg.getScheme != "file") {
      throw XProcException.xiConfigurationException(s"Unacceptable configuration file URI: ${cfg}; only file: scheme URIs are allowed")
    }

    val cfgfile = new File(cfg.getPath)
    if (cfgfile.exists) {
      load(cfgfile)
    } else if (required) {
      throw XProcException.xiConfigurationException(s"Configuration file not found: ${cfgfile.getAbsolutePath}")
    }
  }

  def load(cfg: File): Unit = {
    logger.debug(s"Loading XML Calabash configuration file: ${cfg.getAbsolutePath}")

    val processor = new Processor(false) // explicitly our own because we don't know about schema awareness yet
    val builder = processor.newDocumentBuilder()
    builder.setDTDValidation(false)
    builder.setLineNumbering(true)
    val root = S9Api.documentElement(builder.build(cfg))
    if (root.isDefined) {
      if (root.get.getNodeName == XProcConstants.cc_xmlcalabash) {
        parse(root.get)
      } else {
        logger.error(s"Not an XML Calabash configuration file: ${cfg.getAbsolutePath}")
      }
    } else {
      logger.error(s"Failed to load: ${cfg.getAbsolutePath}")
    }
  }

  private def parse(node: XdmNode): Unit = {
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next()
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT => configure(child)
        case XdmNodeKind.TEXT =>
          if (child.getStringValue.trim != "") {
            logger.error(s"Ignoring text in configuration: ${child.getStringValue}")
          }
        case _ => ()
      }
    }
  }

  private def configure(node: XdmNode): Unit = {
    node.getNodeName match {
      case XProcConstants.cc_system_property =>
        val key = node.getAttributeValue(_key)
        val value = node.getAttributeValue(_value)

        if (key == null || value == null) {
          logger.error(s"Invalid system property configuration: $node")
        } else {
          _parameters += new PipelineSystemProperty(key, value)
        }
      case XProcConstants.cc_saxon_configuration_property =>
        setSaxonConfigurationProperty(node)
      case XProcConstants.cc_serialization =>
        setSerialization(node)
      case _ =>
        val attr = attributes(node)

        if (attr.contains(_key) && attr.contains(_value)) {
          if (attr.size > 2 || node.getStringValue.trim != "") {
            logger.warn(s"Invalid map setting for ${node.getNodeName}")
          }
          setMap(node)
        } else {
          if (attr.nonEmpty) {
            logger.warn(s"Invalid attributes for ${node.getNodeName}")
          }
          _parameters += new PipelineEnvironmentOptionString(node.getNodeName.getEQName, node.getStringValue.trim)
        }
    }
  }

  private def attributes(node: XdmNode): Map[QName,String] = {
    val map = mutable.HashMap.empty[QName,String]
    val iter = node.axisIterator(Axis.ATTRIBUTE)
    while (iter.hasNext) {
      val attr = iter.next()
      map.put(attr.getNodeName, attr.getStringValue)
    }
    map.toMap
  }

  def setMap(node: XdmNode): Unit = {
    val key = node.getAttributeValue(_key)
    val value = node.getAttributeValue(_value)

    if (key == null || value == null) {
      logger.error(s"Invalid map property configuration: $node")
    } else {
      _parameters += new PipelineEnvironmentOptionMap(node.getNodeName.getEQName, key, value)
    }
  }

  private def setSaxonConfigurationProperty(node: XdmNode): Unit = {
    val key = node.getAttributeValue(_key)
    val value = node.getAttributeValue(_value)
    val vtype = guessType(node.getAttributeValue(_type), value)

    if (key == null || value == null) {
      logger.error(s"Invalid Saxon configuration property: missing key or value: $node")
      return
    }

    _parameters += new PipelineSaxonConfigurationProperty(node.getNodeName.getEQName, key, value, vtype)
  }

  private def guessType(vtype: String, value: String): String = {
    if (vtype != null) {
      vtype
    } else {
      if (value == "true" || value == "false") {
        "boolean"
      } else {
        try {
          val i = value.toInt
          return "integer"
        } catch {
          case _ : Throwable => ()
        }
        "string"
      }
    }
  }

  private def setSerialization(node: XdmNode): Unit = {
    val ctype = node.getAttributeValue(XProcConstants._content_type)
    if (ctype == null) {
      logger.error(s"Invalid ${node.getNodeName}, missing content-type")
    } else {
      val map = mutable.HashMap.empty[String,String]
      val iter = node.axisIterator(Axis.ATTRIBUTE)
      while (iter.hasNext) {
        val attr = iter.next()
        if (attr.getNodeName != XProcConstants._content_type) {
          map.put(attr.getNodeName.getEQName, attr.getStringValue)
        }
      }
      _parameters += new PipelineEnvironmentOptionSerialization(ctype, map.toMap)
    }
  }

  def loadProperties(): Unit = {
    val uriEnum = this.getClass.getClassLoader.getResources("com.xmlcalabash.properties")
    while (uriEnum.hasMoreElements) {
      val url = uriEnum.nextElement()
      logger.debug(s"Loading properties: $url")

      val conn = url.openConnection()
      val stream = conn.getInputStream
      val props = new Properties()
      props.load(stream)

      val nsmap = mutable.HashMap.empty[String,String]
      val NSPattern = "namespace\\s+(.+)$".r
      val FPattern = "function\\s+(.+):(.+)$".r
      val SPattern = "step\\s+(.+):(.+)$".r

      // Properties are unordered so find the namespace bindings
      var propIter = props.stringPropertyNames().iterator()
      while (propIter.hasNext) {
        val name = propIter.next()
        val value = props.get(name).asInstanceOf[String]
        value match {
          case NSPattern(uri) =>
            if (nsmap.contains(name)) {
              throw new RuntimeException("Cannot redefine namespace bindings in property file")
            }
            nsmap.put(name, uri)
          case _ => ()
        }
      }

      // Now parse the step and function declarations
      propIter = props.stringPropertyNames().iterator()
      while (propIter.hasNext) {
        val name = propIter.next()
        val value = props.get(name).asInstanceOf[String]
        value match {
          case NSPattern(uri) => ()
          case FPattern(pfx,local) =>
            if (nsmap.contains(pfx)) {
              val qname = new QName(pfx, nsmap(pfx), local)
              _parameters += new PipelineFunctionImplementation(qname.getEQName, value, name)
            } else {
              logger.debug(s"No namespace binding for $pfx, ignoring: $name=$value")
            }
          case SPattern(pfx,local) =>
            if (nsmap.contains(pfx)) {
              val qname = new QName(pfx, nsmap(pfx), local)
              _parameters += new PipelineStepImplementation(qname.getEQName, value, name)
            } else {
              logger.debug(s"No namespace binding for $pfx, ignoring: $name=$value")
            }
          case _ =>
            logger.debug(s"Unparseable property, ignoring: $name=$value")
        }
      }
    }
  }
}
