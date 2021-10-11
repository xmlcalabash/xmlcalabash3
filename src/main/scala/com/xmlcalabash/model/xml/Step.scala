package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.TypeUtils
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable.ListBuffer

class Step(override val config: XMLCalabashConfig) extends Artifact(config) with NamedArtifact {
  protected[xmlcalabash] var _name = Option.empty[String]
  override def stepName: String = _name.getOrElse(tumble_id)
  protected[model] def stepName_=(name: String): Unit = {
    _name = Some(name)
  }

  protected[xml] var _depends = ListBuffer.empty[String]
  def depends: List[String] = _depends.toList

  override def parse(node: XdmNode): Unit = {
    super.parse(node)
    _name = attr(XProcConstants._name)

    var _depstr = Option.empty[String]

    if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
      _depstr = attr(XProcConstants._depends)
      if (attr(XProcConstants.p_depends).isDefined) {
        throw XProcException.xsXProcNamespaceError(XProcConstants.p_depends, location)
      }
    } else {
      _depstr = attr(XProcConstants.p_depends)
      if (attr(XProcConstants._depends).isDefined) {
        throw XProcException.xsUndeclaredOption(node.getNodeName, XProcConstants._depends, location)
      }
    }

    if (_depstr.isDefined) {
      if (_depstr.get.trim == "") {
        throw XProcException.xsBadTypeValue(_depstr.get, "name+", location)
      }

      val tutils = new TypeUtils(config)
      for (name <- _depstr.get.trim().split("\\s+")) {
        if (!tutils.valueMatchesType(name, XProcConstants.xs_NCName)) {
          throw XProcException.xsBadTypeValue(name, "NCName", location)
        }
        _depends += name
      }
    }
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child.validateStructure()
    }
  }

  protected[model] def primaryInput: Option[Port] = {
    for (child <- allChildren) {
      child match {
        case input: DeclareInput =>
          if (input.primary) {
            return Some(input)
          }
        case winput: WithInput =>
          if (winput.primary) {
            return Some(winput)
          }
        case _ => ()
      }
    }
    None
  }

  protected[model] def primaryOutput: Option[Port] = {
    for (child <- allChildren) {
      child match {
        case output: DeclareOutput =>
          if (output.primary) {
            return Some(output)
          }
        case woutput: WithOutput =>
          if (woutput.primary) {
            return Some(woutput)
          }
        case _ => ()
      }
    }
    None
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    super.makeBindingsExplicit()
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    if (depends.nonEmpty) {
      val thisNode = _graphNode.get
      val env = environment()
      for (stepName <- depends) {
        val step = env.step(stepName)
        if (step.isEmpty) {
          throw XProcException.xsNotAStep(stepName, location)
        } else {
          thisNode.dependsOn(step.get._graphNode.get)
        }
      }
    }
  }
}
