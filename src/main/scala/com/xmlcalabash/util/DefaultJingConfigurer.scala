package com.xmlcalabash.util

import com.thaiopensource.util.PropertyMapBuilder
import com.xmlcalabash.config.JingConfigurer

class DefaultJingConfigurer extends JingConfigurer {
  override def configRNC(properties: PropertyMapBuilder): Unit = {
    // nop
  }

  override def configRNG(properties: PropertyMapBuilder): Unit = {
    // nop
  }
}
