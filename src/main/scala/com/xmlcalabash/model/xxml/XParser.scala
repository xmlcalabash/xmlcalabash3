package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xxml.XParser._builtinLibraries
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.XdmNode
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.InputSource

import java.net.URI
import javax.xml.transform.sax.SAXSource
import scala.collection.mutable.ListBuffer

object XParser {
  private var _builtinLibraries: ListBuffer[XLibrary] = _
}

class XParser(val config: XMLCalabash) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val _exceptions = ListBuffer.empty[Exception]

  // Load all of the built in steps
  if (Option(_builtinLibraries).isEmpty) {
    _builtinLibraries = ListBuffer.empty[XLibrary]

    config.standardLibraryParser = true
    val xpls = getClass.getClassLoader.getResources("com.xmlcalabash.library.xpl")
    while (xpls.hasMoreElements) {
      val xpl = xpls.nextElement()
      val xmlbuilder = config.processor.newDocumentBuilder()
      val stream = xpl.openStream()
      val source = new SAXSource(new InputSource(stream))
      xmlbuilder.setDTDValidation(false)
      xmlbuilder.setLineNumbering(true)
      val libnode = xmlbuilder.build(source)
      val library = loadLibrary(libnode)

      _exceptions ++= library.errors

      _builtinLibraries += library
    }

    config.standardLibraryParser = false
  }

  logger.debug("Built in libraries loaded")

  protected[xxml] def builtinLibraries: List[XLibrary] = _builtinLibraries.toList
  def exceptions: List[Exception] = _exceptions.toList

  def exceptions(artifact: XArtifact): List[Exception] = {
    if (Option(artifact).isDefined) {
      _exceptions.toList ++ artifact.exceptions
    } else {
      _exceptions.toList
    }
  }

  def loadLibrary(uri: URI): XLibrary = {
    val request = new DocumentRequest(uri, MediaType.XML)
    val response = config.documentManager.parse(request)
    if (response.contentType.xmlContentType) {
      loadLibrary(response.value.asInstanceOf[XdmNode])
    } else {
      throw XProcException.xsInvalidPipeline(s"Document is not XML: ${uri}", None)
    }
  }

  def loadLibrary(node: XdmNode): XLibrary = {
    val decl = load(node)

    val library = decl match {
      case lib: XLibrary =>
        lib
      case _ =>
        val lib = new XLibrary(config, Option(node.getBaseURI))
        lib.staticContext = new XArtifactContext(lib, node)
        lib.synthetic = true
        lib.syntheticName = XProcConstants.p_library
        lib.addChild(decl)
        lib
    }

    library.xelaborate()
    library
  }

  def loadDeclareStep(uri: URI): XDeclareStep = {
    val request = new DocumentRequest(uri, MediaType.XML)
    val response = config.documentManager.parse(request)
    if (response.contentType.xmlContentType) {
      loadDeclareStep(response.value.asInstanceOf[XdmNode])
    } else {
      throw XProcException.xsInvalidPipeline(s"Document is not XML: ${uri}", None)
    }
  }

  def loadDeclareStep(node: XdmNode): XDeclareStep = {
    val decl = load(node)

    decl match {
      case step: XDeclareStep =>
        step.xelaborate()
        step
      case _ =>
        throw XProcException.xiUserError(s"Pipeline document did not contain a p:declare-step.")
    }
  }

  private def load(node: XdmNode): XDeclContainer = {
    val hier = NodeHierarchy.newInstance(config, node)
    val loader = new Loader(this, hier)
    _exceptions ++= loader.exceptions

    if (!hier.useWhen(hier.root)) {
      _exceptions += XProcException.xsInvalidPipeline("Root element use-when is false; no document", None)
    }

    if (_exceptions.nonEmpty) {
      throw _exceptions.head
    }

    val decl = if (loader.declaredStep.isEmpty) {
      loader.library.get
    } else {
      loader.declaredStep.get
    }

    decl.builtinLibraries = _builtinLibraries.toList
    _exceptions ++= decl.errors

    if (_exceptions.nonEmpty) {
      throw _exceptions.head
    }

    decl
  }
}
