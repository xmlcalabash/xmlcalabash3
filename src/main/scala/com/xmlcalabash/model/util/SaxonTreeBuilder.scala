package com.xmlcalabash.model.util

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.{DefaultLocation, S9Api, SysIdLocation, VoidLocation}
import net.sf.saxon.`type`.{SchemaType, Untyped}
import net.sf.saxon.event.{NamespaceReducer, Receiver}
import net.sf.saxon.expr.instruct.Executable
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap, FingerprintedQName, NameOfNode, NamePool, NamespaceBinding, NamespaceMap, NodeName}
import net.sf.saxon.s9api.{Axis, Location, QName, XdmDestination, XdmNode, XdmNodeKind, XdmValue}
import net.sf.saxon.serialize.SerializationProperties
import net.sf.saxon.trans.XPathException
import net.sf.saxon.{Configuration, Controller}

import java.net.URI
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{BufferHasAsJava, CollectionHasAsScala}

/* N.B. There's a fundamental problem in here somewhere. In order to preserve base URIs correctly
   when, for example, @xml:base attributes have been deleted. The tree walker has to reset the
   systemIdentifier in the receiver several times. This must have something to do with getting
   the right base URIs on the constructed nodes.

   Conversely, in the case where, for example, the file is coming from a p:http-request, the URI
   of the document entity received over the net is supposed to be the base URI of the document.
   But this "resetting" that takes place undoes the value set on the document node. I'm not sure
   how.

   So there's a hacked compromise in here: if the "overrideBaseURI" is the empty string, we ignore
   it. That seems to cover both cases.

   But I am not very confident.
 */

object SaxonTreeBuilder {
  def emptyTree(config: XMLCalabashConfig, baseURI: Option[URI]): XdmNode = {
    val tree = new SaxonTreeBuilder(config)
    tree.startDocument(baseURI)
    tree.endDocument()
    tree.result
  }
  def emptyTree(config: XMLCalabashConfig): XdmNode = {
    val tree = new SaxonTreeBuilder(config)
    tree.startDocument(None)
    tree.endDocument()
    tree.result
  }
}

class SaxonTreeBuilder(runtime: XMLCalabashConfig) {
  protected val config: Configuration = runtime.processor.getUnderlyingConfiguration
  protected val pool: NamePool = config.getNamePool
  protected val controller: Controller = new Controller(config)

  protected var exec: Executable = _
  protected var destination: XdmDestination = _
  protected var receiver: Receiver = _
  private var _inDocument = false
  protected var seenRoot = false
  private val emptyAttributeMap = EmptyAttributeMap.getInstance()
  private var _location = Option.empty[Location]

  def this(runtime: XMLCalabashRuntime) = {
    this(runtime.config)
  }

  def location: Option[Location] = _location
  def location_=(loc: Location): Unit = {
    _location = Some(loc)
  }

  def result: XdmNode = destination.getXdmNode

  def inDocument: Boolean = _inDocument
  protected def inDocument_=(in: Boolean): Unit = {
    _inDocument = in
  }

  def startDocument(baseURI: URI): Unit = {
    startDocument(Option(baseURI))
  }

  def startDocument(baseURI: Option[URI]): Unit = {
    _inDocument = true
    seenRoot  = false
    try {
      destination = new XdmDestination()
      val pipe = controller.makePipelineConfiguration()
      // Make sure line numbers get preserved
      pipe.getConfiguration.setLineNumbering(true);
      receiver = destination.getReceiver(pipe, new SerializationProperties())
      receiver = new NamespaceReducer(receiver)

      receiver.setPipelineConfiguration(pipe)

      if (baseURI.isDefined) {
        receiver.setSystemId(baseURI.get.toASCIIString)
      } else {
        receiver.setSystemId("http://example.com/")
      }

      receiver.open()
      receiver.startDocument(0)
    } catch {
      case t: Throwable => throw t
    }
  }

  def endDocument(): Unit = {
    try {
      receiver.setSystemId("http://norman-was-here.com/")
      receiver.endDocument()
      receiver.close()
    } catch {
      case t: Throwable => throw t
    }
  }

  def addSubtree(node: XdmNode): Unit = {
    try {
      receiver.append(node.getUnderlyingNode)
    } catch {
      case _: UnsupportedOperationException =>
        // do it the hard way
        node.getNodeKind match {
          case XdmNodeKind.DOCUMENT =>
            writeChildren(node)
          case XdmNodeKind.ELEMENT =>
            addStartElement(node)
            writeChildren(node)
            addEndElement()
          case XdmNodeKind.COMMENT =>
            addComment(node.getStringValue)
          case XdmNodeKind.TEXT =>
            addText(node.getStringValue)
          case XdmNodeKind.PROCESSING_INSTRUCTION =>
            addPI(node.getNodeName.getLocalName, node.getStringValue)
          case _ =>
            throw new ModelException(ExceptionCode.BADTREENODE, List(node.getNodeKind.toString, node.getNodeName.toString), node)
        }
      case xpe: XPathException =>
        // FIXME: wrap in xprocexception
        throw new RuntimeException(xpe)
    }
  }

  def addValues(values: XdmValue): Unit = {
    addText(S9Api.valuesToString(values))
  }

  protected def writeChildren(node: XdmNode): Unit = {
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      addSubtree(iter.next())
    }
  }

  def addStartElement(nodeName: QName): Unit = {
    addStartElement(nodeName, EmptyAttributeMap.getInstance())
  }

  def addStartElement(node: XdmNode): Unit = {
    addStartElement(node, node.getNodeName, node.getBaseURI)
  }

  def addStartElement(node: XdmNode, overrideBaseURI: URI): Unit = {
    addStartElement(node, node.getNodeName, overrideBaseURI)
  }

  def addStartElement(node: XdmNode, newName: QName): Unit = {
    addStartElement(node, newName, node.getBaseURI)
  }

  def addStartElement(node: XdmNode, newName: QName, overrideBaseURI: URI): Unit = {
    val attrs = node.getUnderlyingNode.attributes()
    addStartElement(node, newName, overrideBaseURI, attrs)
  }

  def addStartElement(node: XdmNode, attrs: AttributeMap): Unit = {
    val inode = node.getUnderlyingNode

    val baseURI = try {
      node.getBaseURI
    } catch {
      case _: IllegalStateException =>
        throw XProcException.xdInvalidURI(node.getUnderlyingNode.getBaseURI, None)
      case ex: Throwable =>
        throw ex
    }

    addStartElement(NameOfNode.makeName(inode), attrs, inode.getSchemaType, inode.getAllNamespaces, baseURI)
  }

  def addStartElement(node: XdmNode, newName: QName, overrideBaseURI: URI, attrs: AttributeMap): Unit = {
    val inode = node.getUnderlyingNode

    var inscopeNS = if (seenRoot) {
      val nslist = ListBuffer.empty[NamespaceBinding]
      for (binding <- inode.getDeclaredNamespaces(null)) {
        nslist += binding
      }
      new NamespaceMap(nslist.asJava)
    } else {
      inode.getAllNamespaces
    }

    // If the newName has no prefix, then make sure we don't pass along some other
    // binding for the default namespace...
    if (newName.getPrefix == "" && inscopeNS.getDefaultNamespace != "") {
      inscopeNS = inscopeNS.remove("")
    }

    // Hack. See comment at top of file
    if (overrideBaseURI.toASCIIString != "") {
      receiver.setSystemId(overrideBaseURI.toASCIIString)
    }

    val newNameOfNode = new FingerprintedQName(newName.getPrefix, newName.getNamespaceURI, newName.getLocalName)
    addStartElement(newNameOfNode, attrs, inode.getSchemaType, inscopeNS)
  }

  def addStartElement(newName: QName, attrs: AttributeMap): Unit = {
    var nsmap = NamespaceMap.emptyMap()
    if (newName.getNamespaceURI != "") {
      nsmap = nsmap.put(newName.getPrefix, newName.getNamespaceURI)
    }
    addStartElement(newName, attrs, nsmap)
  }

  def addStartElement(newName: QName, attrs: AttributeMap, nsmap: NamespaceMap): Unit = {
    val elemName = new FingerprintedQName(newName.getPrefix, newName.getNamespaceURI, newName.getLocalName)
    addStartElement(elemName, attrs, Untyped.INSTANCE, nsmap)
  }

  def addStartElement(elemName: NodeName, typeCode: SchemaType): Unit = {
    addStartElement(elemName, emptyAttributeMap, typeCode, NamespaceMap.emptyMap())
  }

  def addStartElement(elemName: NodeName, typeCode: SchemaType, nsmap: NamespaceMap): Unit = {
    addStartElement(elemName, emptyAttributeMap, typeCode, nsmap)
  }

  def addStartElement(elemName: NodeName, attrs: AttributeMap, typeCode: SchemaType, nsmap: NamespaceMap, overrideBaseURI: URI): Unit = {
    // Hack. See comment at top of file
    if (overrideBaseURI != null && overrideBaseURI.toASCIIString != "") {
      receiver.setSystemId(overrideBaseURI.toASCIIString)
    }
    addStartElement(elemName, attrs, typeCode, nsmap)
  }

  def addStartElement(elemName: NodeName, attrs: AttributeMap, typeCode: SchemaType, nsmap: NamespaceMap): Unit = {
    // Sort out the namespaces...
    var newmap = updateMap(nsmap, elemName.getPrefix, elemName.getURI)
    // FIXME: this should be iterable?
    for (attr <- attrs.asList.asScala) {
      if (attr.getNodeName.getURI != null && attr.getNodeName.getURI != "") {
        newmap = updateMap(newmap, attr.getNodeName.getPrefix, attr.getNodeName.getURI)
      }
    }

    val loc = if (_location.isDefined) {
      _location.get
    } else if (receiver.getSystemId == null) {
      VoidLocation.instance()
    } else {
      new SysIdLocation(receiver.getSystemId)
    }

    try {
      receiver.startElement(elemName, typeCode, attrs, nsmap, loc, 0)
      _location = None
    } catch {
      case e: XPathException =>
        // FIXME: some sort of XProcException
        throw new RuntimeException(e)
    }
  }

  private def updateMap(nsmap: NamespaceMap, prefix: String, uri: String): NamespaceMap = {
    if (uri == null || "" == uri) {
      return nsmap
    }

    if (prefix == null || "" == prefix) {
      if (!(uri == nsmap.getDefaultNamespace)) {
        return nsmap.put("", uri)
      }
    }

    val curNS = nsmap.getURI(prefix)
    if (curNS == null) {
      return nsmap.put(prefix, uri)
    } else if (curNS == uri) {
      return nsmap
    }

    // FIXME: runtime exception should be some form of xproc exception?
    throw new RuntimeException("Cannot add " + prefix + " to namespace map with URI " + uri)
  }

  def addEndElement(): Unit = {
    receiver.endElement()
  }

  def addComment(comment: String): Unit = {
    val loc = new DefaultLocation(receiver.getSystemId)
    receiver.comment(comment, loc, 0)
  }

  def addText(text: String): Unit = {
    val loc = new DefaultLocation(receiver.getSystemId)
    receiver.characters(text, loc, 0)
  }

  def addPI(target: String, data: String, baseURI: String): Unit = {
    val loc = new DefaultLocation(baseURI)
    receiver.processingInstruction(target, data, loc, 0)
  }

  def addPI(target: String, data: String): Unit = {
    addPI(target, data, receiver.getSystemId)
  }
}
