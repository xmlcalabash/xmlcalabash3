package com.xmlcalabash.test

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.util.URIUtils
import com.xmlcalabash.util.stores.{DataInfo, DataReader, DataWriter, FallbackDataStore}
import net.sf.saxon.s9api.XdmAtomicValue
import org.scalatest.flatspec.AnyFlatSpec

import java.io.{InputStream, OutputStream}
import java.net.URI

class FallbackDataStoreSpec extends AnyFlatSpec {
  private val config = XMLCalabashConfig.newInstance()
  private val fallback = new FallbackDataStore()
  private val base = URIUtils.cwdAsURI
  private val nop = new NopIO()

  "readEntry" should "fail" in {
    var pass = false
    try {
      fallback.readEntry("foo.txt", base, "*/*", None, nop)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "writeEntry" should "fail" in {
    var pass = false
    try {
      fallback.writeEntry("foo.txt", base, "application/octet-stream", nop)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "infoEntry" should "fail" in {
    var pass = false
    try {
      fallback.infoEntry("foo.txt", base, "*/*", nop)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "listEachEntry" should "fail" in {
    var pass = false
    try {
      fallback.listEachEntry("foo.txt", base, "*/*", nop)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "createList" should "fail" in {
    var pass = false
    try {
      fallback.createList("foo.txt", base)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "deleteEntry" should "fail" in {
    var pass = false
    try {
      fallback.deleteEntry("foo.txt", base)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  private class NopIO extends DataReader with DataWriter with DataInfo {
    override def load(id: URI, media: String, content: InputStream, len: Option[Long]): Unit = {
      // nop
    }

    override def store(content: OutputStream): Unit = {
      // nop
    }

    override def list(id: URI, props: Map[String, XdmAtomicValue]): Unit = {
      // nop
    }
  }
}
