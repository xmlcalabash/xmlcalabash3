package com.xmlcalabash.model.xml

import com.jafpl.graph.{Node, TryCatchStart}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable

class Catch(override val config: XMLCalabashConfig) extends Container(config) with NamedArtifact {
  private val _codes = mutable.HashSet.empty[QName]

  def codes: Set[QName] = _codes.toSet

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

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
            throw XProcException.xsCatchInvalidCode(code, location)
        }
      }
      if (repeated.isDefined) {
        throw XProcException.xsCatchRepeatedCode(repeated.get, location)
      }
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    val input = new DeclareInput(config)
    input.port = "error"
    input.sequence = true
    input.primary = true
    addChild(input, firstChild)

    makeContainerStructureExplicit()
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[TryCatchStart]
    val node = start.addCatch(stepName, codes.toList, containerManifold)
    _graphNode = Some(node)

    for (child <- children[Step]) {
      child.graphNodes(runtime, _graphNode.get)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    for (child <- allChildren) {
      child.graphEdges(runtime, _graphNode.get)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    if (_codes.nonEmpty) {
      var codes = ""
      for (code <- _codes) {
        if (codes != "") {
          codes += ", "
        }
        codes += code.toString
      }

      xml.startCatch(tumble_id, stepName, Some(codes))
    } else {
      xml.startCatch(tumble_id, stepName, None)
    }
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endCatch()
  }

  override def toString: String = {
    if (codes.isEmpty) {
      s"p:catch $stepName"
    } else {
      val codestr = codes.mkString(" ")
      s"p:catch $codestr $stepName"
    }
  }
}