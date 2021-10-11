package com.xmlcalabash.drivers

import com.xmlcalabash.config.{DocumentRequest, XMLCalabashConfig}
import com.xmlcalabash.drivers.Main.config
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.testing.TestRunner
import com.xmlcalabash.util.{DefaultErrorExplanation, MediaType, S9Api}
import net.sf.saxon.s9api.{QName, Serializer, XdmAtomicValue, XdmDestination, XdmValue}
import org.slf4j.LoggerFactory

import java.io.{BufferedReader, File, FileReader, PrintWriter}
import java.net.URI
import java.nio.file.{Files, Paths}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.javaapi.CollectionConverters.asJava

object Test extends App {
  private var xmlCalabash: XMLCalabashConfig = _

  private val testlist = ListBuffer.empty[String]
  private var regex = Option.empty[String]
  private var debug = false
  private var showPassing = false
  private var showFailing = false
  private var showSkipping = false
  private var xmlOutput: Option[String] = None
  private var configFile: Option[String] = None
  private val testLocations = ListBuffer.empty[String]

  protected val online: Boolean = try {
    val docreq = new DocumentRequest(new URI("http://www.w3.org/"), MediaType.HTML)
    val doc = xmlCalabash.documentManager.parse(docreq)
    true
  } catch {
    case ex: Exception => false
  }

  crudeArgParse()

  val level = "info"
  val logcontext = LoggerFactory.getILoggerFactory
  val logger = logcontext.getLogger("root")
  logger match {
    case lgr: ch.qos.logback.classic.Logger =>
      lgr.setLevel(ch.qos.logback.classic.Level.toLevel(level))
    case _ =>
      ()
  }

  private val showAll = !showPassing && !showFailing && !showSkipping

  if (testLocations.isEmpty) {
    println("Usage: com.xmlcalabash.drivers.Test [-h htmloutput] [-j junitoutput] [-r regex | -l list] testlocation [testlocation+]")
  }

  if (configFile.isDefined) {
    System.setProperty("com.xmlcalabash.configFile", configFile.get)
  }

  try {
    xmlCalabash = XMLCalabashConfig.newInstance()

    val runner = new TestRunner(xmlCalabash, online, regex, testlist.toList, testLocations.toList)

    if (xmlOutput.isDefined) {
      val junit = runner.junit()
      println(s"Writing JUnit XML test report to ${xmlOutput.get}")
      val serializer = xmlCalabash.processor.newSerializer()
      serializer.setOutputFile(new File(xmlOutput.get))
      serializer.setOutputProperty(Serializer.Property.METHOD, "xml")
      S9Api.serialize(xmlCalabash, junit, serializer)

      // If we can find the previous results and the compare stylesheet, print the changes report
      val outputf = Paths.get(xmlOutput.get).toAbsolutePath
      val prevf = outputf.getParent.resolve("../test-suite/reports/xml-calabash.xml")
      val comp = outputf.getParent.resolve("tools/test-changes.xsl")
      if (Files.exists(prevf) && Files.exists(comp)) {
        val builder = xmlCalabash.processor.newDocumentBuilder()
        val xsl = builder.build(comp.toFile)
        val compiler = xmlCalabash.processor.newXsltCompiler()
        val transformer = compiler.compile(xsl.asSource()).load30()
        transformer.setGlobalContextItem(junit)
        val parameters = mutable.HashMap.empty[QName, XdmValue]
        parameters.put(new QName("", "previous"), new XdmAtomicValue(prevf.toString))
        transformer.setStylesheetParameters(asJava(parameters.toMap))
        val result = new XdmDestination()
        transformer.applyTemplates(junit, result)
        var lines = ListBuffer.empty[String]
        lines ++= result.getXdmNode.toString.split("\n")
        var startOfList = false
        while (!startOfList) {
          startOfList = lines.isEmpty || lines.head.startsWith("Failing tests:")
          if (!startOfList) {
            println(lines.head)
          }
          if (lines.nonEmpty) {
            lines = lines.drop(1)
          }
        }
        if (regex.isEmpty) {
          val faillist = new PrintWriter(new File("failing.txt"))
          while (lines.nonEmpty) {
            faillist.println(lines.head)
            lines = lines.drop(1)
          }
          faillist.close()
        }
      }
    } else {
      var total = 0
      var pass = 0
      var skip = 0
      var fail = 0
      for (result <- runner.run()) {
        total += 1
        if (result.skipped.isDefined) {
          if (showAll || showSkipping) {
            println(s"SKIP: ${result.baseURI}")
          }
          skip += 1
        } else if (result.passed) {
          if (showAll || showPassing) {
            println(s"PASS: ${result.baseURI}")
          }
          pass += 1
        } else if (result.failed) {
          if (showAll || showFailing) {
            println(s"FAIL: ${result.baseURI}")
          }
          fail += 1
        }
      }

      if (total == 1) {
        println(s"$total test: [passed: $pass, skip: $skip, fail: $fail]")
      } else {
        println(s"$total tests: [passed: $pass, skip: $skip, fail: $fail]")
      }
    }
  } catch {
    case xproc: XProcException =>
      if (debug) {
        xproc.printStackTrace()
      }

      val errExplain = if (Option(xmlCalabash).isDefined) {
        xmlCalabash.errorExplanation
      } else {
        new DefaultErrorExplanation()
      }

      val code = xproc.code
      val message = if (xproc.message.isDefined) {
        xproc.message.get
      } else {
        code match {
          case qname: QName =>
            errExplain.message(qname, xproc.variant, xproc.details)
          case _ =>
            s"Configuration error: code ($code) is not a QName"
        }
      }
      if (xproc.location.isDefined) {
        println(s"ERROR ${xproc.location.get} $code $message")
      } else {
        println(s"ERROR $code $message")
      }

    case ex: Exception =>
      println(ex.getMessage)
      if (debug) {
        ex.printStackTrace()
      }
  }

  private def crudeArgParse(): Unit = {
    // Read the command line arguments crudely

    val optc = "-c(.*)".r
    val optd = "-(d)".r
    val optj = "-[jx](.*)".r
    val optp = "-(p)".r
    val optf = "-(f)".r
    val opts = "-(s)".r
    val optr = "-r(.*)".r
    val optl = "-l(.*)".r
    val optx = "-(.*)".r

    var pos = 0
    while (pos < args.length) {
      val arg = args(pos)
      arg match {
        case optc(opt) =>
          configFile = Some(opt)
          if (opt == "") {
            pos += 1
            configFile = Some(args(pos))
          }
        case optd(opt) =>
          debug = true
        case optp(opt) =>
          showPassing = true
        case optf(opt) =>
          showFailing = true
        case opts(opt) =>
          showSkipping = true
        case optj(opt) =>
          xmlOutput = Some(opt)
          if (opt == "") {
            pos += 1
            xmlOutput = Some(args(pos))
          }
        case optr(opt) =>
          regex = Some(opt)
          if (opt == "") {
            pos += 1
            regex = Some(args(pos))
          }
          if (!regex.get.startsWith("^")) {
            regex = Some("^.*" + regex.get)
          }
          if (!regex.get.endsWith("$")) {
            regex = Some(regex.get + ".*$")
          }
        case optl(opt) =>
          var filelist = Some(opt)
          if (opt == "") {
            pos += 1
            filelist = Some(args(pos))
          }
          val tfile = new BufferedReader(new FileReader(new File(filelist.get)))
          var line = tfile.readLine()
          while (Option(line).isDefined) {
            testlist += line.trim()
            line = tfile.readLine()
          }
          tfile.close()
        case optx(opt) =>
          throw new RuntimeException(s"Unknown option: -$opt")
        case _ =>
          testLocations += arg
      }
      pos += 1
    }
  }
}
