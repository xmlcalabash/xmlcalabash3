package com.xmlcalabash.util

import net.sf.saxon.s9api.Location

// Via Florent Georges

object VoidLocation {
  private val loc = new VoidLocation()
  def instance(): VoidLocation = loc
}

class VoidLocation extends Location {
  override def getSystemId: String = null

  override def getPublicId: String = null

  override def getLineNumber: Int = -1

  override def getColumnNumber: Int = -1

  override def saveLocation(): Location = this
}
