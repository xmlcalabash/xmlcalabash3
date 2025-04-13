package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.util.S9Api
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import javax.xml.transform.sax.SAXSource

class SConfigTest {
    @Test
    fun testInitialConstruction() {
        val xmlCalabash = XmlCalabash.newInstance()
        val sconfig = xmlCalabash.saxonConfiguration

        val builder = sconfig.processor.newDocumentBuilder()
        val xml = "<doc/>"
        var stream = ByteArrayInputStream(xml.toByteArray())
        var source = SAXSource(InputSource(stream))
        val node = builder.build(source)
        Assertions.assertNotNull(node)
    }

    @Test
    fun testNewConstruction() {
        val xmlCalabash = XmlCalabash.newInstance()
        val sconfig = xmlCalabash.saxonConfiguration

        val builder = sconfig.processor.newDocumentBuilder()
        val xml = "<doc/>"
        var stream = ByteArrayInputStream(xml.toByteArray())
        var source = SAXSource(InputSource(stream))
        val node = builder.build(source)
        Assertions.assertNotNull(node)
        val root = S9Api.documentElement(node)

        val newconfig = sconfig.newConfiguration()
        val newbuilder = newconfig.processor.newDocumentBuilder()
        val newxml = "<outer><doc/></outer>"
        stream = ByteArrayInputStream(newxml.toByteArray())
        source = SAXSource(InputSource(stream))
        val newnode = newbuilder.build(source)
        val newroot = S9Api.documentElement(newnode)
        val doc = newroot.children().first()

        Assertions.assertEquals(root.nodeName, doc.nodeName)
        Assertions.assertSame(root.processor.underlyingConfiguration.namePool, newroot.processor.underlyingConfiguration.namePool)
    }
}