package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants

import scala.collection.mutable

class XOtherwise(config: XMLCalabash) extends XChooseBranch(config) {
  def this(choose: XChoose) = {
    this(choose.config)
    staticContext = choose.staticContext
    parent = choose
    synthetic = true
    syntheticName = XProcConstants.p_otherwise
    _test = "true()"
  }

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    _test = "true()"
  }

  override def validate(): Unit = {
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
            error(XProcException.xsInvalidPipeline("p:output cannot follow steps in p:otherwise", location))
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

    constructDefaultOutput()

    orderChildren()
  }

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    if (synthetic && initial.isEmpty) {
      // Special case, return an empty sequence
      val step = children[XStep].head
      val input = step.children[XWithInput].head
      input.addChild(new XEmpty(input))
    }

    super.elaborateDefaultReadablePort(initial)
  }
}
