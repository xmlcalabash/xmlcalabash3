package com.xmlcalabash.config

trait XProcConfigurer {
  def xmlCalabashConfigurer: XMLCalabashConfigurer
  def saxonConfigurer: SaxonConfigurer
  def jingConfigurer: JingConfigurer
  def jaxpConfigurer: JaxpConfigurer
}
