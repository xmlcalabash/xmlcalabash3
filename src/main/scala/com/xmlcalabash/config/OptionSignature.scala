package com.xmlcalabash.config

import com.xmlcalabash.model.xxml.XOption
import net.sf.saxon.s9api.{QName, SequenceType, XdmAtomicValue}

import scala.collection.mutable.ListBuffer

class OptionSignature private(val name: QName) {
  private var _required = true
  private var _static = false
  private var _as = "xs:untypedAtomic"
  private var _declaredType = Option.empty[SequenceType]
  private var _occurrence = Option.empty[String]
  private var _tokenList: Option[ListBuffer[XdmAtomicValue]] = None
  private var _defaultValue = Option.empty[String]
  private var _forceQNameKeys = false

  def this(option: XOption) = {
    this(option.name)
    _as = option.as.getOrElse("xs:untypedAtomic")
    _declaredType = option.declaredType
    _required = option.required
    _static = option.static
  }

  def required: Boolean = _required
  def static: Boolean = _static

  def as: String = _as
  def declaredType: Option[SequenceType] = _declaredType

  def occurrence: Option[String] = _occurrence
  def occurrence_=(value: String): Unit = {
    _occurrence = Some(value)
  }

  def forceQNameKeys: Boolean = _forceQNameKeys
  protected[xmlcalabash] def forceQNameKeys_=(force: Boolean): Unit = {
    _forceQNameKeys = force
  }

  def tokenList: Option[List[XdmAtomicValue]] = {
    if (_tokenList.isDefined) {
      Some(_tokenList.get.toList)
    } else {
      None
    }
  }
  def tokenList_=(list: List[XdmAtomicValue]): Unit = {
    _tokenList = Some(ListBuffer.empty[XdmAtomicValue] ++ list)
  }

  def defaultSelect: Option[String] = _defaultValue
  def defaultSelect_=(value: String): Unit = {
    _defaultValue = Some(value)
  }
}
