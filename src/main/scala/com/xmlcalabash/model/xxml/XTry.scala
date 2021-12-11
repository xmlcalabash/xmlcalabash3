package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XTry(config: XMLCalabash) extends XContainer(config) {

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    _drp = initial
    var curdrp = initial
    for (child <- allChildren) {
      child match {
        case _: XTryCatchBranch =>
          curdrp = initial
          child.elaborateDefaultReadablePort(curdrp)
        case _ =>
          curdrp = child.elaborateDefaultReadablePort(curdrp)
      }
    }

    children[XOutput] find { _.primary }
  }

  override protected[xxml] def validate(): Unit = {
    checkAttributes()
    checkEmptyAttributes()

    wrapTryInGroup()

    val codeList = mutable.Set.empty[QName]
    var seenCatch = false
    var seenCatchWithoutCode = false
    var seenFinally = false
    var seenPipeline = false

    //val newScope = checkStepNameScoping(inScopeNames)
    val newChildren = ListBuffer.empty[XArtifact]
    for (child <- allChildren) {
      child.validate()
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case xcatch: XCatch =>
          if (seenFinally) {
            error(XProcException.xsInvalidTryCatch("In a p:try, p:finally must be last", location))
          }
          seenCatch = true
          if (seenCatchWithoutCode) {
            error(XProcException.xsCatchMissingCode(location))
          }
          if (xcatch.codes.isEmpty) {
            seenCatchWithoutCode = true
          } else {
            for (code <- xcatch.codes) {
              if (codeList.contains(code)) {
                error(XProcException.xsCatchBadCode(code, location))
              } else {
                codeList += code
              }
            }
          }
          newChildren += xcatch
        case xfinally: XFinally =>
          if (seenFinally) {
            error(XProcException.xsInvalidTryCatch("In a p:try, there can be at most one p:finally", location))
          }
          seenFinally = true
          newChildren += xfinally
        case step: XGroup =>
          if (seenCatch || seenFinally) {
            error(XProcException.xsInvalidTryCatch("In a p:try, no steps may follow p:catch or p:finally", location))
          } else {
            seenPipeline = true
            newChildren += step
          }
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }

    if ((!seenCatch && !seenFinally) || !seenPipeline) {
      error(XProcException.xsInvalidTryCatch("Catch or finally and pipeline required", None))
      return
    }

    allChildren = newChildren.toList

    val outports = mutable.HashMap.empty[String, Option[Boolean]]

    for (branch <- children[XContainer]) {
      for (output <- branch.children[XOutput]) {
        if (outports.contains(output.port)) {
          val p1 = outports(output.port)
          val p2 = output
          if ((p1.isDefined && !p2.primarySpecified) || (p1.isEmpty && p2.primarySpecified)) {
            // FIXME: setup default for primary on p:output
            error(XProcException.xiUserError("Catch branches with different primary ports"))
          } else if (p1.isDefined) {
            if (p1.get != p2.primary) {
              error(XProcException.xiUserError("Catch branch with different primacy"))
            }
          }
        } else {
          if (output.primarySpecified) {
            outports.put(output.port, Some(output.primary))
          } else {
            outports.put(output.port, None)
          }
        }
      }
    }

    for (port <- outports.keySet) {
      val oport = if (port == "") {
        None
      } else {
        Some(port)
      }
      val output = new XOutput(this, oport)
      output.primary = outports(port).getOrElse(false)
      output.sequence = true
      output.contentTypes = MediaType.MATCH_ANY
      insertBefore(output, allChildren.head)

      for (branch <- children[XContainer]) {
        val out = branch.children[XOutput] find { _.port == port }
        if (out.isDefined) {
          val pipe = new XPipe(output, branch.stepName, port)
          output.addChild(pipe)
        }
      }
    }
  }

  private def wrapTryInGroup(): Unit = {
    val tryElements = ListBuffer.empty[XArtifact]
    var firstCatch = Option.empty[XTryCatchBranch]

    for (child <- allChildren) {
      if (firstCatch.isEmpty) {
        child match {
          case tc: XTryCatchBranch =>
            firstCatch = Some(tc)
          case _ =>
            tryElements += child
        }
      }
    }

    if (tryElements.isEmpty) {
      throw XProcException.xsInvalidTryCatch("A p:try must have at least one step", location)
    }

    if (tryElements.length == 1 && tryElements.head.isInstanceOf[XGroup]) {
      return
    }

    val group = new XGroup(this)
    group.allChildren = tryElements.toList

    val newChildren = ListBuffer.empty[XArtifact]
    newChildren += group

    var found = false
    for (child <- allChildren) {
      if (!found && firstCatch.isDefined) {
        found = firstCatch.get eq child
      }
      if (found) {
        newChildren += child
      }
    }

    allChildren = newChildren.toList
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    val node = start.addTryCatch(stepName, containerManifold)
    runtime.addNode(this, node)
    super.graphNodes(runtime, node)
  }
}
