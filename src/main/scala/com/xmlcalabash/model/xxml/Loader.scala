package com.xmlcalabash.model.xxml

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.util.{TypeUtils, XdmLocation}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap}
import net.sf.saxon.s9api.{Axis, XdmNode, XdmNodeKind}

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.IteratorHasAsScala

class Loader(parser: XParser, hier: NodeHierarchy) {
  private val config = parser.config
  private val _exceptions = ListBuffer.empty[Exception]
  private var _declContainer: XDeclContainer = _
  private var _syntheticLibrary = Option.empty[XLibrary]

  if (!useWhen(hier.root)) {
    throw XProcException.xiUserError("Use-when is false on the root element")
  }

  if (hier.root.getNodeName == XProcConstants.p_declare_step) {
    _declContainer = parseDeclareStep(hier.root)
  } else {
    _declContainer = parseLibrary(hier.root)
  }

  def declaredStep: Option[XDeclareStep] = {
    _declContainer match {
      case step: XDeclareStep => Some(step)
      case _ => None
    }
  }

  def library: Option[XLibrary] = {
    _declContainer match {
      case lib: XLibrary =>
        Some(lib)
      case step: XDeclareStep =>
        if (_syntheticLibrary.isEmpty) {
          val library = new XLibrary(config, hier.baseURI)
          library.staticContext = new XArtifactContext(library, hier.root)
          library.synthetic = true
          library.syntheticName = XProcConstants.p_library
          library.addChild(step)
          _syntheticLibrary = Some(library)
        }
        _syntheticLibrary
      case _ =>
        throw XProcException.xiThisCantHappen("Declared container is neither step nor library?")
    }
  }

  def exceptions: List[Exception] = _exceptions.toList

  private def useWhen(node: XdmNode): Boolean = hier.useWhen(node)

  private def parseChildren(node: XdmNode): List[XArtifact] = {
    val children = ListBuffer.empty[XArtifact]
    for (child <- node.axisIterator(Axis.CHILD).asScala) {
      try {
        child.getNodeKind match {
          case XdmNodeKind.TEXT =>
            if (child.getStringValue.trim != "") {
              throw XProcException.xsTextNotAllowed(child.getStringValue, XdmLocation.from(node))
            }
          case XdmNodeKind.COMMENT =>
            ()
          case XdmNodeKind.PROCESSING_INSTRUCTION =>
            ()
          case XdmNodeKind.ELEMENT =>
            if (useWhen(child)) {
              child.getNodeName match {
                case XProcConstants.p_declare_step =>
                  children += parseDeclareStep(child)
                case XProcConstants.p_library =>
                  children += parseLibrary(child)
                case XProcConstants.p_catch =>
                  children += parseCatch(child)
                case XProcConstants.p_choose =>
                  children += parseChoose(child)
                case XProcConstants.p_document =>
                  children += parseDocument(child)
                case XProcConstants.p_documentation =>
                  children += parseDocumentation(child)
                case XProcConstants.p_empty =>
                  children += parseEmpty(child)
                case XProcConstants.p_finally =>
                  children += parseFinally(child)
                case XProcConstants.p_for_each =>
                  children += parseForEach(child)
                case XProcConstants.p_group =>
                  children += parseGroup(child)
                case XProcConstants.p_if =>
                  children += parseIf(child)
                case XProcConstants.p_import =>
                  val imported = parseImport(child)
                  if (imported.isDefined) {
                    children += imported.get
                  }
                case XProcConstants.p_import_functions =>
                  children += parseImportFunctions(child)
                case XProcConstants.p_inline =>
                  children += parseInline(child)
                case XProcConstants.p_input =>
                  children += parseInput(child)
                case XProcConstants.p_option =>
                  children += parseOption(child)
                case XProcConstants.p_otherwise =>
                  children += parseOtherwise(child)
                case XProcConstants.p_output =>
                  children += parseOutput(child)
                case XProcConstants.p_pipe =>
                  children += parsePipe(child)
                case XProcConstants.p_pipeinfo =>
                  children += parsePipeinfo(child)
                case XProcConstants.p_try =>
                  children += parseTry(child)
                case XProcConstants.p_variable =>
                  children += parseVariable(child)
                case XProcConstants.p_viewport =>
                  children += parseViewport(child)
                case XProcConstants.p_when =>
                  children += parseWhen(child)
                case XProcConstants.p_with_input =>
                  children += parseWithInput(child)
                case XProcConstants.p_with_option =>
                  children += parseWithOption(child)
                case XProcConstants.cx_loop =>
                  children += parseForLoop(child)
                case XProcConstants.cx_until =>
                  children += parseUntilLoop(child)
                case XProcConstants.cx_while =>
                  children += parseWhileLoop(child)

                case _ =>
                  children += parseAtomicStep(child)
              }
            }
          case _ =>
            throw XProcException.xiThisCantHappen(s"Unexpected child node kind: ${child}")
        }
      } catch {
        case ex: Exception =>
          _exceptions += ex
      }
    }
    children.toList
  }

  private def parseAtomicStep(node: XdmNode): XAtomicStep = {
    val decl = new XAtomicStep(config, node.getNodeName)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseDeclareStep(node: XdmNode): XDeclareStep = {
    val decl = new XDeclareStep(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseLibrary(node: XdmNode): XLibrary = {
    val decl = new XLibrary(config, Option(node.getBaseURI))
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseCatch(node: XdmNode): XCatch = {
    val decl = new XCatch(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseChoose(node: XdmNode): XChoose = {
    val decl = new XChoose(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseDocument(node: XdmNode): XDocument = {
    val decl = new XDocument(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseDocumentation(node: XdmNode): XDocumentation = {
    val decl = new XDocumentation(config, nodeContent(node, false))
    decl.parse(node, List())
    decl
  }

  private def parseEmpty(node: XdmNode): XEmpty = {
    val decl = new XEmpty(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseFinally(node: XdmNode): XFinally = {
    val decl = new XFinally(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseForEach(node: XdmNode): XForEach = {
    val decl = new XForEach(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseForLoop(node: XdmNode): XForLoop = {
    val decl = new XForLoop(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseUntilLoop(node: XdmNode): XForUntil = {
    val decl = new XForUntil(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseWhileLoop(node: XdmNode): XForWhile = {
    val decl = new XForWhile(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseGroup(node: XdmNode): XGroup = {
    val decl = new XGroup(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseIf(node: XdmNode): XIf = {
    val decl = new XIf(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseImport(node: XdmNode): Option[XImport] = {
    val ihier = hier.imported(node)
    if (ihier.isEmpty) {
      var found = Option.empty[NodeHierarchy]
      for (rehier <- hier.reimports) {
        if (rehier.baseURI.isDefined
          && rehier.baseURI.get == node.getBaseURI.resolve(node.getAttributeValue(XProcConstants._href))) {
          found = Some(rehier)
        }
      }
      if (found.isDefined && found.get.ximport.isDefined) {
        return found.get.ximport
      } else {
        throw XProcException.xiThisCantHappen("Reimport that doesn't have an XImport?")
      }
    }

    val loader = new Loader(parser, ihier.get)

    _exceptions ++= loader.exceptions

    if (!ihier.get.useWhen(hier.root)) {
      _exceptions += XProcException.xsInvalidPipeline("Root element use-when is false; no document", None)
    }

    if (_exceptions.nonEmpty) {
      throw _exceptions.head
    }

    val lib = loader.library.get

    lib.builtinLibraries = parser.builtinLibraries
    for (imp <- lib.children[XImport]) {
      lib.addInScopeSteps(imp.library.inScopeSteps)
    }

    lib.xelaborate()
    _exceptions ++= lib.errors

    if (_exceptions.nonEmpty) {
      throw _exceptions.head
    }

    val decl = new XImport(config, lib)
    decl.parse(node, parseChildren(node))
    ihier.get.ximport = decl
    Some(decl)
  }

  private def parseImportFunctions(node: XdmNode): XImportFunctions = {
    val decl = new XImportFunctions(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseInline(node: XdmNode): XInline = {
    val decl = new XInline(config, checkAttributes(nodeContent(node, false)), false)
    decl.parse(node, List())
    decl.parseExpandText(node)
    decl.parseExcludedUris(node)
    decl
  }

  private def nodeContent(node: XdmNode, includeNode: Boolean): XdmNode = {
    val builder = new SaxonTreeBuilder(config)

    try {
      builder.startDocument(Option(node.getBaseURI))
    } catch {
      case _: IllegalStateException =>
        _exceptions += XProcException.xdInvalidURI(node.getUnderlyingNode.getBaseURI, None)
      case ex: Exception =>
        _exceptions += ex
    }

    if (includeNode) {
      filterUseWhen(builder, node)
    } else {
      for (child <- node.axisIterator(Axis.CHILD).asScala) {
        filterUseWhen(builder, child)
      }
    }

    builder.endDocument()
    builder.result
  }

  private def filterUseWhen(builder: SaxonTreeBuilder, node: XdmNode): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        for (child <- node.axisIterator(Axis.CHILD).asScala) {
          filterUseWhen(builder, child)
        }
      case XdmNodeKind.ELEMENT =>
        if (useWhen(node)) {
          val usewhen = if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
            XProcConstants._use_when
          } else {
            XProcConstants.p_use_when
          }
          var attrMap: AttributeMap = EmptyAttributeMap.getInstance()
          val iter = node.axisIterator(Axis.ATTRIBUTE)
          while (iter.hasNext) {
            val attr = iter.next()
            if (attr.getNodeName != usewhen) {
              attrMap = attrMap.put(TypeUtils.attributeInfo(attr.getNodeName, attr.getStringValue))
            }
          }
          builder.addStartElement(node, attrMap)
          for (child <- node.axisIterator(Axis.CHILD).asScala) {
            filterUseWhen(builder, child)
          }
          builder.addEndElement()
        }
      case _ =>
        builder.addSubtree(node)
    }
  }

  private def parseInput(node: XdmNode): XInput = {
    val decl = new XInput(config)
    decl.parse(node, parseConnectionChildren(node))
    decl
  }

  private def parseConnectionChildren(node: XdmNode): List[XArtifact] = {
    var connection = false
    var conncount = 0
    var empty = Option.empty[XdmNode]
    var nonElementDecls = false
    val implicitInlines = ListBuffer.empty[XdmNode]

    for (child <- node.axisIterator(Axis.CHILD).asScala) {
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          if (useWhen(child)) {
            child.getNodeName match {
              case XProcConstants.p_inline =>
                connection = true
                conncount += 1
              case XProcConstants.p_empty =>
                connection = true
                conncount += 1
                empty = Some(child)
              case XProcConstants.p_pipe =>
                connection = true
                conncount += 1
              case XProcConstants.p_document =>
                connection = true
                conncount += 1
              case XProcConstants.p_pipeinfo =>
                ()
              case XProcConstants.p_documentation =>
                ()
              case _ =>
                implicitInlines += child
                conncount += 1
            }
          }
        case XdmNodeKind.COMMENT =>
          connection = true
          nonElementDecls = true
          conncount += 1
        case XdmNodeKind.TEXT =>
          if (child.getStringValue.trim() != "") {
            connection = true
            nonElementDecls = true
            conncount += 1
          }
        case XdmNodeKind.PROCESSING_INSTRUCTION =>
          connection = true
          nonElementDecls = true
          conncount += 1
        case _ =>
          throw XProcException.xiThisCantHappen(s"Connection children included ${child}")
      }
    }

    if (connection) {
      if (conncount > 1 && empty.isDefined) {
        throw XProcException.xsNoSiblingsOnEmpty(XdmLocation.from(empty.get))
      }
      if (implicitInlines.nonEmpty) {
        if (nonElementDecls) {
          throw XProcException.xsInlineNotAllowed(XdmLocation.from(implicitInlines.head))
        }
        throw XProcException.xsCantMixConnectionsWithImplicitInlines(implicitInlines.head.getNodeName, XdmLocation.from(implicitInlines.head))
      }
      return parseChildren(node)
    }

    val inlines = ListBuffer.empty[XInline]
    for (child <- implicitInlines) {
      val decl = new XInline(config, checkAttributes(nodeContent(child, true)), true)
      decl.parse(node, List())
      decl.parseExpandText(node)
      decl.parseExcludedUris(node)
      inlines += decl
    }

    inlines.toList
  }

  private def checkAttributes(node: XdmNode): XdmNode = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        for (child <- node.axisIterator(Axis.CHILD).asScala) {
          checkAttributes(child)
        }
      case XdmNodeKind.ELEMENT =>
        if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
          for (attr <- node.axisIterator(Axis.ATTRIBUTE).asScala) {
            if (attr.getNodeName == XProcConstants._expand_text || attr.getNodeName == XProcConstants._inline_expand_text) {
              if (attr.getStringValue != "true" && attr.getStringValue != "false") {
                _exceptions += XProcException.xsInvalidExpandText(attr.getNodeName, attr.getStringValue, XdmLocation.from(node))
              }
            }
          }
        } else {
          for (attr <- node.axisIterator(Axis.ATTRIBUTE).asScala) {
            if (attr.getNodeName == XProcConstants.p_expand_text || attr.getNodeName == XProcConstants.p_inline_expand_text) {
              if (attr.getStringValue != "true" && attr.getStringValue != "false") {
                _exceptions += XProcException.xsInvalidExpandText(attr.getNodeName, attr.getStringValue, XdmLocation.from(node))
              }
            }
          }
        }
        for (child <- node.axisIterator(Axis.CHILD).asScala) {
          checkAttributes(child)
        }
      case _ => ()
    }

    node
  }

  private def parseOption(node: XdmNode): XOption = {
    if (node.getAttributeValue(XProcConstants._static) == "true") {
      // We've already worked out this value while parsing the nodes
    }

    val decl = new XOption(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseOtherwise(node: XdmNode): XOtherwise = {
    val decl = new XOtherwise(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseOutput(node: XdmNode): XOutput = {
    val decl = new XOutput(config)
    decl.parse(node, parseConnectionChildren(node))
    decl
  }

  private def parsePipe(node: XdmNode): XPipe = {
    val decl = new XPipe(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parsePipeinfo(node: XdmNode): XPipeinfo = {
    val decl = new XPipeinfo(config, nodeContent(node, false))
    decl.parse(node, List())
    decl
  }

  private def parseTry(node: XdmNode): XTry = {
    val decl = new XTry(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseVariable(node: XdmNode): XVariable = {
    val decl = new XVariable(config)
    decl.parse(node, parseConnectionChildren(node))
    decl
  }

  private def parseViewport(node: XdmNode): XViewport = {
    val decl = new XViewport(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseWhen(node: XdmNode): XWhen = {
    val decl = new XWhen(config)
    decl.parse(node, parseChildren(node))
    decl
  }

  private def parseWithInput(node: XdmNode): XWithInput = {
    val decl = new XWithInput(config)
    decl.parse(node, parseConnectionChildren(node))
    decl
  }

  private def parseWithOption(node: XdmNode): XWithOption = {
    val decl = new XWithOption(config)
    decl.parse(node, parseConnectionChildren(node))
    decl
  }
}
