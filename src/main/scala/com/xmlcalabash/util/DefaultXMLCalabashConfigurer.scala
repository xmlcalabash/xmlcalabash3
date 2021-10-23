package com.xmlcalabash.util

import com.jafpl.util.DefaultTraceEventManager
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.config.XMLCalabashConfigurer
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.lib.{ModuleURIResolver, UnparsedTextURIResolver}
import net.sf.saxon.s9api.QName
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.EntityResolver

import javax.xml.transform.URIResolver
import scala.collection.mutable.ListBuffer

class DefaultXMLCalabashConfigurer() extends XMLCalabashConfigurer {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def configure(input: List[PipelineParameter]): List[PipelineParameter] = {
    val args = new ArgBundle()

    args.loadProperties()

    val searchProp = ListBuffer.empty[PipelineParameter] ++ input
    val propConfig = Option(System.getProperty("com.xmlcalabash.configFile"))

    if (propConfig.isDefined && (input collect { case opt: PipelineConfigurationFile => opt }).isEmpty) {
      searchProp += new PipelineConfigurationFile(new PipelineFilenameDocument(propConfig.get))
    }

    var loaded = false
    for (config <- searchProp.toList collect { case opt: PipelineConfigurationFile => opt }) {
      loaded = true
      config.doc match {
        case uri: PipelineURIDocument =>
          args.load(uri.value, true)
        case fn: PipelineFilenameDocument =>
          args.load(URIUtils.cwdAsURI.resolve(fn.value), true)
        case file: PipelineFileDocument =>
          args.load(file.value.toURI, true)
        case _ =>
          throw XProcException.xiThisCantHappen("Unexpected configuration file type", None)
      }
    }

    if (!loaded) {
      args.load(URIUtils.homeAsURI.resolve(".xmlcalabash"), false)
      val local = args.environmentOptions(XProcConstants.cc_load_local_config).headOption
      if (local.isEmpty || local.get.getBoolean.getOrElse(true)) {
        args.load(URIUtils.cwdAsURI.resolve(".xmlcalabash"), false)
      }
    }

    searchProp.toList ++ args.parameters
  }

  private def environmentOptions(parameters: List[PipelineParameter], name: QName): List[PipelineEnvironmentOption] = {
    parameters collect { case p: PipelineEnvironmentOption => p } filter { _.eqname == name.getEQName }
  }

  override def update(config: XMLCalabash): Unit = {
    config.traceEventManager = new DefaultTraceEventManager()
    val traces = Option(System.getProperty("com.xmlcalabash.trace")).getOrElse("")
    for (trace <- traces.split("\\s*,\\s*")) {
      if (trace.startsWith("-")) {
        config.traceEventManager.disableTrace(trace.substring(1))
      } else {
        if (trace.startsWith("+")) {
          config.traceEventManager.enableTrace(trace.substring(1))
        } else {
          config.traceEventManager.enableTrace(trace)
        }
      }
    }

    val resolver = new XProcURIResolver(config)
    config.uriResolver = loadResolver(environmentOptions(config.parameters, XProcConstants.cc_uri_resolver).headOption, resolver).asInstanceOf[URIResolver]
    config.entityResolver = loadResolver(environmentOptions(config.parameters, XProcConstants.cc_entity_resolver).headOption, resolver).asInstanceOf[EntityResolver]
    config.unparsedTextURIResolver = loadResolver(environmentOptions(config.parameters, XProcConstants.cc_unparsed_text_uri_resolver).headOption, resolver).asInstanceOf[UnparsedTextURIResolver]
    config.moduleURIResolver = loadResolver(environmentOptions(config.parameters, XProcConstants.cc_module_uri_resolver).headOption, resolver).asInstanceOf[ModuleURIResolver]

    // FIXME: support an alternate error listener (maybe)
    config.errorListener = new DefaultErrorListener()

    config.errorExplanation = new DefaultErrorExplanation()
    config.documentManager = new DefaultDocumentManager(config)
  }

  private def loadResolver(opt: Option[PipelineEnvironmentOption], default: XProcURIResolver): Any = {
    try {
      if (opt.isDefined && opt.get.getString.isDefined) {
        Class.forName(opt.get.getString.get).getDeclaredConstructor().newInstance()
      } else {
        default
      }
    } catch {
      case ex: Exception =>
        logger.error(s"Failed to instantiate resolver ${opt.get.getString.get}: ${ex.getMessage}")
        default
    }
  }
}
