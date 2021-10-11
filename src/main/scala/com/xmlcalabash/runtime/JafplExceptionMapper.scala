package com.xmlcalabash.runtime

import com.jafpl.exceptions.JafplException
import com.xmlcalabash.exceptions.XProcException

object JafplExceptionMapper {
  def remap(ex: JafplException): XProcException = {
    ex.code match {
      case JafplException.INVALID_LOOP_BOUNDS =>
        XProcException.xxInvalidLoop(ex.details)
      case JafplException.INPUT_CARDINALITY_ERROR =>
        XProcException.xdInputSequenceNotAllowed(ex.details.head.asInstanceOf[String], ex.location)
      case JafplException.OUTPUT_CARDINALITY_ERROR =>
        XProcException.xdOutputSequenceNotAllowed(ex.details.head.asInstanceOf[String], ex.location)
      case _ =>
        XProcException.xxUnmappedException(ex, ex.details)
    }
  }
}
