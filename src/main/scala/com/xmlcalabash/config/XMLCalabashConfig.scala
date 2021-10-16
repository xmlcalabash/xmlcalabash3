package com.xmlcalabash.config

import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.util.{DefaultTraceEventManager, ErrorListener, TraceEventManager}
import com.xmlcalabash.exceptions.{ConfigurationException, ExceptionCode, XProcException}
import com.xmlcalabash.functions.FunctionImpl
import com.xmlcalabash.model.util.{ExpressionParser, XProcConstants}
import com.xmlcalabash.model.xml.{DeclContainer, Library}
import com.xmlcalabash.parsers.XPathParser
import com.xmlcalabash.runtime.SaxonExpressionEvaluator
import com.xmlcalabash.sbt.BuildInfo
import com.xmlcalabash.util.{DefaultDocumentManager, DefaultErrorExplanation, DefaultErrorListener, URIUtils, XProcURIResolver}
import net.sf.saxon.lib.{ModuleURIResolver, UnparsedTextURIResolver}
import net.sf.saxon.s9api.{Processor, QName, XdmNode}
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.{EntityResolver, InputSource}

import java.net.URI
import javax.xml.transform.URIResolver
import javax.xml.transform.sax.SAXSource
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object XMLCalabashConfig {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val _configProperty = "com.xmlcalabash.config.XProcConfigurer"
  val _configClass = "com.xmlcalabash.util.DefaultXProcConfigurer"
  var loggedPI = false

  // Available even when XMLCalabashConfig is being initialized
  val defaultErrorExplanation: ErrorExplanation = new DefaultErrorExplanation()

  def newInstance(): XMLCalabashConfig = {
    newInstance(None)
  }

  def newInstance(processor: Processor): XMLCalabashConfig = {
    newInstance(Some(processor))
  }

  private def newInstance(optProcessor: Option[Processor]): XMLCalabashConfig = {
    val configurer = Class.forName(configClass).getDeclaredConstructor().newInstance().asInstanceOf[XProcConfigurer]
    val settings = new ConfigurationSettings()
    configurer.xmlCalabashConfigurer.configure(settings)

    val processor = if (optProcessor.isDefined) {
      optProcessor.get
    } else {
      val cfgfile = settings.get(XProcConstants.cc_saxon_configuration) map {
        _.asString
      }
      try {
        if (cfgfile.isDefined) {
          new Processor(new SAXSource(new InputSource(cfgfile.get)))
        } else {
          val aware = settings.get(XProcConstants.cc_schema_aware)
          val schemaAware = try {
            if (aware.isDefined) {
              aware.get.asBoolean
            } else {
              false
            }
          } catch {
            case _: Exception =>
              throw XProcException.xiConfigurationException(s"Invalid setting for schema-aware: ${aware.get}")
          }
          new Processor(schemaAware)
        }
      } catch {
        case ex: XProcException =>
          throw ex
        case ex: Exception =>
          throw XProcException.xiConfigurationException(s"Failed to instantiate a Saxon processor: ${ex.getMessage}")
      }
    }

    val sysprop = settings.get(XProcConstants.cc_system_property) map { _.asMap }
    if (sysprop.isDefined) {
      for ((name,value) <- sysprop.get) {
        System.setProperty(name, value);
      }
    }

    for (prop <- settings.saxonConfigProperties) {
      processor.getUnderlyingConfiguration.setConfigurationProperty(prop, settings.getSaxonConfigProperty(prop).get)
    }

    val config = new XMLCalabashConfig(configurer, settings, processor)

    for (step <- settings.steps) {
      config.implementAtomicStep(step, settings.getStep(step).get)
    }

    for (fn <- settings.functions) {
      config.implementFunction(fn, settings.getFunction(fn).get)
    }

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
    config.uriResolver = loadResolver(settings.string(XProcConstants.cc_uri_resolver), resolver).asInstanceOf[URIResolver]
    config.entityResolver = loadResolver(settings.string(XProcConstants.cc_entity_resolver), resolver).asInstanceOf[EntityResolver]
    config.unparsedTextURIResolver = loadResolver(settings.string(XProcConstants.cc_unparsed_text_uri_resolver), resolver).asInstanceOf[UnparsedTextURIResolver]
    config.moduleURIResolver = loadResolver(settings.string(XProcConstants.cc_module_uri_resolver), resolver).asInstanceOf[ModuleURIResolver]

    // FIXME: support an alternate error listener (maybe)
    config.errorListener = new DefaultErrorListener()

    config.errorExplanation = new DefaultErrorExplanation()
    config.documentManager = new DefaultDocumentManager(config)

    config.defaultSerializationOptions = settings.defaultSerializations

    config.trimInlineWhitespace = settings.boolean(XProcConstants.cc_trim_inline_whitespace).getOrElse(false)

    val proxyMap = settings.get(XProcConstants.cc_http_proxy)
    if (proxyMap.isDefined) {
      proxyMap.get match {
        case map: ConfigurationStringMap =>
          val proxies = mutable.HashMap.empty[String, URI]
          for ((key, value) <- map.asMap) {
            proxies.put(key, new URI(value))
          }
          config.proxies = proxies.toMap
        case _ =>
          logger.error("The proxy configuration is incorrect (should be a map)")
      }
    }

    if (settings.get(XProcConstants.cc_graphviz).isDefined) {
      config.debugOptions.graphviz_dot = settings.get(XProcConstants.cc_graphviz).get.asString
    }

    if (settings.get(XProcConstants.cc_show_errors).isDefined) {
      config.showErrors = settings.get(XProcConstants.cc_show_errors).get.asBoolean
    }

    val tcountprop = "com.xmlcalabash.threadCount"
    if (Option(System.getProperty(tcountprop)).isDefined) {
      val tcountStr = System.getProperty(tcountprop)
      try {
        val tcount = tcountStr.toInt
        if (tcount <= 0) {
          throw XProcException.xiConfigurationException(s"Invalid thread count in system property ${tcountprop}: ${tcountStr} is not greater than 0")
        }
        config.threadPoolSize = tcount
      } catch {
        case ex: XProcException =>
          throw ex
        case _: Exception =>
          throw XProcException.xiConfigurationException(s"Invalid thread count in system property ${tcountprop}: ${tcountStr} is not an integer")
      }
    } else {
      if (settings.get(XProcConstants.cc_thread_count).isDefined) {
        config.threadPoolSize = settings.get(XProcConstants.cc_thread_count).get.asInt
      }
    }

    // Have to check because assigning none enables the default behavior
    if (settings.get(XProcConstants.cc_debug_output_directory).isDefined) {
      config.debugOptions.outputDirectory = settings.get(XProcConstants.cc_debug_output_directory).get.asString
    }

    if (settings.get(XProcConstants.cc_debug_tree).isDefined) {
      config.debugOptions.tree = settings.get(XProcConstants.cc_debug_tree).get.asBoolean
    }
    if (settings.get(XProcConstants.cc_debug_pipeline).isDefined) {
      config.debugOptions.pipeline = settings.get(XProcConstants.cc_debug_pipeline).get.asBoolean
    }
    if (settings.get(XProcConstants.cc_debug_graph).isDefined) {
      config.debugOptions.graph = settings.get(XProcConstants.cc_debug_graph).get.asBoolean
    }
    if (settings.get(XProcConstants.cc_debug_open_graph).isDefined) {
      config.debugOptions.openGraph = settings.get(XProcConstants.cc_debug_open_graph).get.asBoolean
    }

    settings.close()
    configurer.xmlCalabashConfigurer.update(config, settings)
    config.close()

    if (!loggedPI) {
      config.logProductDetails()
      loggedPI = true
    }

    config
  }

  private def configClass: String = Option(System.getProperty(_configProperty)).getOrElse(_configClass)

  private def loadResolver(klass: Option[String], default: XProcURIResolver): Any = {
    try {
      val resolver = klass map { Class.forName(_).getDeclaredConstructor().newInstance() }
      resolver.getOrElse(default)
    } catch {
      case ex: Exception =>
        logger.error(s"Failed to instantiate resolver ${klass.get}: ${ex.getMessage}")
        default
    }
  }
}

class XMLCalabashConfig(val xprocConfigurer: XProcConfigurer, val configSettings: ConfigurationSettings, val processor: Processor) extends RuntimeConfiguration {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val _expressionEvaluator = new SaxonExpressionEvaluator(this)
  private val _collections = mutable.HashMap.empty[String, List[XdmNode]]
  private var _debugOptions: XMLCalabashDebugOptions = new XMLCalabashDebugOptions(this)

  private var closed = false
  private var _threadPoolSize: Int = 2
  private var _errorListener: ErrorListener = _
  private val _stepImplClasses = mutable.HashMap.empty[QName,String]
  private val _funcImplClasses = mutable.HashMap.empty[QName,String]
  private var _traceEventManager: TraceEventManager = _
  private var _uriResolver: URIResolver = _
  private var _entityResolver: EntityResolver = _
  private var _moduleURIResolver: ModuleURIResolver = _
  private var _unparsedTextURIResolver: UnparsedTextURIResolver = _
  private var _proxies = Map.empty[String,URI]
  private var _errorExplanation: ErrorExplanation = _
  private var _documentManager: DocumentManager = _
  private var _htmlSerializer = false
  private var _trim_inline_whitespace = false
  private var _staticBaseURI = URIUtils.cwdAsURI
  private var _locale = defaultLocale
  private var _episode = computeEpisode
  private var _showErrors = false
  private var _builtinSteps = ListBuffer.empty[Library]
  private var _defaultSerializationOptions = Map.empty[String,Map[QName,String]]
  private val _importedURIs = mutable.HashMap.empty[URI, DeclContainer]
  // Do not allow the order to be random
  private val _imports = ListBuffer.empty[URI]

  def productName: String = BuildInfo.name
  def productVersion: String = BuildInfo.version
  def productHash: String = BuildInfo.gitHash.substring(0,6)
  def jafplVersion: String = BuildInfo.jafplVersion
  def saxonVersion: String = {
    val sver = processor.getSaxonProductVersion
    val sed = processor.getUnderlyingConfiguration.getEditionCode
    s"$sver/$sed"
  }
  def productConfig: String = {
    s"${BuildInfo.version} (with JAFPL $jafplVersion for Saxon $saxonVersion)"
  }
  def vendor: String = "Norman Walsh"
  def vendorURI: String = "https://xmlcalabash.com/"
  def xprocVersion: String = "3.0"
  def xpathVersion: String = "3.1"
  def psviSupported: Boolean = processor.isSchemaAware

  def safeMode: Boolean = false
  def proxies: Map[String,URI] = _proxies
  def proxies_=(map: Map[String,URI]): Unit = {
    _proxies = map
  }

  def logProductDetails(): Unit = {
    logger.info(s"$productName version $productVersion (with JAFPL $jafplVersion and Saxon $saxonVersion)")
    logger.debug(s"Copyright © 2018-2021 $vendor; $vendorURI")
    logger.debug(s"(release id: $productHash; episode: $episode)")
  }

  protected[xmlcalabash] def builtinSteps: List[Library] = _builtinSteps.toList
  protected[xmlcalabash] def builtinSteps_=(libs: List[Library]): Unit = {
    if (_builtinSteps.nonEmpty) {
      throw XProcException.xiThisCantHappen("Attempt to redefine builtin steps", None)
    }
    _builtinSteps ++= libs
  }

  protected[xmlcalabash] def importedURIs: List[URI] = _imports.toList
  protected[xmlcalabash] def importedURI(href: URI): Option[DeclContainer] = {
    _importedURIs.get(href)
  }
  protected[xmlcalabash] def addImportedURI(href: URI, container: DeclContainer): Unit = {
    if (_importedURIs.contains(href)) {
      throw new RuntimeException(s"Attempt to redefine imported uri: $href")
    }
    _importedURIs.put(href, container)
    _imports += href
  }
  protected[xmlcalabash] def clearImportedURIs(): Unit = {
    _importedURIs.clear()
    _imports.clear()
  }

  def debugOptions: XMLCalabashDebugOptions = _debugOptions
  def debugOptions_=(options: XMLCalabashDebugOptions): Unit = {
    _debugOptions = options
  }

  def errorListener: ErrorListener = {
    if (_errorListener == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "errorListener")
    }
    _errorListener
  }
  def errorListener_=(listener: ErrorListener): Unit = {
    checkClosed()
    _errorListener = listener
  }

  def traceEventManager: TraceEventManager = {
    if (_traceEventManager == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "traceEventManager")
    }
    _traceEventManager
  }
  def traceEventManager_=(manager: TraceEventManager): Unit = {
    checkClosed()
    _traceEventManager = manager
  }

  def uriResolver: URIResolver = {
    if (_uriResolver == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "uriResolver")
    }
    _uriResolver
  }
  def uriResolver_=(resolver: URIResolver): Unit = {
    checkClosed()
    _uriResolver = resolver
  }

  def entityResolver: EntityResolver = {
    if (_entityResolver == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "entityResolver")
    }
    _entityResolver
  }
  def entityResolver_=(resolver: EntityResolver): Unit = {
    checkClosed()
    _entityResolver = resolver
  }

  def moduleURIResolver: ModuleURIResolver = {
    if (_moduleURIResolver == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "moduleURIResolver")
    }
    _moduleURIResolver
  }
  def moduleURIResolver_=(resolver: ModuleURIResolver): Unit = {
    checkClosed()
    _moduleURIResolver = resolver
  }

  def unparsedTextURIResolver: UnparsedTextURIResolver = {
    if (_unparsedTextURIResolver == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "uparsedTextURIResolver")
    }
    _unparsedTextURIResolver
  }
  def unparsedTextURIResolver_=(resolver: UnparsedTextURIResolver): Unit = {
    checkClosed()
    _unparsedTextURIResolver = resolver
  }

  def errorExplanation: ErrorExplanation = {
    if (_errorExplanation == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "errorExplanation")
    }
    _errorExplanation
  }
  def errorExplanation_=(explain: ErrorExplanation): Unit = {
    checkClosed()
    _errorExplanation = explain
  }

  def documentManager: DocumentManager = {
    if (_documentManager == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "documentManager")
    }
    _documentManager
  }
  def documentManager_=(manager: DocumentManager): Unit = {
    checkClosed()
    _documentManager = manager
  }

  def htmlSerializer: Boolean = _htmlSerializer
  def htmlSerializer_=(html: Boolean): Unit = {
    checkClosed()
    _htmlSerializer = html
  }

  def trimInlineWhitespace: Boolean = _trim_inline_whitespace
  def trimInlineWhitespace_=(trim: Boolean): Unit = {
    checkClosed()
    _trim_inline_whitespace = trim
  }

  def threadPoolSize: Int = _threadPoolSize
  def threadPoolSize_=(size: Int): Unit = {
    _threadPoolSize = size
  }

  def locale: String = _locale
  def locale_=(language: String): Unit = {
    checkClosed()
    // FIXME: Check for valid format
    _locale = language
  }

  def defaultSerializationOptions: Map[String,Map[QName,String]] = _defaultSerializationOptions
  def defaultSerializationOptions_=(opts: Map[String,Map[QName,String]]): Unit = {
    checkClosed()
    _defaultSerializationOptions = opts
  }

  def staticBaseURI: URI = _staticBaseURI
  def staticBaseURI_=(uri: URI): Unit = {
    checkClosed()
    if (uri.isAbsolute) {
      _staticBaseURI = uri
    } else {
      throw new ConfigurationException(ExceptionCode.MUSTBEABS, "staticBaseURI")
    }
  }

  def episode: String = _episode
  def episode_=(episode: String): Unit = {
    checkClosed()
    _episode = episode
  }

  def showErrors: Boolean = _showErrors
  def showErrors_=(show: Boolean): Unit = {
    checkClosed()
    _showErrors = show
  }

  // ==============================================================================================

  def implementFunction(funcName: QName, className: String): Unit = {
    if (_funcImplClasses.contains(funcName)) {
      throw new RuntimeException("You cannot redefine a function implementation class")
    }
    _funcImplClasses.put(funcName, className)
  }

  def functionImplementation(funcName: QName): Option[String] = _funcImplClasses.get(funcName)

  def implementAtomicStep(stepType: QName, className: String): Unit = {
    if (_stepImplClasses.contains(stepType)) {
      throw new RuntimeException("You cannot redefine a step implementation class")
    }
    _stepImplClasses.put(stepType, className)
  }

  def atomicStepImplementation(stepType: QName): Option[String] = _stepImplClasses.get(stepType)

  // FIXME: Should this be a factory, or should XPathParser be reusable?
  def expressionParser: ExpressionParser = {
    new XPathParser(this)
  }

  override def expressionEvaluator: SaxonExpressionEvaluator = _expressionEvaluator

  override def traceEnabled(trace: String): Boolean = traceEventManager.traceEnabled(trace)

  // Convenience functions
  def trace(message: String, event:String): Unit = traceEventManager.trace(message,event)
  def trace(level: String, message: String, event:String): Unit = traceEventManager.trace(level,message,event)

  // ==============================================================================================

  def setCollection(href: URI, docs: List[XdmNode]): Unit = {
    _collections.put(href.toASCIIString, docs)
  }

  def collection(href: URI): List[XdmNode] = {
    _collections.getOrElse(href.toASCIIString, List.empty[XdmNode])
  }

  // ==============================================================================================

  private def defaultLocale: String = {
    import java.util.Locale

    // Translate _ to - for compatibility with xml:lang
    Locale.getDefault.toString.replace('_', '-')
  }

  def computeEpisode: String = {
    import java.security.MessageDigest
    import java.util.GregorianCalendar

    val digest = MessageDigest.getInstance("MD5")
    val calendar = new GregorianCalendar()

    val hash = digest.digest(calendar.toString.getBytes)
    var episode = "CB"
    for (b <- hash) {
      episode = episode + Integer.toHexString(b & 0xff)
    }

    episode
  }

  // ==============================================================================================

  def close(): Unit = {
    closed = true

    configSettings.close()

    for ((name,klass) <- _funcImplClasses) {
      try {
        val instance = Class.forName(klass).getDeclaredConstructor(this.getClass).newInstance(this)
        val func = instance.asInstanceOf[FunctionImpl]
        if (func.getFunctionQName.getURI != name.getNamespaceURI
          || func.getFunctionQName.getLocalPart != name.getLocalName) {
          logger.warn(s"Failed to register ${name} with implementation ${klass}; class implements ${func.getFunctionQName}")
        } else {
          processor.registerExtensionFunction(func)
          logger.debug(s"Registered ${name} with implementation ${klass}")
        }
      } catch {
        case _: ClassNotFoundException =>
          logger.warn(s"Failed to register ${name} with implementation ${klass}: class not found")
        case _: ClassCastException =>
          logger.warn(s"Failed to register ${name} with implementation ${klass}: class does not implement com.xmlcalabash.functions.FunctionImpl")
        case ex: Throwable =>
          logger.warn(s"Failed to register ${name} with implementation ${klass}: ${ex.getMessage}")
      }
    }
  }

  private def checkClosed(): Unit = {
    if (closed) {
      throw XProcException.xiConfigurationException("Cannot update XML Calabash configuration after initialization")
    }
  }
}
