package com.xmlcalabash.test

import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.steps.archives.*
import com.xmlcalabash.util.UriUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
class ArchiveTest {
    lateinit var stepConfig: XProcStepConfiguration

    @BeforeAll
    fun init() {
        val xmlCalabash = XmlCalabash.newInstance()
        val builder = xmlCalabash.newPipelineBuilder()
        stepConfig = builder.stepConfig
    }

    @Test
    fun read7zFile() {
        val cwd = UriUtils.cwdAsUri()
        val zip = cwd.resolve("../tests/extra-suite/test-suite/documents/documents.7z")
        val doc = stepConfig.environment.documentManager.load(zip, stepConfig)
        val input = SevenZInputArchive(stepConfig, doc as XProcBinaryDocument)
        input.open()

        val stream = input.inputStream(input.entries.get(3))
        val text = String(stream.readAllBytes())
        Assertions.assertTrue(text.contains("Hello, world."))

        /*
        val newdata = stepConfig.environment.documentManager.load(cwd.resolve("../app/pipe.xpl"), stepConfig)
        val newentry = XArchiveEntry(stepConfig, "/pipes/pipe.xpl", newdata)

        val output = SevenZOutputArchive(stepConfig)
        output.create(File("/tmp/out.7z"))
        output.write(input.entries.get(3))
        output.write(newentry)
        output.close()
         */

        input.close()
    }

    @Test
    fun readArFile() {
        val cwd = UriUtils.cwdAsUri()
        val zip = cwd.resolve("../tests/extra-suite/test-suite/documents/documents.a")
        val doc = stepConfig.environment.documentManager.load(zip, stepConfig)
        val input = ArInputArchive(stepConfig, doc as XProcBinaryDocument)
        input.open()

        val stream = input.inputStream(input.entries.get(3))
        val text = String(stream.readAllBytes())
        Assertions.assertTrue(text.contains("Hello, world."))

        /*
        val newdata = stepConfig.environment.documentManager.load(cwd.resolve("../app/pipe.xpl"), stepConfig)
        val newentry = XArchiveEntry(stepConfig, "/pipes/pipe.xpl", newdata)
        val output = ArOutputArchive(stepConfig)
        output.create(File("/tmp/out.a"))
        output.write(input.entries.get(3))
        output.write(newentry)
        output.close()
         */

        input.close()
    }

    @Test
    fun readArjFile() {
        val cwd = UriUtils.cwdAsUri()
        val zip = cwd.resolve("../tests/extra-suite/test-suite/documents/documents.arj")
        val doc = stepConfig.environment.documentManager.load(zip, stepConfig)
        val input = ArjInputArchive(stepConfig, doc as XProcBinaryDocument)
        input.open()

        val stream = input.inputStream(input.entries.get(3))
        val text = String(stream.readAllBytes())
        Assertions.assertTrue(text.contains("Hello, world."))

        input.close()
    }

    @Test
    fun readCpioFile() {
        val cwd = UriUtils.cwdAsUri()
        val zip = cwd.resolve("../tests/extra-suite/test-suite/documents/documents.cpio")
        val doc = stepConfig.environment.documentManager.load(zip, stepConfig)
        val input = CpioInputArchive(stepConfig, doc as XProcBinaryDocument)
        input.open()

        val stream = input.inputStream(input.entries.get(3))
        val text = String(stream.readAllBytes())
        Assertions.assertTrue(text.contains("Hello, world."))

        /*
        val newdata = stepConfig.environment.documentManager.load(cwd.resolve("../app/pipe.xpl"), stepConfig)
        val newentry = XArchiveEntry(stepConfig, "/pipes/pipe.xpl", newdata)

        val output = CpioOutputArchive(stepConfig)
        output.create(File("/tmp/out.cpio"))

        output.write(input.entries.get(3))
        output.write(newentry)
        output.close()
         */

        input.close()
    }

    @Test
    fun readTarFile() {
        val cwd = UriUtils.cwdAsUri()
        val zip = cwd.resolve("../tests/extra-suite/test-suite/documents/documents.tar.gz")
        val doc = stepConfig.environment.documentManager.load(zip, stepConfig)
        val input = TarInputArchive(stepConfig, doc as XProcBinaryDocument)
        input.open()

        val stream = input.inputStream(input.entries.get(3))
        val text = String(stream.readAllBytes())
        Assertions.assertTrue(text.contains("Hello, world."))

        /*
        val newdata = stepConfig.environment.documentManager.load(cwd.resolve("../app/pipe.xpl"), stepConfig)
        val newentry = XArchiveEntry(stepConfig, "/pipes/pipe.xpl", newdata)
        newentry.properties[NsCx.symbolicLink] = "true"
        newentry.properties[NsCx.linkName] = "documents/document.html"

        val output = TarOutputArchive(stepConfig)
        output.create(File("/tmp/out.tar"))

        output.write(input.entries.get(3))
        output.write(newentry)
        output.close()
         */

        input.close()
    }

    @Test
    fun readZipFile() {
        val cwd = UriUtils.cwdAsUri()
        val zip = cwd.resolve("../tests/extra-suite/test-suite/documents/documents.zip")
        val doc = stepConfig.environment.documentManager.load(zip, stepConfig)
        val input = ZipInputArchive(stepConfig, doc as XProcBinaryDocument)
        input.open()

        val stream = input.inputStream(input.entries.get(3))
        val text = String(stream.readAllBytes())
        Assertions.assertTrue(text.contains("Hello, world."))

        /*
        val newdata = stepConfig.environment.documentManager.load(cwd.resolve("../app/pipe.xpl"), stepConfig)
        val newentry = XArchiveEntry(stepConfig, "/pipes/pipe.xpl", newdata)

        val output = ZipOutputArchive(stepConfig)
        output.create(File("/tmp/out.zip"))

        output.write(input.entries.get(3))
        output.write(newentry)
        output.close()
         */

        input.close()
    }
}