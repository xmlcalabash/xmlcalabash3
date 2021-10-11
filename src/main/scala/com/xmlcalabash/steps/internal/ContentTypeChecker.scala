package com.xmlcalabash.steps.internal

import com.jafpl.graph.Location
import com.jafpl.messages.Message
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.BindingSpecification
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.params.ContentTypeCheckerParams
import com.xmlcalabash.runtime.{ImplParams, NameValueBinding, StaticContext, XMLCalabashRuntime, XProcDataConsumer, XProcMetadata, XProcXPathExpression, XmlPortSpecification, XmlStep}
import com.xmlcalabash.util.{MediaType, S9Api, XProcVarValue}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmNode, XdmNodeKind, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Checks the content type and cardinality of documents that flow through it.
  *
  * This is an internal step, it is not intended to be instantiated by pipeline authors.
  * It's also slightly misnamed as it checks both the content type and cardinality of
  * the documents that flow through it. It also performs selection.
  *
  */
class ContentTypeChecker() extends XmlStep {
  private var _location = Option.empty[Location]
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[XProcDataConsumer] = None
  protected var config: XMLCalabashRuntime = _
  protected val bindings = mutable.HashMap.empty[String,Message]
  private val nodeMeta = mutable.HashMap.empty[XdmNode, XProcMetadata]
  private val nodes = ListBuffer.empty[XdmNode]
  protected var allowedTypes = List.empty[MediaType]
  protected var errCode = XProcException.xd0038
  protected var select = Option.empty[String]
  protected var selectContext: StaticContext = _
  protected var portName: String = _
  protected var sequence = false
  protected var inputPort = false

  def location: Option[Location] = _location

  // ==========================================================================

  override def setLocation(location: Location): Unit = {
    _location = Some(location)
  }

  override def inputSpec: XmlPortSpecification = {
    if (sequence) {
      XmlPortSpecification.ANYSOURCESEQ
    } else {
      XmlPortSpecification.ANYSOURCE
    }
  }

  override def outputSpec: XmlPortSpecification = {
    // This is just for completeness; in practice, a cardinality error will be caught on input
    if (sequence) {
      XmlPortSpecification.ANYRESULTSEQ
    } else {
      XmlPortSpecification.ANYRESULT
    }
  }

  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receiveBinding(variable: NameValueBinding): Unit = {
    variable.value match {
      case node: XdmNode =>
        bindings.put(variable.name.getClarkName, new XdmNodeItemMessage(node, XProcMetadata.xml(node), variable.context))
      case item: XdmItem =>
        bindings.put(variable.name.getClarkName, new XdmValueItemMessage(item, XProcMetadata.JSON, variable.context))
    }
  }

  override def setConsumer(consumer: XProcDataConsumer): Unit = {
    this.consumer = Some(consumer)
  }

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    if (allowedTypes.nonEmpty) {
      val allowed = meta.contentType.allowed(allowedTypes)
      if (!allowed) {
        // Hack
        if (errCode == XProcException.xd0072) {
          throw XProcException.xdBadViewportInput(meta.contentType, location)
        } else {
          if (inputPort) {
            throw XProcException.xdBadInputMediaType(meta.contentType, allowedTypes, location)
          } else {
            throw XProcException.xdBadOutputMediaType(meta.contentType, allowedTypes, location)
          }
        }
      }
    }

    if (select.isDefined) {
      item match {
        case node: XdmNode =>
          nodes += node
          nodeMeta.put(node, meta)
        case _ =>
          throw new RuntimeException("Cannot filter non-XML inputs")
      }
    } else {
      consumer.get.receive("result", item, meta)
    }
  }

  override def configure(config: XMLCalabashConfig, stepType: QName, stepName: Option[String], params: Option[ImplParams]): Unit = {
    if (params.isEmpty) {
      throw XProcException.xiWrongImplParams()
    } else {
      params.get match {
        case cp: ContentTypeCheckerParams =>
          allowedTypes = cp.contentTypes
          portName = cp.port
          sequence = cp.sequence
          selectContext = cp.context
          errCode = cp.errCode
          select = cp.select
          inputPort = cp.inputPort
        case _ => throw XProcException.xiWrongImplParams()
      }
    }
  }

  override def initialize(config: RuntimeConfiguration): Unit = {
    config match {
      case xmlCalabash: XMLCalabashRuntime => this.config = xmlCalabash
      case _ => throw XProcException.xiNotXMLCalabash()
    }
  }

  override def run(context: StaticContext): Unit = {
    // FIXME: do whatever logging DefaultXmlStep does.

    for ((name, message) <- selectContext.statics) {
      if (!bindings.contains(name)) {
        bindings.put(name, message)
      }
    }

    for (node <- nodes) {
      val metadata = nodeMeta(node)
      val exprEval = config.expressionEvaluator.newInstance()
      val expr = new XProcXPathExpression(selectContext, select.get, None, None, None)
      val msg = new XdmNodeItemMessage(node, metadata, selectContext)
      val result = exprEval.value(expr, List(msg), bindings.toMap, None)
      val xdmvalue = result.item
      val iter = xdmvalue.iterator()
      while (iter.hasNext) {
        val item = iter.next()
        item match {
          case node: XdmNode =>
            if (node.getNodeKind == XdmNodeKind.ATTRIBUTE) {
              throw XProcException.xdInvalidSelection(select.get, "attribute", location)
            }
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(node.getBaseURI)
            tree.addSubtree(node)
            tree.endDocument()
            consumer.get.receive("result", tree.result, XProcMetadata.xml(node))
          case value: XdmItem =>
            consumer.get.receive("result", value, XProcMetadata.JSON)
          case _ =>
            throw XProcException.xiThisCantHappen(s"Content type checker didn't expect ${item}", location)
        }
      }
    }
  }

  override def reset(): Unit = {
    // nop
  }

  override def abort(): Unit = {
    // nop
  }

  override def stop(): Unit = {
    // nop
  }

  override def toString: String = {
    val defStr = super.toString
    if (defStr.startsWith("com.xmlcalabash.steps")) {
      val objstr = ".*\\.([^\\.]+)@[0-9a-f]+$".r
      defStr match {
        case objstr(name) => name
        case _ => defStr
      }
    } else {
      defStr
    }
  }
}
