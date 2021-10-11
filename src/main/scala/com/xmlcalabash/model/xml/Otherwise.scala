package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XProcXPathExpression
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.XdmNode

class Otherwise(override val config: XMLCalabashConfig) extends ChooseBranch(config) {

  // An otherwise is not atomic, even if it contains only synthetic children
  override def atomic: Boolean = false

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    _collection = false
    _test = "true()"
    testExpr = new XProcXPathExpression(staticContext, _test)

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    if (synthetic && environment().defaultReadablePort.isEmpty) {
      val ident = children[Step].head
      val winput = ident.firstWithInput
      val empty = new Empty(config)
      winput.get.addChild(empty)
    }

    super.makeBindingsExplicit()
  }

  override protected[model] def normalizeToPipes(): Unit = {
    super.normalizeToPipes()

    // There's never any need for a with-input on p:otherwise
    val winput = firstWithInput
    if (winput.isDefined) {
      removeChild(winput.get)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startWhen(tumble_id, stepName, "true()")
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endWhen()
  }

  override def toString: String = {
    s"p:otherwise $stepName"
  }
}
