package com.xmlcalabash.util

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{TestException, XProcException}
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.runtime.XMLCalabashRuntime
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.{Axis, QName, XdmAtomicValue, XdmDestination, XdmNode, XdmNodeKind, XdmValue}
import org.xml.sax.InputSource

import java.net.URI
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.{Source, URIResolver}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.IteratorHasAsScala

class SchematronImpl(runtimeConfig: XMLCalabashConfig) {
  private val s_schema = new QName("http://purl.oclc.org/dsdl/schematron", "schema")
  private val _queryBinding = new QName("queryBinding")
  private val _untyped = StructuredQName.fromClarkName("{http://www.w3.org/2001/XMLSchema}untyped")
  private val fakeBaseURI = "https://sch.xmlcalabash.com"
  private val _params = mutable.HashMap.empty[QName, XdmValue]

  def params: Map[QName,XdmValue] = _params.toMap
  def params_=(map: Map[QName,XdmValue]): Unit = {
    _params.clear()
    _params.addAll(map)
  }

  def this(runtime: XMLCalabashRuntime) = {
    this(runtime.config)
  }

  def test(sourceXML: XdmNode, schemaXML: XdmNode): List[XdmNode] = {
    test(sourceXML, schemaXML, None)
  }

  def test(sourceXML: XdmNode, schemaXML: XdmNode, phase: Option[String]): List[XdmNode] = {
    failedAssertions(report(sourceXML, schemaXML, phase))
  }

  def report(sourceXML: XdmNode, schemaXML: XdmNode, phase: Option[String]): XdmNode = {
    var schema: XdmNode = schemaXML
    val schemaRoot = S9Api.documentElement(schema)
    if (schemaRoot.isDefined) {
      if (schemaRoot.get.getNodeName == s_schema && Option(schemaRoot.get.getAttributeValue(_queryBinding)).isEmpty) {
        // The schema doesn't specify a query binding. That'll cause SchXslt to fail. Patch it.
        val patch = new SaxonTreeBuilder(runtimeConfig)
        patch.startDocument(schemaRoot.get.getBaseURI)
        var amap = schemaRoot.get.getUnderlyingNode.attributes()
        amap = amap.put(TypeUtils.attributeInfo(_queryBinding, "xslt3"))
        patch.addStartElement(s_schema, amap, schemaRoot.get.getUnderlyingNode.getAllNamespaces)
        for (child <- schemaRoot.get.axisIterator(Axis.CHILD).asScala) {
          patch.addSubtree(child)
        }
        patch.addEndElement()
        patch.endDocument()
        schema = patch.result
      }
    }

    val schemaType = sourceXML.getUnderlyingNode.getSchemaType
    val schemaAware = (schemaType != null) && (schemaType.getStructuredQName != _untyped)

    var compiler = runtimeConfig.processor.newXsltCompiler()
    var uResolver = new UResolver(compiler.getURIResolver)
    var result: XdmDestination = new XdmDestination()

    compiler.setSchemaAware(schemaAware)
    compiler.setURIResolver(uResolver)

    val schpipeline = uResolver.resolve("/etc/schxslt-1.8.4/2.0/pipeline-for-svrl.xsl", fakeBaseURI)
    var exec = compiler.compile(schpipeline)
    val schemaCompiler = exec.load()

    if (phase.isDefined) {
      schemaCompiler.setParameter(new QName("", "phase"), new XdmAtomicValue(phase.get))
    }

    // FIXME: We pass parameters to both the compiler and the validator, is that right?
    for ((name, value) <- params) {
      schemaCompiler.setParameter(name, value)
    }

    schemaCompiler.setInitialContextNode(schema)
    schemaCompiler.setDestination(result)
    if (Option(schema.getBaseURI).isDefined) {
      schemaCompiler.setBaseOutputURI(schema.getBaseURI.toASCIIString)
    }

    schemaCompiler.transform()

    // SchXslt 1.8.4 has a bug where it makes the base URI of every node in the compiled schema
    // the same as the static base URI of the compiler stylesheet. This hack puts the right base URI
    // back. Note that it's not especially clever, it doesn't handle embedded xml:base attributes,
    // for example. But how likely are they in a Schematron schema? And I hope David fixes this bug...
    val compiledSchema = if (Option(schema.getBaseURI).isDefined && schema.getBaseURI.toASCIIString != "") {
      S9Api.patchBaseURI(runtimeConfig, result.getXdmNode, Some(schema.getBaseURI))
    } else {
      result.getXdmNode
    }

    val compiledRoot = S9Api.documentElement(compiledSchema)
    if (compiledRoot.isEmpty) {
      throw XProcException.xcNotASchematronDocument()
    }

    compiler = runtimeConfig.processor.newXsltCompiler()
    uResolver = new UResolver(compiler.getURIResolver)
    result = new XdmDestination()

    compiler.setSchemaAware(schemaAware)
    exec = compiler.compile(compiledSchema.asSource())
    val transformer = exec.load()

    for ((name, value) <- params) {
      transformer.setParameter(name, new XdmAtomicValue(value.toString))
    }

    transformer.setURIResolver(uResolver)
    transformer.setInitialContextNode(sourceXML)
    transformer.setDestination(result)
    if (Option(sourceXML.getBaseURI).isDefined) {
      transformer.setBaseOutputURI(sourceXML.getBaseURI.toASCIIString)
    }

    transformer.transform()

    result.getXdmNode
  }

  def failedAssertions(node: XdmNode): List[XdmNode] = {
    val nsBindings = mutable.HashMap.empty[String,String]
    nsBindings.put("svrl", "http://purl.oclc.org/dsdl/svrl")

    val xpath = "//svrl:failed-assert"        // The suppressed-rule problem is in SchXslt, I believe
                                              // https://github.com/schxslt/schxslt/issues/216
    val results = ListBuffer.empty[XdmNode]

    val xcomp = runtimeConfig.processor.newXPathCompiler()
    xcomp.setBaseURI(URI.create("http://example.com/"))
    for ((prefix,value) <- nsBindings) {
      xcomp.declareNamespace(prefix, value)
    }

    val xexec = xcomp.compile(xpath)
    val selector = xexec.load()

    selector.setContextItem(node)

    val values = selector.iterator()
    while (values.hasNext) {
      results += values.next.asInstanceOf[XdmNode]
    }

    results.toList
  }

  class UResolver(next: URIResolver) extends URIResolver {
    def resolve(href: String, base: String): Source = {
      if (base.startsWith(fakeBaseURI)) {
        val uri = new URI(base).resolve(href)
        val fn = uri.toASCIIString.substring(fakeBaseURI.length)
        val skeleton = getClass.getResourceAsStream(fn)
        if (skeleton == null) {
          throw new TestException(s"Failed to ${fn} from resources")
        }
        val source = new InputSource(skeleton)
        source.setSystemId(uri.toASCIIString)
        new SAXSource(source)
      } else {
        next.resolve(href, base)
      }
    }
  }
}
