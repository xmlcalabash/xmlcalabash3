package com.xmlcalabash.config

import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.s9api.QName
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class ConfigurationSettings() {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private var closed = false
  private val _config = mutable.HashMap.empty[QName,ConfigurationValue]
  private val _serialization = mutable.HashMap.empty[String,Map[QName,String]]
  private val _saxonProperties = mutable.HashMap.empty[String,Any]
  private val _functions = mutable.HashMap.empty[QName,String]
  private val _steps = mutable.HashMap.empty[QName,String]

  def close(): Unit = {
    closed = true
  }

  def settings: Set[QName] = _config.keySet.toSet

  def set(name: QName, value: ConfigurationValue): Unit = {
    checkClosed()
    _config.put(name, value)
  }

  def get(name: QName): Option[ConfigurationValue] = {
    _config.get(name)
  }

  def string(name: QName): Option[String] = {
    _config.get(name) map { _.toString }
  }

  def boolean(name: QName): Option[Boolean] = {
    _config.get(name) map { _.asBoolean }
  }

  def integer(name: QName): Option[Int] = {
    _config.get(name) map { _.asInt }
  }

  def double(name: QName): Option[Double] = {
    _config.get(name) map { _.asDouble }
  }

  def defaultSerializations: Map[String,Map[QName,String]] = {
    val map = mutable.HashMap.empty[String,Map[QName,String]]
    for (ctype <- _serialization.keySet) {
      map.put(ctype, _serialization(ctype))
    }
    map.toMap
  }

  def setDefaultSerialization(contentType: String, property: QName, value: String): Unit = {
    checkClosed()
    val map = mutable.HashMap.empty[QName,String]
    if (_serialization.contains(contentType)) {
      map ++= _serialization(contentType)
    }
    map.put(property, value)
    _serialization.put(contentType, map.toMap)
  }

  def getDefaultSerialization(contentType: String): Map[QName,String] = {
    if (_serialization.contains(contentType)) {
      _serialization(contentType)
    } else {
      Map()
    }
  }

  def saxonConfigProperties: Set[String] = _saxonProperties.keySet.toSet

  def setSaxonConfigProperty(property: String, value: Any): Unit = {
    checkClosed()
    _saxonProperties.put(property, value)
  }

  def getSaxonConfigProperty(property: String): Option[Any] = {
    _saxonProperties.get(property)
  }

  def functions: Set[QName] = _functions.keySet.toSet

  def setFunction(name: QName, value: String): Unit = {
    checkClosed()
    if (_functions.contains(name)) {
      logger.error(s"Functions cannot be redefined, ignoring ${name}=${value}")
    } else {
      _functions.put(name, value)
    }
  }

  def getFunction(name: QName): Option[String] = {
    _functions.get(name)
  }

  def steps: Set[QName] = _steps.keySet.toSet

  def setStep(name: QName, value: String): Unit = {
    checkClosed()
    if (_steps.contains(name)) {
      logger.error(s"Steps cannot be redefined, ignoring ${name}=${value}")
    } else {
      _steps.put(name, value)
    }
  }

  def getStep(name: QName): Option[String] = {
    _steps.get(name)
  }

  private def checkClosed(): Unit = {
    if (closed) {
      throw XProcException.xiConfigurationException("Cannot update XML Calabash configuration settings after initialization")
    }
  }
}
