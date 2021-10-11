package com.xmlcalabash.runtime

import com.jafpl.graph.Location
import com.jafpl.messages.{BindingMessage, ExceptionMessage, ItemMessage, Message, PipelineMessage}
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, PortCardinality, Step}
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{AnyItemMessage, XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.params.StepParams
import com.xmlcalabash.util.{MediaType, S9Api, TypeUtils}
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.s9api.{Axis, QName, SequenceType, XdmAtomicValue, XdmItem, XdmMap, XdmNode, XdmNodeKind, XdmValue}
import org.apache.http.util.ByteArrayBuffer
import org.slf4j.{Logger, LoggerFactory}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, IOException, InputStream}
import java.net.URI
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class StepProxy(config: XMLCalabashRuntime, stepType: QName, step: StepExecutable, params: Option[ImplParams], staticContext: StaticContext) extends Step with XProcDataConsumer {
  private val typeUtils = new TypeUtils(config)
  private var _id: String = _
  private val openStreams = ListBuffer.empty[InputStream]
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[DataConsumer] = None
  protected val bindings = mutable.HashSet.empty[QName]
  protected val bindingsMap = mutable.HashMap.empty[String, Message]
  protected var dynamicContext = new DynamicContext()
  protected var received = mutable.HashMap.empty[String,Long]
  protected var running = false
  protected val inputBuffer = mutable.HashMap.empty[String,ListBuffer[Message]]
  protected val outputBuffer = mutable.HashMap.empty[String,ListBuffer[(Any, XProcMetadata)]]

  def nodeId: String = _id
  def nodeId_=(id: String): Unit = {
    if (_id == null) {
      _id = id
    } else {
      throw XProcException.xiRedefId(id, staticContext.location)
    }
  }

  def location: Option[Location] = staticContext.location
  def location_=(location: Location): Unit = {
    throw new RuntimeException("You can't assign the location")
  }

  // =============================================================================================

  override def toString: String = {
    val node = config.node(nodeId)
    if (node.isDefined) {
      node.get.toString
    } else {
      "proxy:" + step.toString
    }
  }

  override def inputSpec: XmlPortSpecification = {
    step match {
      case xstep: XmlStep =>
        xstep.inputSpec
      case _ =>
        val portMap = mutable.HashMap.empty[String,PortCardinality]
        val typeMap = mutable.HashMap.empty[String,List[String]]
        for (key <- step.inputSpec.ports) {
          portMap.put(key, step.inputSpec.cardinality(key).getOrElse(PortCardinality.ZERO_OR_MORE))
          typeMap.put(key, List("application/octet-stream"))
        }
        val spec = new XmlPortSpecification(portMap.toMap, typeMap.toMap)
        spec
    }
  }

  override def outputSpec: XmlPortSpecification = {
    step match {
      case xstep: XmlStep => xstep.outputSpec
      case _ =>
        val portMap = mutable.HashMap.empty[String,PortCardinality]
        val typeMap = mutable.HashMap.empty[String,List[String]]
        for (key <- step.outputSpec.ports) {
          portMap.put(key, step.outputSpec.cardinality(key).getOrElse(PortCardinality.ZERO_OR_MORE))
          typeMap.put(key, List("application/octet-stream"))
        }
        new XmlPortSpecification(portMap.toMap, typeMap.toMap)
    }
  }
  override def bindingSpec: BindingSpecification = step.bindingSpec
  override def setConsumer(consumer: DataConsumer): Unit = {
    this.consumer = Some(consumer)
    step.setConsumer(this)
  }

  override def receiveBinding(bindmsg: BindingMessage): Unit = {
    val valuemsg = bindmsg.message match {
      case item: XdmValueItemMessage => item
      case _ =>
        throw XProcException.xiInvalidMessage(staticContext.location, bindmsg.message)
    }

    val qname = if (bindmsg.name.startsWith("{")) {
      val clarkName = "\\{(.*)\\}(.*)".r
      val qname = bindmsg.name match {
        case clarkName(uri,name) => new QName(uri,name)
        case _ => throw XProcException.xiInvalidClarkName(staticContext.location, bindmsg.name)
      }
      qname
    } else {
      new QName("", bindmsg.name)
    }

    if (step.signature.stepType.isDefined) {
      val ns = step.signature.stepType.get.getNamespaceURI
      if ((ns == XProcConstants.ns_p && qname == XProcConstants._message)
        || (ns != XProcConstants.ns_p && qname == XProcConstants.p_message)) {
        System.err.println(bindmsg.message.toString)
        return
      }
    } else {
      if (qname == XProcConstants.p_message) {
        System.err.println(bindmsg.message.toString)
        return
      }
    }

    bindings += qname
    bindingsMap.put(qname.getClarkName, bindmsg.message)

    val stepsig = step.signature
    if (stepsig.optionNames.contains(qname)) {
      val optsig  = stepsig.option(qname, staticContext.location)
      val opttype: Option[SequenceType] = optsig.declaredType
      val optlist: Option[List[XdmAtomicValue]] = optsig.tokenList

      valuemsg.item match {
        case atomic: XdmAtomicValue =>
          val value = typeUtils.castAtomicAs(atomic, opttype, valuemsg.context)
          if (optlist.isDefined) {
            var found = false
            for (item <- optlist.get) {
              found = found || value.equals(item)
            }
            if (!found) {
              val sb = new StringBuffer()
              sb.append("(")
              var first = true;
              for (item <- optlist.get) {
                if (!first) {
                  sb.append(", ")
                }
                first = false

                item.getPrimitiveTypeName match {
                  case XProcConstants.xs_string =>
                    sb.append("\"")
                    sb.append(item.getStringValue)
                    sb.append("\"")
                  case XProcConstants.xs_boolean =>
                    sb.append(item.getStringValue)
                    sb.append("()")
                  case _ =>
                    sb.append(item.getStringValue)
                }
              }
              sb.append(")")
              throw XProcException.xdValueNotInList(value.getStringValue, sb.toString, valuemsg.context.location)
            }
          }
          step.receiveBinding(new NameValueBinding(qname, value, valuemsg))
        case _ => ()
          val xvalue = valuemsg.item.getUnderlyingValue
          xvalue match {
            case map: MapItem =>
              if (optsig.forceQNameKeys) {
                val qmap = S9Api.forceQNameKeys(map, staticContext)
                step.receiveBinding(new NameValueBinding(qname, qmap, valuemsg))
              } else {
                step.receiveBinding(new NameValueBinding(qname, valuemsg))
              }
            case _ =>
              step.receiveBinding(new NameValueBinding(qname, valuemsg))
          }
      }
    } else {
      // Just pass it through, it's probably an extension attribute
      step.receiveBinding(new NameValueBinding(qname, valuemsg))
    }
  }

  override def initialize(config: RuntimeConfiguration): Unit = {
    config match {
      case _: XMLCalabashRuntime => ()
      case _ => throw XProcException.xiNotXMLCalabash()
    }

    config match {
      case xcfg: XMLCalabashRuntime =>
        if (this.config.config != xcfg.config) {
          throw XProcException.xiDifferentXMLCalabash()
        }
      case _ => ()
    }

    step.initialize(config)
  }

  override def run(): Unit = {
    running = true

    for (port <- inputBuffer.keySet) {
      for (message <- inputBuffer(port)) {
        processInput(port, message)
      }
    }
    inputBuffer.clear()

    for (port <- outputBuffer.keySet) {
      for ((item,meta) <- outputBuffer(port)) {
        receive(port, item, meta)
      }
    }
    outputBuffer.clear()

    // If there are statically computed options for this step, pass them along
    if (params.isDefined && params.get.isInstanceOf[StepParams]) {
      val atomic = params.get.asInstanceOf[StepParams]
      for ((name,value) <- atomic.staticallyComputedOptions) {
        val bindmsg = new BindingMessage(name, value)
        receiveBinding(bindmsg)
      }
    }

    for (qname <- step.signature.optionNames) {
      if (!bindings.contains(qname)) {
        val optsig  = step.signature.option(qname, staticContext.location)
        val opttype: Option[SequenceType] = optsig.declaredType
        if (optsig.defaultSelect.isDefined) {
          val value = typeUtils.castAtomicAs(new XdmAtomicValue(optsig.defaultSelect.get), opttype, staticContext)
          step.receiveBinding(new NameValueBinding(qname, value, XProcMetadata.ANY, staticContext))
        }
      }
    }

    try {
      DynamicContext.withContext(dynamicContext) {
        step.run(staticContext)
      }
    } catch {
      case ex: XProcException =>
        // If an exception was thrown without a location, and we have a location for
        // this step, add the step location to the exception. Somewhere is better than anywhere.
        if (ex.location.isEmpty && staticContext.location.isDefined) {
          throw ex.withLocation(staticContext.location.get)
        } else {
          throw ex
        }
      case ex: Exception =>
        throw ex
    } finally {
      running = false
      var thrown = Option.empty[Exception]
      for (stream <- openStreams) {
        try {
          stream.close()
        } catch {
          case ex: IOException => ()
          case ex: Exception =>
            thrown = Some(ex)
        }
      }
      if (thrown.isDefined) {
        throw thrown.get
      }
    }
  }

  override def reset(): Unit = {
    step.reset()
    running = false
    bindings.clear()
    bindingsMap.clear()
    received.clear()
    outputBuffer.clear()
    inputBuffer.clear()
  }

  override def abort(): Unit = {
    running = false
    step.abort()
  }

  override def stop(): Unit = {
    running = false
    step.stop()
  }

  override def consume(port: String, message: Message): Unit = {
    // We have to buffer inputs because they may arrive before we begin running.
    // If we're in a choose or other optional construct, we must not process
    // them before we've been selected to run.
    if (!inputBuffer.contains(port)) {
      val lb = ListBuffer.empty[Message]
      inputBuffer.put(port, lb)
    }
    inputBuffer(port) += message
  }

  private def processInput(port: String, message: Message): Unit = {
    received.put(port, received.getOrElse(port, 1))
    inputSpec.checkInputCardinality(port, received(port))

    val mtypes = step.signature.input(port, staticContext.location).contentTypes
    if (mtypes.nonEmpty) {
      val ctype = message match {
        case msg: XdmNodeItemMessage => Some(msg.metadata.contentType)
        case msg: XdmValueItemMessage => Some(msg.metadata.contentType)
        case msg: AnyItemMessage => Some(msg.metadata.contentType)
        case msg: PipelineMessage =>
          msg.metadata match {
            case meta: XProcMetadata => Some(meta.contentType)
            case _ => None
          }
        case _ => None
      }

      if (ctype.isDefined && !ctype.get.allowed(mtypes)) {
        throw XProcException.xdBadInputMediaType(ctype.get, mtypes, staticContext.location)
      }
    }

    // Get exceptions out of the way
    message match {
      case msg: ExceptionMessage =>
        msg.item match {
          case ex: XProcException =>
            if (ex.errors.isDefined) {
              step.receive(port, ex.errors.get, XProcMetadata.XML)
            } else {
              step.receive(port, msg.item, XProcMetadata.EXCEPTION)
            }
          case _ =>
            step.receive(port, msg.item, XProcMetadata.EXCEPTION)
        }
        return
      case _ => ()
    }

    message match {
      case msg: XdmNodeItemMessage =>
        dynamicContext.addDocument(msg.item, msg)
        step.receive(port, msg.item, msg.metadata)
      case msg: XdmValueItemMessage =>
        dynamicContext.addDocument(msg.item, msg)
        step.receive(port, msg.item, msg.metadata)
      case msg: AnyItemMessage =>
        step.receive(port, msg.shadow, msg.metadata)
      case msg: PipelineMessage =>
        // Attempt to convert this...
        msg.item match {
          case node: XdmNode =>
            if (node.getNodeKind == XdmNodeKind.DOCUMENT) {
              step.receive(port, node, msg.metadata.asInstanceOf[XProcMetadata])
            } else {
              // Messages have to be documents...
              val builder = new SaxonTreeBuilder(config)
              builder.startDocument(node.getBaseURI)
              builder.addSubtree(node)
              builder.endDocument()
              step.receive(port, builder.result, msg.metadata.asInstanceOf[XProcMetadata])
            }
          case item: XdmItem =>
            step.receive(port, item, msg.metadata.asInstanceOf[XProcMetadata])
          case _ =>
            throw XProcException.xiInvalidMessage(staticContext.location, message)
        }
      case item: ItemMessage =>
        // FIXME: match more types and/or centralize this somewhere
        val atomic = item.item match {
          case long: Long => new XdmAtomicValue(long)
          case int: Int => new XdmAtomicValue(int)
          case char: Char => new XdmAtomicValue(char)
          case str: String => new XdmAtomicValue(str)
          case _ => throw XProcException.xiInvalidMessage(staticContext.location, message)
        }
        step.receive(port, atomic, XProcMetadata.XML)
      case _ =>
        throw XProcException.xiInvalidMessage(staticContext.location, message)
    }
  }

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (!running) {
      val portbuffer = outputBuffer.getOrElse(port, ListBuffer.empty[(Any,XProcMetadata)])
      portbuffer += Tuple2(item, metadata)
      outputBuffer.put(port, portbuffer)
      return
    }

    // Is the content type ok?
    val mtypes = step.signature.output(port, staticContext.location).contentTypes
    if (mtypes.nonEmpty) {
      // FIXME: rethrow XC0070 with a location
      if (!metadata.contentType.allowed(mtypes)) {
        throw XProcException.xdBadOutputMediaType(metadata.contentType, mtypes, staticContext.location)
      }
    }

    // Let's try to validate and normalize what just got sent out of the step.
    // If it claims to be XML, HTML, JSON, or text, we need to get it into an XDM.

    item match {
      case ex: XProcException =>
        // The only way this can happen is if we're in a catch or finally
        // and reading the error port.
        if (ex.errors.isEmpty) {
          val builder = new SaxonTreeBuilder(config)
          builder.startDocument(None)
          builder.endDocument()
          consumer.get.consume(port, new XdmNodeItemMessage(builder.result, XProcMetadata.XML, staticContext))
        } else {
          consumer.get.consume(port, new XdmNodeItemMessage(ex.errors.get, XProcMetadata.XML, staticContext))
        }
      case _ =>
        val contentType = metadata.contentType
        val sendMessage = contentType.classification match {
          case MediaType.XML => makeXmlMessage(item, metadata)
          case MediaType.HTML => makeHtmlMessage(item, metadata)
          case MediaType.JSON => makeJsonMessage(item, metadata)
          case MediaType.TEXT => makeTextMessage(item, metadata)
          case _ => makeBinaryMessage(item,metadata)
        }
        consumer.get.consume(port, sendMessage)
    }


  }

  @scala.annotation.tailrec
  private def makeXmlMessage(item: Any, metadata: XProcMetadata): Message = {
    item match {
      case value: XdmNode =>
        assertXmlDocument(value)
        val patched = S9Api.patchBaseURI(config, value, metadata.baseURI)
        new XdmNodeItemMessage(patched, metadata, staticContext)
      case value: XdmMap =>
        new XdmValueItemMessage(value, metadata, staticContext)
      case value: XdmAtomicValue =>
        val tree = new SaxonTreeBuilder(config)
        tree.startDocument(None)
        tree.addText(value.getStringValue)
        tree.endDocument()
        new XdmNodeItemMessage(tree.result, new XProcMetadata(MediaType.TEXT, metadata), staticContext)
      case value: String =>
        val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), metadata.contentType)
        val result = config.documentManager.parse(req, new ByteArrayInputStream(value.getBytes("UTF-8")))
        makeXmlMessage(result.value, metadata)
      case bytes: Array[Byte] =>
        makeXmlMessage(new ByteArrayInputStream(bytes), metadata)
      case value: InputStream =>
        val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), metadata.contentType)
        val result = config.documentManager.parse(req, value)
        makeXmlMessage(result.value, metadata)
      case value: XdmValue =>
        throw XProcException.xiNotAnXmlDocument(None)
      case _ =>
        throw new RuntimeException(s"Cannot interpret $item as ${metadata.contentType}")
    }
  }

  @scala.annotation.tailrec
  private def makeHtmlMessage(item: Any, metadata: XProcMetadata): Message = {
    item match {
      case value: XdmNode =>
        assertXmlDocument(value)
        val patched = S9Api.patchBaseURI(config, value, metadata.baseURI)
        new XdmNodeItemMessage(patched, metadata, staticContext)
      case value: String =>
        val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), metadata.contentType)
        val result = config.documentManager.parse(req, new ByteArrayInputStream(value.getBytes("UTF-8")))
        makeHtmlMessage(result.value, metadata)
      case value: Array[Byte] =>
        makeHtmlMessage(new ByteArrayInputStream(value), metadata)
      case value: InputStream =>
        val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), metadata.contentType)
        val result = config.documentManager.parse(req, value)
        makeHtmlMessage(result.value, metadata)
      case value: XdmValue =>
        throw XProcException.xiNotAnXmlDocument(None)
      case _ =>
        throw new RuntimeException(s"Cannot interpret $item as ${metadata.contentType}")
    }
  }

  @scala.annotation.tailrec
  private def makeJsonMessage(item: Any, metadata: XProcMetadata): Message = {
    item match {
      case value: XdmNode =>
        throw XProcException.xiNotJSON(None)
      case value: XdmValue =>
        new XdmValueItemMessage(value, metadata, staticContext)
      case value: String =>
        val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), metadata.contentType)
        val result = config.documentManager.parse(req, new ByteArrayInputStream(value.getBytes("UTF-8")))
        makeJsonMessage(result.value, metadata)
      case value: Array[Byte] =>
        makeJsonMessage(new ByteArrayInputStream(value), metadata)
      case value: InputStream =>
        val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), metadata.contentType)
        val result = config.documentManager.parse(req, value)
        makeJsonMessage(result.value, metadata)
      case _ =>
        throw new RuntimeException(s"Cannot interpret $item as ${metadata.contentType}")
    }
  }

  @scala.annotation.tailrec
  private def makeTextMessage(item: Any, metadata: XProcMetadata): Message = {
    item match {
      case value: XdmNode =>
        assertTextDocument(value)
        val patched = S9Api.patchBaseURI(config, value, metadata.baseURI)
        new XdmNodeItemMessage(patched, metadata, staticContext)
      case value: XdmValue =>
        value match {
          case atomic: XdmAtomicValue =>
            val t = atomic.getPrimitiveTypeName
            if (t == XProcConstants.xs_string || t == XProcConstants.xs_NCName || t == XProcConstants.xs_untypedAtomic
              || t == XProcConstants.xs_anyURI || t == XProcConstants.xs_NMTOKEN) {
              makeTextMessage(atomic.getStringValue, metadata)
            } else {
              throw XProcException.xiNotATextDocument(None)
            }
          case _ =>
            throw XProcException.xiNotATextDocument(None)
        }
      case value: String =>
        val tree = new SaxonTreeBuilder(config)
        tree.startDocument(metadata.baseURI)
        tree.addText(value)
        tree.endDocument()
        new XdmNodeItemMessage(tree.result, metadata, staticContext)
      case value: Array[Byte] =>
        makeTextMessage(new ByteArrayInputStream(value), metadata)
      case stream: InputStream =>
        val bos = new ByteArrayOutputStream()
        var totBytes = 0L
        val pagesize = 4096
        val buffer = new ByteArrayBuffer(pagesize)
        val tmp = new Array[Byte](4096)
        var length = 0
        length = stream.read(tmp)
        while (length >= 0) {
          bos.write(tmp, 0, length)
          totBytes += length
          length = stream.read(tmp)
        }
        bos.close()
        stream.close()
        makeTextMessage(bos.toString("UTF-8"), metadata)
      case _ =>
        throw new RuntimeException(s"Cannot interpret $item as ${metadata.contentType}")
    }
  }

  @scala.annotation.tailrec
  private def makeBinaryMessage(item: Any, metadata: XProcMetadata): Message = {
    item match {
      case value: BinaryNode =>
        new AnyItemMessage(value.node, value, metadata, staticContext)
      case value: String =>
        val binary = new BinaryNode(config, value.getBytes("UTF-8"))
        new AnyItemMessage(binary.node, binary, metadata, staticContext)
      case value: Array[Byte] =>
        makeBinaryMessage(new ByteArrayInputStream(value), metadata)
      case value: InputStream =>
        val binary = new BinaryNode(config, value)
        new AnyItemMessage(binary.node, binary, metadata, staticContext)
      case value: XdmNode =>
        val binary = new BinaryNode(config, value.getStringValue.getBytes("UTF-8"))
        new AnyItemMessage(binary.node, binary, metadata, staticContext)
      case value: XdmValue =>
        val binary = new BinaryNode(config, value.getUnderlyingValue.getStringValue.getBytes("UTF-8"))
        new AnyItemMessage(binary.node, binary, metadata, staticContext)
      case _ =>
        throw XProcException.xiNotBinary(None)
    }
  }

  private def assertDocument(node: XdmNode): Unit = {
    if (node.getNodeKind != XdmNodeKind.DOCUMENT) {
      throw XProcException.xiNotADocument(None)
    }
  }

  private def assertTextDocument(node: XdmNode): Unit = {
    assertDocument(node)
    var count = 0
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next()
      if (count > 0 || child.getNodeKind != XdmNodeKind.TEXT) {
        throw XProcException.xiNotATextDocument(None)
      }
      count += 1
    }
  }

  private def assertXmlDocument(node: XdmNode): Unit = {
    assertDocument(node)
    // N.B. We don't assert that documents actually be well-formed XML.
    // This is on purpose; steps can produce any XdmNode tree.
  }
}
