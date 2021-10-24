package com.xmlcalabash.drivers

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xxml.{XOption, XParser}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata}
import com.xmlcalabash.testing.TestRunner
import com.xmlcalabash.util.{DefaultErrorExplanation, MediaType, S9Api}
import net.sf.saxon.s9api.{Processor, QName, Serializer, XdmAtomicValue, XdmDestination, XdmValue}
import org.slf4j.LoggerFactory

import java.io.{BufferedReader, File, FileReader, PrintWriter}
import java.net.URI
import java.nio.file.{Files, Paths}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.javaapi.CollectionConverters.asJava

object ParserTest extends App {
  private val config: XMLCalabash = XMLCalabash.newInstance()
  private val explainer = new DefaultErrorExplanation()

  config.args.config(XProcConstants.cc_graph.getEQName, "pipeline")
  config.args.config(XProcConstants.cc_graph.getEQName, "graph")
  config.args.config(XProcConstants.cc_graph.getEQName, "graph-open")
  config.args.option("limit", 10)

  config.configure()

  val xparser = new XParser(config)
  try {
    //val decl = xparser.loadDeclareStep(new URI("file:///Users/ndw/Projects/xproc/gradle-plugin/examples/json/countdown.xpl"))
    val decl = xparser.loadDeclareStep(new URI("file:///Users/ndw/Projects/xproc/meerschaum/pipe.xpl"))
    if (decl.exceptions.isEmpty) {
      println("SUCCESS")
      println(decl.dump)

      val runtime = decl.runtime()
      //runtime.run()
      //println(decl.dump)
    } else {
      println(s"FAIL: ${decl.exceptions.length}")
      var count = 0
      for (ex <- decl.exceptions) {
        count += 1
        ex match {
          case xp: XProcException =>
            println(s"${count}: ${xp.location.getOrElse("")} ${explain(ex)}")
          case _ =>
            println(s"${count}: ${explain(ex)}")
        }
      }
    }
  } catch {
    case ex: Exception =>
      println(explain(ex))
      var count = 0
      for (ex <- xparser.exceptions) {
        count += 1
        println(s"${count}: ${explain(ex)}")
      }
  }

  private def explain(ex: Exception): String = {
    ex match {
      case xp: XProcException =>
        explainer.message(xp.code, xp.variant, xp.details)
      case _ =>
        ex.getMessage
    }
  }
}
