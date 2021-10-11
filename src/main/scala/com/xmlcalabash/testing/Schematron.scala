package com.xmlcalabash.testing

import java.net.URI
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.TestException
import com.xmlcalabash.util.{S9Api, SchematronImpl}

import javax.xml.transform.sax.SAXSource
import javax.xml.transform.{Source, URIResolver}
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmDestination, XdmNode}
import org.xml.sax.InputSource

import scala.collection.immutable.{HashMap, HashSet}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

// In March, 2021, this step was updated to use the SchXslt implementation of Schematron

class Schematron(runtimeConfig: XMLCalabashConfig) {
  def test(sourceXML: XdmNode, schemaXML: XdmNode): List[XdmNode] = {
    test(sourceXML, schemaXML, None)
  }

  def test(sourceXML: XdmNode, schemaXML: XdmNode, phase: Option[String]): List[XdmNode] = {
    val impl = new SchematronImpl(runtimeConfig)
    impl.failedAssertions(impl.report(sourceXML, schemaXML, phase))
  }
}
