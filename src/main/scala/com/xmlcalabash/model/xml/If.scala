package com.xmlcalabash.model.xml

import com.jafpl.graph.{ChooseStart, ContainerStart, Node}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

class If(override val config: XMLCalabashConfig) extends Choose(config) {
  override protected[model] def makeStructureExplicit(): Unit = {
    // p:if is purely syntactic sugar for a p:choose...so let's implement it that way

    val choose = new Choose(config)
    choose._name = Some(stepName)
    choose._depends = _depends
    choose.p_if = true
    for (child <- children[WithInput]) {
      choose.addChild(child)
    }
    val when = new When(config)
    when.test = ifexpr.get
    if (ifcoll.isDefined) {
      when.collection = ifcoll.get
    }
    for (child <- allChildren) {
      child match {
        case _: WithInput => ()
        case _ =>
          when.addChild(child)
      }
    }
    choose.addChild(when)
    parent.get.replaceChild(choose, this)

    choose.makeStructureExplicit()
  }

  override def toString: String = {
    s"p:choose $stepName (was p:if)"
  }
}