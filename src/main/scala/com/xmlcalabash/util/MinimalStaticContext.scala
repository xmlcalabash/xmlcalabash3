package com.xmlcalabash.util

import com.jafpl.graph.Location
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xxml.{XArtifact, XNameBinding, XOption}
import net.sf.saxon.s9api.{ItemType, ItemTypeFactory, OccurrenceIndicator, QName, SaxonApiException, SequenceType, XdmAtomicValue, XdmNode}

import java.net.URI

abstract class MinimalStaticContext() {
  def baseURI: Option[URI]

  def location: Option[Location]

  def inscopeNamespaces: Map[String, String]

  def inscopeConstants: Map[QName, XNameBinding]

  def parseClarkName(name: String): QName = {
    parseClarkName(name, None)
  }

  def parseClarkName(name: String, prefix: String): QName = {
    parseClarkName(name, Some(prefix))
  }

  private def parseClarkName(name: String, pfx: Option[String]): QName = {
    // FIXME: Better error handling for ClarkName parsing
    if (name.startsWith("{")) {
      val pos = name.indexOf("}")
      val uri = name.substring(1, pos)
      val local = name.substring(pos + 1)
      if (pfx.isDefined) {
        new QName(pfx.get, uri, local)
      } else {
        new QName(uri, local)
      }
    } else {
      new QName("", name)
    }
  }

  def parseQName(name: String): QName = {
    parseQName(Some(name)).get
  }

  def parseQName(optname: Option[String]): Option[QName] = {
    if (optname.isDefined) {
      val name = optname.get

      if (name.startsWith("Q{")) {
        val pos = name.lastIndexOf("}")
        if (pos < 0) {
          throw XProcException.xdCannotResolveQName(name, None)
        }
        val uri = name.substring(2, pos)
        val local = parseNCName(name.substring(pos + 1))
        Some(new QName(uri, local))
      } else if (name.contains(":")) {
        val pos = name.indexOf(":")
        val pfx = parseNCName(name.substring(0, pos))
        val local = parseNCName(name.substring(pos + 1))
        val uri = inscopeNamespaces.get(pfx)
        if (uri.isEmpty) {
          throw XProcException.xdCannotResolveQName(name, None)
        }
        Some(new QName(pfx, uri.get, local))
      } else {
        Some(new QName("", parseNCName(name)))
      }
    } else {
      None
    }
  }

  def parseNCName(name: String): String = {
    parseNCName(Some(name)).get
  }

  def parseNCName(name: Option[String]): Option[String] = {
    if (name.isDefined) {
      try {
        val ncname = castAtomicAs(new XdmAtomicValue(name.get), ItemType.NCNAME)
        Some(ncname.getStringValue)
      } catch {
        case _: SaxonApiException =>
          throw XProcException.xsBadTypeValue(name.get, "NCName", None)
        case e: Exception =>
          throw e
      }
    } else {
      None
    }
  }

  def parseSequenceType(seqType: Option[String], typeFactory: ItemTypeFactory): Option[SequenceType] = {
    if (seqType.isDefined) {
      Some(parseSequenceType(seqType.get, typeFactory))
    } else {
      None
    }
  }

  def parseSequenceType(seqType: String, typeFactory: ItemTypeFactory): SequenceType = {
    // XPathParser.parseSequenceType returns a type.SequenceType.
    // I need an s9api.SequenceType. Michael Kay confirms there's no
    // easy way to convert between them. I'm rolling my own until such time
    // as there's a better way. If it has to stay this way, I should call
    // the actual XPath 3.1 parser for it, but I'm just going to hack my
    // way through it for now.
    val parensre = "^\\((.*)\\)$".r
    val stypere = "^([^*+?()]+)\\s*([*+?])?$".r
    val mtypere = "^map\\s*\\((.*)\\)\\s*([*+?])?$".r
    val atypere = "^array\\s*\\((.*)\\)\\s*([*+?])?$".r
    val ftypere = "^function\\s*\\((.*)\\)\\s*([*+?])?$".r
    val itemre = "^item\\s*\\(\\s*\\)\\s*([*+?])?$".r
    seqType.trim() match {
      case parensre(body) =>
        parseSequenceType(body, typeFactory)
      case stypere(typename, cardchar) =>
        // xs:sometype?
        try {
          val itype = TypeUtils.simpleType(parseQName(typename))
          SequenceType.makeSequenceType(itype, cardinality(cardchar))
        } catch {
          case ex: XProcException =>
            if (ex.code == XProcException.err_xd0036) {
              throw XProcException.xsInvalidSequenceType(typename, "Expected QName", None)
            }
            throw ex
          case ex: Throwable =>
            throw ex
        }
      case mtypere(mbody, cardchar) =>
        if (mbody.trim == "*") {
          return SequenceType.makeSequenceType(ItemType.ANY_MAP, cardinality(cardchar))
        }
        val tuplere = "^([^,]+),(.*)$".r
        mbody match {
          case tuplere(keyseqtype, itemseqtype) =>
            val keytype = TypeUtils.simpleType(parseQName(keyseqtype))
            val itemtype = parseSequenceType(itemseqtype, typeFactory)
            SequenceType.makeSequenceType(typeFactory.getMapType(keytype, itemtype), cardinality(cardchar))
          case _ =>
            throw new RuntimeException(s"Unexpected map syntax: map($mbody)")
        }
      case atypere(abody, cardchar) =>
        if (abody.trim == "*") {
          return SequenceType.makeSequenceType(ItemType.ANY_ARRAY, cardinality(cardchar))
        }
        val itemtype = parseSequenceType(abody, typeFactory)
        SequenceType.makeSequenceType(typeFactory.getArrayType(itemtype), cardinality(cardchar))
      case ftypere(params, cardchar) =>
        SequenceType.makeSequenceType(ItemType.ANY_FUNCTION, cardinality(cardchar))
      case itemre(cardchar) =>
        SequenceType.makeSequenceType(ItemType.ANY_ITEM, cardinality(cardchar))
      case _ =>
        throw new RuntimeException(s"Unexpected sequence type: $seqType")
    }
  }

  def parseFakeMapSequenceType(seqType: String, typeFactory: ItemTypeFactory): SequenceType = {
    // This is just like parseSequenceType except that it lies about the type of maps
    val mtypere = "^map\\s*\\((.*)\\)\\s*([*+?])?$".r
    seqType.trim() match {
      case mtypere(mbody, cardchar) =>
        // I actually only care about the case where the keys are QNames
        if (mbody.trim == "*") {
          return SequenceType.makeSequenceType(ItemType.ANY_MAP, cardinality(cardchar))
        }
        val tuplere = "^([^,]+),(.*)$".r
        mbody match {
          case tuplere(keyseqtype, itemseqtype) =>
            val keytype = ItemType.ANY_ATOMIC_VALUE
            val itemtype = parseSequenceType(itemseqtype, typeFactory)
            SequenceType.makeSequenceType(typeFactory.getMapType(keytype, itemtype), cardinality(cardchar))
          case _ =>
            throw new RuntimeException(s"Unexpected map syntax: map($mbody)")
        }
      case _ =>
        parseSequenceType(seqType, typeFactory)
    }
  }

  private def cardinality(card: String): OccurrenceIndicator = {
    if (card == null) {
      OccurrenceIndicator.ONE
    } else {
      card match {
        case "*" => OccurrenceIndicator.ZERO_OR_MORE
        case "?" => OccurrenceIndicator.ZERO_OR_ONE
        case "+" => OccurrenceIndicator.ONE_OR_MORE
        case _ =>
          throw new RuntimeException(s"Unexpected cardinality $card")
      }
    }
  }

  def parseBoolean(value: Option[String]): Option[Boolean] = {
    if (value.isDefined) {
      if (value.get == "true" || value.get == "false") {
        Some(value.get == "true")
      } else {
        throw XProcException.xsBadTypeValue(value.get, "boolean", None)
      }
    } else {
      None
    }
  }

  def parseSingleContentType(ctype: Option[String]): Option[MediaType] = {
    if (ctype.isDefined) {
      MediaType.parse(ctype)
    } else {
      None
    }
  }

  def parseContentTypes(ctypes: Option[String]): List[MediaType] = {
    if (ctypes.isDefined) {
      try {
        MediaType.parseList(ctypes.get).toList
      } catch {
        case ex: XProcException =>
          if (ex.code == XProcException.err_xc0070) {
            // Map to the static error...
            throw XProcException.xsUnrecognizedContentTypeShortcut(ex.details.head.toString, ex.location)
          } else {
            throw ex
          }
      }
    } else {
      List.empty[MediaType]
    }
  }

  def castAtomicAs(value: XdmAtomicValue, seqType: Option[SequenceType]): XdmAtomicValue = {
    if (seqType.isEmpty) {
      return value
    }

    castAtomicAs(value, seqType.get.getItemType)
  }

  def castAtomicAs(value: XdmAtomicValue, xsdtype: ItemType): XdmAtomicValue = {
    if ((xsdtype == ItemType.UNTYPED_ATOMIC) || (xsdtype == ItemType.STRING || (xsdtype == ItemType.ANY_ITEM))) {
      return value
    }

    if (xsdtype == ItemType.QNAME) {
      val qnamev = value.getPrimitiveTypeName match {
        case XProcConstants.xs_string => new XdmAtomicValue(parseQName(value.getStringValue))
        case XProcConstants.xs_untypedAtomic => new XdmAtomicValue(parseQName(value.getStringValue))
        case XProcConstants.xs_QName => value
        case _ =>
          throw new RuntimeException(s"Don't know how to convert $value to an xs:QName")
      }
      return qnamev
    }

    try {
      new XdmAtomicValue(value.getStringValue, xsdtype)
    } catch {
      case sae: SaxonApiException =>
        if (sae.getMessage.contains("Invalid URI")) {
          throw XProcException.xdInvalidURI(value.getStringValue, location)
        } else {
          throw XProcException.xdBadType(value.getStringValue, xsdtype.toString, location)
        }
      case ex: Exception =>
        throw(ex)
    }
  }
}
