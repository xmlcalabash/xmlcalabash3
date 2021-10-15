package com.xmlcalabash.config

import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.s9api.QName

class ConfigurationStringMap(name: QName, value: Map[String,String]) extends ConfigurationValue {
  override def asBoolean: Boolean = {
    throw XProcException.xiConfigurationException(s"Configuration value ${name} cannot be converted to a boolean")
  }

  override def asMap: Map[String, String] = value

  override def asInt: Integer = {
    throw XProcException.xiConfigurationException(s"Configuration value ${name} cannot be converted to an integer")
  }

  override def asDouble: Double = {
    throw XProcException.xiConfigurationException(s"Configuration value ${name} cannot be converted to a double")
  }

  override def asString: String = {
    throw XProcException.xiConfigurationException(s"Configuration value ${name} cannot be converted to a string")
  }

  override def toString: String = {
    s"${name}: configuration map"
  }
}
