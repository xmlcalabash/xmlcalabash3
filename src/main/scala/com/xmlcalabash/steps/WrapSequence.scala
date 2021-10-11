package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.expr.LastPositionFinder
import net.sf.saxon.om.{Item, NodeInfo}
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode, XdmValue}
import net.sf.saxon.tree.iter.ManualIterator
import net.sf.saxon.value.SequenceExtent

import scala.collection.mutable.ListBuffer

class WrapSequence extends DefaultXmlStep {
  private val _wrapper = new QName("", "wrapper")
  private val _group_adjacent = new QName("", "group-adjacent")

  private val inputs = ListBuffer.empty[XdmNode]
  private var groupAdjacent = Option.empty[String]
  private var groupAdjacentContext = Option.empty[StaticContext]

  private var wrapper: QName = _
  private var index = 1
  private val fakeLastPositionFinder = new MyLastPositionFinder()

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case xdm: XdmNode => inputs += xdm
      case _ => throw XProcException.xiWrapXML(location)
    }
  }

  override def run(staticContext: StaticContext): Unit = {
    super.run(staticContext)

    wrapper = qnameBinding(_wrapper).get
    groupAdjacent = optionalStringBinding(_group_adjacent)
    if (groupAdjacent.isDefined) {
      groupAdjacentContext = Some(bindings(_group_adjacent).context)
    }

    if (groupAdjacent.isEmpty) {
      runSimple(staticContext)
    } else {
      runAdjacent(staticContext)
    }
  }

  def runSimple(staticContext: StaticContext): Unit = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(staticContext.baseURI)

    builder.addStartElement(wrapper)
    for (item <- inputs) {
      builder.addSubtree(item)
    }
    builder.addEndElement()
    builder.endDocument()

    consumer.get.receive("result", builder.result, XProcMetadata.XML)
  }

  def runAdjacent(staticContext: StaticContext): Unit = {
    var inGroup = false
    var lastValue: XdmValue = null
    var builder: SaxonTreeBuilder = null

    index = 0
    for (item <- inputs) {
      index += 1
      val thisValue = adjacentValue(item)
      var equal = false

      if (Option(lastValue).isDefined) {
        equal = S9Api.xpathDeepEqual(config, lastValue, thisValue)

        if (equal) {
          builder.addSubtree(item)
        } else {
          if (inGroup) {
            inGroup = false
            builder.addEndElement()
            builder.endDocument()
            consumer.get.receive("result", builder.result, XProcMetadata.XML)
          }
        }
      }

      if (Option(lastValue).isEmpty || !equal) {
        lastValue = thisValue
        inGroup = true
        builder = new SaxonTreeBuilder(config)
        builder.startDocument(staticContext.baseURI)
        builder.addStartElement(wrapper)
        builder.addSubtree(item)
      }
    }

    if (inGroup) {
      inGroup = false
      builder.addEndElement()
      builder.endDocument()
      consumer.get.receive("result", builder.result, XProcMetadata.XML)
    }
  }

  private def adjacentValue(node: XdmNode): XdmValue = {
    val compiler = config.processor.newXPathCompiler()
    compiler.setBaseURI(groupAdjacentContext.get.baseURI.get)
    for ((pfx, uri) <- bindings(_group_adjacent).context.nsBindings) {
      compiler.declareNamespace(pfx, uri)
    }
    val exec = compiler.compile(groupAdjacent.get)
    val expr = exec.getUnderlyingExpression

    val dyncontext = expr.createDynamicContext()
    val context = dyncontext.getXPathContextObject

    val fakeIterator = new ManualIterator(node.getUnderlyingNode, index)
    fakeIterator.setLastPositionFinder(fakeLastPositionFinder)
    context.setCurrentIterator(fakeIterator)

    XdmValue.wrap(expr.iterate(dyncontext).materialize())
  }

  override def reset(): Unit = {
    super.reset()
    index = 1
    inputs.clear()
  }

  protected class MyLastPositionFinder extends LastPositionFinder {
    override def getLength: Int = inputs.size
  }
}
