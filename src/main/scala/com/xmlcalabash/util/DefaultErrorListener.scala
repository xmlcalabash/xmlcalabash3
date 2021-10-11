package com.xmlcalabash.util

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.Location
import com.jafpl.util.ErrorListener
import com.xmlcalabash.exceptions.{ModelException, XProcException}

class DefaultErrorListener extends ErrorListener {
  override def error(message: String, location: Option[Location]): Unit = {
    msg("error", message, location)
  }

  override def error(cause: Throwable, location: Option[Location]): Unit = {
    msg("error", cause, location)
  }

  override def error(cause: Throwable): Unit = {
    val location = cause match {
      case jafpl: JafplException => jafpl.location
      case model: ModelException => model.location
      case xproc: XProcException => xproc.location
      case _ => None
    }
    msg("error", cause, location)
  }

  override def warning(message: String, location: Option[Location]): Unit = {
    msg("warn", message, location)
  }

  override def warning(cause: Throwable, location: Option[Location]): Unit = {
    msg("warn", cause, location)
  }

  override def warning(cause: Throwable): Unit = {
    val location = cause match {
      case jafpl: JafplException => jafpl.location
      case model: ModelException => model.location
      case xproc: XProcException => xproc.location
      case _ => None
    }
    msg("warn", cause, location)
  }

  override def info(message: String, location: Option[Location]): Unit = {
    msg("info", message, location)
  }

  private def msg(level: String, cause: Throwable, location: Option[Location]): Unit = {
    var loc = location
    var msg = ""

    cause match {
      case je: JafplException =>
        if (loc.isEmpty) {
          loc = je.location
        }
        msg = je.getMessage()
      case pe: XProcException =>
        if (loc.isEmpty) {
          loc = pe.location
        }
        msg = pe.message.getOrElse("???")
      case me: ModelException =>
        if (loc.isEmpty) {
          loc = me.location
        }
        msg = me.message
      case t: Throwable =>
        msg = t.getMessage
    }

    val sloc = loc.getOrElse("")
    println(s"$level:$sloc:$msg")
  }

  private def msg(level: String, message: String, location: Option[Location]): Unit = {
    val sloc = location.getOrElse("")
    println(s"$level:$sloc:$message")
  }
}
