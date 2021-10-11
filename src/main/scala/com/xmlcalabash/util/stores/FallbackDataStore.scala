package com.xmlcalabash.util.stores

import java.net.URI

class FallbackDataStore extends DataStore {
  override def writeEntry(href: String, baseURI: URI, media: String, handler: DataWriter): URI = {
    val uri = baseURI.resolve(href)
    val scheme = uri.getScheme
    throw new RuntimeException(s"Cannot write to $scheme URIs")
  }

  override def readEntry(href: String, baseURI: URI, accept: String, overrideContentType: Option[String], handler: DataReader): Unit = {
    val uri = baseURI.resolve(href)
    val scheme = uri.getScheme
    throw new RuntimeException(s"Cannot read from $scheme URIs")
  }

  override def infoEntry(href: String, baseURI: URI, accept: String, handler: DataInfo): Unit = {
    val uri = baseURI.resolve(href)
    val scheme = uri.getScheme
    throw new RuntimeException(s"Cannot get info for $scheme URIs")
  }

  override def listEachEntry(href: String, baseURI: URI, accept: String, handler: DataInfo): Unit = {
    val uri = baseURI.resolve(href)
    val scheme = uri.getScheme
    throw new RuntimeException(s"Cannot list entries for $scheme URIs")
  }

  override def createList(href: String, baseURI: URI): URI = {
    val uri = baseURI.resolve(href)
    val scheme = uri.getScheme
    throw new RuntimeException(s"Cannot create directory for $scheme URIs")
  }

  override def deleteEntry(href: String, baseURI: URI): Unit = {
    val uri = baseURI.resolve(href)
    val scheme = uri.getScheme
    throw new RuntimeException(s"Cannot delete $scheme URIs")
  }
}
