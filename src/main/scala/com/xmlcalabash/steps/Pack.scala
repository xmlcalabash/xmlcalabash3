package com.xmlcalabash.steps

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable.ListBuffer

class Pack() extends DefaultXmlStep {
  private val _wrapper = new QName("wrapper")

  private val sources = ListBuffer.empty[XdmNode]
  private val alternates = ListBuffer.empty[XdmNode]

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.ZERO_OR_MORE, "alternate" -> PortCardinality.ZERO_OR_MORE),
    // FIXME: more media types
    Map("source" -> List("application/xml"), "alternate" -> List("application/xml"))
  )
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (port == "source") {
      sources += item.asInstanceOf[XdmNode]
    } else {
      alternates += item.asInstanceOf[XdmNode]
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val wrapName = qnameBinding(_wrapper).get

    var source = sources.headOption
    var alternate = alternates.headOption
    while (source.isDefined || alternate.isDefined) {
      val tree = new SaxonTreeBuilder(config)
      tree.startDocument(context.baseURI)
      tree.addStartElement(wrapName)

      if (source.isDefined) {
        tree.addSubtree(source.get)
      }
      if (alternate.isDefined) {
        tree.addSubtree(alternate.get)
      }

      tree.addEndElement()
      tree.endDocument()
      consumer.get.receive("result", tree.result, XProcMetadata.XML)

      if (sources.nonEmpty) {
        sources.remove(0)
      }

      if (alternates.nonEmpty) {
        alternates.remove(0)
      }

      source = sources.headOption
      alternate = alternates.headOption
    }
  }

  override def reset(): Unit = {
    super.reset()
    sources.clear()
    alternates.clear()
  }
}
