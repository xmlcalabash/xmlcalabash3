package com.xmlcalabash.config

trait XMLCalabashConfigurer {
  def configure(settings: ConfigurationSettings): Unit
  def update(config: XMLCalabashConfig, settings: ConfigurationSettings): Unit
}
