package com.xmlcalabash.drivers

import com.jafpl.exceptions.{JafplException, JafplLoopDetected}
import com.jafpl.graph.{Binding, Node}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.{ModelException, ParseException, XProcException}
import net.sf.saxon.s9api.QName
import org.slf4j.LoggerFactory

import java.io.{BufferedReader, InputStream, InputStreamReader}

object Main extends App {
  val xmlcalabash = XMLCalabash.newInstance()
  var errored = false

  try {
    xmlcalabash.args.parse(args.toList)
    if (xmlcalabash.args.pipeline.isDefined) {
      runPipeline()
    } else {
      if (xmlcalabash.args.help) {
        longHelp()
      } else {
        shortHelp()
      }
    }
  } catch {
    case ex: Exception =>
      errored = true
      println(xmlcalabash.errorMessage)
      if (xmlcalabash.debugOptions.stacktrace) {
        ex.printStackTrace(System.err)
      }
  }

  if (errored) {
    System.exit(1)
  }

  def shortHelp(): Unit = {
    help(getClass.getResourceAsStream("/usage-short.txt"))
  }

  def longHelp(): Unit = {
    help(getClass.getResourceAsStream("/usage-long.txt"))
  }

  private def help(stream: InputStream): Unit = {
    if (stream == null) {
      throw XProcException.xiThisCantHappen("help text is missing.", None)
    }
    val reader = new BufferedReader(new InputStreamReader(stream))
    var line = reader.readLine()
    while (Option(line).isDefined) {
      println(line)
      line = reader.readLine()
    }
  }

  def runPipeline(): Unit = {
    xmlcalabash.configure()

    if (xmlcalabash.debugOptions.logLevel.isDefined) {
      // Try to tweak the log level. This will only work if the user hasn't
      // reconfigured logging. But if they've reconfigured logging, presumably
      // they don't need this!
      val level = xmlcalabash.debugOptions.logLevel.get
      val logcontext = LoggerFactory.getILoggerFactory
      val logger = logcontext.getLogger("root")
      logger match {
        case lgr: ch.qos.logback.classic.Logger =>
          lgr.setLevel(ch.qos.logback.classic.Level.toLevel(level))
        case _ =>
          logger.warn(s"Logging configuration doesn't support command line --${level} option")
      }
    }

    xmlcalabash.run()
  }
}
