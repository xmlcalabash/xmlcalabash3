package com.xmlcalabash.test

import com.xmlcalabash.util.S9Api
import com.xmlcalabash.util.SaxonTreeBuilder
import com.xmlcalabash.util.XAttributeMap
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.QName
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class XAttributeMapTest {
    private val processor = Processor(false)

    @Test
    fun noAttributesTest() {
        val builder = SaxonTreeBuilder(processor)
        builder.startDocument(null)
        val amap = XAttributeMap()
        builder.addStartElement(QName("attributes"), amap.attributes)
        builder.addEndElement()
        builder.endDocument()
        val result = S9Api.documentElement(builder.result)
        var count = 0
        for (attr in result.axisIterator(Axis.ATTRIBUTE)) {
            count++
        }
        Assertions.assertEquals(0, count)
    }

    @Test
    fun oneAttributesTest() {
        val builder = SaxonTreeBuilder(processor)
        builder.startDocument(null)
        val amap = XAttributeMap()
        amap[QName("one")] = "one"
        builder.addStartElement(QName("attributes"), amap.attributes)
        builder.addEndElement()
        builder.endDocument()
        val result = S9Api.documentElement(builder.result)
        var count = 0
        for (attr in result.axisIterator(Axis.ATTRIBUTE)) {
            count++
        }
        Assertions.assertEquals(1, count)
        Assertions.assertEquals("one", result.getAttributeValue(QName("one")))
    }

    @Test
    fun twoAttributesTest() {
        val builder = SaxonTreeBuilder(processor)
        builder.startDocument(null)
        val amap = XAttributeMap()
        amap[QName("one")] = "one"
        amap[QName("two")] = "two"
        builder.addStartElement(QName("attributes"), amap.attributes)
        builder.addEndElement()
        builder.endDocument()
        val result = S9Api.documentElement(builder.result)
        var count = 0
        for (attr in result.axisIterator(Axis.ATTRIBUTE)) {
            count++
        }
        Assertions.assertEquals(2, count)
        Assertions.assertEquals("one", result.getAttributeValue(QName("one")))
        Assertions.assertEquals("two", result.getAttributeValue(QName("two")))
    }

    @Test
    fun nullValueTest() {
        val builder = SaxonTreeBuilder(processor)
        builder.startDocument(null)
        val amap = XAttributeMap()
        amap[QName("one")] = "one"
        amap[QName("two")] = null
        builder.addStartElement(QName("attributes"), amap.attributes)
        builder.addEndElement()
        builder.endDocument()
        val result = S9Api.documentElement(builder.result)
        var count = 0
        for (attr in result.axisIterator(Axis.ATTRIBUTE)) {
            count++
        }
        Assertions.assertEquals(1, count)
        Assertions.assertEquals("one", result.getAttributeValue(QName("one")))
    }

}