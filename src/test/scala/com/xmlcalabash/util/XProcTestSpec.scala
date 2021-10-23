package com.xmlcalabash.util

/* I'm going to copy this file around from project to project for a bit, then I'm going to get
   frustrated at some point and make a separate project to hold it.
   /me takes an "I told you so" token, 18 Oct 2018.
 */

import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.testing.TestRunner
import net.sf.saxon.s9api.Processor
import org.scalatest.funspec.AnyFunSpec

import java.io.File
import java.net.{URI, URL}
import scala.collection.mutable.ListBuffer

class XProcTestSpec extends AnyFunSpec {
  System.setProperty("com.xmlcalabash.configFile", "src/test/resources/config.xml")

  protected val processor = new Processor(true)
  protected val testFiles: ListBuffer[String] = ListBuffer.empty[String]

  protected val online: Boolean = try {
    val url = new URL("https://xproc.org/")
    val conn = url.openConnection()
    conn.connect()
    true
  } catch {
    case _: Exception => false
  }

  private def verboseOutput: Boolean = {
    (Option(System.getenv("VERBOSE_TEST_OUTPUT")).getOrElse("false") == "true"
      || Option(System.getProperty("VERBOSE_TEST_OUTPUT")).getOrElse("false") == "true")
  }

  protected def runtests(title: String, testFiles: List[String]): Unit = {
    describe(title) {
      testFiles foreach {
        case filename =>
          val pos = filename.indexOf("/tests/")
          val name = if (pos >= 0) {
            filename.substring(pos+7)
          } else {
            filename
          }
          it (s"test: $name") {
            test(filename)
          }
      }
    }
  }

  protected def runtest(title: String, filename: String): Unit = {
    describe(title) {
      val pos = filename.indexOf("/tests/")
      val name = if (pos >= 0) {
        filename.substring(pos+7)
      } else {
        filename
      }
      it (s"test: $name") {
        test(filename)
      }
    }
  }

  protected def test(fn: String): Unit = {
    val runner = new TestRunner(processor, online, None, List.empty[String], List(fn))
    val results = runner.run()
    for (result <- results) {
      if (verboseOutput) {
        if (result.passed) {
          println(s"PASS: $fn")
        } else {
          if (result.errorWasPass) {
            println(s"FAIL: $fn (should have raised exception)")
          } else {
            println(s"FAIL: $fn")
          }
        }
      }
      assert(result.passed)
    }
  }

  protected def recurse(dir: File): Unit = {
    val fnregex = "^.*.xml".r

    if (dir.isDirectory) {
      for (file <- dir.listFiles()) {
        if (file.isDirectory) {
          recurse(file)
        } else {
          file.getName match {
            case fnregex() =>
              testFiles += file.getAbsolutePath
            case _ => ()
          }
        }
      }
    } else {
      testFiles += dir.getAbsolutePath
    }
  }

}
