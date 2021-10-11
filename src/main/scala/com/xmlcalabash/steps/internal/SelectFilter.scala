package com.xmlcalabash.steps.internal

import com.jafpl.graph.Location
import com.jafpl.messages.Message
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.BindingSpecification
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{AnyItemMessage, XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser}
import com.xmlcalabash.runtime.params.SelectFilterParams
import com.xmlcalabash.runtime.{BinaryNode, ImplParams, StaticContext, XMLCalabashRuntime, XProcDataConsumer, XProcMetadata, XProcXPathExpression, XmlPortSpecification, XmlStep}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.{MediaType, XProcVarValue}
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode, XdmNodeKind, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Performs XPath selections on the document(s) that flow through it
  *
  * This is an internal step, it is not intended to be instantiated by pipeline authors.
  */
class SelectFilter() extends DefaultXmlStep {
  protected var allowedTypes = List.empty[MediaType]
  protected var portName: String = _
  protected var sequence = false
  private val msgBindings = mutable.HashMap.empty[String, Message]
  private val nodeMeta = mutable.HashMap.empty[Any, XProcMetadata]
  private val nodes = ListBuffer.empty[Any]
  private var select: String = _
  private var selectContext: StaticContext = _
  private var port: String = _
  private var ispec: XmlPortSpecification = _

  // ==========================================================================

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ
  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    nodes += item
    nodeMeta.put(item,metadata)
  }

  override def configure(config: XMLCalabashConfig, stepType: QName, stepName: Option[String], params: Option[ImplParams]): Unit = {
    super.configure(config, stepType, stepName, params)

    if (params.isEmpty) {
      throw XProcException.xiWrongImplParams()
    } else {
      params.get match {
        case cp: SelectFilterParams =>
          select = cp.select
          selectContext = cp.context
          port = cp.port
          ispec = cp.ispec
        case _ => throw XProcException.xiWrongImplParams()
      }
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    msgBindings.clear()
    msgBindings ++= selectContext.statics
    for ((name, binding) <- bindings) {
      msgBindings.put(name.getClarkName, new XdmValueItemMessage(binding.value, binding.meta, context))
    }

    if (nodes.isEmpty) {
      makeSelection(List())
    } else {
      for (node <- nodes) {
        val metadata = nodeMeta(node)
        val msg = node match {
          case value: XdmNode =>
            new XdmNodeItemMessage(value, metadata, selectContext)
          case value: XdmValue =>
            new XdmValueItemMessage(value, metadata, selectContext)
          case value: BinaryNode =>
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(metadata.baseURI)
            tree.endDocument()
            new AnyItemMessage(tree.result, value, metadata, selectContext)
          case _ =>
            throw XProcException.xiThisCantHappen(s"Unexpected node type ${node}", location)
        }
        makeSelection(List(msg))
      }
    }
  }

  private def makeSelection(context: List[Message]): Unit = {
    val expr = new XProcXPathExpression(selectContext, select, None, None, None)
    val exprEval = config.expressionEvaluator.newInstance()
    val result = exprEval.value(expr, context, msgBindings.toMap, None)
    val iter = result.item.iterator()
    var count = 0
    while (iter.hasNext) {
      val item = iter.next()
      count += 1

      if (!ispec.cardinality("source").get.withinBounds(count)) {
        throw XProcException.xdInputSequenceNotAllowed(port, location)
      }

      item match {
        case node: XdmNode =>
          if (node.getNodeKind == XdmNodeKind.ATTRIBUTE) {
            throw XProcException.xdInvalidSelection(select, "attribute", location)
          }
          val tree = new SaxonTreeBuilder(config)
          tree.startDocument(node.getBaseURI)
          tree.addSubtree(node)
          tree.endDocument()
          consume(tree.result, "result")
        case _ =>
          consume(item, "result")
      }
    }

    if (!ispec.cardinality("source").get.withinBounds(count)) {
      throw XProcException.xdInputSequenceNotAllowed(port, location)
    }
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
