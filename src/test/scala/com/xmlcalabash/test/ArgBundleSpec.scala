package com.xmlcalabash.test

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.util.{ArgBundle, PipelineEnvironmentOptionString, PipelineFilenameDocument, PipelineInputDocument, PipelineInputFilename, PipelineOption, PipelineOutputDocument, PipelineOutputFilename, PipelineStringOption, PipelineUntypedOption}
import net.sf.saxon.s9api.{ItemType, QName, XdmAtomicValue}
import org.scalatest.Assertions.fail
import org.scalatest.flatspec.AnyFlatSpec

object ArgBundleSpec {
  def assertPipeline(bundle: ArgBundle, fn: String): Unit = {
    assert(bundle.pipeline.isDefined)
    bundle.pipeline.get match {
      case str: PipelineFilenameDocument => assert(str.value == fn)
      case _ => fail()
    }
  }

  def assertInputLength(bundle: ArgBundle, port: String, count: Int): Unit = {
    val inputs = bundle.parameters collect { case p: PipelineInputDocument => p } filter { _.port == port }
    assert(inputs.length == count)
  }

  def assertInput(bundle: ArgBundle, port: String, pos: Int, fn: String): Unit = {
    val inputs = bundle.parameters collect { case p: PipelineInputDocument => p } filter { _.port == port }
    val files = inputs map { case p: PipelineInputFilename => p }
    assert(files.length >= pos && files(pos).value == fn)
  }

  def assertInputs(bundle: ArgBundle, port: String, fns: List[String]): Unit = {
    assertInputLength(bundle, port, fns.length)
    for (pos <- fns.indices) {
      assertInput(bundle, port, pos, fns(pos))
    }
  }

  def assertOutputLength(bundle: ArgBundle, port: String, count: Int): Unit = {
    val outputs = bundle.parameters collect { case p: PipelineOutputDocument => p } filter { _.port == port }
    assert(outputs.length == count)
  }

  def assertOutput(bundle: ArgBundle, port: String, pos: Int, fn: String): Unit = {
    val outputs = bundle.parameters collect { case p: PipelineOutputDocument => p } filter { _.port == port }
    val files = outputs collect { case p: PipelineOutputFilename => p }
    assert(files.length >= pos && files(pos).value == fn)
  }

  def assertOutputs(bundle: ArgBundle, port: String, fns: List[String]): Unit = {
    assertOutputLength(bundle, port, fns.length)
    for (pos <- fns.indices) {
      assertOutput(bundle, port, pos, fns(pos))
    }
  }

  def assertOptionValue(bundle: ArgBundle, eqname: String, value: String): Unit = {
    val opt = bundle.parameters collect { case p: PipelineOption => p } filter { _.eqname == eqname }
    assert(opt.length == 1)
    opt.head match {
      case s: PipelineUntypedOption =>
        assert(s.value == value)
      case s: PipelineStringOption =>
        assert(s.value == value)
      case _ =>
        fail()
    }
  }

  def assertVerbose(bundle: ArgBundle): Unit = {
    assert(bundle.parameters collect {
      case p: PipelineEnvironmentOptionString => p
    } filter {
      _.eqname == XProcConstants.cc_verbose.getEQName
    } exists {
      _.value == "true"
    })
  }
}

class ArgBundleSpec extends AnyFlatSpec {
  private val identity_xpl = "src/test/resources/identity.xpl"
  
  System.setProperty("com.xmlcalabash.configFile", "src/test/resources/config.xml")

  "Parsing pipeline" should "succeed" in {
    val bundle = new ArgBundle()
    val args = identity_xpl.split("\\s+")
    bundle.parse(args.toList)
    ArgBundleSpec.assertPipeline(bundle, identity_xpl)
  }

  // -i | --input

  "Parsing --input" should "fail" in {
    val bundle = new ArgBundle()
    val args = "--input".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing -i" should "fail" in {
    val bundle = new ArgBundle()
    val args = "-i".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing -isource=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle()
    val args = s"-isource=doc.xml ${identity_xpl}".split("\\s+")
    bundle.parse(args.toList)
    ArgBundleSpec.assertPipeline(bundle, identity_xpl)
    ArgBundleSpec.assertInputs(bundle, "source", List("doc.xml"))
  }

  "Parsing -isource=doc1.xml -isource=doc2.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle()
    val args = s"-isource=doc1.xml -isource=doc2.xml ${identity_xpl}".split("\\s+")
    bundle.parse(args.toList)
    ArgBundleSpec.assertPipeline(bundle, identity_xpl)
    ArgBundleSpec.assertInputs(bundle, "source", List("doc1.xml", "doc2.xml"))
  }

  "Parsing -visource=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle()
    val args = s"-visource=doc.xml ${identity_xpl}".split("\\s+")
    bundle.parse(args.toList)
    ArgBundleSpec.assertPipeline(bundle, identity_xpl)
    ArgBundleSpec.assertInputs(bundle, "source", List("doc.xml"))
    ArgBundleSpec.assertVerbose(bundle)
  }

  "Parsing --input source=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle()
    val args = s"--input source=doc.xml ${identity_xpl}".split("\\s+")
    bundle.parse(args.toList)
    ArgBundleSpec.assertPipeline(bundle, identity_xpl)
    ArgBundleSpec.assertInputs(bundle, "source", List("doc.xml"))
  }

  "Parsing -v --input source=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle()
    val args = s"-v --input source=doc.xml ${identity_xpl}".split("\\s+")
    bundle.parse(args.toList)
    ArgBundleSpec.assertPipeline(bundle, identity_xpl)
    ArgBundleSpec.assertInputs(bundle, "source", List("doc.xml"))
    ArgBundleSpec.assertVerbose(bundle)
  }

  "Parsing --input source=doc1.xml --input source=doc2.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle()
    val args = s"--input source=doc1.xml --input source=doc2.xml ${identity_xpl}".split("\\s+")
    bundle.parse(args.toList)
    ArgBundleSpec.assertPipeline(bundle, identity_xpl)
    ArgBundleSpec.assertInputs(bundle, "source", List("doc1.xml", "doc2.xml"))
  }

  "Parsing --input doc.xml pipeline" should "fail" in {
    val bundle = new ArgBundle()
    val args = s"--input doc.xml ${identity_xpl}".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  // -o | --output

  "Parsing --output" should "fail" in {
    val bundle = new ArgBundle()
    val args = "--output".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing -o" should "fail" in {
    val bundle = new ArgBundle()
    val args = "-o".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing -oresult=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle()
    val args = s"-oresult=doc.xml ${identity_xpl}".split("\\s+")
    bundle.parse(args.toList)
    ArgBundleSpec.assertPipeline(bundle, identity_xpl)
    ArgBundleSpec.assertOutputs(bundle, "result", List("doc.xml"))
  }

  "Parsing -oresult=doc1.xml -oresult=doc2.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle()
    val args = s"-oresult=doc1.xml -oresult=doc2.xml ${identity_xpl}".split("\\s+")
    bundle.parse(args.toList)
    ArgBundleSpec.assertPipeline(bundle, identity_xpl)
    ArgBundleSpec.assertOutputs(bundle, "result", List("doc1.xml", "doc2.xml"))
  }

  "Parsing -voresult=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle()
    val args = s"-voresult=doc.xml ${identity_xpl}".split("\\s+")
    bundle.parse(args.toList)
    ArgBundleSpec.assertPipeline(bundle, identity_xpl)
    ArgBundleSpec.assertOutputs(bundle, "result", List("doc.xml"))
  }

  "Parsing --output result=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle()
    val args = s"--output result=doc.xml ${identity_xpl}".split("\\s+")
    bundle.parse(args.toList)
    ArgBundleSpec.assertPipeline(bundle, identity_xpl)
    ArgBundleSpec.assertOutputs(bundle, "result", List("doc.xml"))
  }

  "Parsing -v --output result=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle()
    val args = s"-v --output result=doc.xml ${identity_xpl}".split("\\s+")
    bundle.parse(args.toList)
    ArgBundleSpec.assertPipeline(bundle, identity_xpl)
    ArgBundleSpec.assertOutputs(bundle, "result", List("doc.xml"))
    ArgBundleSpec.assertVerbose(bundle)
  }

  "Parsing --output result=doc1.xml --output result=doc2.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle()
    val args = s"--output result=doc1.xml --output result=doc2.xml ${identity_xpl}".split("\\s+")
    bundle.parse(args.toList)
    ArgBundleSpec.assertPipeline(bundle, identity_xpl)
    ArgBundleSpec.assertOutputs(bundle, "result", List("doc1.xml", "doc2.xml"))
  }

  "Parsing --output doc.xml pipeline" should "fail" in {
    val bundle = new ArgBundle()
    val args = s"--output doc.xml ${identity_xpl}".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  // --norun

  "Parsing --norun" should "succeed" in {
    val bundle = new ArgBundle()
    val args = "--norun".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.parameters map {
      case p: PipelineEnvironmentOptionString => p
    } filter {
      _.eqname == XProcConstants.cc_run.getEQName
    } exists {
      _.value == "false"
    })
  }

  // -G | --graph

  "Parsing --graph" should "fail" in {
    val bundle = new ArgBundle()
    val args = "--graph".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing -G" should "fail" in {
    val bundle = new ArgBundle()
    val args = "-G".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  // param=value

  "Parsing foo=bar pipeline" should "succeed" in {
    val xmlcalabash = XMLCalabash.newInstance()
    xmlcalabash.args.parse("foo=bar src/test/resources/identity.xpl".split("\\s+").toList)
    xmlcalabash.resolve()
    assert(xmlcalabash.options(new QName("", "foo")).value == new XdmAtomicValue("bar"))
    xmlcalabash.pipeline match {
      case str: PipelineFilenameDocument => assert(str.value == identity_xpl)
      case _ => fail()
    }
  }

  "Parsing foo=2 pipeline" should "succeed" in {
    val xmlcalabash = XMLCalabash.newInstance()
    xmlcalabash.args.parse(s"foo=2 ${identity_xpl}".split("\\s+").toList)
    xmlcalabash.resolve()
    val two = new XdmAtomicValue("2", ItemType.UNTYPED_ATOMIC)
    assert(xmlcalabash.options(new QName("", "foo")).value == two)
    xmlcalabash.pipeline match {
      case str: PipelineFilenameDocument => assert(str.value == identity_xpl)
      case _ => fail()
    }
  }

  "Parsing -bex=foo ex:foo=bar pipeline" should "succeed" in {
    val xmlcalabash = XMLCalabash.newInstance()
    xmlcalabash.args.parse(s"-bex=foo ex:foo=bar ${identity_xpl}".split("\\s+").toList)
    xmlcalabash.resolve()
    assert(xmlcalabash.options(new QName("foo", "foo")).value == new XdmAtomicValue("bar"))
    xmlcalabash.pipeline match {
      case str: PipelineFilenameDocument => assert(str.value == identity_xpl)
      case _ => fail()
    }
  }

  "Parsing --bind ex=foo ex:foo=bar pipeline" should "succeed" in {
    val xmlcalabash = XMLCalabash.newInstance()
    xmlcalabash.args.parse(s"--bind ex=foo ex:foo=bar ${identity_xpl}".split("\\s+").toList)
    xmlcalabash.resolve()
    assert(xmlcalabash.options(new QName("foo", "foo")).value == new XdmAtomicValue("bar"))
    xmlcalabash.pipeline match {
      case str: PipelineFilenameDocument => assert(str.value == identity_xpl)
      case _ => fail()
    }
  }

  "Parsing ex:foo=bar pipeline" should "fail" in {
    var pass = false
    try {
      val xmlcalabash = XMLCalabash.newInstance()
      xmlcalabash.args.parse(s"ex:foo=bar ${identity_xpl}".split("\\s+").toList)
      xmlcalabash.resolve()
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  // ?param=value

  "Parsing ?foo=3+4 pipeline" should "succeed" in {
    val xmlcalabash = XMLCalabash.newInstance()
    xmlcalabash.args.parse(s"?foo=3+4 ${identity_xpl}".split("\\s+").toList)
    xmlcalabash.resolve()
    assert(xmlcalabash.options(new QName("", "foo")).value == new XdmAtomicValue(7))
    xmlcalabash.pipeline match {
      case str: PipelineFilenameDocument => assert(str.value == identity_xpl)
      case _ => fail()
    }
  }

  "Parsing ?foo=3+4 ?bar=3+$foo pipeline" should "succeed" in {
    val xmlcalabash = XMLCalabash.newInstance()
    xmlcalabash.args.parse(s"?foo=3+4 ?bar=3+$$foo ${identity_xpl}".split("\\s+").toList)
    xmlcalabash.resolve()
    assert(xmlcalabash.options(new QName("", "foo")).value == new XdmAtomicValue(7))
    assert(xmlcalabash.options(new QName("", "bar")).value == new XdmAtomicValue(10))
    xmlcalabash.pipeline match {
      case str: PipelineFilenameDocument => assert(str.value == identity_xpl)
      case _ => fail()
    }
  }

}
