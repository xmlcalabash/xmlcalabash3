package com.xmlcalabash.util

import com.jafpl.steps.DataConsumer
import com.xmlcalabash.model.xxml.XStaticContext
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
  val contentType: MediaType
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

class PipelineFilenameDocument(val value: String) extends PipelineDocument with PipelineParameterStringValue {
  override def toString = s"Filename: ${value}"
}
class PipelineURIDocument(val value: URI) extends PipelineDocument with PipelineParameterURIValue {
  override def toString = s"URI: ${value}"
}
class PipelineFileDocument(val value: File, val contentType: MediaType) extends PipelineDocument with PipelineParameterFileValue {
  override def toString = s"File ${contentType}: ${value.getAbsolutePath}"
}
class PipelineXdmDocument(val value: XdmNode) extends PipelineDocument with PipelineParameterXdmNodeValue {
  override def toString = s"XDM: ..."
}
class PipelineTextDocument(val text: String, val contentType: MediaType) extends PipelineDocument with PipelineParameterTextValue {
  def this(text: String, contentType: String) = this(text, MediaType.parse(contentType))
  override def toString = s"Text ${contentType}: ..."
}

abstract class PipelineInputDocument(val port: String) extends PipelineDocument { }

class PipelineInputFilename(port: String, val value: String) extends PipelineInputDocument(port) with PipelineParameterStringValue {
  override def toString = s"${port} <- ${value}"
}
class PipelineInputURI(port: String, val value: URI) extends PipelineInputDocument(port) with PipelineParameterURIValue {
  override def toString = s"${port} <- ${value}"
}
class PipelineInputFile(port: String, val value: File, val contentType: MediaType) extends PipelineInputDocument(port) with PipelineParameterFileValue {
  def this(port: String, value: File, contentType: String) = this(port, value, MediaType.parse(contentType))
  override def toString = s"${port} <- ${contentType} from ${value.getAbsolutePath}"
}
class PipelineInputXdm(port: String, val value: XdmNode) extends PipelineInputDocument(port) with PipelineParameterXdmNodeValue {
  override def toString = s"${port} <- XDM"
}
class PipelineInputText(port: String, val text: String, val contentType: MediaType) extends PipelineInputDocument(port) with PipelineParameterTextValue {
  def this(port: String, text: String, contentType: String) = this(port, text, MediaType.parse(contentType))
  override def toString = s"${port} <- ${contentType} from String"
}

abstract class PipelineOutputDocument(val port: String) extends PipelineDocument { }

class PipelineOutputFilename(port: String, val value: String) extends PipelineOutputDocument(port) with PipelineParameterStringValue {
  override def toString = s"${port} -> ${value}"
}
class PipelineOutputURI(port: String, val value: URI) extends PipelineOutputDocument(port) with PipelineParameterURIValue {
  override def toString = s"${port} -> ${value}"
}
class PipelineOutputConsumer(port: String, val value: DataConsumer) extends PipelineOutputDocument(port) with PipelineParameterConsumerValue {
  override def toString = s"${port} -> DataConsumer"
}

class PipelineOption(val eqname: String) extends PipelineParameter {}
class PipelineUntypedOption(eqname: String, val value: String) extends PipelineOption(eqname) {
  override def toString = s"${eqname} = ${value}"
}
class PipelineStringOption(eqname: String, val value: String) extends PipelineOption(eqname) {
  override def toString = s"${eqname} = ${value}"
}
class PipelineBooleanOption(eqname: String, val value: Boolean) extends PipelineOption(eqname) {
  override def toString = s"${eqname} = ${value}"
}
class PipelineIntegerOption(eqname: String, val value: Integer) extends PipelineOption(eqname) {
  override def toString = s"${eqname} = ${value}"
}
class PipelineDoubleOption(eqname: String, val value: Double) extends PipelineOption(eqname) {
  override def toString = s"${eqname} = ${value}"
}
class PipelineUriOption(eqname: String, val value: URI) extends PipelineOption(eqname) {
  override def toString = s"${eqname} = ${value}"
}
class PipelineXdmValueOption(eqname: String, val value: XdmValue) extends PipelineOption(eqname) {
  override def toString = s"${eqname} = XDM ..."
}
class PipelineDocumentOption(eqname: String, val value: PipelineDocument) extends PipelineOption(eqname) {
  override def toString = s"${eqname} = ${value}"
}
class PipelineExpressionOption(eqname: String, val expression: String) extends PipelineOption(eqname) {
  override def toString = s"${eqname} = XPath: ${expression}"
}

class PipelineOptionValue(val context: XStaticContext, val value: XdmValue) extends PipelineParameter {}

class PipelineSystemProperty(val name: String, val value: String) extends PipelineParameter {
  override def toString = s"${name} = ${value}"
}
class PipelineSaxonConfigurationProperty(val name: String, val key: String, val value: String, val as: String) extends PipelineParameter {}
class PipelineConfigurationFile(val doc: PipelineDocument) extends PipelineParameter {
  override def toString = s"${doc}"
}
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
  override def toString: String = s"config: ${eqname} = ${value}"
}

class PipelineEnvironmentOptionMap(eqname: String, var key: String, var value: String) extends PipelineEnvironmentOption(eqname) {
  def this(name: QName, key: String, value: String) = this(name.getEQName, key, value)
  override def toString = s"${eqname} = ${key}/${value}"
}

class PipelineStepImplementation(eqname: String, val displayName: String, val className: String) extends PipelineEnvironmentOption(eqname) {
  def this(name: QName, displayName: String, className: String) = this(name.getEQName, displayName, className)
  override def toString = s"${displayName} = ${className}"
}

class PipelineFunctionImplementation(eqname: String, val displayName: String, val className: String) extends PipelineEnvironmentOption(eqname) {
  def this(name: QName, displayName: String, className: String) = this(name.getEQName, displayName, className)
  override def toString = s"${displayName} = ${className}"
}
