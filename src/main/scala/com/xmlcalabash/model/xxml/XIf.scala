package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants

class XIf(config: XMLCalabash) extends XChooseBranch(config) {

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    val coll = attr(XProcConstants._collection)
    if (coll.isDefined) {
      coll.get match {
        case "true" => _collection = true
        case "false" => _collection = false
        case _ =>
          error(XProcException.xsBadTypeValue(coll.get, "xs:boolean", None))
      }
    }

    if (attributes.contains(XProcConstants._test)) {
      _test = attr(XProcConstants._test).get
    } else {
      error(XProcException.xsMissingRequiredAttribute(XProcConstants._test, None))
    }
  }

  override protected[xxml] def validate(): Unit = {
    super.validate()
    val xwi = children[XWithInput].headOption
    if (xwi.isDefined) {
      if (xwi.get.port != "#anon") {
        error(XProcException.xsPortNotAllowed(xwi.get.port, stepName, location))
      }
      xwi.get.port = "condition"
    }

    val primary = children[XOutput] find { _.primary }
    if (primary.isEmpty) {
      error(XProcException.xsPrimaryOutputRequired(location))
    }
  }

  override def elaborateSyntacticSugar(): Unit = {
    val choose = new XChoose(parent.get)
    choose.dependsOn ++= dependsOn
    choose.tumble_id = tumble_id
    if (name.isDefined) {
      choose.stepName = name.get
    }
    val when = new XWhen(choose, _test, _collection, true)
    choose.addChild(when)
    for (child <- allChildren) {
      when.addChild(child)
    }
    allChildren = List()
    parent.get.replaceChild(this, choose)
    choose.elaborateSyntacticSugar()
    choose.validate()
  }
}
