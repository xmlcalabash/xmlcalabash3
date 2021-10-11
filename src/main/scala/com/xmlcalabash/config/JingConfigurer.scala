package com.xmlcalabash.config

import com.thaiopensource.util.PropertyMapBuilder

trait JingConfigurer {
  def configRNC(properties: PropertyMapBuilder): Unit
  def configRNG(properties: PropertyMapBuilder): Unit
}
