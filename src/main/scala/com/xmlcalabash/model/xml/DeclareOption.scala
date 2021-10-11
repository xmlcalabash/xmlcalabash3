package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.runtime.{ExprParams, XMLCalabashRuntime, XProcMetadata, XProcXPathExpression, XProcXPathValue}
import com.xmlcalabash.util.{S9Api, XProcVarValue}
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, SaxonApiException, SequenceType, XdmMap, XdmValue}
import net.sf.saxon.trans.XPathException

import java.net.URI
import scala.collection.mutable

class DeclareOption(override val config: XMLCalabashConfig) extends NameBinding(config) {
  private var _runtimeBindings = Map.empty[QName,XProcVarValue]

  override def toString: String = {
    s"p:option $name $tumble_id"
  }

  override def declaredType: SequenceType = {
    if (_as.isEmpty) {
      _declaredType = staticContext.parseSequenceType(Some("Q{http://www.w3.org/2001/XMLSchema}string"))
    } else {
      _declaredType = _as
    }
    _declaredType.get
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child match {
        case _: WithInput => ()
        case _: NamePipe => ()
        case _ =>
          throw new RuntimeException(s"Invalid content in $this")
      }
    }
  }

  def runtimeBindings(bindings: Map[QName, XProcVarValue]): Unit = {
    _runtimeBindings = bindings
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    super.makeBindingsExplicit()

    val env = environment()

    val bindings = mutable.HashSet.empty[QName]
    if (_select.isDefined) {
      bindings ++= staticContext.findVariableRefsInString(_select.get)
    }

    var nonStaticBindings = false
    for (ref <- bindings) {
      val binding = env.variable(ref)
      if (binding.isEmpty) {
        throw new RuntimeException("Reference to undefined variable")
      }
      nonStaticBindings = nonStaticBindings || !binding.get.static
    }

    if (nonStaticBindings) {
      var winput = firstWithInput
      if (winput.isEmpty) {
        val input = new WithInput(config)
        input.port = "source"
        addChild(input)
        winput = Some(input)
      }
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    if (static) {
      // Statics have already been evaluated, they don't appear in the graph
      return
    }

    val container = this.parent.get
    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]
    if (cnode.parent.nonEmpty) {
      throw new ModelException(ExceptionCode.INTERNAL, "Don't know what to do about opts here", location)
    }

    val params = new XPathBindingParams(collection)
    val init = if (_runtimeBindings.contains(name)) {
      new XProcXPathValue(staticContext, _runtimeBindings(name), as, _allowedValues, params)
    } else {
      val bindings = mutable.HashSet.empty[QName]
      if (_select.isDefined) {
        bindings ++= staticContext.findVariableRefsInString(_select.get)
      }

      val extext = _select.getOrElse("()")
      val expr = new XProcXPathExpression(staticContext, extext, as, _allowedValues, params)

      // Let's see if we think this is a syntactically valid expression (see bug #506 and test ab-option-024)
      val xcomp = config.processor.newXPathCompiler()
      xcomp.setBaseURI(URI.create("http://example.com/"))
      for (varname <- bindings) {
        xcomp.declareVariable(varname)
      }
      for ((prefix, uri) <- staticContext.nsBindings) {
        xcomp.declareNamespace(prefix, uri)
      }
      try {
        xcomp.compile(extext)
      } catch {
        case sae: SaxonApiException =>
          sae.getCause match {
            case _: XPathException =>
              throw XProcException.xsStaticErrorInExpression(extext, sae.getMessage, location)
            case _ => throw sae
          }
        case other: Throwable =>
          throw other
      }

      expr
    }

    val node = parent.asInstanceOf[ContainerStart].addOption(_name.getClarkName, init, xpathBindingParams(), topLevel = true)

    for (np <- _dependentNameBindings) {
      val binding = findInScopeOption(np.name)
      if (binding.isEmpty) {
        throw XProcException.xsNoBindingInExpression(np.name, location)
      }
      np.patchNode(binding.get.graphNode.get)
      np.graphEdges(runtime, node)
    }

    _graphNode = Some(node)
  }


  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startOption(tumble_id, tumble_id, name)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endOption()
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parNode: Node): Unit = {
    if (static) {
      return
    }

    val env = environment()
    for (stepName <- depends) {
      val step = env.step(stepName)
      if (step.isEmpty) {
        throw XProcException.xsNotAStep(stepName, location)
      } else {
        _graphNode.get.dependsOn(step.get._graphNode.get)
      }
    }

    val toNode = parNode
    val fromPort = "result"
    val fromNode = _graphNode.get
    val toPort = "#bindings"
    runtime.graph.addEdge(fromNode, fromPort, toNode, toPort)

    for (child <- allChildren) {
      child.graphEdges(runtime, _graphNode.get)
    }
  }
}

