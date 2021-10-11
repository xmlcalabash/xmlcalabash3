package com.xmlcalabash.util

import net.sf.saxon.s9api.Location

// Via Florent Georges

class SysIdLocation(sysid: String) extends Location {
  override def getSystemId: String = sysid
  override def getPublicId: String = null
  override def getLineNumber: Int = -1
  override def getColumnNumber: Int = -1
  override def saveLocation(): Location = this
}
