package com.xmlcalabash.model.xxml

import com.jafpl.graph.Location
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, UniqueId, XProcConstants}
import com.xmlcalabash.util.{TypeUtils, URIUtils}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap}
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.reflect.ClassTag

abstract class XArtifact(val config: XMLCalabash) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var _synthetic = false
  protected var _syntheticName = Option.empty[QName]
  private var _tumble_id = Option.empty[String]

  private var _staticContext: XArtifactContext = _
  private var _xml_id = Option.empty[String]
  private val _exceptions = ListBuffer.empty[Exception]
  private val _attributes = mutable.HashMap.empty[QName, String]
  private var _parent = Option.empty[XArtifact]
  private val _children = ListBuffer.empty[XArtifact]

  def synthetic: Boolean = _synthetic
  protected[xxml] def synthetic_=(synth: Boolean): Unit = {
    _synthetic = synth
  }
  def syntheticName: Option[QName] = _syntheticName
  protected[xxml] def syntheticName_=(name: QName): Unit = {
    _syntheticName = Some(name)
  }

  def nodeName: QName = {
    if (_synthetic) {
      if (_syntheticName.isDefined) {
        _syntheticName.get
      } else {
        XProcConstants._synthetic
      }
    } else {
      _staticContext.nodeName
    }
  }
  def location: Option[Location] = _staticContext.location
  def staticContext: XArtifactContext = _staticContext
  protected[xxml] def staticContext_=(context: XArtifactContext): Unit = {
    _staticContext = context
  }
  protected[xxml] def tumble_id: String = {
    if (_tumble_id.isEmpty) {
      _tumble_id = Some(s"!syn_${UniqueId.nextId}")
    }
    _tumble_id.get
  }
  protected[xxml] def tumble_id_=(id: String): Unit = {
    _tumble_id = Some(id)
  }

  def error(ex: Exception): Unit = {
    ex match {
      case ex: XProcException =>
        if (location.isDefined) {
          _exceptions += ex.withLocation(location.get)
        } else {
          _exceptions += ex
        }
      case _ =>
        _exceptions += ex
    }
    // FIXME:
    throw ex
  }

  def exceptions: List[Exception] = {
    val exlist = ListBuffer.empty[Exception] ++ _exceptions
    for (child <- allChildren) {
      exlist ++= child.exceptions
    }
    exlist.toList
  }

  def parent: Option[XArtifact] = _parent
  protected[xxml] def parent_=(parent: XArtifact): Unit = {
    _parent = Some(parent)
  }

  def root: XArtifact = {
    var p: Option[XArtifact] = Some(this)
    while (p.get.parent.isDefined) {
      p = p.get.parent
    }
    p.get
  }

  def attributes: Map[QName,String] = _attributes.toMap

  protected[xxml] def ancestor[T <: XArtifact](implicit tag: ClassTag[T]): Option[T] = {
    this match {
      case art: T => Some(art)
      case _ =>
        if (parent.isDefined) {
          parent.get.ancestor[T]
        } else {
          None
        }
    }
  }

  def allChildren: List[XArtifact] = _children.toList
  protected[xxml] def allChildren_=(children: List[XArtifact]): Unit = {
    _children.clear()
    for (child <- children) {
      addChild(child)
    }
  }

  protected[xmlcalabash] def children[T <: XArtifact](implicit tag: ClassTag[T]): List[T] = {
    allChildren.flatMap {
      case art: T => Some(art)
      case _ => None
    }
  }

  protected[xxml] def addChild(child: XArtifact): Unit = {
    child.parent = this
    _children += child
  }

  private def findChild(find: XArtifact): Int = {
    var idx = 0
    var found = false
    for (child <- allChildren) {
      if (found || (child eq find)) {
        found = true
      } else {
        idx += 1
      }
    }
    idx
  }

  protected[xxml] def removeChild(remove: XArtifact): Unit = {
    _children.remove(findChild(remove))
  }

  protected[xxml] def replaceChild(remove: XArtifact, add: XArtifact): Unit = {
    val idx = findChild(remove)
    _children.remove(idx)
    _children.insert(idx, add)
  }

  protected[xxml] def insertBefore(insert: XArtifact, before: XArtifact): Unit = {
    val idx = findChild(before)
    insert.parent = this
    _children.insert(idx, insert)
  }

  def attr(name: QName): Option[String] = {
    if (_attributes.contains(name)) {
      val value = _attributes(name)
      _attributes.remove(name)
      Some(value)
    } else {
      None
    }
  }

  def parse(node: XdmNode, children: List[XArtifact]): Unit = {
    _staticContext = new XArtifactContext(this, node)
    _tumble_id = Some(tumbleId(node))

    if (!synthetic) {
      if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
        for (attr <- node.axisIterator(Axis.ATTRIBUTE).asScala) {
          if (attr.getNodeName != XProcConstants._use_when) {
            _attributes.put(attr.getNodeName, attr.getStringValue)
          }
          if (attr.getNodeName == XProcConstants._expand_text) {
            if (attr.getStringValue != "true" && attr.getStringValue != "false") {
              error(XProcException.xsInvalidExpandText(attr.getNodeName, attr.getStringValue, location))
            }
          }
        }

      } else {
        for (attr <- node.axisIterator(Axis.ATTRIBUTE).asScala) {
          if (attr.getNodeName != XProcConstants.p_use_when) {
            _attributes.put(attr.getNodeName, attr.getStringValue)
          }
          if (attr.getNodeName == XProcConstants.p_expand_text) {
            if (attr.getStringValue != "true" && attr.getStringValue != "false") {
              error(XProcException.xsInvalidExpandText(attr.getNodeName, attr.getStringValue, location))
            }
          }
        }
      }
    }

    for (child <- children) {
      child.parent = this
    }

    _children ++= children
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

  protected def parsePipeAttribute(parent: XArtifact, pipe: String): List[XPipe] = {
    val pipes = ListBuffer.empty[XPipe]
    for (token <- pipe.trim.split("\\s+")) {
      val pos = token.indexOf("@")
      val pstr = if (pos < 0) {
        token
      } else {
        token.substring(0, pos)
      }
      val sstr = if (pos < 0) {
        ""
      } else {
        if (pos+1 == token.length) {
          error(XProcException.xsInvalidPipeToken(token, location))
        }
        token.substring(pos + 1)
      }

      try {
        val port = if (pstr == "") {
          None
        } else {
          Some(staticContext.parseNCName(pstr))
        }

        val step = if (sstr == "") {
          None
        } else {
          Some(staticContext.parseNCName(sstr))
        }

        pipes += new XPipe(parent, step, port)
      } catch {
        case _: XProcException =>
          error(XProcException.xsInvalidPipeToken(token, location))
        case ex: Exception =>
          error(ex)
      }
    }
    pipes.toList
  }

  protected[xxml] def checkEmptyAttributes(): Unit = {
    val p_element = nodeName.getNamespaceURI == XProcConstants.ns_p
    val globalAttributes = if (synthetic || p_element) {
      List(XProcConstants.xml_base, XProcConstants._exclude_inline_prefixes,
        XProcConstants._expand_text, XProcConstants._use_when)
    } else {
      List(XProcConstants.xml_base, XProcConstants.p_exclude_inline_prefixes,
        XProcConstants.p_expand_text, XProcConstants.p_use_when)
    }
    for (name <- attributes.keySet) {
      if (!globalAttributes.contains(name)) {
        if (p_element && name.getNamespaceURI == XProcConstants.ns_p) {
          error(XProcException.xsXProcNamespaceError(name, nodeName, None))
        } else {
          this match {
            case step: XStep =>
              if (step.stepDeclaration.isDefined && step.stepDeclaration.get.stepType.isDefined) {
                error(XProcException.xsUndeclaredOption(step.stepDeclaration.get.stepType.get, name, location))
              } else {
                error(XProcException.xsBadAttribute(name, None))
              }
            case _ =>
              error(XProcException.xsBadAttribute(name, None))
          }
        }
      }
    }
  }

  protected[xxml] def checkAttributes(): Unit = {
    val base = attr(XProcConstants.xml_base)
    if (base.isDefined) {
      val curbase = staticContext.baseURI.getOrElse(URIUtils.cwdAsURI)
      staticContext.baseURI = curbase.resolve(base.get)
    }
    _xml_id = attr(XProcConstants.xml_id)
  }

  protected[xxml] def validate(): Unit = {
    println(s"ERROR: ${this} does not override validate() (${this.getClass.getName})")
  }

  protected[xxml] def elaborateAttributes(): Unit = {
    checkAttributes()
    checkEmptyAttributes()
    allChildren foreach { _.elaborateAttributes() }
  }

  protected def hoistSourcesToPipes(): Unit = {
    val moveMap = mutable.HashMap.empty[XArtifact,XPipe]
    val stepMap = mutable.HashMap.empty[XArtifact,XArtifact]
    val pstep: Option[XStep] = if (parent.isDefined) {
      parent.get match {
        case xs: XStep => Some(xs)
        case _ => None
      }
    } else {
      None
    }

    for (child <- allChildren) {
      child match {
        case inline: XInline =>
          val step = new XInlineLoader(inline, this)

          // If the parent has dependencies, copy them onto the inline.
          if (pstep.isDefined) {
            step.dependsOn ++= pstep.get.dependsOn
          }

          val pipe = new XPipe(this, Some(step.tumble_id), Some("result"))
          moveMap.put(inline, pipe)
          stepMap.put(pipe, step)
        case doc: XDocument =>
          val step = new XDocumentLoader(doc, this)

          // If the parent has dependencies, copy them onto the inline.
          if (pstep.isDefined) {
            step.dependsOn ++= pstep.get.dependsOn
          }

          val pipe = new XPipe(this, Some(step.tumble_id), Some("result"))
          moveMap.put(doc, pipe)
          stepMap.put(pipe, step)
        case empty: XEmpty =>
          val step = new XEmptyLoader(this)
          val pipe = new XPipe(this, Some(step.tumble_id), Some("result"))
          moveMap.put(empty, pipe)
          stepMap.put(pipe, step)
        case _: XPipe =>
          ()
        case _ =>
          child.hoistSourcesToPipes()
      }
    }

    if (moveMap.isEmpty) {
      return
    }

    val portName = this match {
      case port: XPort =>
        port.port
      case _ => ""
    }

    // Sometimes we're hoisting from within a p:with-input to the grandparent,
    // sometimes we're hoisting from within a p:variable to the parent
    var insertChild = this
    var insertParent = parent
    var found = false
    while (!found && insertParent.isDefined) {
      insertParent.get match {
        case _: XTry =>
          // Only TryCatchBranches allowed
          ()
        case _: XChoose =>
          // Only ChooseBranches allowed
          ()
        case _: XWhen =>
          // Make sure we hoist any inputs for the condition outside the when
          found = portName != "condition"
        case _: XLoopingStep =>
          // Make sure we hoist any inputs for the loop outside the loop
          found = portName != "#anon"
        case _: XContainer =>
          found = true
        case _ =>
          ()
      }
      if (!found) {
        insertChild = insertParent.get
        insertParent = insertChild.parent
      }
    }

    if (!found) {
      throw XProcException.xiThisCantHappen("Failed to find container ancestor for hoisting?")
    }

    for ((child, pipe) <- moveMap) {
      replaceChild(child, pipe)
      val ipipe = stepMap(pipe)
      insertParent.get.insertBefore(ipipe, insertChild)
    }
  }

  protected[xxml] def ancestorContainer: Option[XContainer] = {
    var p: Option[XArtifact] = Some(this)
    while (p.isDefined) {
      p.get match {
        case cont: XContainer =>
          return Some(cont)
        case _ => ()
      }
      p = p.get.parent
    }
    None
  }

  protected[xxml] def ancestorStep: Option[XStep] = {
    var p: Option[XArtifact] = Some(this)
    while (p.isDefined) {
      p.get match {
        case step: XDeclareStep =>
          return Some(step)
        case step: XStep =>
          return Some(step)
        case _ => ()
      }
      p = p.get.parent
    }
    None
  }

  protected[xxml] def ancestorNode: Option[XArtifact] = {
    var p: Option[XArtifact] = Some(this)
    while (p.isDefined) {
      p.get match {
        case _: XStep => return p
        case _: XVariable => return p
        case _: XOption => return p
        case _ => ()
      }
      p = p.get.parent
    }
    None
  }

  protected[xxml] def ancestorOf(step: XArtifact): Boolean = {
    var p: Option[XArtifact] = Some(step)
    while (p.isDefined) {
      if (p.get eq this) {
        return true
      }
      p = p.get.parent
    }
    false
  }

  protected[xxml] def stepDeclaration: Option[XDeclareStep] = {
    val dstep = ancestor[XStep]
    if (dstep.isEmpty) {
      return None
    }

    dstep.get match {
      case decl: XDeclareStep =>
        Some(decl)
      case atomic: XAtomicStep =>
        val dcontainer = ancestor[XDeclContainer]
        if (dcontainer.isDefined) {
          dcontainer.get.findDeclaration(atomic.stepType)
        } else {
          None
        }
      case _ =>
        None
    }
  }

  protected[xxml] def elaborateSyntacticSugar(): Unit = {
    for (child <- allChildren) {
      child.elaborateSyntacticSugar()
    }
  }

  protected[xxml] def computeReadsFrom(): Unit = {
    for (child <- allChildren) {
      child.computeReadsFrom()
    }
  }

  protected[xxml] def validateExplicitConnections(href: Option[String], pipe: Option[String]): List[XArtifact] = {
    if (href.isDefined && pipe.isDefined) {
      error(XProcException.xsPipeAndHref(None))
    }

    val pipeOk = if (parent.isDefined) {
      parent.get match {
        case _: XInput => false
        case _ => true
      }
    } else {
      true
    }

    var seenEmpty = false
    val newChildren = ListBuffer.empty[XArtifact]
    for (child <- allChildren) {
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case empty: XEmpty =>
          if (href.isDefined) {
            error(XProcException.xsHrefAndOtherSources(None))
          }
          if (pipe.isDefined) {
            error(XProcException.xsPipeAndOtherSources(None))
          }
          if (seenEmpty) {
            error(XProcException.xsNoSiblingsOnEmpty(location))
          }
          empty.validate()
          newChildren += empty
          seenEmpty = true
        case doc: XDocument =>
          if (href.isDefined) {
            error(XProcException.xsHrefAndOtherSources(None))
          }
          if (pipe.isDefined) {
            error(XProcException.xsPipeAndOtherSources(None))
          }
          if (seenEmpty) {
            error(XProcException.xsNoSiblingsOnEmpty(location))
          }
          doc.validate()
          newChildren += doc
        case inline: XInline =>
          if (href.isDefined) {
            error(XProcException.xsHrefAndOtherSources(None))
          }
          if (pipe.isDefined) {
            error(XProcException.xsPipeAndOtherSources(None))
          }
          if (seenEmpty) {
            error(XProcException.xsNoSiblingsOnEmpty(location))
          }
          inline.validate()
          newChildren += inline
        case xpipe: XPipe =>
          if (pipeOk) {
            if (href.isDefined) {
              error(XProcException.xsHrefAndOtherSources(None))
            }
            if (pipe.isDefined) {
              error(XProcException.xsPipeAndOtherSources(None))
            }
            if (seenEmpty) {
              error(XProcException.xsNoSiblingsOnEmpty(location))
            }
            xpipe.validate()
            newChildren += xpipe
          } else {
            error(XProcException.xsElementNotAllowed(child.nodeName, None))
          }
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }

    if (exceptions.isEmpty) {
      if (href.isDefined) {
        val doc = new XDocument(this, href.get)
        doc.validate()
        newChildren += doc
        if (seenEmpty) {
          error(XProcException.xsNoSiblingsOnEmpty(location))
        }
      }
      if (pipe.isDefined) {
        for (pipe <- parsePipeAttribute(this, pipe.get)) {
          pipe.validate()
          newChildren += pipe
        }
        if (seenEmpty) {
          error(XProcException.xsNoSiblingsOnEmpty(location))
        }
      }
    }

    newChildren.toList
  }

  protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    var curdrp = initial
    for (child <- allChildren) {
      curdrp = child.elaborateDefaultReadablePort(curdrp)
    }
    curdrp
  }

  protected[xxml] def elaborateNameBindings(initial: XNameBindingContext): XNameBindingContext = {
    var curcontext = initial

    staticContext = staticContext.withConstants(initial)

    for (child <- allChildren) {
      child match {
        case decl: XDeclareStep =>
          decl.elaborateNameBindings(curcontext.onlyStatics)
        case _ =>
          curcontext = child.elaborateNameBindings(curcontext)
      }
    }

    curcontext
  }

  protected[xxml] def elaborateScopedFeatures(): Unit = {
    for (child <- allChildren) {
      child.elaborateScopedFeatures()
    }
  }

  protected[xxml] def elaboratePortConnections(): Unit = {
    for (child <- allChildren) {
      child.elaboratePortConnections()
    }
  }

  protected[xxml] def elaborateDependsConnections(inScopeSteps: Map[String, XStep]): Unit = {
    for (child <- children[XStep]) {
      child.elaborateDependsConnections(inScopeSteps)
    }
  }

  protected[xxml] def elaborateDynamicVariables(): Unit = {
    allChildren foreach { _.elaborateDynamicVariables() }
  }

  protected[xxml] def elaborateValidatePortConnections(ports: XPortBindingContext): Unit = {
    var curports = ports

    this match {
      case cont: XContainer =>
        curports = curports.withContainer(cont)
      case _ =>
        ()
    }

    // Remove "irrelevant" inputs after we've validated them
    val irrelevant = ListBuffer.empty[XArtifact]
    for (child <- allChildren) {
      child.elaborateValidatePortConnections(curports)
      child match {
        case port: XPort =>
          if (port.irrelevant) {
            irrelevant += port
          }
        case _ => ()
      }
    }
    for (child <- irrelevant) {
      if (child.exceptions.isEmpty) {
        removeChild(child)
      }
    }
  }

  def dump: XdmNode = {
    val sb = new SaxonTreeBuilder(config)
    sb.startDocument(staticContext.baseURI)
    dumpTree(sb)
    sb.endDocument()
    sb.result
  }

  def dumpTree(sb: SaxonTreeBuilder): Unit = {
    throw XProcException.xiThisCantHappen("Don't know how to dump an artifact")
  }

  def dumpTree(sb: SaxonTreeBuilder, name: String, attr: Map[String,Option[Any]]): Unit = {
    dumpTree(sb, name, attr, None)
  }

  def dumpTree(sb: SaxonTreeBuilder, name: String, attr: Map[String,Option[Any]], text: String): Unit = {
    dumpTree(sb, name, attr, Some(text))
  }

  private def dumpTree(sb: SaxonTreeBuilder, name: String, attr: Map[String,Option[Any]], text: Option[String]): Unit = {
    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    for ((name, value) <- attr) {
      if (value.isDefined) {
        amap = amap.put(TypeUtils.attributeInfo(staticContext.parseQName(name), value.get.toString))
      }
    }
    if (_synthetic) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._synthetic, "true"))
    }

    if (!attr.contains("name") || attr("name").isEmpty || attr("name").get != tumble_id) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._tumble_id, tumble_id))
    }

    sb.addStartElement(staticContext.parseQName(name), amap)
    if (text.isDefined) {
      sb.addText(text.get)
    }
    allChildren foreach { child => child.dumpTree(sb) }
    sb.addEndElement()
  }
}
