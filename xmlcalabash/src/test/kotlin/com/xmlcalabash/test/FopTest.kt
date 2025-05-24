package com.xmlcalabash.test

import com.xmlcalabash.XmlCalabash
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.spi.PagedMediaManager
import com.xmlcalabash.spi.PagedMediaServiceProvider
import com.xmlcalabash.util.BufferingReceiver
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileOutputStream
import java.net.URI

class FopTest {
    companion object {
        const val WRITE_OUTPUT = false
    }

    @Test
    fun testFopXslFormatter() {
        val managers = mutableListOf<PagedMediaManager>()
        for (provider in PagedMediaServiceProvider.providers()) {
            managers.add(provider.create())
        }
        Assertions.assertTrue(managers.isNotEmpty())
        var xslManager: PagedMediaManager? = null
        for (manager in managers) {
            if (manager.formatterAvailable(URI("https://xmlcalabash.com/paged-media/xsl-formatter/fop"))) {
                xslManager = manager
                break
            }
        }

        Assertions.assertNotNull(xslManager)
        Assertions.assertTrue(xslManager!!.formatterAvailable(URI("https://xmlcalabash.com/paged-media/xsl-formatter/fop")))
        Assertions.assertTrue(xslManager.formatterAvailable(URI("https://xmlcalabash.com/paged-media/xsl-formatter")))
        Assertions.assertFalse(xslManager.formatterAvailable(URI("https://xmlcalabash.com/paged-media/css-formatter")))
    }

    @Test
    fun testFormatter() {
        val calabash = XmlCalabash.newInstance()
        val parser = calabash.newXProcParser()
        val decl = parser.parse("src/test/resources/fop.xpl")

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