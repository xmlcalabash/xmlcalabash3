package com.xmlcalabash.model.xxml

import com.jafpl.graph.Node
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, SaxonApiException}

import scala.collection.mutable

abstract class XStep(config: XMLCalabash) extends XArtifact(config) with XNamedArtifact with XGraphableArtifact {
  private var _name = Option.empty[String]
  private var _drp = Option.empty[XPort]
  protected var _type: Option[QName] = None
  protected[xxml] val dependsOn = mutable.HashMap.empty[String, Option[XStep]]

  def name: Option[String] = _name

  def stepName: String = _name.getOrElse(tumble_id)

  protected[xxml] def stepName_=(name: String): Unit = {
    try {
      _name = Some(staticContext.parseNCName(name))
    } catch {
      case ex: Exception =>
        error(ex)
    }
  }

  protected[xxml] def drp: Option[XPort] = _drp

  protected[xxml] def drp_=(port: Option[XPort]): Unit = {
    _drp = port
  }

  def primaryOutput: Option[XPort]

  def outputs: Set[XOutput] = {
    val decl = stepDeclaration
    if (decl.isDefined) {
      decl.get.children[XOutput].toSet
    } else {
      children[XOutput].toSet
    }
  }

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()

    val pstep = if (synthetic) {
      syntheticName.get.getNamespaceURI == XProcConstants.ns_p
    } else {
      staticContext.nodeName.getNamespaceURI == XProcConstants.ns_p
    }

    for (name <- attributes.keySet) {
      if (pstep && name.getNamespaceURI == XProcConstants.ns_p) {
        error(XProcException.xsXProcNamespaceError(name, staticContext.nodeName, location))
      }

      name match {
        case XProcConstants._name =>
          val aname = attr(XProcConstants._name)
          try {
            stepName = staticContext.parseNCName(aname.get)
          } catch {
            case _: SaxonApiException =>
              error(XProcException.xsBadTypeValue(aname.get, "xs:NCName", None))
          }
        case XProcConstants.p_message =>
          if (!pstep) {
            syntheticOption(XProcConstants.p_message, attr(name).get)
          }
        case XProcConstants._message =>
          if (pstep) {
            syntheticOption(XProcConstants._message, attr(name).get)
          }
        case XProcConstants.p_depends =>
          if (!pstep) {
            depends(attr(name).get)
          }
        case XProcConstants._depends =>
          if (pstep) {
            staticContext.nodeName.getLocalName match {
              case "when" => error(XProcException.xsBadAttribute(name, location))
              case "otherwise" => error(XProcException.xsBadAttribute(name, location))
              case "catch" => error(XProcException.xsBadAttribute(name, location))
              case "finally" => error(XProcException.xsBadAttribute(name, location))
              case _ => depends(attr(name).get)
            }
          }
        case XProcConstants.p_expand_text =>
          if (!pstep) {
            val value = attr(name).get
            if (value != "true" && value != "false") {
              error(XProcException.xsInvalidExpandText(name, value, location))
            }
          } else {
            error(XProcException.xsBadAttribute(name, None))
          }
        case XProcConstants._expand_text =>
          if (pstep) {
            val value = attr(name).get
            if (value != "true" && value != "false") {
              error(XProcException.xsInvalidExpandText(name, value, location))
            }
          } else {
            error(XProcException.xsBadAttribute(name, None))
          }
        case XProcConstants.p_timeout =>
          if (!pstep) {
            syntheticOption(name, attr(name).get)
          }
        case XProcConstants._timeout =>
          if (pstep) {
            syntheticOption(name, attr(name).get)
          }
        case _ =>
          ()
      }
    }
  }

  protected def syntheticOption(name: QName, value: String): Unit = {
    val option = new XWithOption(this, name, Some(value), None)
    addChild(option)
  }

  private def depends(value: String): Unit = {
    if (value.trim == "") {
      error(XProcException.xsBadTypeEmpty(value, location))
      return
    }

    for (name <- value.trim.split("\\s+")) {
      try {
        staticContext.parseNCName(name)
        dependsOn.put(name, None)
      } catch {
        case _: Exception =>
          error(XProcException.xsBadTypeValue(name, "xs:NCName", location))
      }
    }
  }

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    _drp = initial
    super.elaborateDefaultReadablePort(initial)
  }

  override protected[xxml] def elaborateDependsConnections(inScopeSteps: Map[String, XStep]): Unit = {
    for (name <- dependsOn.keySet) {
      if (inScopeSteps.contains(name)) {
        dependsOn.put(name, Some(inScopeSteps(name)))
      } else {
        error(XProcException.xsNotAStep(name, location))
      }
    }
    super.elaborateDependsConnections(inScopeSteps)
  }

  def container: XContainer = {
    val art = if (parent.isDefined) {
      parent.get
    } else {
      this
    }

    art match {
      case decl: XContainer =>
        decl
      case _ =>
        throw XProcException.xiThisCantHappen("Parent of step isn't a container?")
    }
  }

  def declarationContainer: XDeclContainer = {
    var art = parent
    while (art.isDefined) {
      art.get match {
        case decl: XDeclContainer =>
          return decl
        case _ => ()
      }
      art = art.get.parent
    }

    if (art.isEmpty) {
      this match {
        case decl: XDeclContainer =>
          return decl
        case _ => ()
      }
    }

    throw XProcException.xiThisCantHappen("No ancestor of step is a declaration container?")
  }

  protected def elaborateDynamicOptions(): Unit = {
    children[XStep] foreach { _.elaborateDynamicOptions() }
  }

  protected[xxml] def elaborateInsertSelectFilters(): Unit = {
    if (parent.isDefined && parent.get.isInstanceOf[XContainer]) {
      // If we're in a container, we could add filters
      val container = parent.get.asInstanceOf[XContainer]

      for (child <- children[XPort]) {
        val filterable = child match {
          case _: XInput => true
          case _: XWithInput => true
          case _ => false
        }
        if (filterable && child.select.isDefined) {
          addInputFilter(child, new XSelectFilter(container, this, child))
        }
      }
    }

    children[XStep] foreach { _.elaborateInsertSelectFilters() }
  }

  protected[xxml] def elaborateInsertContentTypeFilters(): Unit = {
    if (parent.isDefined && parent.get.isInstanceOf[XContainer]) {
      // If we're in a container, we could add filters
      val container = parent.get.asInstanceOf[XContainer]

      // We don't have to filter compound steps because they'll have
      // filters for their inputs.
      if (stepDeclaration.get.atomic) {
        for (child <- children[XWithInput]) {
          if (!MediaType.OCTET_STREAM.allowed(child.contentTypes)) {
            var filter = false
            for (pipe <- child.children[XPipe]) {
              val from = pipe.from
              if (from.isDefined) {
                for (ctype <- pipe.from.get.contentTypes filter { _.inclusive }) {
                  filter = filter || !ctype.allowed(child.contentTypes)
                }
              } else {
                throw XProcException.xiThisCantHappen("Pipe has unknown origin.")
              }
            }

            if (filter) {
              addInputFilter(child, new XContentTypeChecker(container, child))
            }
          }
        }
      }
    }

    for (child <- children[XStep]) {
      child.elaborateInsertContentTypeFilters()
    }
  }

  protected def addInputFilter(child: XPort, filter: XStep): Unit = {
    val container = parent.get match {
      case cont: XContainer => cont
      case _ =>
        error(XProcException.xiThisCantHappen("Parent of filtering step isn't a container?"))
        return
    }

    val filterxwi = new XWithInput(filter, "source")
    filterxwi.allChildren = child.allChildren
    val filterxwo = new XWithOutput(filter, "result")

    val stepxwi = new XWithInput(this, child.port)

    insertBefore(stepxwi, child)
    removeChild(child)

    stepxwi.validate()

    filter.addChild(filterxwi)

    if (child.selectBindings.nonEmpty) {
      filter.addChild(child.selectBindings.get)
    }

    filter.addChild(filterxwo)

    val pipe = new XPipe(stepxwi, filterxwo)
    stepxwi.addChild(pipe)

    container.insertBefore(filter, this)
    filterxwi.validate()
    filterxwo.validate()
  }

  def withinTryCatch: Boolean = {
    var par: Option[XArtifact] = Some(this)
    while (par.isDefined) {
      par.get match {
        case _: XTry =>
          return true
        case _ =>
          ()
      }
      par = par.get.parent
    }
    false
  }

  def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    for (child <- allChildren) {
      child match {
        case graphable: XGraphableArtifact =>
          graphable.graphNodes(runtime, parent)
        case _ => ()
      }
    }
  }

  protected def graphEdges(runtime: XMLCalabashRuntime): Unit = {
    for (child <- allChildren) {
      child match {
        case step: XStep =>
          step.graphEdges(runtime)
        case input: XInput =>
          input.graphEdges(runtime)
        case input: XWithInput =>
          input.graphEdges(runtime)
        case output: XOutput =>
          output.graphEdges(runtime)
        case nb: XNameBinding =>
          nb.graphEdges(runtime)
        case _ =>
          ()
      }
    }

    for (name <- dependsOn.keySet) {
      val step = dependsOn(name).get
      val node = runtime.node(this)
      runtime.graph.addOrderedEdge(runtime.node(step), "#depends_from", node, "#depends_to")
    }
  }

  override def toString: String = {
    if (stepName != tumble_id) {
      s"${staticContext.nodeName}(${stepName};${tumble_id})"
    } else {
      s"${staticContext.nodeName}(${stepName})"
    }
  }
}
