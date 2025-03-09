package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.ItemType
import net.sf.saxon.s9api.QName

object NsXs {
    val namespace: NamespaceUri = NamespaceUri.of("http://www.w3.org/2001/XMLSchema")

    val schema = QName(namespace, "xs:schema")
    val ENTITY = QName(namespace, "xs:ENTITY")
    val ID = QName(namespace, "xs:ID")
    val IDREF = QName(namespace, "xs:IDREF")
    val NCName = QName(namespace, "xs:NCName")
    val NMTOKEN = QName(namespace, "xs:NMTOKEN")
    val QName = QName(namespace, "xs:QName")
    val anyAtomicType = QName(namespace, "xs:anyAtomicType")
    val anyURI = QName(namespace, "xs:anyURI")
    val base64Binary = QName(namespace, "xs:base64Binary")
    val boolean = QName(namespace, "xs:boolean")
    val byte = QName(namespace, "xs:byte")
    val date = QName(namespace, "xs:date")
    val dateTime = QName(namespace, "xs:dateTime")
    val dateTimeStamp = QName(namespace, "xs:dateTimeStamp")
    val dayTimeDuration = QName(namespace, "xs:dayTimeDuration")
    val decimal = QName(namespace, "xs:decimal")
    val double = QName(namespace, "xs:double")
    val duration = QName(namespace, "xs:duration")
    val float = QName(namespace, "xs:float")
    val gDay = QName(namespace, "xs:gDay")
    val gMonth = QName(namespace, "xs:gMonth")
    val gMonthDay = QName(namespace, "xs:gMonthDay")
    val gYear = QName(namespace, "xs:gYear")
    val gYearMonth = QName(namespace, "xs:gYearMonth")
    val hexBinary = QName(namespace, "xs:hexBinary")
    val int = QName(namespace, "xs:int")
    val integer = QName(namespace, "xs:integer")
    val language = QName(namespace, "xs:language")
    val long = QName(namespace, "xs:long")
    val name = QName(namespace, "xs:name")
    val negativeInteger = QName(namespace, "xs:negativeInteger")
    val nonNegativeInteger = QName(namespace, "xs:nonNegativeInteger")
    val nonPositiveInteger = QName(namespace, "xs:nonPositiveInteger")
    val normalizedString = QName(namespace, "xs:normalizedString")
    val notation = QName(namespace, "xs:notation")
    val positiveInteger = QName(namespace, "xs:positiveInteger")
    val short = QName(namespace, "xs:short")
    val string = QName(namespace, "xs:string")
    val time = QName(namespace, "xs:time")
    val token = QName(namespace, "xs:token")
    val unsignedByte = QName(namespace, "xs:unsignedByte")
    val unsignedInt = QName(namespace, "xs:unsignedInt")
    val unsignedLong = QName(namespace, "xs:unsignedLong")
    val unsignedShort = QName(namespace, "xs:unsignedShort")
    val untypedAtomic = QName(namespace, "xs:untypedAtomic")
    val yearMonthDuration = QName(namespace, "xs:yearMonthDuration")

    val numericTypes = listOf(float, double, decimal, byte, short, int, integer, long,
        negativeInteger, nonNegativeInteger, nonPositiveInteger, positiveInteger,
        unsignedByte, unsignedInt, unsignedLong, unsignedShort, untypedAtomic)

    val untyped = QName(namespace, "xs:untyped")

    fun typeOf(qname: QName): ItemType {
        return when(qname) {
            anyURI ->             ItemType.ANY_URI
            base64Binary ->       ItemType.BASE64_BINARY
            boolean ->            ItemType.BOOLEAN
            byte ->               ItemType.BYTE
            date ->               ItemType.DATE
            dateTime ->           ItemType.DATE_TIME
            dateTimeStamp ->      ItemType.DATE_TIME_STAMP
            dayTimeDuration ->    ItemType.DAY_TIME_DURATION
            decimal ->            ItemType.DECIMAL
            double ->             ItemType.DOUBLE
            duration ->           ItemType.DURATION
            ENTITY ->             ItemType.ENTITY
            float ->              ItemType.FLOAT
            gDay ->               ItemType.G_DAY
            gMonth ->             ItemType.G_MONTH
            gMonthDay ->          ItemType.G_MONTH_DAY
            gYear ->              ItemType.G_YEAR
            gYearMonth ->         ItemType.G_YEAR_MONTH
            hexBinary ->          ItemType.HEX_BINARY
            ID ->                 ItemType.ID
            IDREF ->              ItemType.IDREF
            int ->                ItemType.INT
            integer ->            ItemType.INTEGER
            language ->           ItemType.LANGUAGE
            long ->               ItemType.LONG
            name ->               ItemType.NAME
            NCName ->             ItemType.NCNAME
            negativeInteger ->    ItemType.NEGATIVE_INTEGER
            NMTOKEN ->            ItemType.NMTOKEN
            nonNegativeInteger -> ItemType.NON_NEGATIVE_INTEGER
            nonPositiveInteger -> ItemType.NON_POSITIVE_INTEGER
            normalizedString ->   ItemType.NORMALIZED_STRING
            notation ->           ItemType.NOTATION
            positiveInteger ->    ItemType.POSITIVE_INTEGER
            QName ->              ItemType.QNAME
            short ->              ItemType.SHORT
            string ->             ItemType.STRING
            time ->               ItemType.TIME
            token ->              ItemType.TOKEN
            unsignedByte ->       ItemType.UNSIGNED_BYTE
            unsignedInt ->        ItemType.UNSIGNED_INT
            unsignedLong ->       ItemType.UNSIGNED_LONG
            unsignedShort ->      ItemType.UNSIGNED_SHORT
            untypedAtomic ->      ItemType.UNTYPED_ATOMIC
            yearMonthDuration ->  ItemType.YEAR_MONTH_DURATION
            anyAtomicType ->      ItemType.ANY_ATOMIC_VALUE
            else -> throw RuntimeException("invalid sequence type") // XProcError.xsInvalidSequenceType(qname.toString()).exception()
        }
    }
}