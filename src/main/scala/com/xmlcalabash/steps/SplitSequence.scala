package com.xmlcalabash.steps

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{BinaryNode, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.expr.LastPositionFinder
import net.sf.saxon.om.{FocusIterator, Item, NodeInfo}
import net.sf.saxon.s9api.{QName, XdmItem, XdmMap, XdmNode}
import net.sf.saxon.tree.iter.{LookaheadIterator, ManualIterator}

import scala.collection.mutable.ListBuffer

class SplitSequence() extends DefaultXmlStep {
  private val _initial_only = new QName("", "initial-only")

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.ZERO_OR_MORE),
    Map("source" -> List("application/xml", "text/html"))
  )
  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("matched" -> PortCardinality.ZERO_OR_MORE, "not-matched" -> PortCardinality.ZERO_OR_MORE),
    Map("matched" -> List("application/xml", "text/html"), "not-matched" -> List("application/xml", "text/html"))
  )

  private val sources = ListBuffer.empty[Any]
  private val metas = ListBuffer.empty[XProcMetadata]

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    sources += item
    metas += metadata
  }

  override def run(staticContext: StaticContext): Unit = {
    super.run(staticContext)

    val initialOnly = bindings(_initial_only).value.getUnderlyingValue.effectiveBooleanValue()
    val testExpr = bindings(XProcConstants._test).value.getUnderlyingValue.getStringValue
    var more = true
    var index = 0

    val fakeLastPositionFinder = new MyLastPositionFinder()

    for (source <- sources) {
      val meta = metas(index)
      index += 1
      if (more) {
        val compiler = config.processor.newXPathCompiler()
        if (staticContext.baseURI.isDefined) {
          compiler.setBaseURI(staticContext.baseURI.get)
        }
        for ((pfx, uri) <- bindings(XProcConstants._test).context.nsBindings) {
          compiler.declareNamespace(pfx, uri)
        }
        val exec = compiler.compile(testExpr)
        val expr = exec.getUnderlyingExpression

        val dyncontext = expr.createDynamicContext()
        val context = dyncontext.getXPathContextObject

        source match {
          case item: XdmItem =>
            val fakeIterator = new ManualIterator(item.getUnderlyingValue, index)
            fakeIterator.setLastPositionFinder(fakeLastPositionFinder)
            context.setCurrentIterator(fakeIterator)
          case bin: BinaryNode =>
            val fakeIterator = new ManualIterator(bin.node.getUnderlyingValue, index)
            fakeIterator.setLastPositionFinder(fakeLastPositionFinder)
            context.setCurrentIterator(fakeIterator)
          case _ =>
            logger.debug(s"p:split-sequence saw a ${source} go by")
        }

        val value = expr.evaluate(dyncontext)

        val matches = value.size() match {
          case 0 => false
          case 1 => value.get(0).effectiveBooleanValue()
          case _ => true
        }

        if (matches) {
          consumer.get.receive("matched", source, meta)
        } else {
          consumer.get.receive("not-matched", source, meta)
          more = !initialOnly
        }
      } else {
        consumer.get.receive("not-matched", source, meta)
      }
    }
  }

  protected class Document(val source: XdmNode, val meta: XProcMetadata) {
    // nop
  }

  protected class MyLastPositionFinder extends LastPositionFinder {
    override def getLength: Int = sources.size
  }
}
