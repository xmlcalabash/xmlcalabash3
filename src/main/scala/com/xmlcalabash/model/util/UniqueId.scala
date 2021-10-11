package com.xmlcalabash.model.util

object UniqueId {
  private var theNextId: Long = 0
  def nextId: Long = {
    this.synchronized {
      val id = theNextId
      theNextId = theNextId + 1
      id
    }
  }
}