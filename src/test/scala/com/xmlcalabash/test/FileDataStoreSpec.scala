package com.xmlcalabash.test

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.util.stores.{DataInfo, DataReader, DataWriter, FallbackDataStore, FileDataStore}
import net.sf.saxon.s9api.{Processor, XdmAtomicValue}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

import java.io.{File, InputStream, OutputStream}
import java.net.URI

class FileDataStoreSpec extends AnyFlatSpec with BeforeAndAfter {
  System.setProperty("com.xmlcalabash.configFile", "src/test/resources/config.xml")
  private val config = XMLCalabash.newInstance(new Processor(false))
  private val fileStore = new FileDataStore(config, new FallbackDataStore())
  private val testIO = new TestIO()
  private val tempDir: File = File.createTempFile("xml-calabash-test-", ".dir")
  private var tempFile: File = null

  before {
    tempDir.delete()
    tempDir.mkdir()
    tempFile = File.createTempFile("xml-calabash-test-", ".bin", tempDir)
  }

  after {
    tempFile.delete()
    tempDir.delete()
  }

  "readEntry" should "pass" in {
    var pass = true
    try {
      fileStore.readEntry(tempFile.getName, tempDir.toURI, "*/*", None, testIO)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  "writeEntry" should "pass" in {
    var pass = true
    try {
      fileStore.writeEntry("foo.txt", tempDir.toURI, "text/plain", testIO)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  "infoEntry" should "pass" in {
    var pass = true
    try {
      fileStore.infoEntry(tempFile.getName, tempDir.toURI, "*/*", testIO)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  "listEachEntry" should "pass" in {
    var pass = true
    try {
      fileStore.listEachEntry("", tempDir.toURI, "*/*", testIO)
    } catch {
      case _: Throwable => pass = false
    }
    pass = pass && testIO.sawFile
    assert(pass)
  }

  "createList" should "pass" in {
    var pass = true
    try {
      fileStore.createList("subdir", tempDir.toURI)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  "deleteEntry" should "pass" in {
    var pass = true
    try {
      fileStore.deleteEntry("foo.txt", tempDir.toURI)
      fileStore.deleteEntry("subdir", tempDir.toURI)
    } catch {
      case _: Throwable => pass = false
    }
    assert(pass)
  }

  private class TestIO extends DataReader with DataWriter with DataInfo {
    var sawFile = false

    override def load(id: URI, media: String, content: InputStream, len: Option[Long]): Unit = {
      // nop
    }

    override def store(content: OutputStream): Unit = {
      // nop
    }

    override def list(id: URI, props: Map[String, XdmAtomicValue]): Unit = {
      sawFile = sawFile || id.toASCIIString.endsWith("/foo.txt")
    }
  }

}
