package com.xmlcalabash.config

import com.xmlcalabash.model.xxml.XDataSource
import com.xmlcalabash.util.MediaType

import scala.collection.mutable.ListBuffer

class PortSignature(val port: String) {
  private var _cardinality = "1"
  private var _primary = Option.empty[Boolean]
  private val _contentTypes = ListBuffer.empty[MediaType]
  private val _defaultBindings = ListBuffer.empty[XDataSource]

  def this(port: String, primary: Boolean, sequence: Boolean) = {
    this(port)
    _primary = Some(primary)
    if (sequence) {
      _cardinality = "*"
    }
  }

  def this(port: String, primary: Boolean, sequence: Boolean, bindings: List[XDataSource]) = {
    this(port, primary, sequence)
    _defaultBindings ++= bindings
  }

  def sequence: Boolean = (_cardinality == "*")
  def cardinality: String = _cardinality
  def cardinality_=(card: String): Unit = {
    _cardinality = card
  }

  def primary: Boolean = _primary.getOrElse(false)
  def primary_=(primary: Boolean): Unit = {
    _primary = Some(primary)
  }
  def primaryDeclared: Boolean = _primary.isDefined

  def contentTypes: List[MediaType] = _contentTypes.toList
  def contentTypes_=(types: List[MediaType]): Unit = {
    _contentTypes.clear()
    _contentTypes ++= types
  }

  def defaultBindings: List[XDataSource] = _defaultBindings.toList
}
