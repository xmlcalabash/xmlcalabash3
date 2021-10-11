package com.xmlcalabash.runtime

import com.jafpl.messages.Message
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser}
import net.sf.saxon.om.{AttributeInfo, AttributeMap, NameOfNode, NamespaceResolver}
import net.sf.saxon.s9api._
import net.sf.saxon.serialize.SerializationProperties
import net.sf.saxon.trans.XPathException

import java.net.URI
import java.util
import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{IteratorHasAsJava, SeqHasAsJava}

class ProcessMatch(config: XMLCalabashConfig,
                   processor: ProcessMatchingNodes,
                   context: StaticContext,
                   bindings: Option[Map[String,Message]]) extends SaxonTreeBuilder(config) {
  def this(runtime: XMLCalabashRuntime, processor: ProcessMatchingNodes, context: StaticContext) = {
    this(runtime.config, processor, context, None)
  }
  def this(runtime: XMLCalabashConfig, processor: ProcessMatchingNodes, context: StaticContext) = {
    this(runtime, processor, context, None)
  }
  def this(config: XMLCalabashConfig, processor: ProcessMatchingNodes, context: StaticContext, bindings: Map[String,Message]) = {
    this(config, processor, context, Some(bindings))
  }

  private val SAW_ELEMENT = 1
  private val SAW_WHITESPACE = 2
  private val SAW_TEXT = 4
  private val SAW_PI = 8
  private val SAW_COMMENT = 16

  var selector: XPathSelector = _
  var nodeCount: Integer = _
  private var saw = 0

  def process(doc: XdmNode, pattern: String): Unit = {
    selector = compilePattern(pattern)
    destination = new XdmDestination()
    val pipe = controller.makePipelineConfiguration()
    receiver = destination.getReceiver(pipe, new SerializationProperties())

    receiver.setPipelineConfiguration(pipe)
    receiver.setSystemId(doc.getBaseURI.toASCIIString)
    receiver.open()

    // If we start a match at an element, fake a document wrapper
    if (doc.getNodeKind != XdmNodeKind.DOCUMENT) {
      startDocument(doc.getBaseURI)
    }

    traverse(doc)

    if (doc.getNodeKind != XdmNodeKind.DOCUMENT) {
      endDocument()
    }

    receiver.close()
  }

  // We've already done a bunch of setup, don't do it again
  override def startDocument(baseURI: URI): Unit = {
    inDocument = true
    seenRoot = false
    receiver.startDocument(0)
  }

  def count(doc: XdmNode, pattern: String, deep: Boolean): Integer = {
    selector = compilePattern(pattern)

    nodeCount = 0

    traverse(doc, deep)

    nodeCount
  }

  def getResult: XdmNode = destination.getXdmNode

  def matches(node: XdmNode): Boolean = {
    try {
      selector.setContextItem(node)
      selector.effectiveBooleanValue()
    } catch {
      case sae: XPathException => false
      case t: Exception => throw t
    }
  }

  private def traverse(node: XdmNode): Unit = {
    val nmatch = matches(node)
    var processChildren = false

    if (!nmatch) {
      node.getNodeKind match {
        case XdmNodeKind.ELEMENT => saw |= SAW_ELEMENT
        case XdmNodeKind.TEXT =>
          if (node.getStringValue.trim == "") {
            saw |= SAW_WHITESPACE
          } else {
            saw |= SAW_TEXT
          }
        case XdmNodeKind.COMMENT => saw |= SAW_COMMENT
        case XdmNodeKind.PROCESSING_INSTRUCTION => saw |= SAW_PI
        case _ => ()
      }
    }

    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        if (nmatch) {
          processChildren = processor.startDocument(node)
          saw = 0
        } else {
          startDocument(node.getBaseURI)
        }

        if (!nmatch || processChildren) {
          traverseChildren(node)
        }

        if (nmatch) {
          processor.endDocument(node)
        } else {
          endDocument()
        }

      case XdmNodeKind.ELEMENT =>
        var allAttributes = node.getUnderlyingNode.attributes()
        val matchingAttributes = ListBuffer.empty[AttributeInfo]
        val nonMatchingAttributes = ListBuffer.empty[AttributeInfo]

        val iter = node.axisIterator(Axis.ATTRIBUTE)
        while (iter.hasNext) {
          val child = iter.next()
          val name = NameOfNode.makeName(child.getUnderlyingNode)
          val attr = allAttributes.get(name)
          if (matches(child)) {
            matchingAttributes += attr
          } else {
            nonMatchingAttributes += attr
          }
        }

        if (matchingAttributes.nonEmpty) {
          val processed = processor.attributes(node,
            AttributeMap.fromList(matchingAttributes.toList.asJava),
            AttributeMap.fromList(nonMatchingAttributes.toList.asJava))
          if (processed.isDefined) {
            allAttributes = processed.get
          }
        }

        if (nmatch) {
          processChildren = processor.startElement(node, allAttributes)
          saw = 0
        } else {
          addStartElement(node, allAttributes)
        }


        if (!nmatch || processChildren) {
          traverseChildren(node)
        }

        if (nmatch) {
          processor.endElement(node)
        } else {
          addEndElement()
        }

      case XdmNodeKind.COMMENT =>
        if (nmatch) {
          processor.comment(node)
          saw = 0
        } else {
          addComment(node.getStringValue)
        }

      case XdmNodeKind.TEXT =>
        if (nmatch) {
          processor.text(node)
          saw = 0
        } else {
          addText(node.getStringValue)
        }

      case XdmNodeKind.PROCESSING_INSTRUCTION =>
        if (nmatch) {
          processor.pi(node)
          saw = 0
        } else {
          addPI(node.getNodeName.getLocalName, node.getStringValue)
        }

      case _ => throw new UnsupportedOperationException(s"Unexpected node type: $node")
    }
  }

  private def traverse(node: XdmNode, deep: Boolean): Unit = {
    val nmatch = matches(node)

    if (nmatch) {
      nodeCount += 1
    }

    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        if (!nmatch || deep) {
          traverseDeepChildren(node, deep, Axis.CHILD)
        }
      case XdmNodeKind.ELEMENT =>
        if (!nmatch || deep) {
          traverseDeepChildren(node, deep, Axis.ATTRIBUTE)
          traverseDeepChildren(node, deep, Axis.CHILD)
        }
      case _ => ()
    }
  }

  private def traverseChildren(node: XdmNode): Unit = {
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next
      traverse(child)
    }
  }

  private def traverseDeepChildren(node: XdmNode, deep: Boolean, axis: Axis): Unit = {
    val iter = node.axisIterator(axis)
    while (iter.hasNext) {
      val child = iter.next
      traverse(child, deep)
    }
  }

  private def nsBindings(node: XdmNode): Map[String,String] = {
    var bindings = HashMap.empty[String,String]
    val nsIter = node.axisIterator(Axis.NAMESPACE)
    while (nsIter.hasNext) {
      val ns = nsIter.next
      val nodeName = ns.getNodeName
      val uri = ns.getStringValue
      if (nodeName == null) {
        // Huh?
        bindings += ("" -> uri)
      } else {
        bindings += (nodeName.getLocalName -> uri)
      }
    }
    bindings
  }

  private def compilePattern(pattern: String): XPathSelector = {
    val xcomp = config.processor.newXPathCompiler()

    if (bindings.isDefined) {
      for (varname <- bindings.get.keySet) {
        val qname = ValueParser.parseClarkName(varname)
        xcomp.declareVariable(qname)
      }
    }

    for ((prefix, uri) <- context.nsBindings) {
      xcomp.declareNamespace(prefix, uri)
    }

    try {
      val matcher = xcomp.compilePattern(pattern)
      val selector = matcher.load()

      if (bindings.isDefined) {
        for ((varname, varvalue) <- bindings.get) {
          val qname = ValueParser.parseClarkName(varname)
          varvalue match {
            case value: XdmValueItemMessage =>
              selector.setVariable(qname, value.item)
          }
        }
      }

      selector
    } catch {
      case sae: SaxonApiException =>
        sae.getCause match {
          case xpe: XPathException =>
            if (xpe.getMessage.contains("Undeclared variable")
              || xpe.getMessage.contains("Cannot find")) {
              throw XProcException.xsStaticErrorInExpression(pattern, sae.getMessage, context.location)
            } else {
              throw sae
            }
          case _ => throw sae
        }
      case other: Throwable =>
        throw other
    }
  }

  private class MatchingNamespaceResolver(ns: Map[String, String]) extends NamespaceResolver {
    override def getURIForPrefix(prefix: String, useDefault: Boolean): String = {
      if ("" == prefix && !useDefault) {
        return ""
      }

      ns(prefix)
    }

    override def iteratePrefixes(): util.Iterator[String] = {
      val p = ListBuffer.empty[String]
      for (pfx <- ns.keySet) {
        p += pfx
      }
      p.iterator.asJava
    }
  }
}
