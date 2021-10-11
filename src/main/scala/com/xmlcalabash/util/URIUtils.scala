package com.xmlcalabash.util

import com.xmlcalabash.exceptions.XProcException

import java.io.File
import java.net.{URI, URLConnection}

object URIUtils {

  def homeAsURI: URI = {
    dirAsURI(System.getProperty("user.home"))
  }

  def cwdAsURI: URI = {
    dirAsURI(System.getProperty("user.dir"))
  }

  def resolve(baseURI: URI, relativeURI: URI): URI = {
    try {
      baseURI.resolve(relativeURI)
    } catch {
      case ex: IllegalArgumentException =>
        throw XProcException.xiMalformedURI(relativeURI.toString, ex.getMessage, None)
      case ex: Throwable =>
        throw ex
    }
  }

  def resolve(baseURI: URI, relativeURI: String): URI = {
    try {
      baseURI.resolve(relativeURI)
    } catch {
      case ex: IllegalArgumentException =>
        throw XProcException.xiMalformedURI(relativeURI, ex.getMessage, None)
      case ex: Throwable =>
        throw ex
    }
  }

  def dirAsURI(dir: String): URI = {
    var path = encode(dir)
    if (!path.endsWith("/")) {
      path += "/"
    }
    if (!path.startsWith("/")) {
      path = "/" + path
    }
    new URI("file:" + path)
  }

  def encode(uri: URI): String = {
    encode(uri.toASCIIString)
  }

  def encode(src: String): String = {
    val genDelims = ":/?#[]@"
    val subDelims = "!$&'()*+,;="
    val unreserved = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~"
    val okChars = genDelims + subDelims + unreserved + "%"

    // don't double-escape %-escaped chars!
    // FIXME: This should be more general, but Windows seems to be the only problem
    // and I'm oto lazy to look up how to dyanmically escape "\"

    val filesep = System.getProperty("file.separator")
    val adjsrc = if ("\\" == filesep) {
      src.replaceAll("\\\\", "/")
    }  else {
      src
    }

    var encoded = ""
    val bytes = src.getBytes("UTF-8")
    for (ch <- bytes) {
      if (okChars.indexOf(ch) >= 0) {
        encoded += ch.toChar
      } else {
        encoded += "%%%02X".format(ch)
      }
    }

    encoded
  }

  def toFile(uri: URI): File = {
    if (!"file".equalsIgnoreCase(uri.getScheme)) {
      throw new IllegalStateException("Expecting a file URI: " + uri.toASCIIString)
    }
    if (uri.getAuthority != null && uri.getAuthority.nonEmpty) {
      new File("//" + uri.getAuthority + uri.getPath)
    }
    else {
      new File(uri.getPath)
    }
  }

  def guessContentType(href: URI): MediaType = {
    guessContentType(href.toASCIIString)
  }

  def guessContentType(href: String): MediaType = {
    // Using the filename sort of sucks, but it's what the OSes do at this point so...sigh
    // You can extend the set of known extensions by pointing the system property
    // `content.types.user.table` at your own mime types file. The default file to
    // start with is in $JAVA_HOME/lib/content-types.properties
    val contentTypeString = Option(URLConnection.guessContentTypeFromName(href)).getOrElse("application/octet-stream")
    MediaType.parse(contentTypeString).assertValid
  }
}
