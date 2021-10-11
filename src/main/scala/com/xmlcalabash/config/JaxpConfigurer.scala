package com.xmlcalabash.config

import javax.xml.validation.SchemaFactory

trait JaxpConfigurer {
  def configSchemaFactory(factory: SchemaFactory): Unit
}
