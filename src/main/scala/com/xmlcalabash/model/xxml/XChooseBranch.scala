package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ChooseStart, Node}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcXPathExpression}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class XChooseBranch(config: XMLCalabash) extends XContainer(config) {
  protected var _test = ""
  protected var _collection = false

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    _drp = initial
    var curdrp  = initial
    for (child <- allChildren) {
      curdrp = child.elaborateDefaultReadablePort(curdrp)
    }
    initial
  }

  protected def orderChildren(): Unit = {
    if (exceptions.nonEmpty) {
      return
    }
    val newChildren = ListBuffer.empty[XArtifact]
    newChildren ++= children[XWithInput]
    newChildren ++= children[XOutput]
    for (child <- allChildren) {
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case _: XWithInput => ()
        case _: XOutput => ()
        case _ =>
          newChildren += child
      }
    }
    allChildren = newChildren.toList
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ChooseStart]

    val params = new XPathBindingParams(_collection)
    val testExpr = new XProcXPathExpression(staticContext, _test, None, None, Some(params))

    val node = start.addWhen(testExpr, stepName, containerManifold)
    runtime.addNode(this, node)
    super.graphNodes(runtime, node)
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    attr.put("name", Some(stepName))

    if (this.isInstanceOf[XWhen]) {
      attr.put("test", Some(_test))
    }

    if (_collection) {
      attr.put("collection", Some(_collection))
    }
    dumpTree(sb, nodeName.toString, attr.toMap)
  }

}
