package com.xmlcalabash.model.xxml

import com.jafpl.graph.{Node, TryCatchStart}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import net.sf.saxon.s9api.QName

import scala.collection.mutable

class XCatch(config: XMLCalabash) extends XTryCatchBranch(config) {
  private val _codes = mutable.HashSet.empty[QName]

  def codes: Set[QName] = _codes.toSet

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    if (attributes.contains(XProcConstants._code)) {
      val str = attr(XProcConstants._code).get
      var repeated = Option.empty[QName]
      for (code <- str.split("\\s+")) {
        try {
          val qname = staticContext.parseQName(code)
          if (repeated.isEmpty && _codes.contains(qname)) {
            repeated = Some(qname)
          }
          _codes += qname
        } catch {
          case _: Exception =>
            error(XProcException.xsCatchInvalidCode(code, None))
        }
      }
      if (repeated.isDefined) {
        error(XProcException.xsCatchRepeatedCode(repeated.get, None))
      }
    }
  }

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    var curdrp: Option[XPort] = children[XInput] find { _.primary == true }
    for (child <- allChildren) {
      curdrp = child.elaborateDefaultReadablePort(curdrp)
    }
    initial
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[TryCatchStart]
    val node = start.addCatch(stepName, _codes.toList, containerManifold)
    runtime.addNode(this, node)
    super.graphNodes(runtime, node)
  }
}
