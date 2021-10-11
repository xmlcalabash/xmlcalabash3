package com.xmlcalabash.util

import com.jafpl.graph
import net.sf.saxon.expr.parser
import net.sf.saxon.s9api.Location

object DefaultLocation {
  val voidLocation = new DefaultLocation(None, None, None)
}

class DefaultLocation(href: Option[String], lnum: Option[Long], cnum: Option[Long]) extends graph.Location with Location {
  def this(href: String) = {
    this(Some(href), None, None)
  }
  def this(lnum: Long) = {
    this(None, Some(lnum), None)
  }

  def this(lnum: Long, cnum: Long) = {
    this(None, Some(lnum), Some(cnum))
  }

  def this(href: String, lnum: Long, cnum: Long) = {
    this(Some(href), Some(lnum), Some(cnum))
  }

  def this(href: Option[String], lnum: Long, cnum: Long) = {
    this(href, Some(lnum), Some(cnum))
  }

  /** The URI */
  override def uri: Option[String] = href

  /** The line number. */
  override def line: Option[Long] = lnum

  /** The column number. */
  override def column: Option[Long] = cnum

  override def getSystemId: String = {
    if (href.isDefined) {
      href.get
    } else {
      null
    }
  }

  override def getColumnNumber: Int = {
    if (cnum.isDefined) {
      cnum.get.toInt
    } else {
      -1
    }
  }

  override def saveLocation(): DefaultLocation = {
    this // ours are immutable
  }

  override def getPublicId: String = {
    null
  }

  override def getLineNumber: Int = {
    if (lnum.isDefined) {
      lnum.get.toInt
    } else {
      -1
    }
  }

  override def toString: String = {
    s"${href.getOrElse("")}:${lnum.getOrElse("")}:${cnum.getOrElse("")}"
  }

}
