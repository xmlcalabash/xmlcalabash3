package com.xmlcalabash.util

import com.jafpl.steps.DataConsumer
import com.xmlcalabash.runtime.StaticContext
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}
import org.slf4j.LoggerFactory

import java.io.File
import java.net.URI

abstract class PipelineParameter {}

trait PipelineParameterStringValue {
  val value: String
}

trait PipelineParameterURIValue {
  val value: URI
}

trait PipelineParameterFileValue {
  val value: File
}

trait PipelineParameterXdmNodeValue {
  val value: XdmNode
}

trait PipelineParameterConsumerValue {
  val value: DataConsumer
}

trait PipelineParameterTextValue {
  val text: String
  val contentType: MediaType
}

class PipelineNamespace(val prefix: String, val namespace: String) extends PipelineParameter {}

abstract class PipelineDocument() extends PipelineParameter { }

class PipelineFilenameDocument(val value: String) extends PipelineDocument with PipelineParameterStringValue {}
class PipelineURIDocument(val value: URI) extends PipelineDocument with PipelineParameterURIValue {}
class PipelineFileDocument(val value: File) extends PipelineDocument with PipelineParameterFileValue {}
class PipelineXdmDocument(val value: XdmNode) extends PipelineDocument with PipelineParameterXdmNodeValue {}
class PipelineTextDocument(val text: String, val contentType: MediaType) extends PipelineDocument with PipelineParameterTextValue {
  def this(text: String, contentType: String) = this(text, MediaType.parse(contentType))
}

abstract class PipelineInputDocument(val port: String) extends PipelineDocument { }

class PipelineInputFilename(port: String, val value: String) extends PipelineInputDocument(port) with PipelineParameterStringValue {}
class PipelineInputURI(port: String, val value: URI) extends PipelineInputDocument(port) with PipelineParameterURIValue {}
class PipelineInputFile(port: String, val value: File) extends PipelineInputDocument(port) with PipelineParameterFileValue {}
class PipelineInputXdm(port: String, val value: XdmNode) extends PipelineInputDocument(port) with PipelineParameterXdmNodeValue {}
class PipelineInputText(port: String, val text: String, val contentType: MediaType) extends PipelineInputDocument(port) with PipelineParameterTextValue {
  def this(port: String, text: String, contentType: String) = this(port, text, MediaType.parse(contentType))
}

abstract class PipelineOutputDocument(val port: String) extends PipelineDocument { }

class PipelineOutputFilename(port: String, val value: String) extends PipelineOutputDocument(port) with PipelineParameterStringValue {}
class PipelineOutputURI(port: String, val value: URI) extends PipelineOutputDocument(port) with PipelineParameterURIValue {}
class PipelineOutputConsumer(port: String, val value: DataConsumer) extends PipelineOutputDocument(port) with PipelineParameterConsumerValue {}

class PipelineOption(val eqname: String) extends PipelineParameter {}
class PipelineUntypedOption(eqname: String, val value: String) extends PipelineOption(eqname) { }
class PipelineStringOption(eqname: String, val value: String) extends PipelineOption(eqname) { }
class PipelineBooleanOption(eqname: String, val value: Boolean) extends PipelineOption(eqname) { }
class PipelineIntegerOption(eqname: String, val value: Integer) extends PipelineOption(eqname) { }
class PipelineDoubleOption(eqname: String, val value: Double) extends PipelineOption(eqname) { }
class PipelineUriOption(eqname: String, val value: URI) extends PipelineOption(eqname) { }
class PipelineXdmValueOption(eqname: String, val value: XdmValue) extends PipelineOption(eqname)
class PipelineDocumentOption(eqname: String, val value: PipelineDocument) extends PipelineOption(eqname) { }
class PipelineExpressionOption(eqname: String, val exression: String) extends PipelineOption(eqname) { }

class PipelineOptionValue(val context: StaticContext, val value: XdmValue) extends PipelineParameter {}

class PipelineSystemProperty(val name: String, val value: String) extends PipelineParameter {}
class PipelineSaxonConfigurationProperty(val name: String, val key: String, val value: String, val as: String) extends PipelineParameter {}
class PipelineConfigurationFile(val doc: PipelineDocument) extends PipelineParameter {}
class PipelineInjectable(val value: String) extends PipelineParameter {}

class PipelineEnvironmentOptionSerialization(contentType: String, val value: Map[String,String]) extends PipelineEnvironmentOption(contentType) {}

class PipelineEnvironmentOption(val eqname: String) extends PipelineParameter {
  def this(name: QName) = this(name.getEQName)
  def getBoolean: Option[Boolean] = {
    LoggerFactory.getLogger(this.getClass).error(s"Configuration ${eqname} is not a valid boolean")
    None
  }
  def getString: Option[String] = {
    LoggerFactory.getLogger(this.getClass).error(s"Configuration ${eqname} is not a valid string")
    None
  }
}

class PipelineEnvironmentOptionString(eqname: String, val value: String) extends PipelineEnvironmentOption(eqname) {
  def this(name: QName, value: String) = this(name.getEQName, value)
  override def getBoolean: Option[Boolean] = {
    value match {
      case "true" => Some(true)
      case "false" => Some(false)
      case _ =>
        LoggerFactory.getLogger(this.getClass).error(s"Configuration ${eqname} is not a valid boolean: ${value}")
        None
    }
  }
  override def getString: Option[String] = Some(value)
}

class PipelineEnvironmentOptionMap(eqname: String, var key: String, var value: String) extends PipelineEnvironmentOption(eqname) {
  def this(name: QName, key: String, value: String) = this(name.getEQName, key, value)
}

class PipelineStepImplementation(eqname: String, val displayName: String, val className: String) extends PipelineEnvironmentOption(eqname) {
  def this(name: QName, displayName: String, className: String) = this(name.getEQName, displayName, className)
}

class PipelineFunctionImplementation(eqname: String, val displayName: String, val className: String) extends PipelineEnvironmentOption(eqname) {
  def this(name: QName, displayName: String, className: String) = this(name.getEQName, displayName, className)
}
