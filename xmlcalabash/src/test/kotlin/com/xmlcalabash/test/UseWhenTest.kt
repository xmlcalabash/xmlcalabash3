package com.xmlcalabash.test

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.util.UriUtils
import com.xmlcalabash.parsers.xpl.elements.ElementNode
import com.xmlcalabash.parsers.xpl.elements.XplDocumentManager
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class UseWhenTest {
    private fun findYes(node: ElementNode): Boolean {
        var ok = false
        if (node.useWhen == true) {
            if (node.node.nodeName == QName("yes")) {
                ok = true
            }
            if (node.node.nodeName == QName("no")) {
                throw RuntimeException("Found a <no/>.")
            }
            for (child in node.children.filterIsInstance<ElementNode>()) {
                ok = ok || findYes(child)
            }
        }
        return ok
    }

    @Test
    fun nonePipeline() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val manager = XplDocumentManager(builder)
        val document = manager.load(UriUtils.cwdAsUri().resolve("src/test/resources/usewhen/none.xpl"))
        Assertions.assertTrue(findYes(document.rootNode))
    }

    @Test
    fun simplePipeline() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val manager = XplDocumentManager(builder)
        val document = manager.load(UriUtils.cwdAsUri().resolve("src/test/resources/usewhen/simple.xpl"))
        Assertions.assertTrue(findYes(document.rootNode))
    }

    @Test
    fun declTrue() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val manager = XplDocumentManager(builder)
        val document = manager.load(UriUtils.cwdAsUri().resolve("src/test/resources/usewhen/decltrue.xpl"))
        Assertions.assertTrue(findYes(document.rootNode))
    }

    @Test
    fun declLoop() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val manager = XplDocumentManager(builder)
        try {
            manager.load(UriUtils.cwdAsUri().resolve("src/test/resources/usewhen/declloop.xpl"))
            fail()
        } catch (ex: Exception) {
            if (ex is XProcException) {
                Assertions.assertEquals(NsErr.xs(115), ex.error.code)
            } else {
                fail()
            }
        }
    }

    @Test
    fun declImport1() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val manager = XplDocumentManager(builder)
        val document = manager.load(UriUtils.cwdAsUri().resolve("src/test/resources/usewhen/declimport1.xpl"))
        Assertions.assertTrue(findYes(document.rootNode))
    }

    @Test
    fun declImport2() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val manager = XplDocumentManager(builder)
        val document = manager.load(UriUtils.cwdAsUri().resolve("src/test/resources/usewhen/declimport2.xpl"))
        Assertions.assertTrue(findYes(document.rootNode))
    }

    @Test
    fun declImport3() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val manager = XplDocumentManager(builder)
        val document = manager.load(UriUtils.cwdAsUri().resolve("src/test/resources/usewhen/declimport3.xpl"))
        Assertions.assertTrue(findYes(document.rootNode))
    }

    @Test
    fun declOption1() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val manager = XplDocumentManager(builder)
        val document = manager.load(UriUtils.cwdAsUri().resolve("src/test/resources/usewhen/decloption1.xpl"))
        Assertions.assertTrue(findYes(document.rootNode))
    }

    @Test
    fun declOption2() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val manager = XplDocumentManager(builder)
        val document = manager.load(UriUtils.cwdAsUri().resolve("src/test/resources/usewhen/decloption2.xpl"))
        Assertions.assertTrue(findYes(document.rootNode))
    }

    @Test
    fun declOption3() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        builder.option(QName("i", "http://example.com/import/test", "option"), XdmAtomicValue(12))
        val manager = XplDocumentManager(builder)
        val document = manager.load(UriUtils.cwdAsUri().resolve("src/test/resources/usewhen/decloption3.xpl"))
        Assertions.assertTrue(findYes(document.rootNode))
    }

    @Test
    fun declOption4() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        builder.option(QName("i", "http://example.com/import/test", "option"), XdmAtomicValue("not-a-number"))
        val manager = XplDocumentManager(builder)
        try {
            manager.load(UriUtils.cwdAsUri().resolve("src/test/resources/usewhen/decloption3.xpl"))
            fail()
        } catch (ex: Exception) {
            if (ex is XProcException) {
                Assertions.assertEquals(NsErr.xd(36), ex.error.code)
            } else {
                fail()
            }
        }
    }

    @Test
    fun pUseWhenPipeline() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val manager = XplDocumentManager(builder)
        val document = manager.load(UriUtils.cwdAsUri().resolve("src/test/resources/usewhen/pusewhen.xpl"))
        Assertions.assertTrue(findYes(document.rootNode))
    }

    @Test
    fun excludedImport() {
        val xmlCalabash = XmlCalabash.newInstance();
        val builder = xmlCalabash.newPipelineBuilder()
        val manager = XplDocumentManager(builder)
        val document = manager.load(UriUtils.cwdAsUri().resolve("src/test/resources/usewhen/declexcluded.xpl"))
        Assertions.assertTrue(findYes(document.rootNode))
    }


}