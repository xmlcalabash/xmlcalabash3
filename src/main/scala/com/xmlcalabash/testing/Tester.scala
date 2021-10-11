package com.xmlcalabash.testing

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{ModelException, TestException, XProcException}
import com.xmlcalabash.messages.XdmNodeItemMessage
import com.xmlcalabash.model.xml.Parser
import com.xmlcalabash.runtime.{BufferingConsumer, StaticContext, XMLCalabashRuntime, XProcMetadata}
import com.xmlcalabash.util.{MediaType, S9Api, XProcVarValue}
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Tester(runtimeConfig: XMLCalabashConfig) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var _pipeline = Option.empty[XdmNode]
  private var _schematron = Option.empty[XdmNode]
  private var _inputs   = mutable.HashMap.empty[String, ListBuffer[XdmNode]]
  private var _bindings = mutable.HashMap.empty[QName, XdmValue]
  private var _tests    = Option.empty[String]
  private val _test     = new QName("", "test")
  private val _parser   = new Parser(runtimeConfig)
  private val context   = new StaticContext(runtimeConfig)

  //runtimeConfig.debugOptions.jafplGraph = None

  def pipeline: Option[XdmNode] = _pipeline
  def pipeline_=(tpipe: XdmNode): Unit = {
    if (_pipeline.isEmpty) {
      val pipe = S9Api.removeNamespaces(runtimeConfig, tpipe, Set("http://xproc.org/ns/testsuite/3.0"), true)
      _pipeline = Some(pipe)
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

  def addInput(port: String, item: XdmNode): Unit = {
    if (_inputs.contains(port)) {
      _inputs(port) += item
    } else {
      val list = ListBuffer.empty[XdmNode]
      list += item
      _inputs.put(port, list)
    }
  }

  def addBinding(optname: QName, item: XdmValue): Unit = {
    _bindings.put(optname, item)
  }

  def run(): TestResult = {
    if (_pipeline.isEmpty) {
      throw new TestException("No pipeline specified")
    }

    var runtime: XMLCalabashRuntime = null
    try {
      val decl = _parser.loadDeclareStep(_pipeline.get)
      runtime = decl.runtime()
      val result = new BufferingConsumer(decl.output("result"))

      for (port <- _inputs.keySet) {
        for (item <- _inputs(port)) {
          runtime.input(port, new XdmNodeItemMessage(item, new XProcMetadata(MediaType.XML), context))
        }
      }

      runtime.output("result", result)

      for (bind <- _bindings.keySet) {
        runtime.option(bind, _bindings(bind), context)
      }

      runtime.run()

      val resultDoc = if (result.messages.nonEmpty) {
        result.messages.head.item.asInstanceOf[XdmNode]
      }  else {
        return new TestResult(false, "Result returned no messages")
      }
      //println(resultDoc)

      //System.err.println("RESULTDOC:")
      //System.err.println(resultDoc)

      if (_schematron.isDefined) {
        var fail = ""
        val schematest = new Schematron(runtimeConfig)
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
          System.err.println(resultDoc)
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
      case xproc: XProcException =>
        println(s"XProc exception: ${xproc.getMessage}")
        if (runtime != null) {
          runtime.stop()
        }

        val code = xproc.code
        val message = if (xproc.message.isDefined) {
          xproc.message.get
        } else {
          code match {
            case qname: QName =>
              runtimeConfig.errorExplanation.message(qname, xproc.variant, xproc.details)
            case _ =>
              s"Configuration error: code ($code) is not a QName"
          }
        }

        if (xproc.location.isDefined) {
          println(s"ERROR ${xproc.location.get} $code $message")
        } else {
          println(s"ERROR $code $message")
        }

        new TestResult(xproc)
      case xproc: ModelException =>
        println(s"XProc exception: model exception")
        if (runtime != null) {
          runtime.stop()
        }

        val code = xproc.code
        val message = "Model Exception"

        if (xproc.location.isDefined) {
          println(s"ERROR ${xproc.location.get} $code $message")
        } else {
          println(s"ERROR $code $message")
        }

        new TestResult(xproc)
      case ex: Exception =>
        println(s"Exception: ${ex.getMessage}")
        if (runtime != null) {
          runtime.stop()
        }
        val mappedex = XProcException.mapPipelineException(ex)
        new TestResult(mappedex)
    }
  }
}
