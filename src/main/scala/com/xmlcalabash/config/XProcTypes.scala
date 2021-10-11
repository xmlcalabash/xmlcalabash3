package com.xmlcalabash.config

import net.sf.saxon.s9api.{QName, XdmItem, XdmValue}

object XProcTypes {
  type Parameters = Map[QName,XdmValue]
  type DocumentProperties = Map[QName, XdmItem]
}
