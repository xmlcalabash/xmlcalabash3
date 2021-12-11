package com.xmlcalabash.steps.internal

import com.jafpl.graph.Location
import com.jafpl.messages.Message
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.BindingSpecification
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{AnyItemMessage, XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser}
import com.xmlcalabash.runtime.params.SelectFilterParams
import com.xmlcalabash.runtime.{BinaryNode, ImplParams, StaticContext, XMLCalabashRuntime, XProcDataConsumer, XProcMetadata, XProcXPathExpression, XmlPortSpecification, XmlStep}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.{MediaType, MinimalStaticContext, XProcVarValue}
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
  private var selectContext: MinimalStaticContext = _
  private var port: String = _

  // ==========================================================================

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ
  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    nodes += item
    nodeMeta.put(item,metadata)
  }

  override def configure(config: XMLCalabash, stepType: QName, stepName: Option[String], params: Option[ImplParams]): Unit = {
    super.configure(config, stepType, stepName, params)

    if (params.isEmpty) {
      throw XProcException.xiWrongImplParams()
    } else {
      params.get match {
        case cp: SelectFilterParams =>
          select = cp.select
          selectContext = cp.context
          port = cp.port
          sequence = cp.sequence
        case _ => throw XProcException.xiWrongImplParams()
      }
    }
  }

  override def run(context: MinimalStaticContext): Unit = {
    super.run(context)

    for ((name, value) <- selectContext.inscopeConstants) {
      msgBindings.put(name.getClarkName, value.constantValue.get)
    }
    for ((name, binding) <- bindings) {
      msgBindings.put(name.getClarkName, new XdmValueItemMessage(binding.value, binding.meta, context))
    }

    val items = ListBuffer.empty[Tuple2[Any, XProcMetadata]]
    for (node <- nodes) {
      items += Tuple2(node, nodeMeta(node))
    }

    val xpselector = new XPathSelector(config.config, items.toList, select, context, msgBindings.toMap)
    val results = xpselector.select()

    if (results.length != 1 && !sequence) {
      throw XProcException.xdInputSequenceNotAllowed(port, None)
    }

    for (result <- results) {
      result match {
        case node: XdmNode =>
          consume(node, "result")
        case value: XdmValue =>
          consume(value, "result")
        case _ =>
          throw XProcException.xiThisCantHappen("XPathSelector returned something that wasn't an XdmValue?")
      }
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
