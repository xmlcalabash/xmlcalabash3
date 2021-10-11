package com.xmlcalabash.exceptions

import com.jafpl.graph.Location

class ParseException(val message: String, val location: Option[Location]) extends Exception {
  def this(msg: String, loc: Location) = {
    this(msg, Some(loc))
  }

  override def toString: String = {
    if (location.isDefined) {
      s"ParseException('$message', ${location.get})"
    } else {
      s"ParseException('$message')"
    }
  }
}
