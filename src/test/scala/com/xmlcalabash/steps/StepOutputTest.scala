package com.xmlcalabash.steps

import java.io.ByteArrayInputStream

import com.xmlcalabash.model.xml.XMLContext
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import com.xmlcalabash.util.TypeUtils.castAsXml
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmMap, XdmValue}
import net.sf.saxon.value.AtomicValue

class StepOutputTest() extends DefaultXmlStep {
  private val _content_type = XProcConstants._content_type
  private val _result_type = new QName("", "result-type")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def run(context: StaticContext): Unit = {
    val contentType = MediaType.parse(stringBinding(_content_type))
    val resultType = stringBinding(_result_type)
    val metadata = new XProcMetadata(contentType, Map.empty[QName,XdmValue])

    contentType.classification match {
      case MediaType.JSON =>
        resultType match {
          case "text-node" =>
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(None)
            tree.addText("{\"result-type\": \"text-node\"}")
            tree.endDocument()
            consumer.get.receive("result", tree.result, metadata)
          case "element-node" =>
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(None)
            tree.addStartElement(new QName("doc"))
            tree.addText("{\"result-type\": \"element-node\"}")
            tree.addEndElement()
            tree.endDocument()
            consumer.get.receive("result", tree.result, metadata)
          case "string" =>
            val s = "{\"result-type\": \"string\"}"
            consumer.get.receive("result", s, metadata)
          case "bytes" =>
            val s = "{\"result-type\": \"bytes\"}"
            consumer.get.receive("result", s.getBytes("UTF-8"), metadata)
          case "stream" =>
            val s = "{\"result-type\": \"stream\"}"
            val stream = new ByteArrayInputStream(s.getBytes("UTF-8"))
            consumer.get.receive("result", stream, metadata)
          case "map-value" =>
            var map = new XdmMap()
            map = map.put(new XdmAtomicValue("result-type"), new XdmAtomicValue("map-value"))
            consumer.get.receive("result", map, metadata)
          case "string-value" =>
            consumer.get.receive("result", new XdmAtomicValue("string value."), metadata)
          case _ =>
            throw new RuntimeException(s"Unexpected result type ${contentType.toString} / $resultType")
        }
      case MediaType.HTML =>
        resultType match {
          case "text-node" =>
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(None)
            tree.addText("Some text.")
            tree.endDocument()
            consumer.get.receive("result", tree.result, metadata)
          case "element-node" =>
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(None)
            tree.addStartElement(new QName("p"))
            tree.addText("Some element.")
            tree.addEndElement()
            tree.endDocument()
            consumer.get.receive("result", tree.result, metadata)
          case "string" =>
            val s = "<p>Some string.</p>"
            consumer.get.receive("result", s, metadata)
          case "bytes" =>
            val s = "<p>Some bytes.</p>"
            consumer.get.receive("result", s.getBytes("UTF-8"), metadata)
          case "stream" =>
            val s = "<p>Some stream.</p>"
            val stream = new ByteArrayInputStream(s.getBytes("UTF-8"))
            consumer.get.receive("result", stream, metadata)
          case "map-value" =>
            var map = new XdmMap()
            map = map.put(new XdmAtomicValue("result-type"), new XdmAtomicValue("value"))
            consumer.get.receive("result", map, metadata)
          case "string-value" =>
            consumer.get.receive("result", new XdmAtomicValue("string value."), metadata)
          case _ =>
            throw new RuntimeException(s"Unexpected result type ${contentType.toString} / $resultType")
        }
      case MediaType.OCTET_STREAM =>
        resultType match {
          case "text-node" =>
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(None)
            tree.addText("Some text.")
            tree.endDocument()
            consumer.get.receive("result", tree.result, metadata)
          case "element-node" =>
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(None)
            tree.addStartElement(new QName("doc"))
            tree.addText("Some element.")
            tree.addEndElement()
            tree.endDocument()
            consumer.get.receive("result", tree.result, metadata)
          case "string" =>
            val s = "Some string."
            consumer.get.receive("result", s, metadata)
          case "bytes" =>
            val s = "Some bytes."
            consumer.get.receive("result", s.getBytes("UTF-8"), metadata)
          case "stream" =>
            val s = "Some stream."
            val stream = new ByteArrayInputStream(s.getBytes("UTF-8"))
            consumer.get.receive("result", stream, metadata)
          case "map-value" =>
            var map = new XdmMap()
            map = map.put(new XdmAtomicValue("result-type"), new XdmAtomicValue("value"))
            consumer.get.receive("result", map, metadata)
          case "string-value" =>
            consumer.get.receive("result", new XdmAtomicValue("string value."), metadata)
          case _ =>
            throw new RuntimeException(s"Unexpected result type ${contentType.toString} / $resultType")
        }
      case MediaType.TEXT =>
        resultType match {
          case "text-node" =>
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(None)
            tree.addText("Some text.")
            tree.endDocument()
            consumer.get.receive("result", tree.result, metadata)
          case "element-node" =>
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(None)
            tree.addStartElement(new QName("doc"))
            tree.addText("Some element.")
            tree.addEndElement()
            tree.endDocument()
            consumer.get.receive("result", tree.result, metadata)
          case "string" =>
            val s = "Some string."
            consumer.get.receive("result", s, metadata)
          case "bytes" =>
            val s = "Some bytes."
            consumer.get.receive("result", s.getBytes("UTF-8"), metadata)
          case "stream" =>
            val s = "Some stream."
            val stream = new ByteArrayInputStream(s.getBytes("UTF-8"))
            consumer.get.receive("result", stream, metadata)
          case "map-value" =>
            var map = new XdmMap()
            map = map.put(new XdmAtomicValue("result-type"), new XdmAtomicValue("value"))
            consumer.get.receive("result", map, metadata)
          case "string-value" =>
            consumer.get.receive("result", new XdmAtomicValue("Some string value."), metadata)
          case _ =>
            throw new RuntimeException(s"Unexpected result type ${contentType.toString} / $resultType")
        }
      case _ =>
        resultType match {
          case "text-node" =>
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(None)
            tree.addText("Some text.")
            tree.endDocument()
            consumer.get.receive("result", tree.result, metadata)
          case "element-node" =>
            val tree = new SaxonTreeBuilder(config)
            tree.startDocument(None)
            tree.addStartElement(new QName("doc"))
            tree.addText("Some element.")
            tree.addEndElement()
            tree.endDocument()
            consumer.get.receive("result", tree.result, metadata)
          case "string" =>
            val s = "<doc>Some string.</doc>"
            consumer.get.receive("result", s, metadata)
          case "bytes" =>
            val s = "<doc>Some bytes.</doc>"
            consumer.get.receive("result", s.getBytes("UTF-8"), metadata)
          case "stream" =>
            val s = "<doc>Some stream.</doc>"
            val stream = new ByteArrayInputStream(s.getBytes("UTF-8"))
            consumer.get.receive("result", stream, metadata)
          case "map-value" =>
            var map = new XdmMap()
            map = map.put(new XdmAtomicValue("result-type"), new XdmAtomicValue("value"))
            consumer.get.receive("result", map, metadata)
          case "string-value" =>
            consumer.get.receive("result", new XdmAtomicValue("string value."), metadata)
          case _ =>
            throw new RuntimeException(s"Unexpected result type ${contentType.toString} / $resultType")
        }
    }
  }
}
