package com.xmlcalabash.runtime

import com.jafpl.config.Jafpl
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Graph, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.{GraphRuntime, RuntimeConfiguration}
import com.jafpl.steps.DataConsumer
import com.jafpl.util.{ErrorListener, TraceEventManager}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.config.{DocumentManager, DocumentRequest, Signatures, XProcConfigurer}
import com.xmlcalabash.exceptions.{ConfigurationException, ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.messages.{AnyItemMessage, XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{ExpressionParser, XProcConstants}
import com.xmlcalabash.model.xxml.{XArtifact, XDeclareStep, XDocument, XEmpty, XInline, XInput, XStaticContext}
import com.xmlcalabash.steps.internal.{InlineExpander, XPathSelector}
import com.xmlcalabash.util.{MediaType, MinimalStaticContext, TypeUtils}
import com.xmlcalabash.util.stores.{DataStore, FallbackDataStore, FileDataStore, HttpDataStore}
import net.sf.saxon.lib.{ModuleURIResolver, UnparsedTextURIResolver}
import net.sf.saxon.s9api.{Processor, QName, XdmAtomicValue, XdmNode, XdmValue}
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.EntityResolver

import java.net.URI
import javax.xml.transform.URIResolver
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XMLCalabashRuntime protected[xmlcalabash] (val decl: XDeclareStep) extends XMLCalabashProcessor with RuntimeConfiguration {
  val config: XMLCalabash = decl.config

  //FIXME: why?
  override def threadPoolSize: Int = config.threadPoolSize

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected[runtime] val joinGateMarker = new XdmAtomicValue(new QName(XProcConstants.ns_cx, "JOIN-GATE-MARKER"))

  private var _traceEventManager = config.traceEventManager
  private var _errorListener = config.errorListener
  private var _documentManager = config.documentManager
  private var _entityResolver = config.entityResolver
  private var _uriResolver = config.uriResolver
  private var _moduleURIResolver = config.moduleURIResolver
  private var _unparsedTextURIResolver = config.unparsedTextURIResolver
  private var _episode = config.computeEpisode
  private var _defaultSerializationOptions: Map[String,Map[QName,String]] = Map.empty[String,Map[QName,String]]
  private var _trim_inline_whitespace = config.trimInlineWhitespace
  private val idMap = mutable.HashMap.empty[String,XArtifact]
  private var ran = false
  private var _signatures: Signatures = _
  private var runtime: GraphRuntime = _
  private var _staticOptions: Map[QName,XdmValueItemMessage] = _
  private var _datastore = Option.empty[DataStore]
  private val _usedPorts = mutable.HashSet.empty[String]
  private val _graphNodes = mutable.HashMap.empty[XArtifact, Node]

  val jafpl: Jafpl = Jafpl.newInstance()
  val graph: Graph = jafpl.newGraph()

  if (decl.name.isDefined) {
    graph.label = decl.name.get
  }

  def hasNode(art: XArtifact): Boolean = {
    _graphNodes.contains(art)
  }

  def node(art: XArtifact): Node = {
    _graphNodes(art)
  }
  def addNode(art: XArtifact, node: Node): Unit = {
    _graphNodes.put(art, node)
  }

  def staticOptions: Map[QName, XdmValueItemMessage] = _staticOptions

  protected[xmlcalabash] def init(decl: XDeclareStep): Unit = {
    try {
      config.debugOptions.dumpPipeline(decl)
      config.debugOptions.dumpOpenGraph(decl, graph)

      runtime = new GraphRuntime(graph, this)
      config.debugOptions.dumpGraph(decl, graph)

      _staticOptions = config.staticOptions

      runtime.traceEventManager = _traceEventManager
    } catch {
      case ex: JafplException =>
        ex.code match {
          case JafplException.BAD_LOOP_INPUT_PORT =>
            throw XProcException.xsLoop(ex.details(1).asInstanceOf[String], ex.details.head.asInstanceOf[String], ex.location)
          case _ =>
            throw ex
        }
      case ex: Throwable =>
        throw ex
    }
  }

  // ===================================================================================

  def inputs: Set[String] = decl.inputPorts
  def outputs: Set[String] = decl.outputPorts

  protected[runtime] def inputMessage(port: String, msg: Message): Unit = {
    runtime.inputs(port).send(msg)
  }

  def input(port: String, item: Any, metadata: XProcMetadata): Unit = {
    val context = new XStaticContext()
    item match {
      case xnode: XdmNode =>
        input(port, new XdmNodeItemMessage(xnode, metadata, context))
      case xitem: XdmValue =>
        input(port, new XdmValueItemMessage(xitem, metadata, context))
      case bitem: BinaryNode =>
        input(port, new AnyItemMessage(bitem.node, bitem, metadata, context))
      case _ =>
        throw XProcException.xiThisCantHappen(s"Unexpected value on pipeline input: ${item}", None)
    }
    _usedPorts += port
  }

  def input(port: String, message: Message): Unit = {
    if (runtime.inputs.contains(port)) {
      runtime.inputs(port).send(message)
    }
    _usedPorts += port
  }

  def output(port: String, consumer: DataConsumer): Unit = {
    val dcp = runtime.outputs(port)
    dcp.setConsumer(consumer)
  }

  def serializationOptions(port: String): Map[QName,String] = {
    decl.output(port).serialization
  }

  def option(name: QName, value: XdmValue, context: MinimalStaticContext): Unit = {
    if (runtime.bindings.contains(name.getClarkName)) {
      val optdecl = decl.option(name).get
      val typeUtils = new TypeUtils(processor, context)
      val msg = new XdmValueItemMessage(value, XProcMetadata.ANY, context)
      val cmsg = typeUtils.convertType(name, msg, optdecl.declaredType, optdecl.tokenList)
      runtime.bindings(name.getClarkName).setValue(cmsg)
    }
  }

  def usedPorts(ports: Set[String]): Unit = {
    _usedPorts ++= ports
  }

  def run(): Unit = {
    if (ran) {
      throw new RuntimeException("You must call reset() before running a pipeline a second time.")
    }

    for (xinput <- decl.children[XInput]) {
      if (!_usedPorts.contains(xinput.port) && xinput.defaultInputs.nonEmpty) {
        val defaults = ListBuffer.empty[DocumentRequest]
        for (default <- xinput.defaultInputs) {
          default match {
            case _: XEmpty =>
              ()
            case inline: XInline =>
              val expander = new InlineExpander(inline)
              if (inline.documentProperties.isDefined) {
                expander.documentProperties = inline.documentProperties.get
              }
              expander.copyStaticOptionsToBindings(this)
              defaults += expander.loadDocument(inline.expandText)
            case doc: XDocument =>
              defaults += doc.loadDocument()
            case _ =>
              throw XProcException.xiThisCantHappen(s"Unexpected default input type: ${default}", None)
          }
        }
        for (source <- defaults) {
          val resp = config.documentManager.parse(source)
          if (xinput.select.isDefined) {
            val items = Tuple2(resp.value, new XProcMetadata(resp.contentType, resp.props))
            val bindings = mutable.HashMap.empty[String, Message]
            for ((name,value) <- config.staticOptions) {
              bindings.put(name.getClarkName, value)
            }
            val xpselector = new XPathSelector(config, List(items), xinput.select.get, xinput.staticContext, bindings.toMap)
            val results = xpselector.select()
            if (results.length != 1 && !xinput.sequence) {
              throw XProcException.xdInputSequenceNotAllowed(xinput.port, None)
            }
            for (result <- results) {
              input(xinput.port, result, new XProcMetadata(resp.contentType, resp.props))
            }
          } else {
            input(xinput.port, resp.value, new XProcMetadata(resp.contentType, resp.props))
          }
        }
      }
    }

    runCommon()
  }

  private def runCommon(): Unit = {
    ran = true

    try {
      runtime.runSync()
    } catch {
      case ex: Exception =>
        ex match {
          case j: JafplException =>
            throw JafplExceptionMapper.remap(j)
          case _ =>
            throw ex
        }
    } finally {
      runtime.stop()
    }
  }

  def reset(): Unit = {
    /*
    //_staticOptionBindings.clear()
    outputSet.clear()
    bindingsMap.clear()

    _graph = decl.pipelineGraph()
    _graph.close()
    runtime = new GraphRuntime(_graph, this)
    runtime.traceEventManager = _traceEventManager
    ran = false
     */
  }

  def stop(): Unit = {
    runtime.stop()
  }

  // ===================================================================================

  def xprocConfigurer: XProcConfigurer = config.configurer

  def productName: String = config.productName
  def productVersion: String = config.productVersion
  def jafplVersion: String = config.jafplVersion
  def saxonVersion: String = config.saxonVersion
  def productConfig: String = config.productConfig
  def vendor: String = config.vendor
  def vendorURI: String = config.vendorURI
  def xprocVersion: String = config.xprocVersion
  def xpathVersion: String = config.xpathVersion
  def psviSupported: Boolean = config.psviSupported
  def processor: Processor = config.processor
  def staticBaseURI: URI = config.staticBaseURI
  def episode: String = _episode

  def entityResolver: EntityResolver = _entityResolver
  def uriResolver: URIResolver = _uriResolver
  def moduleURIResolver: ModuleURIResolver = _moduleURIResolver
  def unparsedTextURIResolver: UnparsedTextURIResolver = _unparsedTextURIResolver

  def documentManager: DocumentManager = _documentManager
  def documentManager_=(manager: DocumentManager): Unit = {
    _documentManager = manager
  }

  def errorListener: ErrorListener = _errorListener
  def errorListener_=(listener: ErrorListener): Unit = {
    _errorListener = listener
  }

  def traceEventManager: TraceEventManager = _traceEventManager
  def traceEventManager_=(manager: TraceEventManager): Unit = {
    _traceEventManager = manager
  }
  override def traceEnabled(trace: String): Boolean = _traceEventManager.traceEnabled(trace)

  // We need expression evaluators with access to the *runtime*
  private val  _expressionEvaluator = new SaxonExpressionEvaluator(this)
  override def expressionEvaluator: SaxonExpressionEvaluator = _expressionEvaluator

  def expressionParser: ExpressionParser = config.expressionParser

  def datastore: DataStore = {
    if (_datastore.isEmpty) {
      val fallback = new FallbackDataStore()
      val filestore = new FileDataStore(config, fallback)
      _datastore = Some(new HttpDataStore(config, filestore))
    }

    _datastore.get
  }

  // ====================================================================================

  def addNode(id: String, artifact: XArtifact): Unit = {
    idMap.put(id, artifact)
  }

  def node(id: String): Option[XArtifact] = idMap.get(id)


  def signatures: Signatures = {
    if (_signatures == null) {
      throw new ConfigurationException(ExceptionCode.CFGINCOMPLETE, "signatures")
    }
    _signatures
  }

  def signatures_=(signatures: Signatures): Unit = {
    _signatures = signatures
  }

  def defaultSerializationOptions(contentType: MediaType): Map[QName,String] = {
    _defaultSerializationOptions.getOrElse(contentType.toString, Map.empty[QName,String])
  }

  def defaultSerializationOptions(contentType: String): Map[QName,String] = {
    _defaultSerializationOptions.getOrElse(contentType, Map.empty[QName,String])
  }

  protected[xmlcalabash] def setDefaultSerializationOptions(opts: Map[String,Map[QName,String]]): Unit = {
    _defaultSerializationOptions = opts
  }

  def trimInlineWhitespace: Boolean = _trim_inline_whitespace
  def trimInlineWhitespace_=(trim: Boolean): Unit = {
    _trim_inline_whitespace = trim
  }
}
