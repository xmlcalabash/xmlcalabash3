package com.xmlcalabash.util

import com.xmlcalabash.util.xc.Errors
import javax.xml.transform.{ErrorListener, TransformerException}
import net.sf.saxon.`type`.ValidationException
import org.xml.sax.{ErrorHandler, SAXParseException}

import scala.collection.mutable.ListBuffer

class CachingErrorListener(errors: Errors) extends ErrorListener with ErrorHandler {
  private val _exceptions = ListBuffer.empty[Exception]
  private var _listener = Option.empty[ErrorListener]
  private var _handler = Option.empty[ErrorHandler]

  private var showErrors = errors.config.showErrors

  def this(errors: Errors, listener: ErrorListener) = {
    this(errors)
    _listener = Some(listener)
  }

  def this(errors: Errors, handler: ErrorHandler) = {
    this(errors)
    _handler = Some(handler)
  }

  def chainedListener: Option[ErrorListener] = _listener
  def chainedListener_=(listen: ErrorListener): Unit = {
    _listener = Some(listen)
  }

  def chainedHandler: Option[ErrorHandler] = _handler
  def chainedHandler_=(handler: ErrorHandler): Unit = {
    _handler = Some(handler)
  }

  def exceptions: List[Exception] = _exceptions.toList

  override def warning(exception: TransformerException): Unit = {
    if (_listener.isDefined) {
      _listener.get.warning(exception)
    }
    if (showErrors) {
      println(exception)
    }
    report(exception)
  }

  override def error(exception: TransformerException): Unit = {
    if (_listener.isDefined) {
      _listener.get.error(exception)
    }
    if (showErrors) {
      println(exception)
    }
    report(exception)
    _exceptions += exception
  }

  override def fatalError(exception: TransformerException): Unit = {
    if (_listener.isDefined) {
      _listener.get.fatalError(exception)
    }
    if (showErrors) {
      println(exception)
    }
    report(exception)
    _exceptions += exception
  }

  override def warning(exception: SAXParseException): Unit = {
    if (_handler.isDefined) {
      _handler.get.warning(exception)
    }
    if (showErrors) {
      println(exception)
    }
    report(exception)
  }

  override def error(exception: SAXParseException): Unit = {
    if (_handler.isDefined) {
      _handler.get.error(exception)
    }
    if (showErrors) {
      println(exception)
    }
    report(exception)
    _exceptions += exception
  }

  override def fatalError(exception: SAXParseException): Unit = {
    if (_handler.isDefined) {
      _handler.get.fatalError(exception)
    }
    if (showErrors) {
      println(exception)
    }
    report(exception)
    _exceptions += exception
  }

  private def report(exception: Exception): Unit = {
    exception match {
      case ve: ValidationException =>
        val msg = ve.getMessage
        val fail = ve.getValidationFailure
        errors.xsdValidationError(msg, fail)
      case _: Exception =>
        errors.xsdValidationError(exception.getLocalizedMessage)
    }
  }
}
