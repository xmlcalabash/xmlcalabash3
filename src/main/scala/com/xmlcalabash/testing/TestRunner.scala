package com.xmlcalabash.testing

import com.jafpl.messages.Message
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.TestException
import com.xmlcalabash.messages.XdmNodeItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser}
import com.xmlcalabash.model.xml.XMLContext
import com.xmlcalabash.runtime.{SaxonExpressionEvaluator, StaticContext, XProcLocation, XProcMetadata, XProcXPathExpression}
import com.xmlcalabash.util.{MediaType, S9Api, TypeUtils, URIUtils, Urify}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap}
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind, XdmValue}
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.InputSource

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Calendar
import javax.xml.transform.sax.SAXSource
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object TestRunner {
  private val lock = new Object()
}

class TestRunner(runtimeConfig: XMLCalabashConfig, online: Boolean, regex: Option[String], testlist: List[String], testloc: List[String]) {
  private val _testsuite = new QName("", "testsuite")
  private val _properties = new QName("", "properties")
  private val _property = new QName("", "property")
  private val _value = new QName("", "value")
  private val _testcase = new QName("", "testcase")
  private val _classname = new QName("", "classname")
  private val _time = new QName("", "time")
  private val _errors = new QName("", "errors")
  private val _timestamp = new QName("", "timestamp")
  private val _system_out = new QName("", "system-out")
  private val _system_err = new QName("", "system-err")
  private val _hostname = new QName("", "hostname")
  private val _tests = new QName("", "tests")
  private val _error = new QName("", "error")
  private val _failure = new QName("", "failure")
  private val _skipped = new QName("", "skipped")
  private val _failures = new QName("", "failures")
  private val _message = new QName("", "message")
  private val _type = new QName("", "type")

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val tsns = "http://xproc.org/ns/testsuite/3.0"
  private val t_test_suite = new QName(tsns, "test-suite")
  private val t_div = new QName(tsns, "div")
  private val t_property= new QName(tsns, "property")
  private val t_info= new QName(tsns, "info")
  private val t_title = new QName(tsns, "title")
  private val t_description = new QName(tsns, "description")
  private val t_test = new QName(tsns, "test")
  private val t_pipeline = new QName(tsns, "pipeline")
  private val t_schematron = new QName(tsns, "schematron")
  private val t_input = new QName(tsns, "input")
  private val t_option = new QName(tsns, "option")
  private val _src = new QName("", "src")
  private val _port = new QName("", "port")
  private val _name = new QName("", "name")
  private val _select = new QName("", "select")
  private val _expected = new QName("", "expected")
  private val _code = new QName("", "code")
  private val _when = new QName("", "when")
  private val _features = new QName("", "features")

  private val testFiles = ListBuffer.empty[String]
  private val fnregex = "^.*.xml".r

  private val verboseOutput = Option(System.getenv("VERBOSE_TEST_OUTPUT")).getOrElse("false") == "true"

  private val context = new StaticContext(runtimeConfig)
  private val processor = runtimeConfig.processor
  private val builder = processor.newDocumentBuilder()
  builder.setDTDValidation(false)
  builder.setLineNumbering(true)

  for (path <- testloc) {
    val dir = new File(path)
    if (dir.exists) {
      if (dir.isDirectory) {
        recurse(dir)
      } else {
        rematch(path)
      }
    } else {
      throw new RuntimeException(s"Test location does not exist: $path")
    }
  }

  if (testFiles.isEmpty) {
    throw new TestException(s"Test runner cannot find tests at: $testloc")
  }

  private val sortedList = testFiles.sorted
  testFiles.clear()
  testFiles ++= sortedList

  def rematch(path: String): Unit = {
    if (testlist.nonEmpty) {
      var found = false
      for (fn <- testlist) {
        found = found || path.endsWith(fn)
      }
      if (found) {
        testFiles += path
      }
      return
    }

    if (regex.isEmpty || path.matches(regex.get)) {
      testFiles += path
    }
  }

  def run(): ListBuffer[TestResult] = {
    val resultList = ListBuffer.empty[TestResult]

    var count = 0
    for (fn <- testFiles) {
      count += 1

      if (verboseOutput) {
        println(s"Running $count of ${testFiles.length}: $fn")
        val source = new SAXSource(new InputSource(fn))
        val node = builder.build(source)
        resultList ++= runTestDocument(node)
      } else {
        val stdout = new ByteArrayOutputStream()
        val psout = new PrintStream(stdout)

        val stderr = new ByteArrayOutputStream()
        val pserr = new PrintStream(stderr)

        Console.withOut(psout) {
          Console.withErr(pserr) {
            val source = new SAXSource(new InputSource(fn))
            val node = builder.build(source)
            resultList ++= runTestDocument(node)
          }
        }

        psout.close()
        pserr.close()
      }
    }

    resultList
  }

  def junit(): XdmNode = {
    val junit = new SaxonTreeBuilder(runtimeConfig)
    junit.startDocument(URIUtils.cwdAsURI)

    val suite_start_ms = Calendar.getInstance().getTimeInMillis

    var count = 0
    var failures = 0
    var skip = 0
    val total = testFiles.length
    for (fn <- testFiles) {
      count += 1
      val percdone = (count * 100.0) / total
      val percfail = ((total - failures) * 100.0) / total

      // 1/2141 (0.0%, 3 fail, 0.0% pass)
      logger.info(f"$count%d/$total%d, $percdone%04.1f%% ($failures%d fail, $percfail%5.2f%% pass): ${fnsuffix(fn)}%s")

      val stdout = new ByteArrayOutputStream()
      val psout = new PrintStream(stdout)

      val stderr = new ByteArrayOutputStream()
      val pserr = new PrintStream(stderr)

      val pos = fn.lastIndexOf("/")
      val name = fn.substring(pos+1)

      Console.withOut(psout) {
        Console.withErr(pserr) {
          val source = new SAXSource(new InputSource(fn))
          val node = builder.build(source)

          var error: Throwable = null

          val start_ms = Calendar.getInstance().getTimeInMillis

          val results = try {
            runTestDocument(node)
          } catch {
            case t: Throwable =>
              error = t
              val tempResults = ListBuffer.empty[TestResult]
              tempResults += new TestResult(false, "ERROR")
          }

          val end_ms = Calendar.getInstance().getTimeInMillis

          var amap: AttributeMap = EmptyAttributeMap.getInstance()
          amap = amap.put(TypeUtils.attributeInfo(_name, name))
          amap = amap.put(TypeUtils.attributeInfo(_classname, this.getClass.getName))
          amap = amap.put(TypeUtils.attributeInfo(_time, ((end_ms - start_ms) / 1000.0).toString))
          junit.addStartElement(_testcase, amap)

          for (result <- results) {
            if (result.passed) {
              if (result.skipped.isDefined) {
                skip += 1
                junit.addStartElement(_skipped)
                junit.addText(result.skipped.get)
                junit.addEndElement()
              }
            } else {
              failures += 1
              logger.info(s"**** FAIL **** $failures **** ")
              if (result.baseURI.isDefined) {
                logger.info(s"      ${result.baseURI.get}")
              }

              if (Option(error).isDefined) {
                logger.info(error.getMessage)

                amap = EmptyAttributeMap.getInstance()
                amap = amap.put(TypeUtils.attributeInfo(_message, error.getMessage))
                amap = amap.put(TypeUtils.attributeInfo(_type, error.getClass.getName))
                junit.addStartElement(_error, amap)

                val stderr2 = new ByteArrayOutputStream()
                val pstrace = new PrintStream(stderr2)

                /*
                Console.withErr(pstrace) {
                  error.printStackTrace()
                }
                 */

                junit.addText(stderr2.toString)
                junit.addEndElement()

              } else {
                junit.addStartElement(_failure)
                junit.addText(result.toString())
                junit.addEndElement()
              }
            }
          }
        }

        psout.close()
        pserr.close()
      }

      junit.addStartElement(_system_out)
      junit.addText(stdout.toString())
      junit.addEndElement()

      junit.addStartElement(_system_err)
      junit.addText(stderr.toString())
      junit.addEndElement()

      junit.addEndElement()
    }

    junit.endDocument()

    val suite_end_ms = Calendar.getInstance().getTimeInMillis

    logger.info(s"$failures of $count tests failed")

    val wrapper = new SaxonTreeBuilder(runtimeConfig)
    wrapper.startDocument(URIUtils.cwdAsURI)

    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(_name, "XProc Test Suite"))

    val today   = Calendar.getInstance().getTime
    val dformat = new SimpleDateFormat("YYYY-MM-dd")
    val tformat = new SimpleDateFormat("HH:mm:ss")
    val stamp   = dformat.format(today) + "T" + tformat.format(today)
    amap = amap.put(TypeUtils.attributeInfo(_timestamp, stamp))

    amap = amap.put(TypeUtils.attributeInfo(_time, ((suite_end_ms - suite_start_ms) / 1000.0).toString))

    amap = amap.put(TypeUtils.attributeInfo(_hostname, InetAddress.getLocalHost.getHostName))
    amap = amap.put(TypeUtils.attributeInfo(_tests, count.toString))
    amap = amap.put(TypeUtils.attributeInfo(_failures, "0"))
    amap = amap.put(TypeUtils.attributeInfo(_skipped, skip.toString))
    amap = amap.put(TypeUtils.attributeInfo(_errors, failures.toString))

    wrapper.addStartElement(_testsuite, amap)
    wrapper.addStartElement(_properties)
    wrapper.addText("\n")

    amap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(_name, "processor"))
    amap = amap.put(TypeUtils.attributeInfo(_value, runtimeConfig.productName))
    wrapper.addStartElement(_property, amap)
    wrapper.addEndElement()
    wrapper.addText("\n")

    amap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(_name, "version"))
    amap = amap.put(TypeUtils.attributeInfo(_value, s"${runtimeConfig.productVersion} (with JAFPL ${runtimeConfig.jafplVersion} for Saxon ${runtimeConfig.saxonVersion})"))
    wrapper.addStartElement(_property, amap)
    wrapper.addEndElement()
    wrapper.addText("\n")

    amap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(_name, "productVersion"))
    amap = amap.put(TypeUtils.attributeInfo(_value, runtimeConfig.productVersion))
    wrapper.addStartElement(_property, amap)
    wrapper.addEndElement()
    wrapper.addText("\n")

    amap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(_name, "jafplVersion"))
    amap = amap.put(TypeUtils.attributeInfo(_value, runtimeConfig.jafplVersion))
    wrapper.addStartElement(_property, amap)
    wrapper.addEndElement()
    wrapper.addText("\n")

    amap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(_name, "saxonVersion"))
    amap = amap.put(TypeUtils.attributeInfo(_value, runtimeConfig.saxonVersion))
    wrapper.addStartElement(_property, amap)
    wrapper.addEndElement()
    wrapper.addText("\n")

    amap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(_name, "vendor"))
    amap = amap.put(TypeUtils.attributeInfo(_value, runtimeConfig.vendor))
    wrapper.addStartElement(_property, amap)
    wrapper.addEndElement()
    wrapper.addText("\n")

    amap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(_name, "vendorURI"))
    amap = amap.put(TypeUtils.attributeInfo(_value, runtimeConfig.vendorURI))
    wrapper.addStartElement(_property, amap)
    wrapper.addEndElement()
    wrapper.addText("\n")

    amap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(_name, "xprocVersion"))
    amap = amap.put(TypeUtils.attributeInfo(_value, runtimeConfig.xprocVersion))
    wrapper.addStartElement(_property, amap)
    wrapper.addEndElement()
    wrapper.addText("\n")

    amap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(_name, "xpathVersion"))
    amap = amap.put(TypeUtils.attributeInfo(_value, runtimeConfig.xpathVersion))
    wrapper.addStartElement(_property, amap)
    wrapper.addEndElement()
    wrapper.addText("\n")

    amap = EmptyAttributeMap.getInstance()
    amap = amap.put(TypeUtils.attributeInfo(_name, "psviSupported"))
    amap = amap.put(TypeUtils.attributeInfo(_value, runtimeConfig.psviSupported.toString))
    wrapper.addStartElement(_property, amap)
    wrapper.addEndElement()
    wrapper.addText("\n")

    wrapper.addEndElement()
    wrapper.addText("\n")

    wrapper.addSubtree(junit.result)

    wrapper.addEndElement()
    wrapper.endDocument()
    wrapper.result
  }

  private def fnsuffix(fn: String): String = {
    var pwd = System.getProperty("user.dir").split("[\\/]")
    var path = fn.split("[\\/]")
    var common = false
    while (pwd.nonEmpty && path.nonEmpty && pwd.head == path.head) {
      pwd = pwd.drop(1)
      path = path.drop(1)
      common = true
    }
    val res = path.mkString(System.getProperty("file.separator"))
    if (common) {
      s".../$res"
    } else {
      s"/$res"
    }
  }

  private def runTestDocument(node: XdmNode): ListBuffer[TestResult] = {
    val resultList = ListBuffer.empty[TestResult]

    if (node.getNodeKind != XdmNodeKind.DOCUMENT) {
      throw new TestException("Unexpected node type in runTestSuite(): " + node.getNodeKind)
    }

    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next()
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          if (child.getNodeName == t_test_suite) {
            resultList ++= runTestSuite(child)
          } else if (child.getNodeName == t_test) {
            resultList += runTest(child)
          } else {
            throw new TestException(s"Unexpected element ${child.getNodeName} in ${child.getBaseURI}")
          }
        case XdmNodeKind.TEXT =>
          if (node.getStringValue.trim != "") {
            throw new TestException(s"Unexpected text ${node.getStringValue} in ${child.getBaseURI}")
          }
        case _ => ()
      }
    }

    resultList
  }

  private def runTestSuite(node: XdmNode): ListBuffer[TestResult] = {
    val resultList = ListBuffer.empty[TestResult]

    if ((node.getNodeKind != XdmNodeKind.ELEMENT) || (node.getNodeName != t_test_suite)) {
      throw new TestException("Unexpected node in runTestSuite(): " + node.getNodeKind)
    }

    val when = node.getAttributeValue(_when)
    if (when != null) {
      val evaluator = new SaxonExpressionEvaluator(runtimeConfig)
      val expr = new XProcXPathExpression(context, when)
      val run = evaluator.booleanValue(expr, List(), Map.empty[String,Message], None)
      if (!run) {
        val result = new TestResult(true, "Skipped test suite")
        result.skipped = s"When '$when' evaluated to false"
        resultList += result
        return resultList
      }
    }

    val src = node.getAttributeValue(_src)
    if (src != null) {
      val suite = loadResource(node)
      if (suite.isDefined) {
        return runTestSuite(S9Api.documentElement(suite.get).get)
      } else {
        throw new TestException(s"Failed to load test-suite: $src")
      }
    }

    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next()
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          if (child.getNodeName == t_div) {
            resultList ++= runTestDiv(child)
          } else if (child.getNodeName == t_test) {
            resultList += runTest(child)
          } else if (child.getNodeName == t_title) {
            ()
          } else {
            throw new TestException(s"Unexpected element ${child.getNodeName} in ${child.getBaseURI}")
          }
        case XdmNodeKind.TEXT =>
          if (node.getStringValue.trim != "") {
            throw new TestException(s"Unexpected text ${node.getStringValue} in ${child.getBaseURI}")
          }
        case _ => ()
      }
    }

    resultList
  }

  private def runTestDiv(node: XdmNode): ListBuffer[TestResult] = {
    val resultList = ListBuffer.empty[TestResult]

    if ((node.getNodeKind != XdmNodeKind.ELEMENT) || (node.getNodeName != t_div)) {
      throw new TestException("Unexpected node in runTestSuite(): " + node.getNodeKind)
    }

    val when = node.getAttributeValue(_when)
    if (when != null) {
      val evaluator = new SaxonExpressionEvaluator(runtimeConfig)
      val expr = new XProcXPathExpression(context, when)
      val run = evaluator.booleanValue(expr, List(), Map.empty[String,Message], None)
      if (!run) {
        logger.info("Skipping test-div")
        val result = new TestResult(true, "Skipped test-div")
        result.skipped = s"When '$when' evaluated to false"
        resultList += result
        return resultList
      }
    }

    val src = node.getAttributeValue(_src)
    if (src != null) {
      val divdoc = loadResource(node)
      if (divdoc.isDefined) {
        return runTestDiv(S9Api.documentElement(divdoc.get).get)
      } else {
        throw new TestException(s"Failed to load test div: $src")
      }
    }

    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next()
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          if (child.getNodeName == t_test) {
            resultList += runTest(child)
          } else if (child.getNodeName == t_title) {
            ()
          } else {
            throw new TestException(s"Unexpected element ${child.getNodeName} in ${child.getBaseURI}")
          }
        case XdmNodeKind.TEXT =>
          if (node.getStringValue.trim != "") {
            throw new TestException(s"Unexpected text ${node.getStringValue} in ${child.getBaseURI}")
          }
        case _ => ()
      }
    }

    resultList
  }

  private def runTest(node: XdmNode): TestResult = {
    if ((node.getNodeKind != XdmNodeKind.ELEMENT) || (node.getNodeName != t_test)) {
      throw new TestException("Unexpected node in runTestSuite(): " + node.getNodeKind)
    }

    val when = node.getAttributeValue(_when)
    if (when != null) {
      val evaluator = new SaxonExpressionEvaluator(runtimeConfig)
      val expr = new XProcXPathExpression(context, when)
      val run = evaluator.booleanValue(expr, List(), Map.empty[String,Message], None)
      if (!run) {
        val result = new TestResult(true) // skipped counts as a pass...
        result.baseURI = node.getBaseURI
        result.skipped = s"When '$when' evaluated to false"
        return result
      }
    }

    var urifyFeature = Option.empty[String]
    val features = node.getAttributeValue(_features)
    if (features != null) {
      if (features.contains("lazy-eval")) {
        val result = new TestResult(true) // skipped counts as a pass...
        result.baseURI = node.getBaseURI
        result.skipped = "The 'lazy-eval' feature is not supported"
        return result
      }
      if (features.contains("xslt-1")) {
        val result = new TestResult(true) // skipped counts as a pass...
        result.baseURI = node.getBaseURI
        result.skipped = "XSLT 1.0 is not supported"
        return result
      }
      if (features.contains("xquery_1_0")) {
        val result = new TestResult(true) // skipped counts as a pass...
        result.baseURI = node.getBaseURI
        result.skipped = "XQuery 1.0 is not supported"
        return result
      }
      if (!online && features.contains("webaccess")) {
        val result = new TestResult(true) // skipped counts as a pass...
        result.baseURI = node.getBaseURI
        result.skipped = "The 'webaccess' feature is not supported when the test runner is offline"
        return result
      }
      if (features.contains("p-validate-with-xsd")) {
        if (runtimeConfig.processor.getSchemaManager == null) {
          val result = new TestResult(true) // skipped counts as a pass...
          result.baseURI = node.getBaseURI
          result.skipped = "The 'p-validate-with-xsd' feature is not supported with this version of Saxon"
          return result
        }
      }
      if (features.contains("urify-windows")) {
        urifyFeature = Some("windows")
      }
      if (features.contains("urify-non-windows")) {
        if (urifyFeature.nonEmpty) {
          logger.warn("Attempting to set multiple urify features")
        }
        urifyFeature = Some("non-windows")
      }
    }

    val src = node.getAttributeValue(_src)
    if (src != null) {
      val testdoc = loadResource(node)
      if (testdoc.isDefined) {
        return runTest(S9Api.documentElement(testdoc.get).get)
      } else {
        throw new TestException(s"Failed to load test div: $src")
      }
    }

    val expected = node.getAttributeValue(_expected)
    if ((expected != "pass") && (expected != "fail")) {
      throw new TestException("Test expectation unspecified")
    }

    var pipeline = Option.empty[XdmNode]
    var schematron = Option.empty[XdmNode]
    val inputs = mutable.HashMap.empty[String, ListBuffer[XdmNode]]
    val bindings = mutable.HashMap.empty[QName, XdmValue]

    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next()
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          if (child.getNodeName == t_pipeline) {
            if (pipeline.isDefined) {
              throw new TestException("Pipeline is already defined")
            }
            pipeline = loadResource(child)
          } else if (child.getNodeName == t_schematron) {
            if (schematron.isDefined) {
              throw new TestException("Schematron is already defined")
            }
            schematron = loadResource(child)
          } else if (child.getNodeName == t_input) {
            val port = child.getAttributeValue(_port)
            if (port == null) {
              throw new TestException("Input has no port")
            }

            val list = inputs.getOrElse(port, ListBuffer.empty[XdmNode])
            val doc = loadResource(child)
            if (doc.isEmpty) {
              throw new TestException(s"Failed to load input for $port")
            }
            list += doc.get
            inputs.put(port, list)
          } else if (child.getNodeName == t_option) {
            val name = child.getAttributeValue(_name)
            if (name == null) {
              throw new TestException("Option has no name")
            }
            // FIXME: what about qnames?
            val qname = new QName("", name)
            if (bindings.contains(qname)) {
              throw new TestException(s"Binding $name is already defined")
            }
            val value = loadBinding(child)
            if (value.isEmpty) {
              throw new TestException(s"Failed to load binding for $name")
            }
            bindings.put(qname, value.get)
          } else if (child.getNodeName == t_property) {
            val name = child.getAttributeValue(_name)
            val value = child.getAttributeValue(_value)
            if (name == null) {
              throw new TestException("Property has no name")
            }
            if (value == null) {
              throw new TestException("Property has no value")
            }
            System.setProperty(name, value)
          } else if (child.getNodeName == t_info || child.getNodeName == t_title || child.getNodeName == t_description) {
            ()
          } else {
            throw new TestException(s"Unexpected element ${child.getNodeName} in ${child.getBaseURI}")
          }

        case XdmNodeKind.TEXT =>
          if (child.getStringValue.trim != "") {
            throw new TestException(s"Unexpected text ${child.getStringValue} in ${child.getBaseURI}")
          }
        case _ => ()
      }
    }

    if (pipeline.isEmpty) {
      throw new TestException("No pipeline for test")
    }

    val tester = new Tester(XMLCalabashConfig.newInstance(runtimeConfig))

    tester.pipeline = pipeline.get

    if (schematron.isEmpty) {
      if (expected == "pass") {
        logger.warn("No schematron for test result.")
      }
    } else {
      tester.schematron = schematron.get
    }

    for ((port,list) <- inputs) {
      for (doc <- list) {
        tester.addInput(port,doc)
      }
    }

    for ((name,bind) <- bindings) {
      tester.addBinding(name, bind)
    }

    val result = if (urifyFeature.isEmpty) {
      tester.run()
    } else {
      TestRunner.lock.synchronized {
        val os = Urify.osname
        val sep = Urify.filesep
        if (urifyFeature.get == "windows") {
          Urify.mockOS("Windows", "\\", None)
        } else {
          Urify.mockOS("MacOS", "/", None)
        }
        val mockresult = tester.run()
        Urify.mockOS(os, sep, None)
        mockresult
      }
    }

    result.baseURI = node.getBaseURI

    //System.err.println("RESULT")
    //System.err.println(result)

    if (result.passed) {
      if (expected != "pass") {
        result.passed = false // it was supposed to fail...
      }
      result
    } else {
      if (expected != "fail") {
        result.passed = false // It was supposed to succeed...
      } else {
        val code = node.getAttributeValue(_code)
        if (code == null) {
          throw new TestException("No code attribute for failing test?")
        } else {
          var passed = false
          for (ecode <- code.split("\\s+")) {
            try {
              val ns = mutable.HashMap.empty[String,String]
              ns.put("xqterr", "http://www.w3.org/2005/xqt-errors")
              ns.put("err", "http://www.w3.org/2005/xqt-errors")
              ns ++= S9Api.inScopeNamespaces(node)

              val scontext = new XMLContext(runtimeConfig)
              scontext.nsBindings = ns.toMap
              scontext.location = new XProcLocation(node)
              val qcode = ValueParser.parseQName(ecode, scontext)
              if (result.errQName.isDefined) {
                passed = passed || qcode == result.errQName.get
              }
            } catch {
              case ex: Exception =>
                throw new RuntimeException(s"Test runner error: cannot parse QName: $ecode")
            }
          }
          result.passed = passed
        }
      }

      if (!result.passed && result.exception.isDefined) {
        result.exception.get.printStackTrace()
      }

      result
    }
  }

  private def loadResource(node: XdmNode): Option[XdmNode] = {
    val children = ListBuffer.empty[XdmNode]
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      children += iter.next
    }

    val src = node.getAttributeValue(_src)

    if (src == null) {
      inlineDocument(node)
    } else {
      if (children.nonEmpty) {
        throw new TestException(s"If you specify @src, the ${node.getNodeName} must be empty")
      }

      val docsrc = new SAXSource(new InputSource(node.getBaseURI.resolve(src).toASCIIString))
      val doc = builder.build(docsrc)
      Some(doc)
    }
  }

  private def loadBinding(node: XdmNode): Option[XdmValue] = {
    val children = ListBuffer.empty[XdmNode]
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      children += iter.next
    }

    val src = node.getAttributeValue(_src)
    if ((src == null) && children.isEmpty) {
      val scontext = new XMLContext(runtimeConfig, Some(node.getBaseURI), S9Api.inScopeNamespaces(node), Some(new XProcLocation(node)))
      val value = node.getAttributeValue(_select)
      val contextItem = inlineDocument(node)
      val message = new XdmNodeItemMessage(contextItem.get, new XProcMetadata(MediaType.XML), scontext)
      val eval = runtimeConfig.expressionEvaluator.newInstance()
      val result = eval.singletonValue(new XProcXPathExpression(scontext, value), List(message), Map.empty[String,Message], None)
      Some(result.item)
    } else {
      loadResource(node)
    }
  }

  private def inlineDocument(node: XdmNode): Option[XdmNode] = {
    val builder = new SaxonTreeBuilder(runtimeConfig)
    val iter = node.axisIterator(Axis.CHILD)

    builder.startDocument(node.getBaseURI)
    while (iter.hasNext) {
      builder.addSubtree(iter.next())
    }
    builder.endDocument()

    val rnode = builder.result
    Some(rnode)
  }

  private def recurse(dir: File): Unit = {
    for (file <- dir.listFiles()) {
      if (file.isDirectory) {
        recurse(file)
      } else {
        file.getName match {
          case fnregex() =>
            rematch(file.getAbsolutePath)
          case _ => ()
        }
      }
    }
  }
}
