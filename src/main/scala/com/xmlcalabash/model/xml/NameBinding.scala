package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.jafpl.messages.Message
import com.xmlcalabash.config.{DocumentRequest, XMLCalabashConfig}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcMetadata, XProcVtExpression, XProcXPathExpression}
import com.xmlcalabash.util.{TvtExpander, TypeUtils}
import net.sf.saxon.ma.map.MapType
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.{QName, SaxonApiException, SequenceType, XdmAtomicValue, XdmNode}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class NameBinding(override val config: XMLCalabashConfig) extends Artifact(config) {
  protected var _name: QName = _
  protected var _declaredType = Option.empty[SequenceType]
  protected var _as = Option.empty[SequenceType]
  protected var _values = Option.empty[String]
  protected var _static = Option.empty[Boolean]
  protected var _required = Option.empty[Boolean]
  protected var _select = Option.empty[String]
  protected var _avt = Option.empty[String]
  protected var _visibility = Option.empty[String]
  protected var _allowedValues = Option.empty[List[XdmAtomicValue]]
  protected var _staticValue = Option.empty[XdmValueItemMessage]
  protected var _dependentNameBindings: ListBuffer[NamePipe] = ListBuffer.empty[NamePipe]
  protected var collection = false

  private var _qnameKeys = false
  private var resolvedStatically = false
  private val structuredQName = new StructuredQName("xs", XProcConstants.ns_xs, "QName")
  protected val depends: ListBuffer[String] = ListBuffer.empty[String]

  protected var _href = Option.empty[String]
  protected var _pipe = Option.empty[String]

  private var typeUtils: TypeUtils = _

  def name: QName = _name
  def as: Option[SequenceType] = _as
  protected[model] def as_=(seq: SequenceType): Unit = {
    _as = Some(seq)
  }

  def declaredType: SequenceType = {
    if (_declaredType.isEmpty) {
      _declaredType = staticContext.parseSequenceType(Some("xs:string"))
    }
    _declaredType.get
  }
  protected[model] def declaredType_=(decltype: SequenceType): Unit = {
    _declaredType = Some(decltype)
  }

  def values: Option[String] = _values
  def required: Boolean = _required.getOrElse(false)
  def select: Option[String] = _select
  protected[model] def select_=(select: String): Unit = {
    _select = Some(select)
  }
  def avt: Option[String] = _avt
  protected[model] def avt_=(expr: String): Unit = {
    if (select.isDefined) {
      throw new RuntimeException("Cannot define AVT if select is present")
    }
    _avt = Some(expr)
  }

  def static: Boolean = _static.getOrElse(false)
  def visibility: String = _visibility.getOrElse("public")
  def allowedValues: Option[List[XdmAtomicValue]] = _allowedValues
  def qnameKeys: Boolean = _qnameKeys
  protected[xml] def qnameKeys_=(keys: Boolean): Unit = {
    _qnameKeys = keys
  }

  protected[xmlcalabash] def staticValue: Option[XdmValueItemMessage] = _staticValue
  protected[model] def staticValue_=(value: XdmValueItemMessage): Unit = {
    _staticValue = Some(value)
  }

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    typeUtils = new TypeUtils(config, staticContext)

    if (attributes.contains(XProcConstants._name)) {
      _name = staticContext.parseQName(attr(XProcConstants._name).get)
      this match {
        case _: WithOption =>
          () // This would be ok if the step has an option declared in the p: namespace
        case _ =>
          if (_name.getNamespaceURI == XProcConstants.ns_p) {
            throw XProcException.xsOptionInXProcNamespace(_name, location)
          }
      }
    } else {
      throw XProcException.xsMissingRequiredAttribute(XProcConstants._name, location)
    }

    val seqTypeString = attr(XProcConstants._as)
    _as = typeUtils.parseSequenceType(seqTypeString)

    if (as.isDefined)
      as.get.getUnderlyingSequenceType.getPrimaryType match {
        case map: MapType =>
          if (map.getKeyType.getPrimitiveItemType.getTypeName == structuredQName) {
            // We have to lie about the type of maps with QName keys because we're
            // going to allow users to put strings in there.
            _qnameKeys = true
            _as = Some(typeUtils.parseFakeMapSequenceType(seqTypeString.get))
          }
        case _ => ()
      }

    _values = attr(XProcConstants._values)

    if (_values.isDefined) {
      val exprEval = config.expressionEvaluator.newInstance()
      val expr = new XProcXPathExpression(staticContext, _values.get, None, None, None)
      val value = exprEval.value(expr, List(), Map(), None)
      val iter = value.item.iterator()
      val allowed = ListBuffer.empty[XdmAtomicValue]
      while (iter.hasNext) {
        val token = iter.next()
        token match {
          case atom: XdmAtomicValue =>
            allowed += atom
          case _ =>
            throw XProcException.xsInvalidValues(_values.get, location)
        }
      }
      _allowedValues = Some(allowed.toList)
    }

    _static = staticContext.parseBoolean(attr(XProcConstants._static))
    _required = staticContext.parseBoolean(attr(XProcConstants._required))
    _select = attr(XProcConstants._select)
    _visibility = attr(XProcConstants._visibility)

    val _collection = attr(XProcConstants._collection)
    if (_collection.isDefined) {
      val coll = _collection.get
      if (List("1", "true", "yes").contains(coll)) {
        collection = true
      } else {
        if (List("0", "false", "no").contains(coll)) {
          collection = false
        } else {
          throw XProcException.xsBadTypeValue(coll, "xs:boolean", location)
        }
      }
    }

    _href = attr(XProcConstants._href)
    _pipe = attr(XProcConstants._pipe)

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    if (_href.isDefined && _pipe.isDefined) {
      throw XProcException.xsPipeAndHref(location)
    }

    if (_href.isDefined && allChildren.nonEmpty) {
      throw XProcException.xsHrefAndOtherSources(location)
    }

    if (_pipe.isDefined && allChildren.nonEmpty) {
      throw XProcException.xsPipeAndOtherSources(location)
    }

    if (_href.isDefined) {
      val doc = new Document(config)
      doc.href = _href.get
      addChild(doc)
    }

    if (_pipe.isDefined) {
      var port = Option.empty[String]
      var step = Option.empty[String]
      if (_pipe.get.contains("@")) {
        val re = "(.*)@(.*)".r
        _pipe.get match {
          case re(pname, sname) =>
            if (pname != "") {
              port = Some(pname)
            }
            step = Some(sname)
        }
      } else {
        if (_pipe.get.trim() != "") {
          port = _pipe
        }
      }

      val pipe = new Pipe(config)
      if (step.isDefined) {
        pipe.step = step.get
      }
      if (port.isDefined) {
        pipe.port = port.get
      }
      addChild(pipe)
    }

    for (child <- allChildren) {
      child.makeStructureExplicit()
    }
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    super.makeBindingsExplicit()

    // If the ancestor of a data source has a dependency, so does the data source
    var p = parent
    while (p.isDefined) {
      p.get match {
        case step: Step =>
          for (name <- step.depends) {
            if (!depends.contains(name)) {
              depends += name
            }
          }
        case _ =>
          ()
      }
      p = p.get.parent
    }

    var exprString = ""
    val usesContextItem = try {
      if (_avt.isDefined) {
        exprString = _avt.toString
        val avt = staticContext.parseAvt(_avt.get)
        staticContext.dependsOnContextAvt(avt)
      } else {
        exprString = _select.getOrElse("()")
        staticContext.dependsOnContextString(_select.getOrElse("()"))
      }
    } catch {
      case ex: Throwable =>
        throw XProcException.xsStaticErrorInExpression(exprString, ex.getMessage, location)
    }

    val ds = ListBuffer.empty[DataSource]
    for (child <- allChildren) {
      child match {
        case pipe: Pipe =>
          if (static) {
            throw XProcException.xsStaticRefsContext("Static variables cannot refer to the context.", location)
          }
          ds += pipe
        case source: DataSource =>
          ds += source
        case _ =>
          throw new RuntimeException(s"Unexpected child: $child")
      }
    }

    val env = environment()
    val drp = env.defaultReadablePort

    if (ds.isEmpty) {
      if (drp.isDefined && !static && usesContextItem) {
        val winput = new WithInput(config)
        winput.port = "source"
        addChild(winput)
        val pipe = new Pipe(config)
        pipe.port = drp.get.port
        pipe.step = drp.get.step.stepName
        pipe.link = drp.get
        winput.addChild(pipe)
      }
    } else {
      removeChildren()
      val winput = new WithInput(config)
      winput.port = "source"
      addChild(winput)
      for (source <- ds) {
        winput.addChild(source)
      }
    }

    if (static) {
      val context = ListBuffer.empty[Message]
      for (child <- ds) {
        child match {
          case inline: Inline =>
            val exprContext = staticContext.withStatics(inScopeStatics)
            val expander = new TvtExpander(config, None, exprContext, Map(), location)

            try {
              // FIXME: what should the defaults for initiallyExpand and exludeURIs be?
              val result = expander.expand(inline.node, true, Set())
              context += new XdmNodeItemMessage(result, XProcMetadata.XML, inline.staticContext)
            } catch {
              case ex: XProcException =>
                if (ex.code == XProcException.xs0107 && ex.details(1).toString.contains("Undeclared variable")) {
                  throw XProcException.xsStaticRefsNonStaticStr(ex.details.head.toString, location)
                }
                throw ex
            }

          case doc: Document =>
            for (ref <- staticContext.findVariableRefsInAvt(doc.hrefAvt)) {
              if (!inScopeStatics.contains(ref.getClarkName)) {
                throw XProcException.xsStaticRefsNonStatic(ref, location)
              }
            }
            val econtext = staticContext.withStatics(inScopeStatics)
            val exprEval = config.expressionEvaluator.newInstance()
            val vtexpr = new XProcVtExpression(econtext, doc.hrefAvt)
            val value = exprEval.singletonValue(vtexpr, List(), inScopeStatics, None)
            val parts = ListBuffer.empty[String]
            val viter = value.item.iterator()
            while (viter.hasNext) {
              parts += viter.next().getStringValue
            }

            val href = staticContext.baseURI.get.resolve(parts.mkString(""))

            val request = new DocumentRequest(href, None, location)
            val response = config.documentManager.parse(request)
            context += new XdmValueItemMessage(response.value, XProcMetadata.XML, doc.staticContext)
          case empty: Empty =>
            ()
        }
      }

      val expr = new XProcXPathExpression(staticContext, select.get)
      val exeval = config.expressionEvaluator.newInstance()
      val msg = exeval.value(expr, context.toList, inScopeStatics, None)
      staticValue = msg
      resolvedStatically = true
    }

    if (_select.isDefined) {
      val bindings = mutable.HashSet.empty[QName]
      bindings ++= staticContext.findVariableRefsInString(_select.get)
      if (bindings.isEmpty) {
        try {
          val depends = staticContext.dependsOnContextString(_select.get)
          // FIXME: if depends is false, we can resolve this statically
        } catch {
          case sax: SaxonApiException => ()
        }
      } else {
        for (ref <- bindings) {
          val binding = env.variable(ref)
          if (binding.isEmpty) {
            throw XProcException.xsNoBindingInExpression(ref, location);
          }
          if (!binding.get.static) {
            val pipe = new NamePipe(config, ref, binding.get.tumble_id, binding.get)
            _dependentNameBindings += pipe
            addChild(pipe)
          }
        }
      }
    }
  }

  override protected[model] def validateStructure(): Unit = {
    var hasEmpty = false
    var hasNonEmpty = false

    for (child <- allChildren) {
      child.validateStructure()
      child match {
        case winput: WithInput => ()
        case npipe: NamePipe => ()
        case _ =>
          throw new RuntimeException(s"Invalid content in $this")
      }
    }

    if (hasEmpty && hasNonEmpty) {
      throw XProcException.xsNoSiblingsOnEmpty(location)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    if (resolvedStatically) {
      return
    }

    for (child <- allChildren) {
      child.graphEdges(runtime, _graphNode.get)
    }
  }
}
