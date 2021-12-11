package com.xmlcalabash.model.xxml

import com.jafpl.messages.Message
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XProcXPathExpression}
import com.xmlcalabash.util.{MediaType, S9Api, XdmLocation}
import net.sf.saxon.ma.map.{MapItem, MapType}
import net.sf.saxon.s9api.{Axis, QName, XdmMap, XdmNode, XdmNodeKind}
import org.slf4j.{Logger, LoggerFactory}

import java.net.URI
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.IteratorHasAsScala

object NodeHierarchy {
  def newInstance(config: XMLCalabash, node: XdmNode): NodeHierarchy = {
    config.staticStepsIndeterminate = true

    val hier = new NodeHierarchy(config, node, Option(node.getBaseURI), List())

    var changed = false
    if (hier.conditionalImports.nonEmpty) {
      hier._conditionalImports.clear()
      changed = hier.resolve()
      while (changed) {
        hier._conditionalImports.clear()
        changed = hier.resolve()
      }
      if (hier.conditionalImports.nonEmpty) {
        val node = hier.conditionalImports.head
        throw XProcException.xsUseWhenDeadlock(hier.useWhenExpression(node).get, XdmLocation.from(node))
      }
    }

    config.staticStepsIndeterminate = false

    changed = hier.resolve()
    while (changed) {
      changed = hier.resolve()
    }

    hier.resolveDeadlocks()

    hier.unifyStatus()

    hier
  }
}

class NodeHierarchy private(config: XMLCalabash, node: XdmNode, val baseURI: Option[URI], ancestors: List[NodeHierarchy]) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val _imports = mutable.HashMap.empty[URI, NodeHierarchy]
  private val _reimports = mutable.HashSet.empty[NodeHierarchy]
  private val useWhenStatus = mutable.HashMap.empty[XdmNode, Option[Boolean]]
  private val _knownSteps = mutable.HashSet.empty[QName]
  private val _conditionalSteps = mutable.HashMap.empty[QName, mutable.HashSet[XdmNode]]
  private val _conditionalImports = mutable.HashSet.empty[XdmNode]
  private val _root = S9Api.documentElement(node)
  private var changed = false
  private var _ximport = Option.empty[XImport]

  if (_root.isEmpty) {
    throw XProcException.xsNotAPipeline()
  }

  if (root.getNodeName != XProcConstants.p_library && root.getNodeName != XProcConstants.p_declare_step) {
    throw XProcException.xsNotAPipeline(root.getNodeName)
  }

  recurse(root)

  def root: XdmNode = _root.get
  def imports: List[NodeHierarchy] = _imports.values.toList
  def reimports: Set[NodeHierarchy] = _reimports.toSet
  def knownSteps: Set[QName] = _knownSteps.toSet
  def conditionalSteps: Set[QName] = {
    val steps = mutable.HashSet.empty[QName] ++ _conditionalSteps.keySet
    steps.toSet
  }
  def conditionalImports: Set[XdmNode] = _conditionalImports.toSet
  def useWhen(node: XdmNode): Boolean = {
    if (useWhenStatus.contains(node)) {
      (useWhenStatus(node).get)
    } else {
      true
    }
  }

  protected[xxml] def ximport: Option[XImport] = _ximport
  protected[xxml] def ximport_=(xi: XImport): Unit = {
    _ximport = Some(xi)
  }

  def imported(node: XdmNode): Option[NodeHierarchy] = {
    val href = if (Option(node.getAttributeValue(XProcConstants._href)).isDefined) {
      if (Option(node.getBaseURI).isDefined) {
        node.getBaseURI.resolve(node.getAttributeValue(XProcConstants._href))
      } else {
        // If this wasn't absolute, we'd have thrown an exception in NodeHierarchy
        new URI(node.getAttributeValue(XProcConstants._href))
      }
    } else {
      throw XProcException.xsMissingRequiredAttribute(XProcConstants._href, XdmLocation.from(node))
    }

    _imports.get(href)
  }

  private def recurse(node: XdmNode): Unit = {
    val useWhen = testUseWhen(node)

    if (useWhen.isEmpty && node.getNodeName == XProcConstants.p_import) {
      _conditionalImports += node
    }

    if (useWhen.getOrElse(false)) {
      node.getNodeName match {
        case XProcConstants.p_import =>
          processImport(node)

        case XProcConstants.p_option =>
          val context = new XMLStaticContext(node)
          if (Option(node.getAttributeValue(XProcConstants._name)).isDefined) {
            try {
              val name = context.parseQName(node.getAttributeValue(XProcConstants._name))

              if (Option(node.getAttributeValue(XProcConstants._static)).getOrElse("false") == "true") {
                val value = staticOptionValue(node)
                config.addStatic(name, value)
              } else {
                if (node.getParent.getNodeName == XProcConstants.p_library) {
                  throw XProcException.xsOptionMustBeStatic(name, XdmLocation.from(node))
                }
              }
            } catch {
                case ex: XProcException =>
                  // Map err:XD0015 to err:XS0087 in this case
                  if (ex.code == XProcException.errxd(15)) {
                    throw XProcException.xsOptionUndeclaredNamespace(node.getAttributeValue(XProcConstants._name), ex.location)
                  } else {
                    throw ex
                  }
                case ex: Exception =>
                  throw ex
              }
            }

        case _ => ()
      }

      for (child <- node.axisIterator(Axis.CHILD).asScala) {
        if (child.getNodeKind == XdmNodeKind.ELEMENT) {
          recurse(child)
        }
      }
    }
  }

  private def processImport(node: XdmNode): Unit = {
    if (Option(node.getAttributeValue(XProcConstants._href)).isDefined) {
      val uri = if (baseURI.isDefined) {
        node.getBaseURI.resolve(node.getAttributeValue(XProcConstants._href))
      } else {
        val hrefuri = new URI(node.getAttributeValue(XProcConstants._href))
        if (!hrefuri.isAbsolute) {
          throw XProcException.xdInvalidURI(hrefuri.toString, None)
        }
        hrefuri
      }

      var ancestor = Option.empty[NodeHierarchy]
      for (hier <- ancestors) {
        if (hier.baseURI.isDefined && hier.baseURI.get == uri) {
          ancestor = Some(hier)
        }
      }

      if (_imports.contains(uri)) {
        // nevermind, there's nothing gained from importing it twice at the same level
      } else if (ancestor.isDefined) {
        _reimports += ancestor.get
      } else {
        val newstack = ListBuffer.empty[NodeHierarchy] ++ ancestors
        newstack += this
        val request = new DocumentRequest(uri, MediaType.XML)
        try {
          val response = config.documentManager.parse(request)
          if (response.contentType.xmlContentType) {
            val hier = new NodeHierarchy(config, response.value.asInstanceOf[XdmNode], Some(uri), newstack.toList)

            if (hier.root.getNodeName == XProcConstants.p_declare_step
              && Option(hier.root.getAttributeValue(XProcConstants._type)).isEmpty) {
              throw XProcException.xsStepTypeRequired(XdmLocation.from(root))
            }

            _imports.put(uri, hier)
            _conditionalImports ++= hier._conditionalImports
            changed = true
          } else {
            throw XProcException.xsInvalidPipeline(s"Document is not XML: ${uri}", None)
          }
        } catch {
          case ex: XProcException =>
            if (ex.code == XProcException.errxs(53)) {
              throw ex
            } else {
              throw XProcException.xsImportFailed(uri, None)
            }
          case _: Exception =>
            throw XProcException.xsImportFailed(uri, None)
        }
      }
    }
  }

  private def useWhenExpression(node: XdmNode): Option[String] = {
    if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
      Option(node.getAttributeValue(XProcConstants._use_when))
    } else {
      Option(node.getAttributeValue(XProcConstants.p_use_when))
    }
  }

  private def testUseWhen(node: XdmNode): Option[Boolean] = {
    var context = Option.empty[StaticContext]
    var stepType = Option.empty[QName]
    if (node.getNodeName == XProcConstants.p_declare_step
      && Option(node.getAttributeValue(XProcConstants._type)).isDefined) {
      context = Some(new StaticContext(config, node))
      stepType = Some(context.get.parseQName(node.getAttributeValue(XProcConstants._type)))
      if (_conditionalSteps.contains(stepType.get)) {
        _conditionalSteps(stepType.get) -= node
      }
    }
    if (node.getNodeName == XProcConstants.p_import) {
      _conditionalImports -= node
    }

    val usewhenexpr = useWhenExpression(node)
    if (usewhenexpr.isEmpty) {
      if (stepType.isDefined && pipelineStep(node).getOrElse(false)) {
        _knownSteps += stepType.get
      }
      return Some(true)
    }

    var useWhen = Option.empty[Boolean]
    if (useWhenStatus.contains(node)) {
      useWhen = useWhenStatus(node)
    }
    if (useWhen.isDefined) {
      return useWhen
    }

    if (context.isEmpty) {
      context = Some(new StaticContext(config, node))
    }

    try {
      val expr = new XProcXPathExpression(context.get, usewhenexpr.get)
      val bindings = mutable.HashMap.empty[String, Message]
      for ((name, value) <- config.staticOptions) {
        bindings.put(name.getClarkName, value)
      }
      config.staticStepsAvailable = _knownSteps.toSet
      useWhen = Some(config.expressionEvaluator.booleanValue(expr, List(), bindings.toMap, None))
      logger.debug(s"Use-when: ${useWhen.get}: ${usewhenexpr.get}")
      changed = true

      if (node.getNodeName == XProcConstants.p_import) {
        processImport(node)
      }
    } catch {
      case ex: XProcException =>
        if (ex.code == XProcException.err_stepAvailableIndeterminate) {
          useWhen = None
        } else {
          throw ex
        }
      case ex: Exception =>
        throw ex
    }
    useWhenStatus.put(node, useWhen)

    if (stepType.isDefined) {
      if (useWhen.isEmpty) {
        if (_conditionalSteps.contains(stepType.get)) {
          _conditionalSteps(stepType.get) += node
        } else {
          val set = mutable.HashSet.empty[XdmNode]
          set += node
          _conditionalSteps.put(stepType.get, set)
        }
      } else {
        if (useWhen.get && pipelineStep(node).getOrElse(false)) {
          _knownSteps += stepType.get
        }
      }
    }

    if (useWhen.isEmpty && node.getNodeName == XProcConstants.p_import) {
      _conditionalImports += node
    }

    useWhen
  }

  private def resolve(): Boolean = {
    changed = false

    for (ximport <- imports) {
      _knownSteps ++= ximport.knownSteps
    }
    for (ximport <- _reimports) {
      _knownSteps ++= ximport.knownSteps
    }

    walk(_root.get)

    for (ximport <- imports) {
      ximport.resolve()
      changed = changed || ximport.changed
    }

    changed
  }

  private def walk(node: XdmNode): Unit = {
    val useWhen = testUseWhen(node)

    if (!useWhen.getOrElse(false)) {
      return
    }

    if (node.getNodeName == XProcConstants.p_declare_step && Option(node.getAttributeValue(XProcConstants._type)).isDefined) {
      val context = new StaticContext(config, node)
      if (pipelineStep(node).getOrElse(false)) {
        _knownSteps += context.parseQName(node.getAttributeValue(XProcConstants._type))
      }
    }
    if (node.getNodeName == XProcConstants.p_declare_step || node.getNodeName == XProcConstants.p_library) {
      for (child <- node.axisIterator(Axis.CHILD).asScala) {
        if (child.getNodeKind == XdmNodeKind.ELEMENT
          && child.getNodeName == XProcConstants.p_declare_step
          && Option(child.getAttributeValue(XProcConstants._type)).isDefined) {
          val uw = testUseWhen(child)
          if (uw.getOrElse(false)) {
            val context = new StaticContext(config, child)
            if (pipelineStep(child).getOrElse(false)) {
              _knownSteps += context.parseQName(child.getAttributeValue(XProcConstants._type))
            }
          }
        }
      }
    }

    for (child <- node.axisIterator(Axis.CHILD).asScala) {
      if (child.getNodeKind == XdmNodeKind.ELEMENT) {
        walk(child)
      }
    }
 }

  private def pipelineStep(node: XdmNode): Option[Boolean] = {
    for (step <- node.axisIterator(Axis.CHILD).asScala) {
      if (step.getNodeKind == XdmNodeKind.ELEMENT) {
        step.getNodeName match {
          case XProcConstants.p_input => ()
          case XProcConstants.p_output => ()
          case XProcConstants.p_option => ()
          case XProcConstants.p_documentation => ()
          case XProcConstants.p_pipeinfo => ()
          case XProcConstants.p_import => ()
          case XProcConstants.p_import_functions => ()
          case XProcConstants.p_declare_step => ()
          case _ =>
            val usewhenexpr = useWhenExpression(step)
            if (usewhenexpr.isDefined) {
              var useWhen = Option.empty[Boolean]
              if (useWhenStatus.contains(step)) {
                useWhen = useWhenStatus(step)
              }
              if (usewhenexpr.isDefined && useWhen.isEmpty) {
                return None
              } else {
                if (useWhen.get) {
                  return useWhen
                }
              }
            } else {
              return Some(true)
            }
        }
      }
    }
    Some(false)
  }

  private def resolveDeadlocks(): Unit = {
    walkDeadlocks(_root.get)
    for (ximport <- imports) {
      ximport.resolveDeadlocks()
    }
  }

  private def walkDeadlocks(node: XdmNode): Unit = {
    val usewhenexpr = useWhenExpression(node)
    var useWhen = Option.empty[Boolean]
    if (useWhenStatus.contains(node)) {
      useWhen = useWhenStatus(node)
    }
    if (useWhen.isEmpty && usewhenexpr.isDefined) {
      throw XProcException.xsUseWhenDeadlock(usewhenexpr.get, XdmLocation.from(node))
    } else {
      useWhen = Some(true)
    }

    if (useWhen.getOrElse(false)) {
      for (child <- node.axisIterator(Axis.CHILD).asScala) {
        if (child.getNodeKind == XdmNodeKind.ELEMENT) {
          walkDeadlocks(child)
        }
      }
    }
  }

  private def unifyStatus(): Unit = {
    for (ximport <- imports) {
      ximport.unifyStatus()
      useWhenStatus ++= ximport.useWhenStatus
    }
  }

  private def staticOptionValue(node: XdmNode): XdmValueItemMessage = {
    if (Option(node.getAttributeValue(XProcConstants._name)).isEmpty) {
      throw XProcException.xsMissingRequiredAttribute(XProcConstants._name, XdmLocation.from(node))
    }

    val context = new XMLStaticContext(node)
    val name = context.parseQName(node.getAttributeValue(XProcConstants._name))

    if (Option(node.getAttributeValue(XProcConstants._required)).isDefined) {
      if (node.getAttributeValue(XProcConstants._required) == "true") {
        throw XProcException.xsRequiredAndStatic(name, XdmLocation.from(node))
      }
    }
    if (Option(node.getAttributeValue(XProcConstants._select)).isEmpty) {
      throw XProcException.xsMissingRequiredAttribute(XProcConstants._select, XdmLocation.from(node))
    }
    var value = if (config.options.contains(name)) {
      new XdmValueItemMessage(config.options(name).value, XProcMetadata.ANY, config.options(name).context)
    } else {
      val select = node.getAttributeValue(XProcConstants._select)
      val expr = new XProcXPathExpression(context, select)
      val bindings = mutable.HashMap.empty[String, XdmValueItemMessage]
      for ((name, value) <- config.staticOptions) {
        bindings.put(name.getClarkName, value)
      }
      config.expressionEvaluator.value(expr, List(), bindings.toMap, None)
    }

    var qnameKeys = false
    val as = Option(node.getAttributeValue(XProcConstants._as))
    var declaredType = context.parseSequenceType(as, config.itemTypeFactory)
    if (declaredType.isDefined) {
      declaredType.get.getUnderlyingSequenceType.getPrimaryType match {
        case map: MapType =>
          if (map.getKeyType.getPrimitiveItemType.getTypeName == XProcConstants.structured_xs_QName) {
            // We have to lie about the type of maps with QName keys because we're
            // going to allow users to put strings in there.
            qnameKeys = true
            declaredType = Some(context.parseFakeMapSequenceType(as.get, config.itemTypeFactory))
          }
        case _ => ()
      }

      if (qnameKeys) {
        value.item match {
          case xmap: XdmMap =>
            val qnameMap = S9Api.forceQNameKeys(xmap.getUnderlyingValue, value.context)
            value = new XdmValueItemMessage(qnameMap, value.metadata, value.context)
          case xmap: MapItem =>
            val qnameMap = S9Api.forceQNameKeys(xmap, value.context)
            value = new XdmValueItemMessage(qnameMap, value.metadata, value.context)
          case _ =>
            throw XProcException.xiThisCantHappen(s"Non-map item has qnameKeys: ${value.item}")
        }
      }

      val tokens = XNameBinding.checkValueTokens(config, context, Option(node.getAttributeValue(XProcConstants._values)))
      XNameBinding.promotedValue(config, name, declaredType, tokens, value)
    } else {
      value
    }
  }
}
