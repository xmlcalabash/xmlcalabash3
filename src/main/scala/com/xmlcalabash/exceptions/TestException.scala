package com.xmlcalabash.exceptions

/** Exception thrown by the test harness.
  *
  * @param code An error code
  * @param message The exception message.
  */
class TestException(val code: String, val message: String) extends Throwable {
  def this(msg: String) = {
    this("ERROR", msg)
  }

  override def getMessage: String = {
    message
  }

  override def toString: String = {
    "{" + code + ":" + message + "}"
  }
}
