package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.util.MediaType

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class XLoopingStep(config: XMLCalabash) extends XContainer(config) {

  override protected[xxml] def validate(): Unit = {
    checkAttributes()
    checkEmptyAttributes()

    val seenPorts = mutable.HashSet.empty[String]
    val newChildren = ListBuffer.empty[XArtifact]

    var primaryInput = Option.empty[XWithInput]
    var primaryOutput = Option.empty[XOutput]

    for (input <- children[XWithInput]) {
      input.validate()
      if (primaryInput.isDefined) {
        error(XProcException.xsInvalidPipeline(s"At most one p:with-input is allowed on loops", location))
      } else {
        primaryInput = Some(input)
        newChildren += input
      }
    }

    if (primaryInput.isEmpty && !this.isInstanceOf[XForLoop]) {
      val xwi = new XWithInput(this, "#anon", true, true, MediaType.MATCH_ANY)
      xwi.validate()
      primaryInput = Some(xwi)
      newChildren += xwi
    }

    val current = new XInput(this, Some("current"))
    current.sequence = false
    current.primary = true
    current.contentTypes = MediaType.MATCH_ANY
    current.validate()
    newChildren += current
    seenPorts += "current"

    for (output <- children[XOutput]) {
      output.validate()
      if (seenPorts.contains(output.port)) {
        output.error(XProcException.xsDupPortName(output.port, None))
      } else {
        seenPorts += output.port
        if (output.primary) {
          if (primaryOutput.isDefined) {
            output.error(XProcException.xsDupPrimaryInputPort(output.port, primaryInput.get.port, None))
          } else {
            primaryOutput = Some(output)
          }
        }
        newChildren += output
      }
    }

    //val newScope = checkStepNameScoping(inScopeNames)
    for (child <- allChildren) {
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case _: XWithInput => ()
        case _: XOutput => ()
        case v: XVariable =>
          v.validate()
          newChildren += v
        case step: XStep =>
          step.validate()
          newChildren += step
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }

    allChildren = newChildren.toList

    if (children[XOutput].length == 1) {
      val output = children[XOutput].head
      if (!output.primarySpecified) {
        output.primary = true
      }
    }

    constructDefaultOutput()

    this match {
      case _: XForWhile => constructTestOutput()
      case _: XForUntil => constructTestOutput()
      case _ => ()
    }
  }

  private def constructTestOutput(): Unit = {
    var testOutput = Option.empty[XOutput]
    for (child <- children[XOutput]) {
      if (child.port == "test") {
        testOutput = Some(child)
      }
    }

    if (testOutput.isEmpty) {
      val output = new XOutput(this, Some("test"))
      output.primary = false
      output.sequence = false
      output.contentTypes = MediaType.MATCH_ANY

      val firstStep = children[XStep].head
      val step = children[XStep].last
      val pout = step.primaryOutput
      if (pout.isDefined) {
        val pipe = new XPipe(step.primaryOutput.get)
        output.addChild(pipe)
        if (Option(firstStep).isDefined) {
          insertBefore(output, firstStep)
        } else {
          addChild(output)
        }
      }
    }
  }

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    children[XWithInput] foreach { _.elaborateDefaultReadablePort(initial) }

    var curdrp: Option[XPort] = Some(children[XInput].head)
    for (child <- allChildren) {
      child match {
        case _: XWithInput =>
          () // Don't make this one point to current!
        case _ =>
          curdrp = child.elaborateDefaultReadablePort(curdrp)
      }
    }

    children[XOutput] find { _.primary }
  }

  override protected def addInputFilter(child: XPort, filter: XStep): Unit = {
    val container = parent.get match {
      case cont: XContainer => cont
      case _ =>
        error(XProcException.xiThisCantHappen("Parent of filtering step isn't a container?"))
        return
    }

    val filterxwi = new XWithInput(this, "source", true, true, MediaType.MATCH_ANY)
    filterxwi.allChildren = child.allChildren

    val filterxwo = new XWithOutput(filter, "result")

    val stepxwi = new XWithInput(this, "source")

    val pipe = new XPipe(stepxwi, filter.stepName, "result")

    stepxwi.addChild(pipe)
    insertBefore(stepxwi, child)
    removeChild(child)

    stepxwi.validate()

    filter.addChild(filterxwi)
    filter.addChild(filterxwo)

    container.insertBefore(filter, this)
    filterxwi.validate()
    filterxwo.validate()
  }
}
