package com.xmlcalabash.util

import com.xmlcalabash.config.{JaxpConfigurer, JingConfigurer, SaxonConfigurer, XMLCalabashConfigurer, XProcConfigurer}

class DefaultXProcConfigurer extends XProcConfigurer {
  val defJaxpConfigurer = new DefaultJaxpConfigurer()
  val defSaxonConfigurer = new DefaultSaxonConfigurer()
  val defJingConfigurer = new DefaultJingConfigurer()
  val defXMLCalabashConfigurer = new DefaultXMLCalabashConfigurer()

  override def xmlCalabashConfigurer: XMLCalabashConfigurer = defXMLCalabashConfigurer

  override def saxonConfigurer: SaxonConfigurer = defSaxonConfigurer

  override def jingConfigurer: JingConfigurer = defJingConfigurer

  override def jaxpConfigurer: JaxpConfigurer = defJaxpConfigurer
}
