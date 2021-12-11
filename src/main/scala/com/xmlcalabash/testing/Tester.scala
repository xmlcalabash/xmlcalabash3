package com.xmlcalabash.testing

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.TestException
import com.xmlcalabash.util.{PipelineOutputConsumer, S9Api}
import net.sf.saxon.s9api.{QName, Serializer, XdmNode}
import org.slf4j.{Logger, LoggerFactory}

import java.io.StringWriter

class Tester(xmlcalabash: XMLCalabash) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var _pipeline = Option.empty[XdmNode]
  private var _schematron = Option.empty[XdmNode]
  private val _test     = new QName("", "test")

  def pipeline: Option[XdmNode] = _pipeline
  def pipeline_=(tpipe: XdmNode): Unit = {
    if (_pipeline.isEmpty) {
      val pipe = S9Api.removeNamespaces(xmlcalabash.processor, tpipe, Set("http://xproc.org/ns/testsuite/3.0"), true)
      _pipeline = Some(pipe)
      xmlcalabash.args.pipeline(pipe)
    } else {
      throw new TestException("Cannot reset pipeline in test")
    }
  }

  def schematron: Option[XdmNode] = _schematron
  def schematron_=(schema: XdmNode): Unit = {
    if (_schematron.isEmpty) {
      _schematron = Some(schema)
    } else {
      throw new TestException("Cannot reset schematron in test")
    }
  }

  def run(): TestResult = {
    try {
      var result: Option[BufferingConsumer] = Some(new BufferingConsumer())
      xmlcalabash.parameter(new PipelineOutputConsumer("result", result.get))

      xmlcalabash.configure()

      val decl = xmlcalabash.step
      if (decl.outputPorts.contains("result")) {
        result.get.mediaTypes = decl.output("result").contentTypes
      } else {
        result = None
      }

      xmlcalabash.run()

      val resultDoc = if (result.isDefined && result.get.messages.nonEmpty) {
        result.get.messages.head.item.asInstanceOf[XdmNode]
      }  else {
        return new TestResult(false, "Result returned no messages")
      }

      //println(resultDoc)

      //System.err.println("RESULTDOC:")
      //System.err.println(resultDoc)

      if (_schematron.isDefined) {
        var fail = ""
        val schematest = new Schematron(xmlcalabash)
        val x = schematron.get
        val y = x.getBaseURI
        val results = schematest.test(resultDoc, schematron.get)
        for (result <- results) {
          val xpath = result.getAttributeValue(_test)
          val text = result.getStringValue
          if (fail == "") {
            fail = s"$xpath: $text"
          }
        }
        if (results.isEmpty) {
          new TestResult(true)
        } else {
          val sw = new StringWriter()
          val ser = xmlcalabash.processor.newSerializer(sw)
          ser.setOutputProperty(Serializer.Property.INDENT, "no")
          S9Api.serialize(xmlcalabash, resultDoc, ser)
          sw.close();
          System.err.println(sw.toString)
          for (item <- results) {
            System.err.println(item.getStringValue)
          }
          if (fail == "") {
            new TestResult(false, "SCHEMATRON")
          } else {
            new TestResult(false, fail)
          }
        }
      } else {
        logger.info(s"NONE: ${_pipeline.get.getBaseURI}")
        new TestResult(true)
      }
    } catch {
      case ex: Exception =>
        println(s"XProc exception: ${xmlcalabash.errorMessage}")
        new TestResult(ex)
    }
  }
}
