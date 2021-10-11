package com.xmlcalabash.util

import java.net.{URI, URLConnection}
import com.xmlcalabash.exceptions.XProcException

import java.util.regex.Pattern
import scala.collection.mutable.ListBuffer

// This isn't trying very hard to be strict about the rules
// N.B. This class accepts "*" for type and subtype because it's used for matching

object MediaType {
  val OCTET_STREAM = new MediaType("application", "octet-stream")
  val TEXT = new MediaType("text", "plain")
  val XML  = new MediaType("application", "xml")
  val JSON = new MediaType("application", "json")
  val YAML = new MediaType("application", "vnd.yaml")
  val HTML = new MediaType("text", "html")
  val XHTML = new MediaType("application", "xhtml+xml")
  val ZIP = new MediaType("application", "zip")
  val MULTIPART = new MediaType("multipart", "*")
  val MULTIPART_MIXED = new MediaType("multipart", "mixed")

  val MATCH_XML: Array[MediaType] = Array(
    MediaType.parse("application/xml"),
    MediaType.parse("text/xml"),
    MediaType.parse("*/*+xml"),
    MediaType.parse("-application/xhtml+xml"))

  val MATCH_HTML: Array[MediaType] = Array(
    MediaType.parse("text/html"),
    MediaType.parse("application/xhtml+xml")
  )

  val MATCH_TEXT: Array[MediaType] = Array(
    MediaType.parse("text/*"),
    MediaType.parse("application/relax-ng-compact-syntax"),
    MediaType.parse("application/xquery"),
    MediaType.parse("application/javascript"),
    MediaType.parse("-text/html"),
    MediaType.parse("-text/xml")
  )

  val MATCH_JSON: Array[MediaType] = Array(
    MediaType.parse("application/json"),
    MediaType.parse("*/*+json")
  )

  val MATCH_YAML: Array[MediaType] = Array(
    MediaType.parse("application/vnd.yaml")
  )

  val MATCH_ANY: Array[MediaType] = Array(
    MediaType.parse("*/*")
  )

  def parse(mtype: Option[String]): Option[MediaType] = {
    if (mtype.isDefined) {
      Some(parse(mtype.get))
    } else {
      None
    }
  }

  def parse(mtype: String): MediaType = {
    parse(mtype, None)
  }

  def parse(mtype: String, forceEncoding: Option[String]): MediaType = {
    // [-]type/subtype; name1=val1; name2=val2
    var pos = mtype.indexOf("/")
    var mediaType = if (pos < 0) {
      mtype.trim
    } else {
      mtype.substring(0, pos).trim
    }

    var rest = if (pos < 0) {
      ""
    } else {
      mtype.substring(pos + 1).trim
    }

    var inclusive = true
    if (mediaType.startsWith("-")) {
      inclusive = false
      mediaType = mediaType.substring(1)
    }

    // This is a bit convoluted because of the way 'rest' is reused.
    // There was a bug and this was the easiest fix. #hack

    var params = ""
    pos = rest.indexOf(";")
    if (pos >= 0) {
      params = rest.substring(pos+1)
      rest = rest.substring(0, pos).trim
    }

    var mediaSubtype = rest
    var suffix = Option.empty[String]

    if (mediaSubtype.contains("+")) {
      pos = mediaSubtype.indexOf("+")
      suffix = Some(mediaSubtype.substring(pos+1).trim)
      mediaSubtype = mediaSubtype.substring(0, pos).trim
    }

    if (forceEncoding.isEmpty && params == "") {
      new MediaType(mediaType, mediaSubtype, suffix, inclusive, None)
    } else {
      rest = params
      pos = rest.indexOf(";")
      val plist = ListBuffer.empty[String]
      if (forceEncoding.isDefined) {
        plist += "charset=" + forceEncoding.get
      }

      while (pos >= 0) {
        val param = rest.substring(0, pos).trim
        rest = rest.substring(pos+1)
        if (param != "") {
          if (forceEncoding.isEmpty || !param.startsWith("charset=")) {
            plist.append(param)
          }
        }
        pos = rest.indexOf(";")
      }
      if (rest.trim != "") {
        rest = rest.trim
        if (forceEncoding.isEmpty || !rest.startsWith("charset=")) {
          plist.append(rest)
        }
      }
      new MediaType(mediaType, mediaSubtype, suffix, inclusive, Some(plist.toArray))
    }
  }

  def parseList(ctypes: String): ListBuffer[MediaType] = {
    val contentTypes = ListBuffer.empty[MediaType]
    for (ctype <- ctypes.split("\\s+")) {
      ctype match {
        case "xml"  => contentTypes ++= MATCH_XML
        case "html" => contentTypes ++= MATCH_HTML
        case "text" => contentTypes ++= MATCH_TEXT
        case "json" => contentTypes ++= MATCH_JSON
        case "any"  => contentTypes ++= MATCH_ANY
        case _      =>
          if (ctype.indexOf("/") <= 0) {
            throw XProcException.xsUnrecognizedContentTypeShortcut(ctype, None)
          }
          contentTypes += MediaType.parse(ctype)
      }
    }
    contentTypes
  }
}

class MediaType(val mediaType: String, val mediaSubtype: String, val suffix: Option[String], val inclusive: Boolean, val param: Option[Array[String]]) {
  def this(mediaType: String, mediaSubtype: String) = {
    this(mediaType, mediaSubtype, None, true, None)
  }

  def this(mediaType: String, mediaSubtype: String, suffix: String) = {
    this(mediaType, mediaSubtype, Some(suffix), true, None)
  }

  def parameters: List[String] = {
    if (param.isEmpty) {
      List.empty[String]
    } else {
      val names = ListBuffer.empty[String]
      for (aparam <- param.get) {
        val pos = aparam.indexOf("=")
        if (pos > 0) {
          names += aparam.substring(0, pos)
        }
      }
      names.toList
    }
  }

  def discardParams(): MediaType = {
    if (param.isEmpty) {
      return this
    }
    discardParams(parameters)
  }

  def discardParams(exclude: List[String]): MediaType = {
    if (param.isEmpty) {
      return this
    }

    var found = false
    val newParam = ListBuffer.empty[String]
    for (aparam <- param.get) {
      var pmatch = false
      for (excl <- exclude) {
        val pstr = s"$excl="
        pmatch = pmatch || aparam.startsWith(pstr)
      }
      found = found || pmatch
      if (!pmatch) {
        newParam += aparam
      }
    }

    if (found) {
      if (newParam.isEmpty) {
        new MediaType(mediaType, mediaSubtype, suffix, inclusive, None)
      } else {
        new MediaType(mediaType, mediaSubtype, suffix, inclusive, Some(newParam.toArray[String]))
      }
    } else {
      this
    }
  }

  def addParam(name: String, value: String): MediaType = {
    val newParam = ListBuffer.empty[String]
    if (param.isDefined) {
      val pstr = s"$name="
      for (aparam <- param.get) {
        if (!aparam.startsWith(pstr)) {
          newParam += aparam
        }
      }
    }
    newParam += s"$name=$value"
    new MediaType(mediaType, mediaSubtype, suffix, inclusive, Some(newParam.toArray[String]))
  }

  def classification: MediaType = {
    if (xmlContentType) {
      MediaType.XML
    } else if (htmlContentType) {
      MediaType.HTML
    } else if (textContentType) {
      MediaType.TEXT
    } else if (jsonContentType) {
      MediaType.JSON
    } else if (yamlContentType) {
      MediaType.YAML
    } else {
      MediaType.OCTET_STREAM
    }
  }

  def textContentType: Boolean = {
    val mtype = matchingMediaType(MediaType.MATCH_TEXT)
    mtype.isDefined && mtype.get.inclusive
  }

  def xmlContentType: Boolean = {
    val mtype = matchingMediaType(MediaType.MATCH_XML)
    mtype.isDefined && mtype.get.inclusive
  }

  def jsonContentType: Boolean = {
    val mtype = matchingMediaType(MediaType.MATCH_JSON)
    mtype.isDefined && mtype.get.inclusive
  }

  def yamlContentType: Boolean = {
    val mtype = matchingMediaType(MediaType.MATCH_YAML)
    mtype.isDefined && mtype.get.inclusive
  }

  def htmlContentType: Boolean = {
    val mtype = matchingMediaType(MediaType.MATCH_HTML)
    mtype.isDefined && mtype.get.inclusive
  }

  def anyContentType: Boolean = true

  def markupContentType: Boolean = {
    xmlContentType || htmlContentType
  }

  def matchingMediaType(mtypes: Array[MediaType]): Option[MediaType] = {
    var matching = Option.empty[MediaType]
    for (mtype <- mtypes) {
      //println(s"$this $mtype ${matches(mtype)}")
      if (matches(mtype)) {
        matching = Some(mtype)
      }
    }
    matching
  }

  def matches(mtype: MediaType): Boolean = {
    if (mtype.mediaType == "application" && mtype.mediaSubtype == "octet-stream") {
      return true
    }

    var mmatch = mediaType == mtype.mediaType || mtype.mediaType == "*"
    mmatch = mmatch && (mediaSubtype == mtype.mediaSubtype || mtype.mediaSubtype == "*")
    if (suffix.isDefined && mtype.suffix.isDefined) {
      mmatch = mmatch && suffix.get == mtype.suffix.get
    }

    // application/xml should match */*
    // but text/plain shouldn't match */*+xml

    // This special rule seems necessary but I can't really justify it
    if (mmatch && mtype.mediaType == "*" && mtype.mediaSubtype == "*" && mtype.suffix.isDefined) {
      mmatch = mmatch && suffix.isDefined
    }

    mmatch
  }

  def allowed(types: List[MediaType]): Boolean = {
    // This media type is allowed if it's allowed by at least one member of types
    // and is not forbidden by the last member
    var allowed = false
    for (ctype <- types) {
      if (matches(ctype)) {
        allowed = ctype.inclusive
      }
    }

    allowed
  }

  def paramValue(name: String): Option[String] = {
    val start = name + "="
    if (param.isDefined) {
      for (param <- param.get) {
        if (param.startsWith(start)) {
          return Some(param.substring(start.length))
        }
      }
    }
    None
  }

  def charset: Option[String] = {
    paramValue("charset")
  }

  def assertValid: MediaType = {
    if (assertValidName(mediaType) && assertValidName(mediaSubtype)) {
      return this
    }

    var ctype = mediaType
    if (mediaSubtype != "") {
      ctype += s"/${mediaSubtype}"
    }
    throw XProcException.xdUnrecognizedContentType(ctype, None)
  }

  private def assertValidName(literal: String): Boolean = {
    val name = literal.toLowerCase
    val length_ok = name.nonEmpty && name.length <= 127
    val start_ok = length_ok && name.substring(0,1).matches("^[a-z0-9]")
    val content_ok = start_ok && name.matches(s"[a-z0-9${Pattern.quote("!#$&-^_.+")}]+$$")
    content_ok
  }

  // https://alvinalexander.com/scala/how-to-define-equals-hashcode-methods-in-scala-object-equality
  private def canEqual(obj: Any): Boolean = obj.isInstanceOf[MediaType]

  override def equals(mtype: Any): Boolean =
    mtype match {
      case that: MediaType =>
        that.canEqual(this) &&
          this.mediaType == that.mediaType &&
          this.mediaSubtype == that.mediaSubtype
      case _ => false
    }

  override def hashCode(): Int = {
    val prime = 41
    var result = 1
    result = prime * result + mediaType.hashCode
    result = prime * result + mediaSubtype.hashCode
    result
  }

  override def toString: String = {
    var ctype = if (inclusive) {
      ""
    } else {
      "-"
    }
    ctype += mediaType + "/" + mediaSubtype
    if (suffix.isDefined) {
      ctype = ctype + "+" + suffix.get
    }
    if (param.isDefined) {
      for (param <- param.get) {
        ctype = ctype + "; " + param // Space after ; for compatibility with Morgana XProc results
      }
    }
    ctype
  }
}
