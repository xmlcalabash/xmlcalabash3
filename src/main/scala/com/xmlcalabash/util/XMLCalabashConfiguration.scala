package com.xmlcalabash.util

import com.xmlcalabash.exceptions.XProcException

import java.io.File
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.{Axis, Processor, QName, XdmNode, XdmNodeKind}
import org.slf4j.{Logger, LoggerFactory}

import java.net.URI
import scala.collection.mutable

class XMLCalabashConfiguration {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val ns_cc = "http://xmlcalabash.com/ns/configuration"
  private val cc_xmlcalabash = new QName("cc", ns_cc, "xmlcalabash")
  private val cc_show_messages = new QName("cc", ns_cc, "show-messages")
  private val cc_trim_inline_whitespace = new QName("cc", ns_cc, "trim-inline-whitespace")
  private val cc_schema_aware = new QName("cc", ns_cc, "schema-aware")
  private val cc_saxon_configuration = new QName("cc", ns_cc, "saxon-configuration")
  private val cc_saxon_configuration_property = new QName("cc", ns_cc, "saxon-configuration-property")
  private val cc_serialization = new QName("cc", ns_cc, "serialization")
  private val cc_entity_resolver = new QName("cc", ns_cc, "entity-resolver")
  private val cc_uri_resolver = new QName("cc", ns_cc, "uri-resolver")
  private val cc_module_uri_resolver = new QName("cc", ns_cc, "module-uri-resolver")
  private val cc_unparsed_text_uri_resolver = new QName("cc", ns_cc, "unparsed-text-uri-resolver")
  private val cc_system_property = new QName("cc", ns_cc, "system-property")
  private val cc_thread_pool = new QName("cc", ns_cc, "thread-pool")
  private val cc_graphviz = new QName("cc", ns_cc, "graphviz")
  private val cc_http_proxy = new QName("cc", ns_cc, "http-proxy")
  private val cc_debug_output_directory = new QName("cc", ns_cc, "debug-output-directory")
  private val cc_debug_tree = new QName("cc", ns_cc, "tree")
  private val cc_debug_pipeline = new QName("cc", ns_cc, "pipeline")
  private val cc_debug_graph = new QName("cc", ns_cc, "graph")
  private val cc_debug_open_graph = new QName("cc", ns_cc, "open-graph")
  private val cc_debug_stacktrace = new QName("cc", ns_cc, "stacktrace")
  private val cc_show_errors = new QName("cc", ns_cc, "show-errors")
  private val _key = new QName("key")
  private val _value = new QName("value")
  private val _type = new QName("type")
  private val _dot = new QName("dot")
  private val _host = new QName("host")
  private val _port = new QName("port")
  private val _scheme = new QName("scheme")
  private val _thread_count = new QName("thread-count")

  private var _show_messages = Option.empty[Boolean]
  private var _schema_aware = Option.empty[Boolean]
  private var _trim_inline_whitespace = Option.empty[Boolean]
  private var _saxon_configuration_file = Option.empty[String]
  private var _saxon_configuration_property = mutable.HashMap.empty[String,Any]
  private var _serialization = mutable.HashMap.empty[String,mutable.HashMap[QName,String]]
  private var _thread_pool_size = 1
  private var _entity_resolver = Option.empty[String]
  private var _uri_resolver = Option.empty[String]
  private var _module_uri_resolver = Option.empty[String]
  private var _unparsed_text_uri_resolver = Option.empty[String]
  private var _graphviz_dot = Option.empty[String]
  private var _proxies = mutable.HashMap.empty[String, URI]
  private var _showErrors = false
  private var _debug_output_directory = Option.empty[String]
  private var _debug_tree = Option.empty[String]
  private var _debug_pipeline = Option.empty[String]
  private var _debug_graph = Option.empty[String]
  private var _debug_open_graph = Option.empty[String]
  private var _debug_stacktrace = Option.empty[Boolean]

  def show_messages: Boolean = _show_messages.getOrElse(false)
  def schema_aware: Boolean = _schema_aware.getOrElse(false)
  def trim_inline_whitespace: Boolean = _trim_inline_whitespace.getOrElse(false)
  def saxon_configuration_file: Option[String] = _saxon_configuration_file
  def saxon_configuration_properties: Map[String,Any] = _saxon_configuration_property.toMap

  def thread_pool_size: Int = _thread_pool_size
  def entity_resolver: Option[String] = _entity_resolver
  def uri_resolver: Option[String] = _uri_resolver
  def module_uri_resolver: Option[String] = _module_uri_resolver
  def unparsed_text_uri_resolver: Option[String] = _unparsed_text_uri_resolver
  def proxies: Map[String, URI] = _proxies.toMap
  def graphviz_dot: Option[String] = _graphviz_dot
  def showErrors: Boolean = _showErrors
  def debug_output_directory: Option[String] = _debug_output_directory
  def debug_tree: Option[String] = _debug_tree
  def debug_pipeline: Option[String] = _debug_pipeline
  def debug_graph: Option[String] = _debug_graph
  def debug_open_graph: Option[String] = _debug_open_graph
  def debug_stacktrace: Option[Boolean] = _debug_stacktrace

  def serialization: Map[String,Map[QName,String]] = {
    val map = mutable.HashMap.empty[String, Map[QName,String]]
    for (key <- _serialization.keySet) {
      map.put(key, _serialization(key).toMap)
    }
    map.toMap
  }

  def load(): Unit = {
    val systemConfig = Option(System.getProperty("com.xmlcalabash.configFile"))
    if (systemConfig.isDefined) {
      val fn = URIUtils.resolve(URIUtils.cwdAsURI, systemConfig.get)
      if (fn.getScheme != "file") {
        throw XProcException.xiBadConfigSchema(fn.getScheme)
      }
      val cfg = new File(fn.getPath)
      if (!cfg.exists()) {
        throw XProcException.xiConfigNotFound(fn)
      }
      load(cfg)
    } else {
      var fn = URIUtils.resolve(URIUtils.homeAsURI, ".xmlcalabash")
      var cfg = new File(fn.getPath)

      if (cfg.exists()) {
        load(cfg)
      }

      fn = URIUtils.resolve(URIUtils.cwdAsURI, ".xmlcalabash")
      cfg = new File(fn.getPath)
      if (cfg.exists()) {
        load(cfg)
      }
    }
  }

  private def load(cfg: File): Unit = {
    logger.debug(s"Loading XML Calabash configuration file: ${cfg.getAbsolutePath}")

    val processor = new Processor(false) // explicitly our own because we don't know about schema awareness yet
    val builder = processor.newDocumentBuilder()
    builder.setDTDValidation(false)
    builder.setLineNumbering(true)
    val root = S9Api.documentElement(builder.build(cfg))
    if (root.isDefined) {
      if (root.get.getNodeName == cc_xmlcalabash) {
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
        case XdmNodeKind.ELEMENT => config(child)
        case XdmNodeKind.TEXT =>
          if (child.getStringValue.trim != "") {
            logger.error(s"Ignoring text in configuration: ${child.getStringValue}")
          }
        case _ => ()
      }
    }
  }

  private def config(node: XdmNode): Unit = {
    val option = node.getNodeName.getLocalName

    node.getNodeName match {
      case `cc_show_messages` =>
        _show_messages = setBoolean(node, option)
      case `cc_schema_aware`  =>
        _schema_aware = setBoolean(node, option)
      case `cc_system_property` =>
        setSystemProperty(node)
      case `cc_thread_pool` =>
        setThreadPool(node)
      case `cc_trim_inline_whitespace`  =>
        _trim_inline_whitespace = setBoolean(node, option)
      case `cc_saxon_configuration` =>
        _saxon_configuration_file = Some(setString(node, option))
      case `cc_saxon_configuration_property` =>
        addConfigurationProperty(node)
      case `cc_serialization` =>
        addSerialization(node)
      case `cc_entity_resolver` =>
        _entity_resolver = Some(setString(node, option))
      case `cc_uri_resolver` =>
        _uri_resolver = Some(setString(node, option))
      case `cc_module_uri_resolver` =>
        _module_uri_resolver = Some(setString(node, option))
      case `cc_unparsed_text_uri_resolver` =>
        _unparsed_text_uri_resolver = Some(setString(node, option))
      case `cc_http_proxy` =>
        parseHttpProxy(node)
      case `cc_graphviz` =>
        parseGraphviz(node)
      case `cc_debug_output_directory` =>
        parseDebugOutputDirectory(node)
      case `cc_debug_tree` =>
        parseDebugTree(node)
      case `cc_debug_pipeline` =>
        parseDebugPipeline(node)
      case `cc_debug_graph` =>
        parseDebugGraph(node)
      case `cc_debug_open_graph` =>
        parseDebugOpenGraph(node)
      case `cc_debug_stacktrace` =>
        parseDebugStacktrace(node)
      case `cc_show_errors` =>
        parseShowErrors(node)
      case _ =>
        logger.error(s"Unexpected configuration option: ${node.getNodeName}")
    }
  }

  private def addConfigurationProperty(node: XdmNode): Unit = {
    val key = node.getAttributeValue(_key)
    val value = node.getAttributeValue(_value)
    val vtype = guessType(node.getAttributeValue(_type), value)

    if (key == null || value == null) {
      logger.error(s"Invalid Saxon configuration property: missing key or value: $node")
    }

    vtype match {
      case "boolean" =>
        value match {
          case "true" => _saxon_configuration_property.put(key, true)
          case "false" => _saxon_configuration_property.put(key, false)
          case _ =>
            logger.error(s"Invalid boolean value $value for Saxon configuration property $key")
        }
      case "integer" => _saxon_configuration_property.put(key, value.toInt)
      case "string" => _saxon_configuration_property.put(key, value)
      case _ =>
        logger.error(s"Unexpected key type: $vtype for Saxon configuration property $key")
    }
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
          "integer"
        } catch {
          case _ : Throwable => ()
        }
        "string"
      }
    }
  }

  private def addSerialization(node: XdmNode): Unit = {
    val ctype = node.getAttributeValue(XProcConstants._content_type)
    if (ctype == null) {
      logger.error("Invalid serialization, missing content-type")
    } else {
      val map = _serialization.getOrElse(ctype, new mutable.HashMap[QName, String])
      val iter = node.axisIterator(Axis.ATTRIBUTE)
      while (iter.hasNext) {
        val attr = iter.next()
        if (attr.getNodeName != XProcConstants._content_type) {
          map.put(attr.getNodeName, attr.getStringValue)
        }
      }
      _serialization.put(ctype, map)
    }
  }

  private def parseHttpProxy(node: XdmNode): Unit = {
    val host = Option(node.getAttributeValue(_host))
    val port = Option(node.getAttributeValue(_port))
    val scheme = Option(node.getAttributeValue(_scheme))
    if (host.isEmpty || port.isEmpty || scheme.isEmpty) {
      logger.error("Invalid proxy configuration, host, port and scheme are required")
    } else {
      if (_proxies.contains(scheme.get)) {
        logger.warn(s"Replacing proxy for ${scheme} URIs")
      }
      _proxies.put(scheme.get, new URI("http://" + host.get + ":" + port.get + "/"))
    }
  }

  private def parseGraphviz(node: XdmNode): Unit = {
    val dot = node.getAttributeValue(_dot)
    if (dot != null) {
      for (path <- dot.trim.split("\\s")) {
        val ex = new File(path)
        if (ex.exists() && ex.canExecute) {
          _graphviz_dot = Some(path)
          return
        }
      }
      logger.warn(s"No executables found in ${node.getAttributeValue(_dot)}")
    }
  }

  private def parseShowErrors(node: XdmNode): Unit = {
    val bv = setBoolean(node, "show-errors")
    if (bv.isDefined) {
      _showErrors = bv.get
    }
  }

  private def parseDebugOutputDirectory(node: XdmNode): Unit = {
    val name = node.getStringValue.trim
    val file = new File(name)
    if (!file.exists || !file.isDirectory) {
      logger.error(s"The cc:debug-output-directory value must be a directory")
    } else {
      _debug_output_directory = Some(name)
    }
  }

  private def parseDebugTree(node: XdmNode): Unit = {
    val name = node.getStringValue.trim
    if (name.contains("/")) {
      logger.error(s"The cc:debug-tree value must be a filename (no /'s allowed)")
    } else {
      _debug_tree = Some(name)
    }
  }

  private def parseDebugPipeline(node: XdmNode): Unit = {
    val name = node.getStringValue.trim
    if (name.contains("/")) {
      logger.error(s"The cc:debug-pipeline value must be a filename (no /'s allowed)")
    } else {
      _debug_graph = Some(name)
    }
  }

  private def parseDebugGraph(node: XdmNode): Unit = {
    val name = node.getStringValue.trim
    if (name.contains("/")) {
      logger.error(s"The cc:debug-graph value must be a filename (no /'s allowed)")
    } else {
      _debug_graph = Some(name)
    }
  }

  private def parseDebugOpenGraph(node: XdmNode): Unit = {
    val name = node.getStringValue.trim
    if (name.contains("/")) {
      logger.error(s"The cc:debug-open-graph value must be a filename (no /'s allowed)")
    } else {
      _debug_open_graph = Some(name)
    }
  }

  private def parseDebugStacktrace(node: XdmNode): Unit = {
    val name = node.getStringValue.trim
    _debug_stacktrace = Some(name == "true")
    if (name != "true" && name != "false") {
      logger.error(s"The cc:stacktrace value '${name}' treated as false.")
    }
  }

  private def setBoolean(node: XdmNode, option: String): Option[Boolean] = {
    val value = node.getStringValue.trim
    value match {
      case "true" => Some(true)
      case "false" => Some(false)
      case _ =>
        logger.error(s"Configuration value for $option is not boolean: $value")
        None
    }
  }

  private def setString(node: XdmNode, option: String): String = {
    node.getStringValue.trim
  }

  def setSystemProperty(node: XdmNode): Unit = {
    val key = node.getAttributeValue(_key)
    val value = node.getAttributeValue(_value)

    if (key == null || value == null) {
      logger.error(s"Invalid system property configuration: $node")
    } else {
      setSystemProperty(key, value)
    }
  }

  def setSystemProperty(key: String, value: String): Unit = {
    val props = System.getProperties
    // Only use the configuration file values if overrides haven't been specified on the command line
    if (props.getProperty(key) == null) {
      props.setProperty(key, value)
    }
  }

  def setThreadPool(node: XdmNode): Unit = {
    try {
      val count = node.getAttributeValue(_thread_count)
      if (count != null) {
        setSystemProperty("com.xmlcalabash.threadCount", count.toInt.toString)
      }
    } catch {
      case ex: Exception =>
        logger.error(s"Invalid thread configuration: ${node.getStringValue}")
    }
  }
}
