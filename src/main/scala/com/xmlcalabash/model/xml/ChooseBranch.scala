package com.xmlcalabash.model.xml

import com.jafpl.graph.{ChooseStart, Node}
import com.jafpl.steps.Manifold
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.runtime.{StaticContext, XMLCalabashRuntime, XProcXPathExpression}
import com.xmlcalabash.util.ValueUtils
import net.sf.saxon.s9api.QName

import scala.collection.mutable

class ChooseBranch(override val config: XMLCalabashConfig) extends Container(config) with NamedArtifact {
  protected var _test = ""
  protected var _collection = false
  protected var testExpr: XProcXPathExpression = _

  def test: String = _test
  protected[model] def test_=(expr: String): Unit = {
    _test = expr
    setTestExpr()
  }

  def collection: Boolean = _collection
  protected[model] def collection_=(coll: String): Unit = {
    if (List("true", "1", "yes").contains(coll)) {
      _collection = true
    } else if (List("false", "0", "no").contains(coll)) {
      _collection = false
    } else {
      throw XProcException.xsBadTypeValue(coll, "xs:boolean", location)
    }
    setTestExpr()
  }

  private def setTestExpr(): Unit = {
    val context = new StaticContext(staticContext, this)
    val params = new XPathBindingParams(collection)
    testExpr = new XProcXPathExpression(context, _test, None, None, Some(params))
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    // We don't make the with-input structure explicit here because we want
    // to do it after any potential inputs have been turned into pipes.
    makeContainerStructureExplicit()
  }

  override protected def syntheticDeclaredOutput(): DeclareOutput = {
    var portName = "#result"

    if (synthetic) { // This must be an otherwise
      // If we've created a synthetic otherwise that has a primary output
      // port, make sure the port name we give it is consistent with the
      // primary output port of the when's, if they have one.
      val choose = parent.get
      val when = choose.children[When].head
      for (child <- when.children[DeclareOutput]) {
        if (child.primary) {
          portName = child.port
        }
      }
    }

    val output = new DeclareOutput(config)
    output.port = portName
    output.primary = true
    output.sequence = true
    output
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    super.makeBindingsExplicit()

    var winput = firstWithInput
    if (winput.isEmpty) {
      winput = Some(new WithInput(config))
      winput.get.port = "source"
      winput.get.primary = true
      addChild(winput.get, firstChild)
    }

    val bindings = mutable.HashSet.empty[QName]
    bindings ++= staticContext.findVariableRefsInString(_test)
    if (bindings.isEmpty) {
      val depends = staticContext.dependsOnContextString(_test)
      if (!depends) {
        //FIXME: WHEN/OTHERWISE CAN BE RESOLVED STATICALLY
      }
    } else {
      val env = environment()
      for (ref <- bindings) {
        val binding = env.variable(ref)
        if (binding.isEmpty) {
          throw new RuntimeException("Reference to undefined variable")
        }
        if (!binding.get.static) {
          val pipe = new NamePipe(config, ref, binding.get.tumble_id, binding.get)
          addChild(pipe)
        }
      }
    }
  }

  override protected[model] def normalizeToPipes(): Unit = {
    for (child <- allChildren) {
      child.normalizeToPipes()
    }

    var copyPipes = true
    var winput = firstWithInput
    if (winput.isEmpty) {
      winput = Some(new WithInput(config))
      winput.get.port = "source"
      winput.get.primary = true
      addChild(winput.get, firstChild)
    } else {
      copyPipes = winput.get.children[Pipe].isEmpty
    }

    if (copyPipes) {
      // A bit hacky, but here's where we need to inject the with-input
      val parentWithInput = parent.get.firstWithInput
      for (child <- parentWithInput.get.allChildren) {
        child match {
          case pipe: Pipe =>
            winput.get.addChild(new Pipe(pipe))
          case _ =>
            throw new RuntimeException("with input hasn't been normalized to pipes?")
        }
      }
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ChooseStart]
    val node = start.addWhen(testExpr, stepName, containerManifold)
    _graphNode = Some(node)

    for (child <- children[Step]) {
      child.graphNodes(runtime, node)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    super.graphEdges(runtime, parent)

    for (child <- allChildren) {
      child match {
        case winput: WithInput =>
          for (child <- winput.allChildren) {
            child match {
              case pipe: Pipe =>
                runtime.graph.addEdge(pipe.link.get.parent.get._graphNode.get, pipe.port, _graphNode.get, "condition")
              case pipe: NamePipe =>
                pipe.graphEdges(runtime, _graphNode.get)
              case _ =>
                throw new RuntimeException("non-pipe children in p:with-input?")
            }
          }
        case _ =>
          child.graphEdges(runtime, _graphNode.get)
      }
    }
  }
}
