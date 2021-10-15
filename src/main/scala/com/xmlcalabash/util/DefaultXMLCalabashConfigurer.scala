package com.xmlcalabash.util

import com.xmlcalabash.config.{ConfigurationSettings, ConfigurationString, ConfigurationStringMap, XMLCalabashConfig, XMLCalabashConfigurer}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.{Axis, Processor, QName, XdmNode, XdmNodeKind}
import org.slf4j.{Logger, LoggerFactory}

import java.io.File
import java.util.Properties
import scala.collection.mutable

class DefaultXMLCalabashConfigurer extends XMLCalabashConfigurer {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val _key = new QName("key")
  private val _value = new QName("value")
  private val _type = new QName("type")

  private var config: ConfigurationSettings = _

  override def configure(settings: ConfigurationSettings): Unit = {
    config = settings

    val systemConfig = Option(System.getProperty("com.xmlcalabash.configFile"))
    if (systemConfig.isDefined) {
      val fn = URIUtils.resolve(URIUtils.cwdAsURI, systemConfig.get)
      if (fn.getScheme != "file") {
        throw XProcException.xiConfigurationException(s"Unacceptable configuration file URI: ${fn}; only file: scheme URIs are allowed")
      }
      val cfg = new File(fn.getPath)
      if (!cfg.exists()) {
        throw XProcException.xiConfigurationException(s"Configuration file not found: ${cfg.getAbsolutePath}")
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

    loadProperties()
  }

  private def load(cfg: File): Unit = {
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
          config.set(node.getNodeName, new ConfigurationString(node.getNodeName, node.getStringValue.trim))
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
      logger.error(s"Invalid system property configuration: $node")
    } else {
      val map = mutable.HashMap.empty[String,String]
      config.get(node.getNodeName) map { map ++= _.asMap }
      map.put(key, value)
      config.set(node.getNodeName, new ConfigurationStringMap(node.getNodeName, map.toMap))
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

    vtype match {
      case "boolean" =>
        value match {
          case "true" => config.setSaxonConfigProperty(key, true)
          case "false" => config.setSaxonConfigProperty(key, false)
          case _ =>
            logger.error(s"Invalid boolean value $value for Saxon configuration property $key")
        }
      case "integer" => config.setSaxonConfigProperty(key, value.toInt)
      case "string" => config.setSaxonConfigProperty(key, value)
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
      val iter = node.axisIterator(Axis.ATTRIBUTE)
      while (iter.hasNext) {
        val attr = iter.next()
        if (attr.getNodeName != XProcConstants._content_type) {
          config.setDefaultSerialization(ctype, attr.getNodeName, attr.getStringValue)
        }
      }
    }
  }

  private def loadProperties(): Unit = {
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
              config.setFunction(qname, name)
            } else {
              logger.debug(s"No namespace binding for $pfx, ignoring: $name=$value")
            }
          case SPattern(pfx,local) =>
            if (nsmap.contains(pfx)) {
              val qname = new QName(pfx, nsmap(pfx), local)
              config.setStep(qname, name)
            } else {
              logger.debug(s"No namespace binding for $pfx, ignoring: $name=$value")
            }
          case _ =>
            logger.debug(s"Unparseable property, ignoring: $name=$value")
        }
      }
    }
  }

  override def update(config: XMLCalabashConfig, settings: ConfigurationSettings): Unit = {
    // nop
  }
}
