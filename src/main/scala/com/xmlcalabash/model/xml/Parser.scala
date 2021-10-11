package com.xmlcalabash.model.xml

import com.xmlcalabash.config.{DocumentRequest, XMLCalabashConfig}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcLocation, XProcXPathExpression}
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap, FingerprintedQName}
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}
import org.xml.sax.InputSource

import java.net.URI
import javax.xml.transform.sax.SAXSource
import scala.collection.mutable.ListBuffer

class Parser(config: XMLCalabashConfig) {
  private var _builtInSteps: Option[Library] = config.builtinSteps
  private var matcher: ProcessMatch = _

  // Someone has to load the default steps; feels like here's as good a place as any.
  // It doesn't feel like something the user should have to do explicitly.
  if (_builtInSteps.isEmpty) {
    config.builtinSteps = init_builtins()
    _builtInSteps = config.builtinSteps
  }

  def builtInSteps: Option[Library] = _builtInSteps

  def loadLibrary(root: XdmNode): Library = {
    val node = stripUseWhen(root)
    if (node.getNodeKind != XdmNodeKind.ELEMENT || node.getNodeName != XProcConstants.p_library) {
      throw new RuntimeException(s"Not a library: ${root.getNodeName}")
    }

    parseLibrary(node)
  }

  def loadDeclareStep(uri: URI): DeclareStep = {
    val request = new DocumentRequest(uri, MediaType.XML)
    val response = config.documentManager.parse(request)
    loadDeclareStep(response.value.asInstanceOf[XdmNode])
  }

  def loadDeclareStep(root: XdmNode): DeclareStep = {
    val node = stripUseWhen(root)
    if (node.getNodeKind != XdmNodeKind.ELEMENT || node.getNodeName != XProcConstants.p_declare_step) {
      throw new RuntimeException(s"Not a declare-step: ${root.getNodeName}")
    }

    val decl = parseDeclareStep(node)

    if (node.getParent.getNodeKind == XdmNodeKind.DOCUMENT) {
      decl.loadImports()
      decl.updateInScopeSteps()
      decl.parseDeclarationSignature()

      val toElaborate = ListBuffer.empty[DeclContainer]
      toElaborate += config.builtinSteps.get
      for (uri <- config.importedURIs) {
        toElaborate += config.importedURI(uri).get
      }
      toElaborate += decl

      for (root <- toElaborate) {
        root.makeStructureExplicit()
        root.makeBindingsExplicit()
        root.validateStructure()
      }
    }

    config.clearImportedURIs()

    decl
  }

  private def stripUseWhen(root: XdmNode): XdmNode = {
    val staticContext = new StaticContext(config, None)
    matcher = new ProcessMatch(config, new ProcessUseWhen(staticContext), staticContext)
    matcher.process(root, "*")
    val node = matcher.result
    if (node.getNodeKind == XdmNodeKind.DOCUMENT) {
      S9Api.documentElement(node).get
    } else {
      node
    }
  }

  private def parseContainer[T <: Container](node: XdmNode, container: T): T = {
    container match {
      case _: DeclareStep =>
        parseContainer(node, container, List(), List(XProcConstants.p_with_input))
      case _: If =>
        parseContainer(node, container, List(), List(XProcConstants.p_input, XProcConstants.p_declare_step,
          XProcConstants.p_import, XProcConstants.p_import_functions))
      case _: Choose =>
        parseContainer(node, container, List(XProcConstants.p_with_input, XProcConstants.p_when, XProcConstants.p_otherwise), List())
      case _ =>
        parseContainer(node, container, List(), List(XProcConstants.p_input, XProcConstants.p_declare_step,
          XProcConstants.p_import, XProcConstants.p_import_functions))
    }
  }

  private def parseContainer[T <: Container](node: XdmNode, container: T, allowed: List[QName], forbidden: List[QName]): T = {
    container.parse(node)

    for (child <- children(node)) {
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          if (allowed.nonEmpty && !allowed.contains(child.getNodeName)) {
            throw new RuntimeException(s"Not allowed here: ${child.getNodeName}")
          }
          if (forbidden.nonEmpty && forbidden.contains(child.getNodeName)) {
            throw new RuntimeException(s"Not allowed here: ${child.getNodeName}")
          }

          child.getNodeName match {
            case XProcConstants.p_input =>
              container.addChild(parseInput(child))
            case XProcConstants.p_with_input =>
              container.addChild(parseWithInput(child))
            case XProcConstants.p_output =>
              container.addChild(parseOutput(child))
            case XProcConstants.p_option =>
              container.addChild(parseOption(child))
            case XProcConstants.p_variable =>
              container.addChild(parseVariable(child))
            case XProcConstants.p_declare_step =>
              container.addChild(parseDeclareStep(child))
            case XProcConstants.p_choose =>
              container.addChild(parseChoose(child))
            case XProcConstants.p_when =>
              container.addChild(parseWhen(child))
            case XProcConstants.p_otherwise =>
              container.addChild(parseOtherwise(child))
            case XProcConstants.p_if =>
              container.addChild(parseIf(child))
            case XProcConstants.p_for_each =>
              container.addChild(parseForEach(child))
            case XProcConstants.cx_until =>
              container.addChild(parseUntil(child))
            case XProcConstants.cx_while =>
              container.addChild(parseWhile(child))
            case XProcConstants.cx_loop =>
              container.addChild(parseLoop(child))
            case XProcConstants.p_viewport =>
              container.addChild(parseViewport(child))
            case XProcConstants.p_group =>
              container.addChild(parseGroup(child))
            case XProcConstants.p_try =>
              container.addChild(parseTry(child))
            case XProcConstants.p_catch =>
              container.addChild(parseCatch(child))
            case XProcConstants.p_finally =>
              container.addChild(parseFinally(child))
            case XProcConstants.p_import =>
              container.addChild(parseImport(child))
            case XProcConstants.p_import_functions =>
              container.addChild(parseImportFunctions(child))
            case XProcConstants.p_documentation =>
              container.addChild(parseDocumentation(child))
            case XProcConstants.p_pipeinfo =>
              container.addChild(parsePipeInfo(child))
            case _ =>
              // If we don't recognize it, assume it's an atomic step
              container.addChild(parseAtomicStep(child))
          }
        case XdmNodeKind.COMMENT => ()
        case XdmNodeKind.PROCESSING_INSTRUCTION => ()
        case XdmNodeKind.TEXT =>
          throw XProcException.xsTextNotAllowed(child.getStringValue.trim(), Some(new XProcLocation(child)))
        case _ =>
          throw new RuntimeException(s"Unexpected element kind: ${child.getNodeKind}")
      }
    }

    container
  }

  protected[model] def parseLibrary(node: XdmNode): Library = {
    val library = new Library(config)
    library.parse(node)

    for (child <- children(node)) {
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          child.getNodeName match {
            case XProcConstants.p_declare_step =>
              library.addChild(parseDeclareStep(child))
            case XProcConstants.p_variable =>
              library.addChild(parseVariable(child))
            case XProcConstants.p_function =>
              library.addChild(parseFunction(child))
            case XProcConstants.p_import =>
              library.addChild(parseImport(child))
            case XProcConstants.p_import_functions =>
              library.addChild(parseImportFunctions(child))
            case XProcConstants.p_documentation =>
              library.addChild(parseDocumentation(child))
            case XProcConstants.p_pipeinfo =>
              library.addChild(parsePipeInfo(child))
            case _ =>
              throw new RuntimeException(s"Unexpected element: ${child.getNodeName}")
          }
        case XdmNodeKind.COMMENT => ()
        case XdmNodeKind.PROCESSING_INSTRUCTION => ()
        case XdmNodeKind.TEXT =>
          throw XProcException.xsTextNotAllowed(child.getStringValue.trim(), Some(new XProcLocation(child)))
        case _ =>
          throw new RuntimeException(s"Unexpected element kind: ${child.getNodeKind}")
      }
    }

    library
  }

  protected[model] def parseDeclareStep(node: XdmNode): DeclareStep = {
    val decl = new DeclareStep(config)
    parseContainer(node, decl)
    decl
  }

  private def parseImport(node: XdmNode): Import = {
    parseNoChildrenAllowed(node, new Import(config))
  }

  private def parseImportFunctions(node: XdmNode): ImportFunctions = {
    parseNoChildrenAllowed(node, new ImportFunctions(config))
  }

  private def parseChoose(node: XdmNode): Choose = {
    parseContainer(node, new Choose(config))
  }

  private def parseWhen(node: XdmNode): When = {
    parseContainer(node, new When(config))
  }

  private def parseOtherwise(node: XdmNode): Otherwise = {
    parseContainer(node, new Otherwise(config))
  }

  private def parseIf(node: XdmNode): If = {
    parseContainer(node, new If(config))
  }

  private def parseForEach(node: XdmNode): ForEach = {
    parseContainer(node, new ForEach(config))
  }

  private def parseUntil(node: XdmNode): ForUntil = {
    parseContainer(node, new ForUntil(config))
  }

  private def parseWhile(node: XdmNode): ForWhile = {
    parseContainer(node, new ForWhile(config))
  }

  private def parseLoop(node: XdmNode): ForLoop = {
    parseContainer(node, new ForLoop(config))
  }

  private def parseViewport(node: XdmNode): Viewport = {
    parseContainer(node, new Viewport(config))
  }

  private def parseGroup(node: XdmNode): Group = {
    parseContainer(node, new Group(config))
  }

  private def parseTry(node: XdmNode): Try = {
    parseContainer(node, new Try(config))
  }

  private def parseCatch(node: XdmNode): Catch = {
    parseContainer(node, new Catch(config))
  }

  private def parseFinally(node: XdmNode): Finally = {
    parseContainer(node, new Finally(config))
  }

  private def parseConnections[T <: Artifact](node: XdmNode, art: T): T = {
    art match {
      case input: DeclareInput =>
        parseConnections(node, art, List(XProcConstants.p_pipe))
      case _ =>
        parseConnections(node, art, List())
    }
  }

  private def parseConnections[T <: Artifact](node: XdmNode, art: T, forbidden: List[QName]): T = {
    art.parse(node)

    var implicitInline = false
    for (child <- children(node)) {
      if (forbidden.nonEmpty && forbidden.contains(child.getNodeName)) {
        throw XProcException.xsElementNotAllowed(child.getNodeName, Some(new XProcLocation(child)))
      }

      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          child.getNodeName match {
            case XProcConstants.p_empty =>
              art.addChild(parseEmpty(child))
            case XProcConstants.p_document =>
              art.addChild(parseDocument(child))
            case XProcConstants.p_inline =>
              art.addChild(parseInline(child))
            case XProcConstants.p_pipe =>
              art.addChild(parsePipe(child))
            case XProcConstants.p_documentation =>
              art.addChild(parseDocumentation(child))
            case XProcConstants.p_pipeinfo =>
              art.addChild(parsePipeInfo(child))
            case _ =>
              implicitInline = true
              art.addChild(parseSyntheticInline(child))
          }
        case XdmNodeKind.COMMENT =>
          if (implicitInline) {
            throw XProcException.xsInlineCommentNotAllowed(child.getStringValue.trim(), Some(new XProcLocation(child)))
          }
        case XdmNodeKind.PROCESSING_INSTRUCTION =>
          if (implicitInline) {
            throw XProcException.xsInlinePiNotAllowed(child.getStringValue.trim(), Some(new XProcLocation(child)))
          }
        case XdmNodeKind.TEXT =>
          if (implicitInline) {
            throw XProcException.xsInlineTextNotAllowed(child.getStringValue.trim(), Some(new XProcLocation(child)))
          }
          throw XProcException.xsTextNotAllowed(child.getStringValue.trim(), Some(new XProcLocation(child)))
        case _ =>
          throw new RuntimeException(s"Unexpected element kind: ${child.getNodeKind}")
      }
    }

    art
  }

  private def parseInput(node: XdmNode): DeclareInput = {
    parseConnections(node, new DeclareInput(config))
  }

  private def parseOutput(node: XdmNode): DeclareOutput = {
    parseConnections(node, new DeclareOutput(config))
  }

  private def parseOption(node: XdmNode): DeclareOption = {
    parseNoChildrenAllowed(node, new DeclareOption(config))
  }

  private def parseVariable(node: XdmNode): Variable = {
    parseConnections(node, new Variable(config))
  }

  private def parseAtomicStep(node: XdmNode): AtomicStep = {
    val atomic = new AtomicStep(config)
    atomic.parse(node)

    for (child <- children(node)) {
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          child.getNodeName match {
            case XProcConstants.p_with_input =>
              atomic.addChild(parseWithInput(child))
            case XProcConstants.p_with_option =>
              atomic.addChild(parseWithOption(child))
            case XProcConstants.p_documentation =>
              atomic.addChild(parseDocumentation(child))
            case XProcConstants.p_pipeinfo =>
              atomic.addChild(parsePipeInfo(child))
            case _ =>
              throw new RuntimeException(s"Unexpected element: ${child.getNodeName}")
          }
        case XdmNodeKind.COMMENT => ()
        case XdmNodeKind.PROCESSING_INSTRUCTION => ()
        case XdmNodeKind.TEXT =>
          throw XProcException.xsTextNotAllowed(child.getStringValue.trim(), Some(new XProcLocation(child)))
        case _ =>
          throw new RuntimeException(s"Unexpected element kind: ${child.getNodeKind}")
      }
    }

    atomic
  }

  private def parseWithInput(node: XdmNode): WithInput = {
    parseConnections(node, new WithInput(config))
  }

  private def parseWithOption(node: XdmNode): WithOption = {
    parseConnections(node, new WithOption(config))
  }

  private def parseNoChildrenAllowed[T <: Artifact](node: XdmNode, empty: T): T = {
    empty.parse(node)

    for (child <- children(node)) {
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          child.getNodeName match {
            case XProcConstants.p_documentation =>
              empty.addChild(parseDocumentation(child))
            case XProcConstants.p_pipeinfo =>
              empty.addChild(parsePipeInfo(child))
            case _ =>
              throw new RuntimeException("no elements allowed")
          }
        case XdmNodeKind.COMMENT => ()
        case XdmNodeKind.PROCESSING_INSTRUCTION => ()
        case XdmNodeKind.TEXT =>
          throw XProcException.xsTextNotAllowed(child.getStringValue.trim(), Some(new XProcLocation(child)))
        case _ =>
          throw new RuntimeException(s"Unexpected element kind: ${child.getNodeKind}")
      }
    }

    empty
  }

  private def parseEmpty(node: XdmNode): Empty = {
    parseNoChildrenAllowed(node, new Empty(config))
  }

  private def parseDocument(node: XdmNode): Document = {
    parseNoChildrenAllowed(node, new Document(config))
  }

  private def parsePipe(node: XdmNode): Pipe = {
    parseNoChildrenAllowed(node, new Pipe(config))
  }

  private def parseFunction(node: XdmNode): DeclareFunction = {
    parseNoChildrenAllowed(node, new DeclareFunction(config))
  }

  private def parseInline(node: XdmNode): Inline = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(node.getBaseURI)
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next()
      builder.addSubtree(child)
    }
    builder.endDocument()

    val excludeURIs = S9Api.excludeInlineURIs(node)
    val inlineNode = S9Api.removeNamespaces(config, builder.result, excludeURIs, true)

    val inline = new Inline(config, inlineNode)
    inline.parse(node)
    inline
  }

  private def parseSyntheticInline(node: XdmNode): Inline = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(node.getBaseURI)
    builder.addSubtree(node)
    builder.endDocument()

    val excludeURIs = S9Api.excludeInlineURIs(node)
    val inlineNode = S9Api.removeNamespaces(config, builder.result, excludeURIs, true)

    if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
      throw new RuntimeException("Elements in the XProc namespace cannot be synthetic inlines")
    }

    val inline = new Inline(config, inlineNode, true)
    inline.parse(node)
    inline
  }

  private def parseDocumentation(node: XdmNode): Documentation = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(node.getBaseURI)
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next()
      builder.addSubtree(child)
    }
    builder.endDocument()

    val docs = new Documentation(config, builder.result)
    docs.parse(node)
    docs
  }

  private def parsePipeInfo(node: XdmNode): PipeInfo = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(node.getBaseURI)
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next()
      builder.addSubtree(child)
    }
    builder.endDocument()

    val info = new PipeInfo(config, builder.result)
    info.parse(node)
    info
  }

  def init_builtins(): Library = {
    val xmlbuilder = config.processor.newDocumentBuilder()
    val stream = getClass.getResourceAsStream("/standard-steps.xpl")
    val source = new SAXSource(new InputSource(stream))
    xmlbuilder.setDTDValidation(false)
    xmlbuilder.setLineNumbering(true)
    val libnode = xmlbuilder.build(source)
    val library = loadLibrary(libnode)
    library.loadImports()
    library.updateInScopeSteps()
    library
  }

  // ============================================================================

  private def children(node: XdmNode): List[XdmNode] = {
    children(node, true)
  }

  private def children(node: XdmNode, ignoreWS: Boolean): List[XdmNode] = {
    val list = ListBuffer.empty[XdmNode]
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next()
      child.getNodeKind match {
        case XdmNodeKind.TEXT =>
          if (!ignoreWS || child.getStringValue.trim() != "") {
            list += child
          }
        case _ => list += child
      }
    }
    list.toList
  }

  private class ProcessUseWhen(val staticContext: StaticContext) extends ProcessMatchingNodes {
    private var useStack = ListBuffer.empty[Boolean]
    private var inlineStack = ListBuffer.empty[Boolean]

    override def startDocument(node: XdmNode): Boolean = {
      throw XProcException.xiThisCantHappen("Processing use-when matched a document node", None)
    }

    override def endDocument(node: XdmNode): Unit = {
      matcher.endDocument()
    }

    override def startElement(node: XdmNode, attributes: AttributeMap): Boolean = {
      val useWhenName = if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
        new FingerprintedQName("", "", "use-when")
      } else {
        new FingerprintedQName("p", XProcConstants.ns_p, "use-when")
      }

      val useWhen = Option(attributes.get(useWhenName))

      var use = true
      val inline = inlineStack.nonEmpty && inlineStack.last
      if (useWhen.isDefined && !inline) {
        val exprContext = new StaticContext(config, None, node)
        val expr = new XProcXPathExpression(exprContext, useWhen.get.getValue)
        use = config.expressionEvaluator.booleanValue(expr, List(), Map(), None)
      }

      if (use) {
        matcher.location = node.getUnderlyingNode.saveLocation()
        if (inline) {
          matcher.addStartElement(node, attributes)
        } else {
          matcher.addStartElement(node, attributes.remove(useWhenName))
        }
      }
      useStack += use
      inlineStack += ((inlineStack.nonEmpty && inlineStack.last) || node.getNodeName == XProcConstants.p_inline)
      use
    }

    override def attributes(node: XdmNode, matchingAttributes: AttributeMap, nonMatchingAttributes: AttributeMap): Option[AttributeMap] = {
      throw new RuntimeException("This can't happen")
    }

    override def endElement(node: XdmNode): Unit = {
      if (useStack.last) {
        matcher.addEndElement()
      }
      useStack = useStack.dropRight(1)
      inlineStack = inlineStack.dropRight(1)
    }

    override def text(node: XdmNode): Unit = {
      throw XProcException.xiThisCantHappen("Processing use-when matched a text node", None)
    }

    override def comment(node: XdmNode): Unit = {
      throw XProcException.xiThisCantHappen("Processing use-when matched a comment node", None)
    }

    override def pi(node: XdmNode): Unit = {
      throw XProcException.xiThisCantHappen("Processing use-when matched a processing-instruction node", None)
    }
  }

}
