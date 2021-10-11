package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Node}
import com.jafpl.steps.Manifold
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.runtime.params.ContentTypeCheckerParams
import com.xmlcalabash.util.MediaType
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Viewport(override val config: XMLCalabashConfig) extends Container(config) with NamedArtifact {
  private var _match: String = _
  protected var _dependentNameBindings: ListBuffer[NamePipe] = ListBuffer.empty[NamePipe]

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (attributes.contains(XProcConstants._match)) {
      _match = attr(XProcConstants._match).get
    } else {
      throw new RuntimeException("Viewport must have match")
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    val first = firstChild
    if (firstWithInput.isDefined) {
      val fwi = firstWithInput.get
      fwi.port match {
        case "" =>
          // It may be anonymous in XProc, but it mustn't be anonymous in the graph
          fwi.port = "source"
        case "source" => ()
        case _ => throw XProcException.xiThisCantHappen(s"Viewport withinput is named '${fwi.port}''", location)
      }
    } else {
      val input = new WithInput(config)
      input.port = "source"
      input.primary = true
      addChild(input, first)
    }

    val current = new DeclareInput(config)
    current.port = "current"
    current.primary = true
    addChild(current, first)

    makeContainerStructureExplicit()
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    super.makeBindingsExplicit()

    val env = environment()

    val bindings = mutable.HashSet.empty[QName]
    bindings ++= staticContext.findVariableRefsInString(_match)

    for (ref <- bindings) {
      val binding = env.variable(ref)
      if (binding.isEmpty) {
        throw XProcException.xsStaticErrorInExpression(s"$$${ref.toString}", "Reference to undefined variable", location)
      }
      if (!binding.get.static) {
        val pipe = new NamePipe(config, ref, binding.get.tumble_id, binding.get)
        _dependentNameBindings += pipe
        addChild(pipe)
      }
    }
  }

  override protected[model] def normalizeToPipes(): Unit = {
    super.normalizeToPipes()

    // Viewport needs a content type checker in front of its source
    val input = children[WithInput].head

    logger.debug(s"Adding content-type-checker for viewport source")
    val params = new ContentTypeCheckerParams(input.port, List(MediaType.XML, MediaType.HTML, MediaType.XHTML), staticContext, None,
      XProcException.xd0072, inputPort = true, true)
    val atomic = new AtomicStep(config, params)
    atomic.stepType = XProcConstants.cx_content_type_checker
    // Put this outside the viewport so that its output is attached to the viewport input, not the viewport output
    parent.get.addChild(atomic)

    val winput = new WithInput(config)
    winput.port = "source"
    atomic.addChild(winput)

    val woutput = new WithOutput(config)
    woutput.port = "result"
    atomic.addChild(woutput)

    val pipes = ListBuffer.empty[Pipe] ++ input.children[Pipe]
    input.removeChildren()
    for (origpipe <- pipes) {
      winput.addChild(origpipe)

      val opipe = new Pipe(config)
      opipe.step = atomic.stepName
      opipe.port = "result"
      opipe.link = atomic.children[WithOutput].head
      input.addChild(opipe)
      opipe.makeBindingsExplicit()
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    val context = staticContext.withStatics(inScopeStatics)
    val composer = new XMLViewportComposer(config, context, _match)
    val node = start.addViewport(composer, stepName, containerManifold)
    _graphNode = Some(node)

    for (child <- children[Step]) {
      child.graphNodes(runtime, node)
    }

    // The binding links we created earlier now need to be patched so that this
    // is the node they go to.
    for (np <- _dependentNameBindings) {
      val binding = findInScopeOption(np.name)
      if (binding.isDefined) {
        np.patchNode(binding.get.graphNode.get)
      }
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    super.graphEdges(runtime, parent)

    val winput = firstWithInput
    if (winput.isDefined) {
      for (child <- winput.get.allChildren) {
        child match {
          case pipe: Pipe =>
            pipe.graphEdges(runtime, _graphNode.get)
          case pipe: NamePipe =>
            pipe.graphEdges(runtime, _graphNode.get)
          case _ => ()
        }
      }
    }

    for (pipe <- children[NamePipe]) {
      pipe.graphEdges(runtime, _graphNode.get)
    }

    for (output <- children[DeclareOutput]) {
      for (pipe <- output.children[Pipe]) {
        runtime.graph.addOrderedEdge(pipe.link.get.parent.get._graphNode.get, pipe.port, _graphNode.get, output.port)
      }
    }

    for (child <- children[Step]) {
      child.graphEdges(runtime, _graphNode.get)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startViewport(tumble_id, stepName, _match)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endViewport()
  }

  override def toString: String = {
    s"p:viewport $stepName"
  }
}