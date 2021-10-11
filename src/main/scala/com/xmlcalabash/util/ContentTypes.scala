package com.xmlcalabash.util

import java.io.{BufferedInputStream, File, FileInputStream, IOException}
import java.util.Properties

import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

object ContentTypes {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val userTablePath = System.getProperty("content.types.user.table")
  private val contentTypes: Properties = new Properties()
  private val cachedMapping = mutable.HashMap.empty[String, String]

  contentTypes.put("application/json", "file_extensions=.json")
  contentTypes.put("application/javascript", "file_extensions=.js")
  contentTypes.put("text/css", "file_extensions=.css")
  contentTypes.put("application/xml", "file_extensions=.xml")
  contentTypes.put("application/zip", "file_extensions=.zip")
  contentTypes.put("text/plain", "file_extensions=.txt")

  private val file = if (userTablePath != null && new File(userTablePath).exists) {
    new File(userTablePath)
  } else {
    val lib = new File(System.getProperty("java.home"), "lib")
    new File(lib, "content-types.properties")
  }

  try {
    val is = new BufferedInputStream(new FileInputStream(file))
    try {
      contentTypes.load(is)
    }
    finally {
      is.close()
    }
  } catch {
    case e: IOException =>
      logger.warn("Failed to load content types: " + file.getAbsolutePath)
      logger.debug(e.getMessage, e)
  }

  def extension(contentType: String): String = {
    var media = contentType
    var pos = media.indexOf(';')
    if (pos > 0) {
      media = media.substring(0, pos)
    }
    pos = media.indexOf(',')
    if (pos > 0) {
      media = media.substring(0, pos)
    }
    val attr = contentTypes.get(media.trim).asInstanceOf[String]
    if (attr != null && attr.indexOf("file_extensions") >= 0) {
      val start = attr.indexOf('=', attr.indexOf("file_extensions")) + 1
      var end = attr.indexOf(',', start)
      if (end < 0) end = attr.indexOf(';', start)
      if (end < 0) end = attr.length
      return attr.substring(start, end).trim
    } else {
      val plus = media.lastIndexOf('+')
      if (plus > 0) {
        val primary = media.substring(0, media.indexOf('/') + 1)
        val subtype = media.substring(plus + 1)
        return extension(primary + subtype)
      }
      else if (!media.startsWith("application/")) {
        val subtype = media.substring(media.indexOf('/') + 1)
        return extension("application/" + subtype)
      }
    }
    ""
  }

  def contentType(name: String): String = {
    val ext = getFileExtension(name)
    if (ext.isEmpty) {
      return "application/octet-stream"
    }

    if (cachedMapping.isEmpty) {
      val iter = contentTypes.propertyNames()
      while (iter.hasMoreElements) {
        val ctype = iter.nextElement().asInstanceOf[String]
        val attrs = contentTypes.getProperty(ctype)
        val tokens: Array[String] = attrs.split("\\s*;\\s*")
        for (tok <- tokens) {
          if (tok.startsWith("file_extensions=")) {
            val extList: String = tok.substring(16)
            val exts: Array[String] = extList.split("\\s*,\\s*")
            for (e <- exts) {
              cachedMapping.put(e, ctype)
            }
          }
        }
      }
    }

    cachedMapping.getOrElse(ext.get, "application/octet-stream")
  }

  private def getFileExtension(fname: String): Option[String] = {
    var end = fname.indexOf('#')
    if (end < 0) {
      end = fname.length
    }
    val start = fname.lastIndexOf('.', end)
    if (start >= 0 && fname.charAt(start) == '.') {
      Some(fname.substring(start, end).toLowerCase)
    } else {
      None
    }
  }
}
