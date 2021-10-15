package com.xmlcalabash.config

trait ConfigurationValue {
  def asBoolean: Boolean

  def asMap: Map[String, String]

  def asInt: Integer

  def asDouble: Double

  def asString: String
}
