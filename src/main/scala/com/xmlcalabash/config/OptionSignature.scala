package com.xmlcalabash.config

import net.sf.saxon.s9api.{QName, SequenceType, XdmAtomicValue}

import scala.collection.mutable.ListBuffer

class OptionSignature(val name: QName) {
  private var _required = true
  private var _declaredType = Option.empty[SequenceType]
  private var _occurrence = Option.empty[String]
  private var _tokenList: Option[ListBuffer[XdmAtomicValue]] = None
  private var _defaultValue = Option.empty[String]
  private var _forceQNameKeys = false

  def this(name: QName, optType: SequenceType, required: Boolean) = {
    this(name)
    _declaredType = Some(optType)
    _required = required
  }

  def required: Boolean = _required
  def required_=(req: Boolean): Unit = {
    _required = req
  }

  def declaredType: Option[SequenceType] = _declaredType
  def declaredType_=(value: SequenceType): Unit = {
    _declaredType = Some(value)
  }

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
