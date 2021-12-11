package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import net.sf.saxon.s9api.QName

import java.net.URI

class XLibrary(config: XMLCalabash, val href: Option[URI]) extends XDeclContainer(config) {

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    try {
      if (attributes.contains(XProcConstants._version)) {
        val vstr = attr(XProcConstants._version).get
        try {
          _version = Some(vstr.toDouble)
        } catch {
          case _: NumberFormatException =>
            error(XProcException.xsBadVersion(vstr, None))
        }
        if (_version.get != 3.0) {
          error(XProcException.xsInvalidVersion(_version.get, None))
        }
      }
      if (_version.isEmpty && parent.isEmpty && !synthetic) {
          error(XProcException.xsVersionRequired(None))
        }
    } catch {
      case ex: Exception =>
        error(ex)
    }
  }

  protected[xxml] def elaborateLibraryApi(): Unit = {
    checkAttributes()
    checkEmptyAttributes()

    for (option <- children[XOption]) {
      option.checkAttributes()
      option.checkEmptyAttributes()
      if (_xoptions.contains(option.name)) {
        error(XProcException.xsDuplicateOptionName(option.name, None))
      } else {
        _xoptions.put(option.name, option)
      }
    }
  }

  override protected[xxml] def validate(): Unit = {
    // Anything to do here?
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    dumpTree(sb, "p:library", Map())
  }

  override def toString: String = {
    var baseuri = if (href.isDefined) {
      href.get.toString
    } else {
      ""
    }
    if (baseuri != "") {
      baseuri = ": " + baseuri
    }
    if (stepName != tumble_id) {
      s"${staticContext.nodeName}(${stepName};${tumble_id})${baseuri}"
    } else {
      s"${staticContext.nodeName}(${stepName})${baseuri}"
    }
  }
}
