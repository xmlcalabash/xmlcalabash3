package com.xmlcalabash.util.stores

import java.io.{InputStream, OutputStream}
import java.net.URI

import net.sf.saxon.s9api.XdmAtomicValue

// Ported from XML Calabash V1.0
// @author James Leigh &lt;james@3roundstones.com&gt;

trait DataWriter {
  def store(content: OutputStream): Unit
}

trait DataReader {
  def load(id: URI, media: String, content: InputStream, len: Option[Long]): Unit
}

trait DataInfo {
  def list(id: URI, props: Map[String, XdmAtomicValue]): Unit
}

trait DataStore {
  def writeEntry(href: String, base: URI, media: String, handler: DataWriter): URI
  def readEntry(href: String, base: URI, accept: String, overrideContentType: Option[String], handler: DataReader): Unit
  def infoEntry(href: String, base: URI, accept: String, handler: DataInfo): Unit
  def listEachEntry(href: String, base: URI, accept: String, handler: DataInfo): Unit
  def createList(href: String, base: URI): URI
  def deleteEntry(href: String, base: URI): Unit
}
