package com.xmlcalabash.util

import com.xmlcalabash.config.SaxonConfigurer
import net.sf.saxon.Configuration

class DefaultSaxonConfigurer extends SaxonConfigurer {
  override def configureSchematron(config: Configuration): Unit = {
    // nop
  }

  override def configureXSD(config: Configuration): Unit = {
    // nop
  }

  override def configureXQuery(config: Configuration): Unit = {
    // nop
  }

  override def configureXSLT(config: Configuration): Unit = {
    // nop
  }
}
