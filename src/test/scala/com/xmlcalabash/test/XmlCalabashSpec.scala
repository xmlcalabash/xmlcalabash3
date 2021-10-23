package com.xmlcalabash.test

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.util.{MediaType, URIUtils}
import net.sf.saxon.s9api.Processor
import org.scalatest.flatspec.AnyFlatSpec
import org.xml.sax.InputSource

import java.io.{ByteArrayInputStream, File, InputStream}
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.xml.transform.sax.SAXSource

class XmlCalabashSpec extends AnyFlatSpec {

  "Simple initialization " should " succeed" in {
    val proc = XMLCalabash.newInstance()
    assert(proc != null)
  }

  "Inputs " should " work" in {
    val proc = XMLCalabash.newInstance()
    proc.args.pipeline("pipe.xpl")
    proc.args.input("source", "src/test/resources/config.xml")
    proc.args.input("source", new File("src/test/resources/config.xml"))
    proc.args.input("source", URI.create(s"${System.getProperty("user.dir")}/src/test/resources/config.xml"))
    proc.resolve()
  }

  "Outputs " should " work" in {
    val proc = XMLCalabash.newInstance()
    proc.args.pipeline("pipe.xpl")
    proc.args.input("result1", "/tmp/one")
    proc.args.input("result2", new File("/tmp/two"))
    proc.args.input("result3", URI.create("file:///tmp/three"))
    proc.resolve()
  }

  "Repeating an outputs " should " make a list" in {
    val proc = XMLCalabash.newInstance()
    proc.args.pipeline("pipe.xpl")
    proc.args.input("result", "/tmp/one")
    proc.args.input("result", new File("/tmp/two"))
    proc.args.input("result", URI.create("file:///tmp/three"))
    proc.resolve()
  }

  "Non-file output URI " should " raise an exception" in {
    try {
      val proc = XMLCalabash.newInstance()
      proc.args.pipeline("pipe.xpl")
      proc.args.input("result3", URI.create("http://example.com/output"))
      proc.resolve()
      fail()
    } catch {
      case _: Throwable =>
        ()
    }
  }

  "EQName options " should " succeed" in {
    val proc = XMLCalabash.newInstance()
    proc.args.pipeline("pipe.xpl")
    proc.args.option("test", "string value")
    proc.args.option("Q{http://example.com/}test", 35)
    proc.args.option("test2", 3.4)
    proc.args.option("truthy", true)
    proc.resolve()
  }

  "Repeating an option " should " create a sequence" in {
    val proc = XMLCalabash.newInstance()
    proc.args.pipeline("pipe.xpl")
    proc.args.option("test", "string value")
    proc.args.option("Q{}test", 35)
    proc.resolve()
  }

  "QNames " should " work for defined namespaces" in {
    val proc = XMLCalabash.newInstance()
    proc.args.pipeline("pipe.xpl")
    proc.args.namespace("ex", "http://example.com/one")
    proc.args.option("ex:test", "string value")
    proc.args.namespace("ex", "http://example.com/two")
    proc.args.option("ex:test", 35)
    proc.args.option("test2", 3.4)
    proc.resolve()
  }

  "QNames " should " fail for missing namespaces" in {
    try {
      val proc = XMLCalabash.newInstance()
      proc.args.option("ex:test", "string value")
      proc.resolve()
      fail()
    } catch {
      case _: Throwable =>
        ()
    }
  }

  "Documents " should " work for options" in {
    val proc = XMLCalabash.newInstance()
    proc.args.pipeline("pipe.xpl")
    proc.args.optionDocument("json", URIUtils.cwdAsURI.resolve("src/test/resources/doc.json"))
    proc.args.optionDocument("xmlstring", "src/test/resources/config.xml")
    proc.args.optionDocument("xmlfile", new File("src/test/resources/config.xml"))
    proc.resolve()
  }

  "Expressions " should " work for options" in {
    val proc = XMLCalabash.newInstance()
    proc.args.pipeline("pipe.xpl")
    proc.args.optionExpression("json", "map{'test': 1}")
    proc.args.optionExpression("sum", "3+4")
    proc.args.optionExpression("list", "(1,2, QName('', 'name'))")
    proc.resolve()
  }

  "Expressions " should " be able to refer to preceding options" in {
    val proc = XMLCalabash.newInstance()
    proc.args.pipeline("pipe.xpl")
    proc.args.optionExpression("sum", "3+4")
    proc.args.optionExpression("sumlist", "(1,2, $sum)")
    proc.args.optionExpression("sumsum", "$sum + 5")
    proc.resolve()
  }

  "Expressions " should " not be able to refer to other options" in {
    try {
      val proc = XMLCalabash.newInstance()
      proc.args.pipeline("pipe.xpl")
      proc.args.optionExpression("no", "$foo")
      proc.resolve()
      fail()
    } catch {
      case _: Throwable =>
        ()
    }
  }

  "Loading a node from a different processor " should " fail" in {
    try {
      val processor = new Processor(false)
      val proc = XMLCalabash.newInstance()

      val bytes = s"<p:declare-step xmlns:p='${XProcConstants.ns_p}' version='3.0'><p:identity><p:with-input><doc/></p:with-input></p:identity><p:sink/></p:declare-step>".getBytes(StandardCharsets.UTF_8)
      val bais = new ByteArrayInputStream(bytes)
      val builder = processor.newDocumentBuilder()
      val doc = builder.build(new SAXSource(new InputSource(bais)))

      proc.args.pipeline(doc)
      proc.resolve()
      fail()
    } catch {
      case ex: XProcException =>
        assert(ex.code == XProcException.cx_XI0073)
      case _: Exception =>
        fail()
    }
  }

    "Loading a node from the same processor " should " succeed" in {
      try {
        val processor = new Processor(false)
        val proc = XMLCalabash.newInstance(processor)

        val bytes = s"<p:declare-step xmlns:p='${XProcConstants.ns_p}' version='3.0'><p:identity><p:with-input><doc/></p:with-input></p:identity><p:sink/></p:declare-step>".getBytes(StandardCharsets.UTF_8)
        val bais = new ByteArrayInputStream(bytes)
        val builder = processor.newDocumentBuilder()
        val doc = builder.build(new SAXSource(new InputSource(bais)))

        proc.args.pipeline(doc)
        proc.resolve()
      } catch {
        case _: Exception =>
          fail()
      }
  }

  "Loading a string " should " succeed" in {
    try {
      val proc = XMLCalabash.newInstance()

      val doc = s"<p:declare-step xmlns:p='${XProcConstants.ns_p}' version='3.0'><p:identity><p:with-input><doc/></p:with-input></p:identity><p:sink/></p:declare-step>"

      proc.args.pipeline(doc, MediaType.XML)
      proc.resolve()
    } catch {
      case _: Exception =>
        fail()
    }
  }

  /*
  "Run " should " run a pipeline" in {
    val proc = XMLCalabash.newInstance()
    proc.args.pipeline("pipe.xpl")
    proc.args.optionExpression("value", "'test' || string(3+4)")
    proc.args.input("source", "pipe.xpl")
    proc.run()
  }
   */
}
