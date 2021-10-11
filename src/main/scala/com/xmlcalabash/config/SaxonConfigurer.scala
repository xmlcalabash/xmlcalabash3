package com.xmlcalabash.config

import net.sf.saxon.Configuration

trait SaxonConfigurer {
  def configureSchematron(config: Configuration): Unit
  def configureXSD(config: Configuration): Unit
  def configureXQuery(config: Configuration): Unit
  def configureXSLT(config: Configuration): Unit
}
