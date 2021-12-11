package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.runtime.XProcXPathExpression

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class XTryCatchBranch(config: XMLCalabash) extends XContainer(config) {

  protected def orderChildren(): Unit = {
    // nop
  }

  override protected[xxml] def validate(): Unit = {
    checkAttributes()
    checkEmptyAttributes()
    var seenPipeline = false

    //val newScope = checkStepNameScoping(inScopeNames)
    for (child <- allChildren) {
      child.validate()
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case _: XOutput =>
          if (seenPipeline) {
            error(XProcException.xiUserError("output can't follow steps"))
          }
        case _: XVariable =>
          seenPipeline = true
        case _: XStep =>
          seenPipeline = true
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }

    if (children[XOutput].length == 1) {
      val output = children[XOutput].head
      if (!output.primarySpecified) {
        output.primary = true
      }
    }

    val xi = new XInput(this, Some("error"))
    xi.primary = true
    xi.sequence = true
    if (allChildren.isEmpty) {
      addChild(xi)
    } else {
      insertBefore(xi, allChildren.head)
    }

    constructDefaultOutput()

    orderChildren()
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    attr.put("name", Some(stepName))
    dumpTree(sb, nodeName.toString, attr.toMap)
  }
}
