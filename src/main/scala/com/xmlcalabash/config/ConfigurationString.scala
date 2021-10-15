package com.xmlcalabash.config

import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.s9api.QName

class ConfigurationString(name: QName, value: String) extends ConfigurationValue {
  override def asBoolean: Boolean = {
    if (value == "true" || value == "1" || value == "yes") {
      true
    } else {
      if (!List("false", "0", "no").contains(value)) {
        throw XProcException.xiConfigurationException(s"Configuration value ${name} cannot be convert to a boolean: ${value}")
      }
      false
    }
  }

  override def asMap: Map[String, String] = {
    throw XProcException.xiConfigurationException(s"Configuration value ${name} cannot be converted to a map: ${value}")
  }

  override def asInt: Integer = {
    try {
      value.toInt
    } catch {
      case _: Exception =>
        throw XProcException.xiConfigurationException(s"Configuration value ${name} cannot be converted to an integer: ${value}")
    }
  }

  override def asDouble: Double = {
    try {
      value.toDouble
    } catch {
      case _: Exception =>
        throw XProcException.xiConfigurationException(s"Configuration value ${name} cannot be converted to a double")
    }
  }

  override def asString: String = value

  override def toString: String = value
}
