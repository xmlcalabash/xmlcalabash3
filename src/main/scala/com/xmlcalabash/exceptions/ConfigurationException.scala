package com.xmlcalabash.exceptions

import com.jafpl.graph.Location
import com.xmlcalabash.exceptions.ExceptionCode.ExceptionCode

class ConfigurationException(val code: ExceptionCode, val data: List[String], val location: Option[Location]) extends Throwable {
  def this(code: ExceptionCode, data: List[String], loc: Location) = {
    this(code, data, Some(loc))
  }

  def this(code: ExceptionCode, data: List[String]) = {
    this(code, data, None)
  }

  def this(code: ExceptionCode, data: String) = {
    this(code, List(data), None)
  }

  def message: String = {
    code match {
      case ExceptionCode.CFGINCOMPLETE => s"Incomplete configuration: no ${data.head} supplied"
      case ExceptionCode.MUSTBEABS => s"URI must be absolute: ${data.head}"
      case ExceptionCode.CLOSED => s"Attempt to change closed ${data.head} object"
    }
  }

  override def toString: String = {
    val loc = if (location.isDefined) {
      location.get.toString + ":"
    } else {
      ""
    }

    s"configuration error:$loc$message"
  }
}
