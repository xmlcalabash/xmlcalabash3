package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.params.SelectFilterParams
import com.xmlcalabash.runtime.{StaticContext, XMLCalabashRuntime, XmlPortSpecification}
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.functions.ConstantFunction.{False, True}
import net.sf.saxon.s9api.{QName, XdmNode}

class WithInput(override val config: XMLCalabashConfig) extends Port(config) {
  private val _exclude_inline_prefixes = List.empty[String]
  private var _context: StaticContext = _
  private var portSpecified = false

  def exclude_inline_prefixes: List[String] = _exclude_inline_prefixes

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (attributes.contains(XProcConstants._port)) {
      portSpecified = true
      _port = staticContext.parseNCName(attr(XProcConstants._port)).get
    }
    _context = new StaticContext(config, Some(this), node)
    _select = attr(XProcConstants._select)

    _href = attr(XProcConstants._href)
    _pipe = attr(XProcConstants._pipe)

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    if (portSpecified) {
      parent.get match {
        case _: ForEach =>
          throw XProcException.xsPortNotAllowed(_port, "p:for-each", location)
        case _: Viewport =>
          throw XProcException.xsPortNotAllowed(_port, "p:viewport", location)
        case _: Choose =>
          throw XProcException.xsPortNotAllowed(_port, "p:choose or p:if", location)
        case _: When =>
          throw XProcException.xsPortNotAllowed(_port, "p:when", location)
        case _: ForLoop =>
          throw XProcException.xsPortNotAllowed(_port, "cx:loop", location)
        case _: ForUntil =>
          throw XProcException.xsPortNotAllowed(_port, "cx:until", location)
        case _: ForWhile =>
          throw XProcException.xsPortNotAllowed(_port, "cx:while", location)
        case _ => ()
      }
    }

    examineBindings()
    super.makeBindingsExplicit()

    var primaryInput = primary
    parent.get match {
      case atom: AtomicStep =>
        val decl = declaration(atom.stepType).get
        primaryInput = decl.input(_port, None).primary
        sequence = decl.input(_port, None).sequence
      case _ => ()
    }

    if (allChildren.nonEmpty) {
      // If there are explicit children, we'll check them elsewhere
      return
    }

    val env = environment()
    val drp = env.defaultReadablePort

    if (primaryInput && drp.isDefined) {
      val pipe = new Pipe(config)
      pipe.port = drp.get.port
      pipe.step = drp.get.step.stepName
      pipe.link = drp.get
      addChild(pipe)
      return
    }

    // This is err:XS0032 unless it's a special case

    // One of the special cases is, if this is an atomic step, and that
    // step has a default input for this port, then this is not an error,
    // the default input should be used. UGH.
    parent.get match {
      case atomic: AtomicStep =>
        if (declaration(atomic.stepType).isDefined) {
          val sig = declaration(atomic.stepType).get
          val psig = sig.input(_port, location)
          if (psig.defaultBindings.nonEmpty) {
            for (binding <- psig.defaultBindings) {
              binding match {
                case empty: Empty =>
                  addChild(new Empty(empty))
                case inline: Inline =>
                  val x= new Inline(inline)
                  addChild(x)
                case document: Document =>
                  addChild(new Document(document))
                case _ =>
                  throw XProcException.xiThisCantHappen("Default input is not empty, inline, or document", location)
              }
            }
            return
          }
        }
      case _ => ()
    }

    var raiseError = true
    if (synthetic) {
      // All the other special cases involve synthetic elements
      parent.get match {
        case _: Choose => raiseError = false
        case _: When => raiseError = false
        case _: Otherwise => raiseError = false
        case _ =>
          if (parent.get.parent.isDefined
            && parent.get.parent.get.synthetic
            && parent.get.parent.get.isInstanceOf[Otherwise]) {
            raiseError = false
          }
      }
    }

    if (raiseError) {
      if (primary) {
        throw XProcException.xsUnconnectedPrimaryInputPort(step.stepName, port, location)
      } else {
        throw XProcException.xsUnconnectedInputPort(step.stepName, port, location)
      }
    }
  }

  override protected[model] def addFilters(): Unit = {
    super.addFilters()
    if (select.isEmpty) {
      return
    }

    val context = staticContext.withStatics(inScopeStatics)

    val ispec = if (sequence) {
      XmlPortSpecification.ANYSOURCESEQ
    } else {
      XmlPortSpecification.ANYSOURCE
    }

    val params = new SelectFilterParams(context, select.get, port, ispec)
    val filter = new AtomicStep(config, params)
    filter.stepType = XProcConstants.cx_select_filter

    val finput = new WithInput(config)
    finput.port = "source"
    finput.primary = true

    val foutput = new WithOutput(config)
    foutput.port = "result"
    foutput.primary = true

    filter.addChild(finput)
    filter.addChild(foutput)

    for (name <- staticContext.findVariableRefsInString(_select.get)) {
      var binding = _inScopeDynamics.get(name)
      if (binding.isDefined) {
        val npipe = new NamePipe(config, name, binding.get.tumble_id, binding.get)
        filter.addChild(npipe)
      } else {
        binding = _inScopeStatics.get(name.getClarkName)
        if (binding.isEmpty) {
          throw new RuntimeException(s"Reference to variable not in scope: $name")
        }
      }
    }

    val step = parent.get
    val container = step.parent.get.asInstanceOf[Container]
    container.addChild(filter, step)

    for (child <- allChildren) {
      child match {
        case pipe: Pipe =>
          finput.addChild(pipe)
      }
    }
    removeChildren()

    val pipe = new Pipe(config)
    pipe.step = filter.stepName
    pipe.port = "result"
    pipe.link = foutput
    addChild(pipe)
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parNode: Node): Unit = {
    for (child <- allChildren) {
      child.graphEdges(runtime, parNode)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startWithInput(tumble_id, tumble_id, port)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endWithInput()
  }

  override def toString: String = {
    if (tumble_id.startsWith("!syn")) {
      s"p:with-input $port"
    } else {
      s"p:with-input $port $tumble_id"
    }
  }
}
