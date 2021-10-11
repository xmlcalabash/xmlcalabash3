package com.xmlcalabash.model.xml

import java.net.URI

import com.jafpl.graph.{Location, Node}
import com.jafpl.messages.Message
import com.xmlcalabash.config.{StepSignature, XMLCalabashConfig}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{UniqueId, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.params.{StepParams, XPathBindingParams}
import com.xmlcalabash.runtime.{StaticContext, XMLCalabashRuntime, XProcLocation, XProcXPathExpression}
import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{Axis, QName, SaxonApiException, XdmNode, XdmNodeKind, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

class Artifact(val config: XMLCalabashConfig) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected[model] var _readablePorts = List.empty[Port]
  protected[model] var _graphNode: Option[Node] = None
  private var _parent: Option[Artifact] = None
  private val _children: ListBuffer[Artifact] = ListBuffer.empty[Artifact]
  protected[model] var _staticContext: XMLContext = new XMLContext(config, this)
  private var _xmlId = Option.empty[String]
  protected var _synthetic = true
  private val _uid = UniqueId.nextId
  private var _tumbleId = s"!syn_$uid"
  private var _expand_text = Option.empty[Boolean]
  protected[model] var _inScopeStatics = mutable.HashMap.empty[String, NameBinding]
  protected[model] var _inScopeDynamics = mutable.HashMap.empty[QName, NameBinding]

  protected[model] val attributes = mutable.HashMap.empty[QName, String]
  protected[model] val extensionAttributes = mutable.HashMap.empty[QName, String]

  def graphNode: Option[Node] = _graphNode

  protected[model] def expand_text: Boolean = {
    if (_expand_text.isDefined) {
      _expand_text.get
    } else {
      if (parent.isDefined) {
        parent.get.expand_text
      } else {
        true
      }
    }
  }

  protected[model] def attr(name: QName): Option[String] = {
    if (attributes.contains(name)) {
      val str = attributes(name)
      attributes.remove(name)
      Some(str)
    } else {
      None
    }
  }
  protected[xmlcalabash] def extensionAttr(name: QName): Option[String] = {
    extensionAttributes.get(name)
  }

  protected[xmlcalabash] def parent: Option[Artifact] = _parent
  protected[model] def parent_=(art: Artifact): Unit = {
    _parent = Some(art)
  }
  protected[model] def rawChildren: List[Artifact] = _children.toList

  protected[model] def allChildren: List[Artifact] = {
    _children.toList.flatMap {
      case _: Documentation => None
      case _: PipeInfo => None
      case art:Artifact => Some(art)
    }
  }

  protected[model] def children[T <: Artifact](implicit tag: ClassTag[T]): List[T] = {
    allChildren.flatMap {
      case art: T => Some(art)
      case _ => None
    }
  }

  protected[model] def findAll[T <: Artifact](implicit tag: ClassTag[T]): List[T] = {
    root.findDescendants(tag)
  }

  protected[model] def findDescendants[T <: Artifact](implicit tag: ClassTag[T]): List[T] = {
    val arts = ListBuffer.empty[T]
    for (child <- allChildren) {
      child match {
        case t: T =>
          arts += t
          arts ++= t.findDescendants(tag)
        case _ =>
          arts ++= child.findDescendants(tag)
      }
    }
    arts.toList
  }

  protected[model] def root: Artifact = {
    var root: Artifact = this
    while (root.parent.isDefined) {
      root = root.parent.get
    }
    root
  }

  protected[model] def findInScopeOption(name: QName): Option[NameBinding] = {
    findInScopeOption(this, name, this)
  }

    protected[model] def findInScopeOption(art: Artifact, name: QName, self: Artifact): Option[NameBinding] = {
      var found = Option.empty[NameBinding]

      art match {
        case _: AtomicStep =>
          () // No one can see the options of atomic steps
        case _ =>
          for (nb <- art.children[NameBinding]) {
            if ((nb ne self) && nb.name == name) {
              found = Some(nb)
            }
          }
      }

      if (found.isEmpty && art.parent.isDefined) {
        findInScopeOption(art.parent.get, name, self)
      } else {
        found
      }
  }

  protected[model] def ancestor(art: Artifact): Boolean = {
    var p: Option[Artifact] = parent
    while (p.isDefined) {
      if (p.get == art) {
        return true
      }
      p = p.get.parent
    }
    false
  }

  protected[model] def containingStep: Option[Step] = {
    var p: Option[Artifact] = Some(this)
    while (p.isDefined) {
      p.get match {
        case step: Step =>
          return Some(step)
        case _ =>
          p = p.get.parent
      }
    }
    None
  }

  protected[xmlcalabash] def location: Option[Location] = _staticContext.location
  protected[model] def staticContext: XMLContext = _staticContext
  protected[model] def staticContext_=(context: StaticContext): Unit = {
    _staticContext = new XMLContext(config, this, context.baseURI, context.nsBindings, context.location)
  }
  protected[model] def uid: Long = _uid
  protected[model] def xml_id: Option[String] = _xmlId
  protected[model] def tumble_id: String = _tumbleId
  protected[model] def synthetic: Boolean = _synthetic

  protected[model] def inScopeStatics: Map[String, Message] = {
    val statics = mutable.HashMap.empty[String,Message]
    for ((name,binding) <- _inScopeStatics) {
      if (binding.staticValue.isDefined) {
        statics.put(name, binding.staticValue.get)
      }
    }
    statics.toMap
  }

  protected[model] def addChild(art: Artifact): Unit = {
    _children += art
    art.parent = this
  }

  protected[model] def addChild(art: Artifact, before: Option[Artifact]): Unit = {
    if (before.isDefined) {
      addChild(art, before.get)
    } else {
      addChild(art)
    }
  }

  protected[model] def addChild(art: Artifact, before: Artifact): Unit = {
    var pos = 0
    var found = false
    for (child <- allChildren) {
      found = found || child == before
      if (!found) {
        pos += 1
      }
    }
    if (!found) {
      throw new RuntimeException("Insert before is not a child of the container")
    }

    _children.insert(pos, art)
    art.parent = this
  }

  protected[model] def replaceChild(art: Artifact, target: Artifact): Unit = {
    var pos = 0
    var found = false
    for (child <- rawChildren) {
      found = found || child == target
      if (!found) {
        pos += 1
      }
    }
    if (!found) {
      throw new RuntimeException("Replace target is not a child of the container")
    }

    _children.insert(pos, art)
    _children.remove(pos+1)
    art.parent = this
  }

  protected[model] def removeChild(target: Artifact): Unit = {
    var pos = 0
    var childPos = -1
    for (child <- _children) {
      if (child == target) {
        childPos = pos
      }
      pos += 1
    }
    if (childPos >= 0) {
      _children.remove(childPos)
    }
  }

  protected[model] def removeChildren(): Unit = {
    _children.clear()
  }

  protected[model] def firstChild: Option[Artifact] = allChildren.headOption

  protected[model] def firstWithInput: Option[WithInput] = {
    children[WithInput].headOption
  }

  protected[model] def firstStepChild: Option[Step] = {
    children[Step].headOption
  }

  private def tumbleId(node: XdmNode): String = {
    var count = 1
    val iter = node.axisIterator(Axis.PRECEDING_SIBLING)
    while (iter.hasNext) {
      val child = iter.next()
      if (child.getNodeKind == XdmNodeKind.ELEMENT) {
        count += 1
      }
    }
    val parent = Option(node.getParent)
    if (parent.isEmpty) {
      "!"
    } else {
      val pid = tumbleId(parent.get)
      if (pid == "!") {
        s"$pid$count"
      } else {
        s"$pid.$count"
      }
    }
  }

  protected[model] def parse(node: XdmNode): Unit = {
    _synthetic = false
    _tumbleId = tumbleId(node)
    _staticContext.baseURI = node.getBaseURI
    _staticContext.location = new XProcLocation(node)
    _staticContext.nsBindings = S9Api.inScopeNamespaces(node)

    // Parse attributes
    val aiter = node.axisIterator(Axis.ATTRIBUTE)
    while (aiter.hasNext) {
      val attr = aiter.next()
      attr.getNodeName match {
        case XProcConstants.xml_base => staticContext.baseURI = new URI(attr.getStringValue)
        case XProcConstants.xml_id => _xmlId = Some(attr.getStringValue)
        case _ =>
          if (attr.getNodeName.getNamespaceURI == "" || attr.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
            attributes.put(attr.getNodeName, attr.getStringValue)
          } else {
            extensionAttributes.put(attr.getNodeName, attr.getStringValue)
          }
      }
    }

    val ns = S9Api.inScopeNamespaces(node)
    var same = parent.isDefined
    if (parent.isDefined) {
      val inScopeNS = parent.get.staticContext.nsBindings
      for (prefix <- inScopeNS.keySet) {
        same = same && (ns.contains(prefix) && (ns(prefix) == inScopeNS(prefix)))
      }
      for (prefix <- ns.keySet) {
        same = same && (inScopeNS.contains(prefix) && (ns(prefix) == inScopeNS(prefix)))
      }
    }

    if (same) {
      staticContext.nsBindings = parent.get.staticContext.nsBindings
    } else {
      staticContext.nsBindings = ns
    }

    if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
      _expand_text = staticContext.parseBoolean(attr(XProcConstants._expand_text))
    } else {
      _expand_text = staticContext.parseBoolean(attr(XProcConstants.p_expand_text))
    }
  }


  protected[model] def declaration(stepType: QName): Option[StepSignature] = {
    // First find the decl container
    var container: Option[Artifact] = Some(this)
    while (container.isDefined) {
      container.get match {
        case decl: DeclContainer =>
          return declaration(stepType, decl)
        case _ =>
          container = container.get.parent
      }
    }
    None
  }

  protected[model] def declaration(stepType: QName, container: DeclContainer): Option[StepSignature] = {
    if (parent.isDefined) {
      parent.get.declaration(stepType, container)
    } else {
      None
    }
  }

  protected[model] def environment(): Environment = {
    // N.B. Do not cache this; the structure changes when filters are added.
    Environment.newEnvironment(this)
  }

  protected[model] def makeStructureExplicit(): Unit = {
    for (child <- allChildren) {
      child.makeStructureExplicit()
    }
  }

  protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child.validateStructure()
    }
  }

  protected[model] def makeBindingsExplicit(): Unit = {
    val env = environment()

    for (sbinding <- env.staticVariables) {
      _inScopeStatics.put(sbinding.name.getClarkName, sbinding)
    }

    for (dbinding <- env.variables) {
      _inScopeDynamics.put(dbinding.name, dbinding)
    }

    for (child <- allChildren) {
      child.makeBindingsExplicit()
    }
  }

  protected[model] def normalizeToPipes(): Unit = {
    for (child <- allChildren) {
      child.normalizeToPipes()
    }
  }

  protected[model] def addFilters(): Unit = {
    for (child <- allChildren) {
      child.addFilters()
    }
  }

  protected[model] def insertPipe(source: Port, pipe: Pipe): Unit = {
    for (child <- allChildren) {
      child.insertPipe(source, pipe)
    }
  }

  protected[model] def replumb(oldSource: Port, newSource: Port): Unit = {
    for (child <- allChildren) {
      child.replumb(oldSource, newSource)
    }
  }

  protected[model] def xpathBindingParams(): XPathBindingParams = {
    val statics = mutable.HashMap.empty[QName, XdmValue]
    for ((name,binding) <- _inScopeStatics) {
      val qname = ValueParser.parseClarkName(name)
      val smsg = binding.staticValue.get
      smsg match {
        case msg: XdmNodeItemMessage =>
          statics.put(qname, msg.item)
        case msg: XdmValueItemMessage =>
          statics.put(qname, msg.item)
        case _  =>
          throw new RuntimeException("Unexpected message type")
      }
    }
    statics.toMap
    new XPathBindingParams(statics.toMap)
  }

  protected[model] def stepParams(): StepParams = {
    new StepParams(inScopeStatics)
  }

  def computeStatically(xpathExpression: String): XdmValueItemMessage = {
    // Evaluate it; no reference to context or non-statics is allowed.
    val exprContext = staticContext.withStatics(inScopeStatics)
    val expr = new XProcXPathExpression(staticContext, xpathExpression)
    try {
      config.expressionEvaluator.newInstance().value(expr, List(), exprContext.statics, None)
    } catch {
      case sae: SaxonApiException =>
        throw XProcException.xsStaticErrorInExpression(xpathExpression, sae.getMessage, exprContext.location)
    }
  }

  def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    if (allChildren.nonEmpty) {
      if (_graphNode.isDefined) {
        for (child <- allChildren) {
          child.graphNodes(runtime, _graphNode.get)
        }
      } else {
        println("cannot graphNodes for children of " + this)
      }
    }
  }

  def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    println(s"ERROR: $this doesn't override graphEdges")
  }

  def dump(): Unit = {
    dump("", Set.empty[Artifact])
  }

  private def dump(indent: String, dumped: Set[Artifact]): Unit = {
    println(s"$indent$this")
    for (child <- allChildren) {
      child match {
        case _: Step =>
          println("")
        case _ => ()
      }
      if (dumped.contains(child)) {
        println(s"$indent$this...")
      } else {
        child.dump(s"$indent  ", dumped + child)
      }
    }
  }

  def xdump(xml: ElaboratedPipeline): Unit = {
    throw new RuntimeException(s"Don't know how to xdump $this")
  }
}
