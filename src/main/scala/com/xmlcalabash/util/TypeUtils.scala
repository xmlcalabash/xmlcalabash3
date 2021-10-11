package com.xmlcalabash.util

import java.util
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.parsers.SequenceBuilder
import com.xmlcalabash.runtime.{StaticContext, XMLCalabashRuntime}
import jdk.nashorn.api.scripting.ScriptObjectMirror
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.{AttributeInfo, FingerprintedQName}
import net.sf.saxon.s9api._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsJava, MapHasAsScala}

object TypeUtils {
  def castAsXml(value: Any): XdmValue = {
    if (value == null) {
      return XdmEmptySequence.getInstance()
    }
    value match {
      case value: XdmValue => value
      case str: String => new XdmAtomicValue(str)
      case int: Integer => new XdmAtomicValue(int)
      case bool: Boolean => new XdmAtomicValue(bool)
      case doub: Double => new XdmAtomicValue(doub)
      case som: ScriptObjectMirror =>
        if (som.isArray) {
          // Is there a faster way to do this?
          val items = ListBuffer.empty[XdmValue]
          for (key <- som.keySet.asScala) {
            val obj = castAsXml(som.get(key))
            items += obj
          }
          var arr = new XdmArray(items.toArray)
          arr
        } else {
          var map = new XdmMap()
          for (key <- som.keySet.asScala) {
            val obj = castAsXml(som.get(key))
            map = map.put(new XdmAtomicValue(key), obj)
          }
          map
        }
      case _ => throw XProcException.xiCastXML(value, None)
    }
  }

  def castAsJava(value: Any): Any = {
    if (value == null) {
      return value
    }
    value match {
      case node: XdmNode =>
        throw XProcException.xiNodesNotAllowed(node)
      case atomic: XdmAtomicValue =>
        atomic.getValue
      case xarr: XdmArray =>
        val list = ListBuffer.empty[Any]
        var idx = 0
        for (idx <- 0  until xarr.arrayLength()) {
          val value = xarr.get(idx)
          list += castAsJava(value)
        }
        list.toArray
      case xmap: XdmMap =>
        val map = xmap.asMap()
        val jmap = mutable.HashMap.empty[Any,Any]
        for (key <- map.asScala.keySet) {
          val value = map.asScala(key)
          jmap.put(castAsJava(key), castAsJava(value))
        }
        jmap.toMap.asJava
      case _ =>
        value
    }
  }

  def castAsScala(value: Any): Any = {
    if (value == null) {
      return value
    }
    value match {
      case node: XdmNode =>
        throw XProcException.xiNodesNotAllowed(node)
      case atomic: XdmAtomicValue =>
        atomic.getValue
      case xarr: XdmArray =>
        val list = ListBuffer.empty[Any]
        var idx = 0
        for (idx <- 0  until xarr.arrayLength()) {
          val value = xarr.get(idx)
          list += castAsScala(value)
        }
        list.toArray
      case xmap: XdmMap =>
        val map = xmap.asMap()
        val jmap = mutable.HashMap.empty[Any,Any]
        for (key <- map.asScala.keySet) {
          val value = map.asScala(key)
          jmap.put(castAsScala(key), castAsScala(value))
        }
        jmap.toMap
      case _ =>
        value
    }
  }

  def mediaType(value: Any): MediaType = {
    value match {
      case v: XdmMap => vnd("map")
      case v: Boolean => vnd("boolean")
      case _ => throw XProcException.xiMediaType(value, None)
    }
  }

  def fqName(name: QName): FingerprintedQName = {
    new FingerprintedQName(name.getPrefix, name.getNamespaceURI, name.getLocalName)
  }

  def attributeInfo(name: QName, value: String): AttributeInfo = {
    TypeUtils.attributeInfo(name, value, null)
  }

  def attributeInfo(name: QName, value: String, location: Location): AttributeInfo = {
    new AttributeInfo(TypeUtils.fqName(name), BuiltInAtomicType.UNTYPED_ATOMIC, value, location, ReceiverOption.NONE)
  }

  private def vnd(t: String): MediaType = {
    MediaType.parse(s"application/vnd.xmlcalabash.$t+xml")
  }
}

class TypeUtils(val processor: Processor, val context: StaticContext) {
  private val err_XD0045 = new QName(XProcConstants.ns_err, "XD0045")

  def this(config: XMLCalabashConfig, context: StaticContext) = {
    this(config.processor, context)
  }
  def this(config: XMLCalabashRuntime, context: StaticContext) = {
    this(config.processor, context)
  }
  def this(config: XMLCalabashConfig) = {
    this(config.processor, new StaticContext(config))
  }
  def this(config: XMLCalabashRuntime) = {
    this(config.processor, new StaticContext(config))
  }

  val typeFactory = new ItemTypeFactory(processor)

  def castAtomicAs(value: XdmAtomicValue, seqType: Option[SequenceType], context: StaticContext): XdmAtomicValue = {
    if (seqType.isEmpty) {
      return value
    }

    castAtomicAs(value, seqType.get.getItemType, context)
  }

  def castAtomicAs(value: XdmAtomicValue, xsdtype: ItemType, context: StaticContext): XdmAtomicValue = {
    if ((xsdtype == ItemType.UNTYPED_ATOMIC) || (xsdtype == ItemType.STRING || (xsdtype == ItemType.ANY_ITEM))) {
      return value
    }

    if (xsdtype == ItemType.QNAME) {
      val qnamev = value.getPrimitiveTypeName match {
        case XProcConstants.xs_string => new XdmAtomicValue(ValueParser.parseQName(value.getStringValue, context))
        case XProcConstants.xs_untypedAtomic => new XdmAtomicValue(ValueParser.parseQName(value.getStringValue, context))
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
        val location = if (context == null) {
          None
        } else {
          context.location
        }

        if (sae.getMessage.contains("Invalid URI")) {
          throw XProcException.xdInvalidURI(value.getStringValue, location)
        } else {
          throw XProcException.xdBadType(value.getStringValue, xsdtype.toString, location)
        }
      case ex: Exception =>
        throw(ex)
    }
  }

  // This was added experimentally to handle lists in literal values for include-filter and exclude-filter.
  // It was subsequently decided that literal values shouldn't be lists, so this is no longer being used.
  // I'm leaving it around for the time being (19 Aug 2018) in case it turns out to be useful somewhere
  // else.
  def castSequenceAs(value: XdmAtomicValue, xsdtype: Option[QName], occurrence: String, context: StaticContext): XdmValue = {
    // Today, we only need to handle a sequence of strings
    if (xsdtype.isEmpty || xsdtype.get != XProcConstants.xs_string) {
      throw new IllegalArgumentException("Only lists of strings are supported")
    }

    val builder = new SequenceBuilder()
    val list = builder.parse(value.getStringValue)
    val alist = new util.ArrayList[XdmAtomicValue]

    val itype = typeFactory.getAtomicType(XProcConstants.xs_string)
    for (item <- list) {
      if (item.as != XProcConstants.xs_string) {
        throw new IllegalArgumentException("Only lists of strings are supported")
      }
      alist.add(new XdmAtomicValue(item.item, itype))
    }

    new XdmValue(alist)
  }

  def parseSequenceType(seqType: Option[String]): Option[SequenceType] = {
    if (seqType.isDefined) {
      Some(parseSequenceType(seqType.get))
    } else {
      None
    }
  }

  def parseSequenceType(seqType: String): SequenceType = {
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
    val itemre  = "^item\\s*\\(\\s*\\)\\s*([*+?])?$".r
    seqType.trim() match {
      case parensre(body) =>
        parseSequenceType(body)
      case stypere(typename, cardchar) =>
        // xs:sometype?
        val itype = simpleType(typename)
        SequenceType.makeSequenceType(itype, cardinality(cardchar))
      case mtypere(mbody, cardchar) =>
        if (mbody.trim == "*") {
          return SequenceType.makeSequenceType(ItemType.ANY_MAP, cardinality(cardchar))
        }
        val tuplere = "^([^,]+),(.*)$".r
        mbody match {
          case tuplere(keyseqtype,itemseqtype) =>
            val keytype = simpleType(keyseqtype)
            val itemtype = parseSequenceType(itemseqtype)
            SequenceType.makeSequenceType(typeFactory.getMapType(keytype, itemtype), cardinality(cardchar))
          case _ =>
            throw new RuntimeException(s"Unexpected map syntax: map($mbody)")
        }
      case atypere(abody, cardchar) =>
        if (abody.trim == "*") {
          return SequenceType.makeSequenceType(ItemType.ANY_ARRAY, cardinality(cardchar))
        }
        val itemtype = parseSequenceType(abody)
        SequenceType.makeSequenceType(typeFactory.getArrayType(itemtype), cardinality(cardchar))
      case ftypere(params, cardchar) =>
        SequenceType.makeSequenceType(ItemType.ANY_FUNCTION, cardinality(cardchar))
      case itemre(cardchar) =>
        SequenceType.makeSequenceType(ItemType.ANY_ITEM, cardinality(cardchar))
      case _ =>
        throw new RuntimeException(s"Unexpected sequence type: $seqType")
    }
  }

  def parseFakeMapSequenceType(seqType: String): SequenceType = {
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
          case tuplere(keyseqtype,itemseqtype) =>
            val keytype = ItemType.ANY_ATOMIC_VALUE
            val itemtype = parseSequenceType(itemseqtype)
            SequenceType.makeSequenceType(typeFactory.getMapType(keytype, itemtype), cardinality(cardchar))
          case _ =>
            throw new RuntimeException(s"Unexpected map syntax: map($mbody)")
        }
      case _ =>
        parseSequenceType(seqType)
    }
  }

  private def simpleType(typename: String): ItemType = {
    val qname = ValueParser.parseQName(typename, context)
    qname match {
      case XProcConstants.xs_anyURI =>             ItemType.ANY_URI
      case XProcConstants.xs_base64Binary =>       ItemType.BASE64_BINARY
      case XProcConstants.xs_boolean =>            ItemType.BOOLEAN
      case XProcConstants.xs_byte =>               ItemType.BYTE
      case XProcConstants.xs_date =>               ItemType.DATE
      case XProcConstants.xs_dateTime =>           ItemType.DATE_TIME
      case XProcConstants.xs_dateTimeStamp =>      ItemType.DATE_TIME_STAMP
      case XProcConstants.xs_dayTimeDuration =>    ItemType.DAY_TIME_DURATION
      case XProcConstants.xs_decimal =>            ItemType.DECIMAL
      case XProcConstants.xs_double =>             ItemType.DOUBLE
      case XProcConstants.xs_duration =>           ItemType.DURATION
      case XProcConstants.xs_ENTITY =>             ItemType.ENTITY
      case XProcConstants.xs_float =>              ItemType.FLOAT
      case XProcConstants.xs_gDay =>               ItemType.G_DAY
      case XProcConstants.xs_gMonth =>             ItemType.G_MONTH
      case XProcConstants.xs_gMonthDay =>          ItemType.G_MONTH_DAY
      case XProcConstants.xs_gYear =>              ItemType.G_YEAR
      case XProcConstants.xs_gYearMonth =>         ItemType.G_YEAR_MONTH
      case XProcConstants.xs_hexBinary =>          ItemType.HEX_BINARY
      case XProcConstants.xs_ID =>                 ItemType.ID
      case XProcConstants.xs_IDREF =>              ItemType.IDREF
      case XProcConstants.xs_int =>                ItemType.INT
      case XProcConstants.xs_integer =>            ItemType.INTEGER
      case XProcConstants.xs_language =>           ItemType.LANGUAGE
      case XProcConstants.xs_long =>               ItemType.LONG
      case XProcConstants.xs_name =>               ItemType.NAME
      case XProcConstants.xs_NCName =>             ItemType.NCNAME
      case XProcConstants.xs_negativeInteger =>    ItemType.NEGATIVE_INTEGER
      case XProcConstants.xs_NMTOKEN =>            ItemType.NMTOKEN
      case XProcConstants.xs_nonNegativeInteger => ItemType.NON_NEGATIVE_INTEGER
      case XProcConstants.xs_nonPositiveInteger => ItemType.NON_POSITIVE_INTEGER
      case XProcConstants.xs_normalizedString =>   ItemType.NORMALIZED_STRING
      case XProcConstants.xs_notation =>           ItemType.NOTATION
      case XProcConstants.xs_positiveInteger =>    ItemType.POSITIVE_INTEGER
      case XProcConstants.xs_QName =>              ItemType.QNAME
      case XProcConstants.xs_short =>              ItemType.SHORT
      case XProcConstants.xs_string =>             ItemType.STRING
      case XProcConstants.xs_time =>               ItemType.TIME
      case XProcConstants.xs_token =>              ItemType.TOKEN
      case XProcConstants.xs_unsignedByte =>       ItemType.UNSIGNED_BYTE
      case XProcConstants.xs_unsignedInt =>        ItemType.UNSIGNED_INT
      case XProcConstants.xs_unsignedLong =>       ItemType.UNSIGNED_LONG
      case XProcConstants.xs_unsignedShort =>      ItemType.UNSIGNED_SHORT
      case XProcConstants.xs_untypedAtomic =>      ItemType.UNTYPED_ATOMIC
      case XProcConstants.xs_yearMonthDuration =>  ItemType.YEAR_MONTH_DURATION
      case XProcConstants.xs_anyAtomicType =>      ItemType.ANY_ATOMIC_VALUE
      case _ =>
        throw XProcException.xsInvalidSequenceType(qname.getClarkName, "Unknown type", context.location)
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

  def valueMatchesType(value: String, dtype: QName): Boolean = {
    try {
      checkType(value, dtype)
      true
    } catch {
      case _: Exception => false
    }
  }

  def checkType(value: String, dtype: QName): Unit = {
    checkType(value, dtype, None, err_XD0045)
  }

  def checkType(value: String, dtype: QName, node: Option[XdmNode]): Unit = {
    checkType(value, dtype, node, err_XD0045)
  }

  def checkType(value: String, dtype: QName, node: Option[XdmNode], code: QName): Unit = {
    if (XProcConstants.xs_string == dtype || XProcConstants.xs_untypedAtomic == dtype) {
      return
    }

    if (XProcConstants.xs_QName == dtype) {
      if (node.isDefined) {
        new QName(value, node.get)
      } else {
        new QName(value)
      }
    }

    val itype = typeFactory.getAtomicType(dtype)
    new XdmAtomicValue(value, itype)
  }
}
