package com.xmlcalabash.util

import com.xmlcalabash.config.JaxpConfigurer
import javax.xml.validation.SchemaFactory

class DefaultJaxpConfigurer extends JaxpConfigurer {
  override def configSchemaFactory(factory: SchemaFactory): Unit = {
    // nop
  }
}
