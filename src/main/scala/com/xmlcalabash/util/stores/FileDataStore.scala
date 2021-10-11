package com.xmlcalabash.util.stores

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.util.{ContentTypes, URIUtils}
import net.sf.saxon.s9api.XdmAtomicValue

import java.io.{File, FileFilter, FileInputStream, FileNotFoundException, FileOutputStream, IOException}
import java.net.URI
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import scala.collection.mutable

class FileDataStore(config: XMLCalabashConfig, fallback: DataStore) extends DataStore {
  override def writeEntry(href: String, baseURI: URI, media: String, handler: DataWriter): URI = {
    val uri = baseURI.resolve(href)
    if ("file".equalsIgnoreCase(uri.getScheme)) {
      val file = URIUtils.toFile(uri)

      if (config.safeMode) {
        throw new RuntimeException("Can't write in safe mode")
      }

      val suffix = ContentTypes.extension(media)
      if (file.isDirectory || uri.getPath.endsWith("/")) {
        if (!file.isDirectory && !file.mkdirs) throw new FileNotFoundException(file.getAbsolutePath)
        val temp = File.createTempFile("calabash", suffix, file)
        val out = new FileOutputStream(temp)
        try {
          handler.store(out)
        } finally {
          out.close()
        }
        temp.toURI
      } else {
        val dir = file.getParentFile
        if (!dir.isDirectory && !dir.mkdirs) {
          throw new FileNotFoundException(dir.getAbsolutePath)
        }
        val temp = File.createTempFile("calabash-temp", suffix, dir)
        try {
          val out = new FileOutputStream(temp)
          try {
            handler.store(out)
          } finally {
            out.close()
          }
          file.delete
          temp.renameTo(file)

          file.toURI
        } finally {
          if (temp.exists) {
            temp.delete
          }
        }
      }
    }
    else {
      fallback.writeEntry(href, baseURI, media, handler)
    }
  }

  override def readEntry(href: String, baseURI: URI, accept: String, overrideContentType: Option[String], handler: DataReader): Unit = {
    val uri = baseURI.resolve(href)
    if ("file".equalsIgnoreCase(uri.getScheme)) {
      val file = URIUtils.toFile(uri)
      val ctype = overrideContentType.getOrElse(ContentTypes.contentType(file.getName))
      val in = new FileInputStream(file)
      try {
        handler.load(file.toURI, ctype, in, Some(file.length))
      } finally {
        in.close()
      }
    } else {
      fallback.readEntry(href, baseURI, accept, overrideContentType, handler)
    }
  }

  override def infoEntry(href: String, baseURI: URI, accept: String, handler: DataInfo): Unit = {
    val uri = baseURI.resolve(href)
    if ("file".equalsIgnoreCase(uri.getScheme)) {
      val file = URIUtils.toFile(uri)
      if (!file.exists()) {
        throw new FileNotFoundException(file.getAbsolutePath)
      }
      handler.list(file.toURI, fileProperties(file))
    } else {
      fallback.infoEntry(href, baseURI, accept, handler)
    }
  }

  override def listEachEntry(href: String, baseURI: URI, accept: String, handler: DataInfo): Unit = {
    val uri = baseURI.resolve(href)
    if ("file".equalsIgnoreCase(uri.getScheme)) {
      val file = URIUtils.toFile(uri)
      if (!file.exists()) {
        throw new FileNotFoundException(file.getAbsolutePath)
      }
      if (file.isDirectory) {
        for (file <- listAcceptableFiles(file, accept)) {
          handler.list(file.toURI, fileProperties(file))
        }
      } else {
        throw new FileNotFoundException(file.getAbsolutePath + " is not a directory")
      }
    } else {
      fallback.infoEntry(href, baseURI, accept, handler)
    }
  }

  override def createList(href: String, baseURI: URI): URI = {
    val uri = baseURI.resolve(href)
    if ("file".equalsIgnoreCase(uri.getScheme)) {
      val file = URIUtils.toFile(uri)

      if (config.safeMode) {
        throw new RuntimeException("Can't write in safe mode")
      }

      if (file.isDirectory) {
        file.toURI
      } else if (file.exists) {
        throw new FileNotFoundException(file.toURI.toASCIIString)
      } else {
        if (file.mkdirs) {
          file.toURI
        } else {
          throw new IOException("Could not create directory: " + file.getAbsolutePath)
        }
      }
    } else {
      fallback.createList(href, baseURI)
    }
  }

  override def deleteEntry(href: String, baseURI: URI): Unit = {
    val uri = baseURI.resolve(href)
    if ("file".equalsIgnoreCase(uri.getScheme)) {
      val file = URIUtils.toFile(uri)

      if (config.safeMode) {
        throw new RuntimeException("Can't write in safe mode")
      }

      if (!file.exists) {
        throw new FileNotFoundException(file.toURI.toASCIIString)
      } else {
        if (!file.delete) {
          throw new IOException("Could not delete " + file.toURI.toASCIIString)
        }
      }
    } else {
      fallback.deleteEntry(href, baseURI)
    }
  }

  protected def fileProperties(file: File): Map[String,XdmAtomicValue] = {
    val props = mutable.HashMap.empty[String, XdmAtomicValue]

    val offset = TimeZone.getDefault.getOffset(file.lastModified())
    val lastmod = new Date(file.lastModified() - offset)

    val ftype = if (file.isDirectory) {
      "directory"
    } else if (file.isFile) {
      "file"
    } else {
      "other"
    }

    props.put("content-type", new XdmAtomicValue(ContentTypes.contentType(file.getName)))
    props.put("last-modified",
      new XdmAtomicValue(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(lastmod) + "Z"))
    props.put("length", new XdmAtomicValue(file.length()))
    props.put("readable", new XdmAtomicValue(file.canRead))
    props.put("writable", new XdmAtomicValue(file.canWrite))
    props.put("hidden", new XdmAtomicValue(file.isHidden))
    props.put("executable", new XdmAtomicValue(file.canExecute))
    props.put("file-type", new XdmAtomicValue(ftype))
    props.toMap
  }

  protected def listAcceptableFiles(dir: File, acceptTypes: String): List[File] = {
    if (acceptTypes.contains("*/*")) {
      return dir.listFiles().toList
    }

    val list = dir.listFiles(new FileFilter() {
      override def accept(file: File): Boolean = {
        if (!file.isFile) return false
        val ctype = ContentTypes.contentType(file.getName)
        val primary = ctype.substring(0, ctype.indexOf('/'))
        acceptTypes.contains(ctype) || acceptTypes.contains(primary + "/*")
      }
    })

    list.toList
  }
}
