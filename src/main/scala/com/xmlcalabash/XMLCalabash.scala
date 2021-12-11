package com.xmlcalabash

import com.jafpl.exceptions.{JafplException, JafplLoopDetected}
import com.jafpl.graph.{Binding, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.util.{ErrorListener, TraceEventManager}
import com.xmlcalabash.XMLCalabash.loggedProcessorDetail
import com.xmlcalabash.config.{DocumentManager, DocumentRequest, ErrorExplanation, XMLCalabashDebugOptions, XProcConfigurer}
import com.xmlcalabash.exceptions.{ConfigurationException, ExceptionCode, ModelException, ParseException, XProcException}
import com.xmlcalabash.functions.FunctionImpl
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{ExpressionParser, XProcConstants}
import com.xmlcalabash.model.xxml.{XDeclareStep, XNameBinding, XParser, XStaticContext}
import com.xmlcalabash.parsers.XPathParser
import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.runtime.{PrintingConsumer, SaxonExpressionEvaluator, StaticContext, XMLCalabashProcessor, XMLCalabashRuntime, XProcMetadata, XProcXPathExpression}
import com.xmlcalabash.sbt.BuildInfo
import com.xmlcalabash.util.{ArgBundle, DefaultErrorExplanation, DefaultXProcConfigurer, MediaType, PipelineBooleanOption, PipelineDocument, PipelineDocumentOption, PipelineDoubleOption, PipelineEnvironmentOption, PipelineEnvironmentOptionMap, PipelineEnvironmentOptionSerialization, PipelineExpressionOption, PipelineFileDocument, PipelineFilenameDocument, PipelineFunctionImplementation, PipelineInputDocument, PipelineInputFile, PipelineInputFilename, PipelineInputText, PipelineInputURI, PipelineInputXdm, PipelineNamespace, PipelineOption, PipelineOptionValue, PipelineOutputConsumer, PipelineOutputDocument, PipelineOutputFilename, PipelineOutputURI, PipelineParameter, PipelineStepImplementation, PipelineStringOption, PipelineSystemProperty, PipelineTextDocument, PipelineURIDocument, PipelineUntypedOption, PipelineUriOption, PipelineXdmDocument, PipelineXdmValueOption, URIUtils}
import net.sf.saxon.lib.{ModuleURIResolver, UnparsedTextURIResolver}
import net.sf.saxon.s9api.{ItemType, ItemTypeFactory, Processor, QName, XdmAtomicValue, XdmNode, XdmValue}
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.{EntityResolver, InputSource}

import java.io.{ByteArrayInputStream, FileInputStream}
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.xml.transform.URIResolver
import javax.xml.transform.sax.SAXSource
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object XMLCalabash {
  // Available even when XMLCalabash is being initialized
  val defaultErrorExplanation: ErrorExplanation = new DefaultErrorExplanation()

  private var loggedProcessorDetail = false

  def newInstance(): XMLCalabash = {
    unconfiguredInstance(None)
  }

  def newInstance(processor: Processor): XMLCalabash = {
    unconfiguredInstance(Some(processor))
  }

  def newInstance(configurer: XProcConfigurer): XMLCalabash = {
    new XMLCalabash(None, configurer)
  }

  def newInstance(processor: Processor, configurer: XProcConfigurer): XMLCalabash = {
    new XMLCalabash(Some(processor), configurer)
  }

  private def unconfiguredInstance(processor: Option[Processor]): XMLCalabash = {
    val _configProperty = "com.xmlcalabash.config.XProcConfigurer"
    val configurer = if (Option(System.getProperty(_configProperty)).isDefined) {
      val klass = System.getProperty(_configProperty)
      Class.forName(klass).getDeclaredConstructor().newInstance().asInstanceOf[XProcConfigurer]
    } else {
      new DefaultXProcConfigurer()
    }
    new XMLCalabash(processor, configurer)
  }
}

class XMLCalabash private(userProcessor: Option[Processor], val configurer: XProcConfigurer) extends XMLCalabashProcessor with RuntimeConfiguration {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private var _processor: Processor = _
  private var _itemTypeFactory: ItemTypeFactory = _
  private var _expressionEvaluator: SaxonExpressionEvaluator = _
  private val _collections = mutable.HashMap.empty[String, List[XdmNode]]
  private var _debugOptions: XMLCalabashDebugOptions = new XMLCalabashDebugOptions(this)
  private val _parameters = ListBuffer.empty[PipelineParameter]

  private var closed = false
  private var _threadPoolSize: Int = 2
  private var _errorListener: ErrorListener = _
  private var _traceEventManager: TraceEventManager = _
  private var _uriResolver: URIResolver = _
  private var _entityResolver: EntityResolver = _
  private var _moduleURIResolver: ModuleURIResolver = _
  private var _unparsedTextURIResolver: UnparsedTextURIResolver = _
  private var _errorExplanation: ErrorExplanation = _
  private var _documentManager: DocumentManager = _
  private var _htmlSerializer = false
  private var _staticBaseURI = URIUtils.cwdAsURI
  private var _locale = defaultLocale
  private var _episode = computeEpisode

  private var _declaration = Option.empty[XDeclareStep]
  private var _runtime: XMLCalabashRuntime = _

  private var _pipeline = Option.empty[PipelineDocument]
  private val _inputs = mutable.HashMap.empty[String,ListBuffer[PipelineInputDocument]]
  private val _outputs = mutable.HashMap.empty[String,ListBuffer[PipelineOutputDocument]]
  private val _options = mutable.HashMap.empty[QName,PipelineOptionValue]
  private val optionBindings = mutable.HashMap.empty[String,Message]
  private val _staticOptions = mutable.HashMap.empty[QName, XdmValueItemMessage]

  private var _standardLibraryParser = false
  private val _funcImplClasses = mutable.HashMap.empty[QName,String]
  private val _stepImplClasses = mutable.HashMap.empty[QName,String]
  // Only used during static analysis and not valid at runtime
  private val _staticStepsAvailable = mutable.HashSet.empty[QName]
  private var _staticStepsIndeterminate = true

  private var _except: Option[Exception] = None
  private var _longError = ""
  private var _shortError = ""

  val args = new ArgBundle()

  protected[xmlcalabash] def standardLibraryParser: Boolean = _standardLibraryParser
  protected[xmlcalabash] def standardLibraryParser_=(std: Boolean): Unit = {
    _standardLibraryParser = std
  }

  def externalSteps: Map[QName,String] = _stepImplClasses.toMap

  // Only used during static analysis and not valid at runtime
  protected[xmlcalabash] def staticStepsIndeterminate: Boolean = _staticStepsIndeterminate
  protected[xmlcalabash] def staticStepsIndeterminate_=(det: Boolean): Unit = {
    _staticStepsIndeterminate = det
  }

  // Only used during static analysis and not valid at runtime
  protected[xmlcalabash] def staticStepsAvailable: Set[QName] = _staticStepsAvailable.toSet
  protected[xmlcalabash] def staticStepsAvailable_=(aset: Set[QName]): Unit = {
    _staticStepsAvailable.clear()
    _staticStepsAvailable ++= aset
  }

  protected[xmlcalabash] def staticStepAvailable(stepType: QName): Boolean = {
    if (_stepImplClasses.contains(stepType) || _staticStepsAvailable.contains(stepType)) {
      true
    } else {
      if (staticStepsIndeterminate) {
        throw XProcException.xiIndeterminateSteps()
      }
      false
    }
  }

  def inputs: Map[String,List[PipelineInputDocument]] = {
    val map = mutable.HashMap.empty[String,List[PipelineInputDocument]]
    for ((port,list) <- _inputs) {
      map.put(port, list.toList)
    }
    map.toMap
  }

  def outputs: Map[String,List[PipelineOutputDocument]] = {
    val map = mutable.HashMap.empty[String,List[PipelineOutputDocument]]
    for ((port,list) <- _outputs) {
      map.put(port, list.toList)
    }
    map.toMap
  }

  def options: Map[QName,PipelineOptionValue] = _options.toMap
  def staticOptions: Map[QName, XdmValueItemMessage] = _staticOptions.toMap
  def addStatic(name: QName, value: XdmValueItemMessage): Unit = {
    if (_staticOptions.contains(name)) {
      throw XProcException.xsShadowsStatic(name, None)
    }
    _staticOptions.put(name, value)
  }

  def environmentOptions(name: QName): List[PipelineEnvironmentOption] = {
    parameters collect { case p: PipelineEnvironmentOption => p } filter { _.eqname == name.getEQName }
  }

  def configure(): Unit = {
    if (Option(_processor).isEmpty) {
      val cfgparam = configurer.xmlCalabashConfigurer.configure(args.parameters ++ parameters)
      _parameters.clear()
      _parameters ++= cfgparam

      for (prop <- parameters collect { case p: PipelineSystemProperty => p }) {
        System.setProperty(prop.name, prop.value)
      }

      _processor = if (userProcessor.isDefined) {
        userProcessor.get
      } else {
        val cfgfile = parameters collect { case p: PipelineEnvironmentOption => p } find {
          _.eqname == XProcConstants.cc_saxon_configuration.getEQName
        }
        try {
          if (cfgfile.isDefined && cfgfile.get.getString.isDefined) {
            new Processor(new SAXSource(new InputSource(cfgfile.get.getString.get)))
          } else {
            val aware = environmentOptions(XProcConstants.cc_schema_aware).headOption
            if (aware.isDefined) {
              new Processor(aware.get.getBoolean.getOrElse(false))
            } else {
              new Processor(false)
            }
          }
        } catch {
          case ex: XProcException =>
            throw ex
          case ex: Exception =>
            throw XProcException.xiConfigurationException(s"Failed to instantiate a Saxon processor: ${ex.getMessage}")
        }
      }

      _itemTypeFactory = new ItemTypeFactory(processor)

      val context = new XStaticContext()
      for (funcEnv <- parameters collect { case p: PipelineFunctionImplementation => p }) {
        val name = context.parseQName(funcEnv.eqname)
        try {
          val instance = Class.forName(funcEnv.className).getDeclaredConstructor(this.getClass).newInstance(this)
          val func = instance.asInstanceOf[FunctionImpl]
          if (func.getFunctionQName.getURI != name.getNamespaceURI
            || func.getFunctionQName.getLocalPart != name.getLocalName) {
            logger.warn(s"Failed to register ${name} with implementation ${funcEnv.className}; class implements ${func.getFunctionQName}")
          } else {
            _processor.registerExtensionFunction(func)
            _funcImplClasses.put(context.parseQName(funcEnv.eqname), funcEnv.className)
            logger.debug(s"Registered ${name} with implementation ${funcEnv.className}")
          }
        } catch {
          case _: ClassNotFoundException =>
            logger.warn(s"Failed to register ${name} with implementation ${funcEnv.className}: class not found")
          case _: ClassCastException =>
            logger.warn(s"Failed to register ${name} with implementation ${funcEnv.className}: class does not implement com.xmlcalabash.functions.FunctionImpl")
          case ex: Throwable =>
            logger.warn(s"Failed to register ${name} with implementation ${funcEnv.className}: ${ex.getMessage}")
        }
      }

      for (stepEnv <- parameters collect { case p: PipelineStepImplementation => p }) {
        _stepImplClasses.put(context.parseQName(stepEnv.eqname), stepEnv.className)
      }

      _expressionEvaluator = new SaxonExpressionEvaluator(this)
      configurer.xmlCalabashConfigurer.update(this)

      logProductDetails()

      var tcountStr = ""
      try {
        val tcountprop = "com.xmlcalabash.threadCount"
        var tcount = Option.empty[Int]

        if (Option(System.getProperty(tcountprop)).isDefined) {
          tcountStr = System.getProperty(tcountprop)
          tcount = Some(tcountStr.toInt)
        } else {
          val envOpt = environmentOptions(XProcConstants.cc_thread_count).headOption
          if (envOpt.isDefined && envOpt.head.getString.isDefined) {
            tcountStr = envOpt.head.getString.get
            tcount = Some(tcountStr.toInt)
          }
        }

        if (tcount.isDefined && tcount.get <= 0) {
          throw XProcException.xiConfigurationException(s"Invalid thread count, must be greater than 0: ${tcountStr}")
        }

        if (tcount.isDefined) {
          _threadPoolSize = tcount.get
        }
      } catch {
        case ex: XProcException =>
          throw ex
        case _: Exception =>
          throw XProcException.xiConfigurationException(s"Invalid thread count: ${tcountStr} is not an integer")
      }
    }

    // Build the options before compiling in case some of them are statics...
    _options.clear()
    val nsmap = mutable.HashMap.empty[String,String]
    for (param <- args.parameters) {
      param match {
        case ns: PipelineNamespace =>
          nsmap.put(ns.prefix, ns.namespace)
        case opt: PipelineOption =>
          updateOptions(_options, nsmap.toMap, opt)
        case _ =>
          () // Just ignore it?
      }
    }

    _pipeline = args.pipeline

    if (_pipeline.isDefined && _declaration.isEmpty) {
      val parser = new XParser(this)
      var pipeline: XDeclareStep = null
      try {
        pipeline = _pipeline.get match {
          case uri: PipelineURIDocument =>
            parser.loadDeclareStep(uri.value)
          case str: PipelineFilenameDocument =>
            parser.loadDeclareStep(URIUtils.cwdAsURI.resolve(str.value))
          case file: PipelineFileDocument =>
            parser.loadDeclareStep(URIUtils.cwdAsURI.resolve(file.value.getAbsolutePath))
          case node: PipelineXdmDocument =>
            checkNode(node.value)
            parser.loadDeclareStep(node.value)
          case text: PipelineTextDocument =>
            if (text.contentType.markupContentType) {
              val builder = processor.newDocumentBuilder()
              val bais = new ByteArrayInputStream(text.text.getBytes(StandardCharsets.UTF_8))
              parser.loadDeclareStep(builder.build(new SAXSource(new InputSource(bais))))
            } else {
              throw XProcException.xiBadMediaType("Pipelines must be XML", None)
            }
          case _ =>
            throw new RuntimeException("Unexpected pipeline input")
        }
        if (parser.exceptions.nonEmpty) {
          throw parser.exceptions.head
        }
      } catch {
        case ex: XProcException =>
          handleException(ex)
          ex.code match {
            case XProcException.err_xd0036 =>
              // Weirdo remapping to satisfy the test suite
              if (ex.variant == 1) {
                val value = ex.details.head.asInstanceOf[String]
                val seqtype = ex.details(1).asInstanceOf[String]
                throw XProcException.xsBadTypeValue(value, seqtype, ex.location)
              }
            case _ =>
              ()
          }
          throw ex
        case ex: Exception =>
          throw ex
      }

      _declaration = Some(pipeline)
      close()

      //println(_declaration.get.dump)

      debugOptions.dumpTree(pipeline)
      debugOptions.dumpPipeline(pipeline)
    }
  }

  def resolve(): Unit = {
    configure()

    _inputs.clear()
    _outputs.clear()
    _options.clear()

    val nsmap = mutable.HashMap.empty[String,String]

    for (param <- args.parameters) {
      param match {
        case ns: PipelineNamespace =>
          nsmap.put(ns.prefix, ns.namespace)

        case in: PipelineInputDocument =>
          if (!_inputs.contains(in.port)) {
            _inputs.put(in.port, ListBuffer.empty[PipelineInputDocument])
          }

          in match {
            case xdm: PipelineInputXdm =>
              checkNode(xdm.value)
            case _ => ()
          }

          _inputs(in.port) += in

        case _: PipelineOutputDocument =>
          () // See below

        case opt: PipelineOption =>
          updateOptions(_options, nsmap.toMap, opt)

        case _ =>
          () // Just ignore it?
      }
    }

    // To deal with outputs, we have to collate multiple outputs to the same port together.
    // But we also have to check that there's at most one explicit DataConsumer.
    val outputMap = mutable.HashMap.empty[String, ListBuffer[PipelineOutputDocument]]
    for (opt <- args.parameters collect { case p: PipelineOutputDocument => p }) {
      if (!(outputMap.contains(opt.port))) {
        outputMap.put(opt.port, ListBuffer.empty[PipelineOutputDocument])
      }
      opt match {
        case out: PipelineOutputFilename =>
          outputMap(opt.port) += out
        case out: PipelineOutputURI =>
          if (out.value.getScheme != "file") {
            throw new RuntimeException(s"Output to ${out.value.getScheme}: URIs is not supported")
          }
          outputMap(opt.port) += out
        case out: PipelineOutputConsumer =>
          outputMap(opt.port) += out
      }
    }

    for (port <- outputMap.keySet) {
      var consumer = Option.empty[PipelineOutputDocument]
      val files = ListBuffer.empty[PipelineOutputDocument]
      for (doc <- outputMap(port)) {
        doc match {
          case out: PipelineOutputFilename =>
            files += out
          case out: PipelineOutputURI =>
            if (out.value.getScheme != "file") {
              throw new RuntimeException(s"Output to ${out.value.getScheme}: URIs is not supported")
            }
            files += out
          case out: PipelineOutputConsumer =>
            if (consumer.isDefined) {
              throw XProcException.xiBadConsumers("Only one data consumer may be specified for a given port", None)
            }
            consumer = Some(out)
        }
      }

      if (files.nonEmpty && consumer.isDefined) {
        throw XProcException.xiBadConsumers("If a data consumer is specified for a port, no other values may be specified", None)
      }

      _outputs.put(port, ListBuffer.empty[PipelineOutputDocument])
      if (consumer.isDefined) {
        _outputs(port) += consumer.get
      } else {
        _outputs(port) ++= files
      }
    }
  }

  private def updateOptions(options: mutable.HashMap[QName,PipelineOptionValue], nsmap: Map[String,String], opt: PipelineOption): Unit = {
    val context = new XStaticContext(URIUtils.cwdAsURI, nsmap)
    val name = context.parseQName(opt.eqname)
    val value: XdmValue = opt match {
      case v: PipelineBooleanOption => new XdmAtomicValue(v.value)
      case v: PipelineDoubleOption => new XdmAtomicValue(v.value)
      case v: PipelineStringOption => new XdmAtomicValue(v.value)
      case v: PipelineUriOption => new XdmAtomicValue(v.value)
      case v: PipelineUntypedOption => new XdmAtomicValue(v.value, ItemType.UNTYPED_ATOMIC)
      case v: PipelineDocumentOption =>
        v.value match {
          case doc: PipelineFilenameDocument =>
            val req = new DocumentRequest(URIUtils.cwdAsURI.resolve(doc.value))
            val resp = documentManager.parse(req)
            resp.value
          case doc: PipelineURIDocument =>
            val req = new DocumentRequest(URIUtils.cwdAsURI.resolve(doc.value))
            val resp = documentManager.parse(req)
            resp.value
          case doc: PipelineFileDocument =>
            val req = new DocumentRequest(doc.value.toURI, doc.contentType)
            val resp = documentManager.parse(req, new FileInputStream(doc.value))
            resp.value
        }
      case v: PipelineExpressionOption =>
        val bindings = mutable.HashMap.empty[String,Message]
        for ((name, value) <- options) {
          val msg = new XdmValueItemMessage(value.value, XProcMetadata.EMPTY, context)
          bindings.put(name.getClarkName, msg)
        }

        val expr = new XProcXPathExpression(context, v.expression)
        val value = expressionEvaluator.value(expr, List(), bindings.toMap, None)
        value.item
      case v: PipelineXdmValueOption =>
        v.value
      case _ =>
        throw XProcException.xiThisCantHappen(s"Unexpected pipeline option type: ${opt}")
    }
    if (options.contains(name)) {
      val newvalue = options(name).value.append(value)
      options.put(name, new PipelineOptionValue(context, newvalue))
    } else {
      options.put(name, new PipelineOptionValue(context, value))
    }
  }

  private def checkNode(node: XdmNode): Unit = {
    if (processor.getUnderlyingConfiguration.getNamePool ne node.getUnderlyingNode.getConfiguration.getNamePool) {
      throw XProcException.xiWrongProcessor("Node comes from a different configuration", None)
    }
  }

  def loadPipeline(): Unit = {
    resolve()
    if (_pipeline.isEmpty) {
      throw XProcException.xiArgBundleNoPipeline()
    }
    if (Option(_runtime).isEmpty) {
      try {
        _runtime = _declaration.get.runtime()
      } catch {
        case ex: Exception =>
          handleException(ex)
          throw ex
      }
    }
  }

  def run(): Unit = {
    try {
      loadPipeline()

      if (!debugOptions.run) {
        return
      }

      val context = new StaticContext(this)

      for (port <- _inputs.keySet) {
        for (opt <- _inputs(port)) {
          val doc = opt match {
            case in: PipelineInputFilename =>
              loadDocument(context, URIUtils.cwdAsURI.resolve(in.value))
            case in: PipelineInputURI =>
              loadDocument(context, URIUtils.cwdAsURI.resolve(in.value))
            case in: PipelineInputFile =>
              val request = new DocumentRequest(URIUtils.cwdAsURI.resolve(in.value.getAbsolutePath), in.contentType)
              val response = documentManager.parse(request, new FileInputStream(in.value))
              val meta = new XProcMetadata(response.contentType, response.props)
              new XdmValueItemMessage(response.value, meta, context)
            case in: PipelineInputXdm =>
              val meta = new XProcMetadata(MediaType.XML)
              new XdmValueItemMessage(in.value, meta, context)
            case in: PipelineInputText =>
              val bais = new ByteArrayInputStream(in.text.getBytes(StandardCharsets.UTF_8))
              val request = new DocumentRequest(URIUtils.cwdAsURI, in.contentType)
              val response = documentManager.parse(request, bais)
              val meta = new XProcMetadata(response.contentType, response.props)
              new XdmValueItemMessage(response.value, meta, context)
          }
          _runtime.input(port, doc.item, doc.metadata)
        }
      }

      for (port <- runtime.outputs) {
        val output = runtime.decl.output(port)
        val pc = if (_outputs.contains(port)) {
          val opts = _outputs(port)
          if (opts.nonEmpty && opts.head.isInstanceOf[PipelineOutputConsumer]) {
            opts.head.asInstanceOf[PipelineOutputConsumer].value
          } else {
            val files = ListBuffer.empty[String]
            for (opt <- opts.toList) {
              opt match {
                case out: PipelineOutputFilename =>
                  files += out.value
                case out: PipelineOutputURI =>
                  files += URIUtils.cwdAsURI.resolve(out.value).getPath
              }
            }
            new PrintingConsumer(runtime, output, files.toList)
          }
        } else {
          new PrintingConsumer(runtime, output)
        }
        runtime.output(port, pc)
      }

      for (option <- _declaration.get.options) {
        if (_options.contains(option.name)) {
          setOption(option, _options(option.name))
        } else {
          if (option.required) {
            throw XProcException.xsMissingRequiredOption(option.name, None)
          }
          if (option.usedByPipeline) {
            setOption(option)
          }
        }
      }

      for (name <- _options.keySet) {
        if (!optionBindings.contains(name.getClarkName)) {
          logger.info(s"Ignoring option '${name}'; it is not used by the pipeline")
        }
      }

      runtime.run()
    } catch {
      case ex: Exception =>
        if (runtime != null) {
          runtime.stop()
        }

        handleException(ex)
      case ex: Throwable =>
        if (runtime != null) {
          runtime.stop()
        }

        throw ex
    }

    if (_except.isDefined) {
      throw _except.get
    }
  }

  private def setOption(option: XNameBinding): Unit = {
    val eval = expressionEvaluator.newInstance()
    val expr = new XProcXPathExpression(option.staticContext, option.select.getOrElse("()"))
    val value = eval.compute(expr, List(), optionBindings.toMap, Map(), XPathBindingParams.EMPTY)
    runtime.option(option.name, value, option.staticContext)
    val msg = new XdmValueItemMessage(value, XProcMetadata.ANY, option.staticContext)
    optionBindings.put(option.name.getClarkName, msg)
    // FIXME: does the option value satisfy the type constraints?
  }

  private def setOption(option: XNameBinding, value: PipelineOptionValue): Unit = {
    runtime.option(option.name, value.value, value.context)
    val msg = new XdmValueItemMessage(value.value, XProcMetadata.ANY, value.context)
    optionBindings.put(option.name.getClarkName, msg)
    // FIXME: does the option value satisfy the type constraints?
  }

  private def handleException(exception: Exception): Unit = {
    _except = Some(exception)

    val explain = if (Option(_errorExplanation).isDefined) {
      errorExplanation
    } else {
      XMLCalabash.defaultErrorExplanation
    }

    val mappedex = XProcException.mapPipelineException(exception)

    mappedex match {
      case model: ModelException =>
        _shortError = model.getMessage
      case parse: ParseException =>
        _shortError = parse.getMessage
      case jafpl: JafplLoopDetected =>
        val sb = new StringBuilder()
        sb.append("Loop detected:")
        var first = true
        var pnode = Option.empty[Node]
        for (node <- jafpl.nodes) {
          val prefix = if (first) {
            "An output from"
          } else {
            "flows to"
          }

          node match {
            case bind: Binding =>
              val bnode = bind.bindingFor
              pnode = Some(bnode)

              val bnodeLabel = bnode.userLabel.getOrElse(bnode.label)
              val bindLabel = bind.userLabel.getOrElse(bind.label)

              sb.append(s"\tis the context item for $bnodeLabel/$bindLabel; $bnodeLabel")
              if (bnode.location.isDefined) {
                sb.append(s"\t\t${bnode.location.get}")
              }
            case _ =>
              if (pnode.isEmpty || pnode.get != node) {
                sb.append(s"\t$prefix ${node.userLabel.getOrElse(node.label)}")
                if (node.location.isDefined) {
                  sb.append(s"\t\t${node.location.get}")
                }
                pnode = Some(node)
              }
          }

          first = false
        }
        _shortError = sb.toString()
      case jafpl: JafplException =>
        _shortError = jafpl.getMessage()
      case xproc: XProcException =>
        val code = xproc.code
        val message = if (xproc.message.isDefined) {
          xproc.message.get
        } else {
          code match {
            case qname: QName =>
              explain.message(qname, xproc.variant, xproc.details)
            case _ =>
              s"Configuration error: code ($code) is not a QName"
          }
        }
        if (xproc.location.isDefined) {
          _shortError = s"ERROR ${xproc.location.get} $code $message"
        } else {
          _shortError = s"ERROR $code $message"
        }

        _longError = explain.explanation(code, xproc.variant)

      case _ =>
        _shortError = s"Error: ${mappedex.getMessage}"
    }
  }

  private def loadDocument(context: StaticContext, href: URI): XdmValueItemMessage = {
    val request = new DocumentRequest(href)
    val response = documentManager.parse(request)
    val meta = new XProcMetadata(response.contentType, response.props)
    new XdmValueItemMessage(response.value, meta, context)
  }

  def runtime: XMLCalabashRuntime = _runtime
  def processor: Processor = _processor
  def itemTypeFactory: ItemTypeFactory = _itemTypeFactory
  def step: XDeclareStep = _declaration.orNull
  def pipeline: PipelineDocument = _pipeline.orNull

  def errorMessage: String = _shortError
  def errorExplain: String = _longError
  def exception: Exception = _except.orNull

  def productName: String = BuildInfo.name
  def productVersion: String = BuildInfo.version
  def productHash: String = BuildInfo.gitHash.substring(0,6)
  def jafplVersion: String = BuildInfo.jafplVersion
  def saxonVersion: String = {
    val sver = _processor.getSaxonProductVersion
    val sed = _processor.getUnderlyingConfiguration.getEditionCode
    s"$sver/$sed"
  }
  def productConfig: String = {
    s"${BuildInfo.version} (with JAFPL $jafplVersion for Saxon $saxonVersion)"
  }
  def vendor: String = "Norman Walsh"
  def vendorURI: String = "https://xmlcalabash.com/"
  def xprocVersion: String = "3.0"
  def xpathVersion: String = "3.1"
  def psviSupported: Boolean = _processor.isSchemaAware

  def parameters: List[PipelineParameter] = _parameters.toList
  def parameters_=(params: List[PipelineParameter]): Unit = {
    checkClosed()
    _parameters.clear()
    _parameters ++= params
  }
  def parameter(param: PipelineParameter): Unit = {
    checkClosed()
    _parameters += param
  }

  private var _safeMode: Option[Boolean] = None
  def safeMode: Boolean = {
    if (_safeMode.isEmpty) {
      val envOpt = parameters collect { case p: PipelineEnvironmentOption => p } find {
        _.eqname == XProcConstants.cc_safe_mode.getEQName
      }
      if (envOpt.isDefined) {
        _safeMode = Some(envOpt.get.getBoolean.getOrElse(false))
      } else {
        _safeMode = Some(false)
      }
    }
    _safeMode.get
  }

  private var _proxies: Option[Map[String,URI]] = None
  def proxies: Map[String,URI] = {
    if (_proxies.isEmpty) {
      val map = mutable.HashMap.empty[String,URI]
      for (env <- parameters collect { case p: PipelineEnvironmentOption => p } filter { _.eqname == XProcConstants.cc_http_proxy.getEQName }) {
        env match {
          case proxy: PipelineEnvironmentOptionMap =>
            map.put(proxy.key, new URI(proxy.value))
          case _ =>
            logger.error("The proxy configuration is incorrect (should be key, value pair)")
        }
      }
      _proxies = Some(map.toMap)
    }
    _proxies.get
  }

  def logProductDetails(): Unit = {
    if (!loggedProcessorDetail) {
      logger.info(s"$productName version $productVersion (with JAFPL $jafplVersion and Saxon $saxonVersion)")
      logger.debug(s"Copyright © 2018-2021 $vendor; $vendorURI")
      logger.debug(s"(release id: $productHash; episode: $episode)")
      loggedProcessorDetail = true
    } else {
      logger.debug(s"${productName} episode: $episode)")
    }
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

  def trimInlineWhitespace: Boolean = {
    val envOpt = environmentOptions(XProcConstants.cc_trim_inline_whitespace).headOption
    if (envOpt.isDefined && envOpt.head.getBoolean.isDefined) {
      envOpt.head.getBoolean.get
    } else {
      false
    }
  }

  def showErrors: Boolean = {
    val envOpt = environmentOptions(XProcConstants.cc_show_errors).headOption
    if (envOpt.isDefined && envOpt.head.getBoolean.isDefined) {
      envOpt.head.getBoolean.get
    } else {
      false
    }
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

  private var _defaultSerializationOptions: Option[Map[String,Map[QName,String]]] = None
  def defaultSerializationOptions(contentType: String): Map[QName,String] = {
    if (_defaultSerializationOptions.isEmpty) {
      checkClosed()
      val context = new XStaticContext()
      val map = mutable.HashMap.empty[String,Map[QName,String]]
      for (ser <- parameters collect { case p: PipelineEnvironmentOptionSerialization => p }) {
        val smap = mutable.HashMap.empty[QName, String]
        for ((key, value) <- ser.value) {
          smap.put(context.parseQName(key), value)
        }
        map.put(ser.eqname, smap.toMap)
      }
      _defaultSerializationOptions = Some(map.toMap)
    }
    _defaultSerializationOptions.get.getOrElse(contentType, Map())
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
  }

  private def checkClosed(): Unit = {
    if (closed) {
      throw XProcException.xiConfigurationException("Cannot update XML Calabash configuration after initialization")
    }
  }

  private class InputDocument(val value: XdmValue, val contentType: MediaType) {}
}
