package com.xmlcalabash.pagedmedia

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaServiceProvider
import com.xmlcalabash.util.BufferingReceiver
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.io.FileOutputStream
import java.net.URI

class AHSmokeTest {
    companion object {
        const val WRITE_OUTPUT = false
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XMLCALABASH_TEST_AHF", matches = "true")
    fun testGenericXslFormatter() {
        val managers = mutableListOf<PagedMediaManager>()
        for (provider in PagedMediaServiceProvider.providers()) {
            managers.add(provider.create())
        }
        Assertions.assertTrue(managers.isNotEmpty())
        var xslManager: PagedMediaManager? = null
        for (manager in managers) {
            if (manager.formatterAvailable(URI("https://xmlcalabash.com/paged-media/xsl-formatter"))) {
                xslManager = manager
                break
            }
        }

        Assertions.assertNotNull(xslManager)
        Assertions.assertTrue(xslManager!!.formatterAvailable(URI("https://xmlcalabash.com/paged-media/xsl-formatter/antenna-house")))
        Assertions.assertTrue(xslManager.formatterAvailable(URI("https://xmlcalabash.com/paged-media/xsl-formatter")))
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XMLCALABASH_TEST_AHF", matches = "true")
    fun testAhXslFormatter() {
        val managers = mutableListOf<PagedMediaManager>()
        for (provider in PagedMediaServiceProvider.providers()) {
            managers.add(provider.create())
        }
        Assertions.assertTrue(managers.isNotEmpty())
        var xslManager: PagedMediaManager? = null
        for (manager in managers) {
            if (manager.formatterAvailable(URI("https://xmlcalabash.com/paged-media/xsl-formatter/antenna-house"))) {
                xslManager = manager
                break
            }
        }

        Assertions.assertNotNull(xslManager)
        Assertions.assertTrue(xslManager!!.formatterAvailable(URI("https://xmlcalabash.com/paged-media/xsl-formatter/antenna-house")))
        Assertions.assertTrue(xslManager.formatterAvailable(URI("https://xmlcalabash.com/paged-media/xsl-formatter")))
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XMLCALABASH_TEST_AHF", matches = "true")
    fun testGenericCssFormatter() {
        val managers = mutableListOf<PagedMediaManager>()
        for (provider in PagedMediaServiceProvider.providers()) {
            managers.add(provider.create())
        }
        Assertions.assertTrue(managers.isNotEmpty())
        var xslManager: PagedMediaManager? = null
        for (manager in managers) {
            if (manager.formatterAvailable(URI("https://xmlcalabash.com/paged-media/css-formatter"))) {
                xslManager = manager
                break
            }
        }

        Assertions.assertNotNull(xslManager)
        Assertions.assertTrue(xslManager!!.formatterAvailable(URI("https://xmlcalabash.com/paged-media/css-formatter/antenna-house")))
        Assertions.assertTrue(xslManager.formatterAvailable(URI("https://xmlcalabash.com/paged-media/css-formatter")))
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XMLCALABASH_TEST_AHF", matches = "true")
    fun testAhCssFormatter() {
        val managers = mutableListOf<PagedMediaManager>()
        for (provider in PagedMediaServiceProvider.providers()) {
            managers.add(provider.create())
        }
        Assertions.assertTrue(managers.isNotEmpty())
        var xslManager: PagedMediaManager? = null
        for (manager in managers) {
            if (manager.formatterAvailable(URI("https://xmlcalabash.com/paged-media/css-formatter/antenna-house"))) {
                xslManager = manager
                break
            }
        }

        Assertions.assertNotNull(xslManager)
        Assertions.assertTrue(xslManager!!.formatterAvailable(URI("https://xmlcalabash.com/paged-media/css-formatter/antenna-house")))
        Assertions.assertTrue(xslManager.formatterAvailable(URI("https://xmlcalabash.com/paged-media/css-formatter")))
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XMLCALABASH_TEST_AHF", matches = "true")
    fun testCssFormatter() {
        val calabash = XmlCalabash.newInstance()
        val parser = calabash.newXProcParser()
        val decl = parser.parse("src/test/resources/css.xpl")

        val exec = decl.getExecutable()
        val receiver = BufferingReceiver()
        exec.receiver = receiver

        try {
            exec.run()
        } catch (ex: Exception) {
            fail()
        }

        val result = receiver.outputs["result"]!!.first()
        Assertions.assertEquals(MediaType.PDF, result.contentType)

        if (WRITE_OUTPUT) {
            val out = FileOutputStream(File("/tmp/out.pdf"))
            out.write((result as XProcBinaryDocument).binaryValue)
            out.close()
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XMLCALABASH_TEST_AHF", matches = "true")
    fun testFoFormatter() {
        val calabash = XmlCalabash.newInstance()
        val parser = calabash.newXProcParser()
        val decl = parser.parse("src/test/resources/fo.xpl")

        val exec = decl.getExecutable()
        val receiver = BufferingReceiver()
        exec.receiver = receiver

        try {
            exec.run()
        } catch (ex: Exception) {
            fail()
        }

        val result = receiver.outputs["result"]!!.first()
        Assertions.assertEquals(MediaType.PDF, result.contentType)

        if (WRITE_OUTPUT) {
            val out = FileOutputStream(File("/tmp/envelope.pdf"))
            out.write((result as XProcBinaryDocument).binaryValue)
            out.close()
        }
    }
}