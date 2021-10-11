package com.xmlcalabash.util

import com.jafpl.util.DefaultTraceEventManager
import com.xmlcalabash.config.{XMLCalabashConfig, XMLCalabashConfigurer}
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.lib.{ModuleURIResolver, UnparsedTextURIResolver}
import net.sf.saxon.s9api.{Processor, QName}
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.{EntityResolver, InputSource}

import java.util.Properties
import javax.xml.transform.URIResolver
import javax.xml.transform.sax.SAXSource
import scala.collection.mutable

class DefaultXMLCalabashConfigurer extends XMLCalabashConfigurer {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val config = new XMLCalabashConfiguration()

  override def configure(configuration: XMLCalabashConfig): Unit = {
    config.load()

    if (configuration.processorRequired) {
      try {
        configuration.processor = if (config.saxon_configuration_file.isDefined) {
          new Processor(new SAXSource(new InputSource(config.saxon_configuration_file.get)))
        } else {
          new Processor(config.schema_aware)
        }
      } catch {
        case _: RuntimeException =>
          throw XProcException.xiNoSaxon()
      }

      for (key <- config.saxon_configuration_properties.keySet) {
        configuration.processor.getUnderlyingConfiguration.setConfigurationProperty(key, config.saxon_configuration_properties(key))
      }
    }

    configuration.traceEventManager = new DefaultTraceEventManager()
    val traces = Option(System.getProperty("com.xmlcalabash.trace")).getOrElse("")
    for (trace <- traces.split("\\s*,\\s*")) {
      if (trace.startsWith("-")) {
        configuration.traceEventManager.disableTrace(trace.substring(1))
      } else {
        if (trace.startsWith("+")) {
          configuration.traceEventManager.enableTrace(trace.substring(1))
        } else {
          configuration.traceEventManager.enableTrace(trace)
        }
      }
    }

    val resolver = new XProcURIResolver(configuration)
    configuration.uriResolver = loadResolver(config.uri_resolver, resolver).asInstanceOf[URIResolver]
    configuration.entityResolver = loadResolver(config.entity_resolver, resolver).asInstanceOf[EntityResolver]
    configuration.unparsedTextURIResolver = loadResolver(config.unparsed_text_uri_resolver, resolver).asInstanceOf[UnparsedTextURIResolver]
    configuration.moduleURIResolver = loadResolver(config.module_uri_resolver, resolver).asInstanceOf[ModuleURIResolver]
    // FIXME: support an alternate error listener (maybe)
    configuration.errorListener = new DefaultErrorListener()

    configuration.errorExplanation = new DefaultErrorExplanation()
    configuration.documentManager = new DefaultDocumentManager(configuration)
    configuration.defaultSerializationOptions = config.serialization
    configuration.trimInlineWhitespace = config.trim_inline_whitespace
    configuration.proxies = config.proxies
    if (config.graphviz_dot.isDefined) {
      configuration.debugOptions.graphviz_dot = config.graphviz_dot.get
    }
    configuration.showErrors = config.showErrors

    try {
      val tcount = Option(System.getProperty("com.xmlcalabash.threadCount")).getOrElse("1").toInt
      configuration.threadPoolSize = tcount
    } catch {
      case _: Exception =>
        // FIXME: raise exception?
    }

    // Have to check because assigning none enables the default behavior
    if (config.debug_output_directory.isDefined) {
      configuration.debugOptions.outputDirectory = config.debug_output_directory.get
    }

    if (config.debug_tree.isDefined) {
      configuration.debugOptions.tree = config.debug_tree
    }

    if (config.debug_pipeline.isDefined) {
      configuration.debugOptions.graph = config.debug_pipeline
    }

    if (config.debug_graph.isDefined) {
      configuration.debugOptions.graph = config.debug_graph
    }

    if (config.debug_open_graph.isDefined) {
      configuration.debugOptions.openGraph = config.debug_open_graph
    }

    loadProperties(configuration)
  }

  private def loadResolver(klass: Option[String], default: XProcURIResolver): Object = {
    if (klass.isDefined) {
      // FIXME: implement instantiation of resolvers
      logger.error(s"Alternate resolvers not yet implemented: $klass.get")
      default
    } else {
      default
    }
  }

  private def loadProperties(configuration: XMLCalabashConfig): Unit = {
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
              configuration.implementFunction(qname, name)
            } else {
              logger.debug(s"No namespace binding for $pfx, ignoring: $name=$value")
            }
          case SPattern(pfx,local) =>
            if (nsmap.contains(pfx)) {
              val qname = new QName(pfx, nsmap(pfx), local)
              configuration.implementAtomicStep(qname, name)
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
