package com.xmlcalabash.model.xml

import java.net.URI

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.XdmNode

class ImportFunctions(override val config: XMLCalabashConfig) extends Artifact(config) {
  private var _href: URI = _

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (attributes.contains(XProcConstants._href)) {
      _href = node.getBaseURI.resolve(attr(XProcConstants._href).get)
    } else {
      throw XProcException.xsMissingRequiredAttribute(XProcConstants._href, location)
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override def toString: String = {
    s"p:import-functions $tumble_id ${_href}"
  }
}
