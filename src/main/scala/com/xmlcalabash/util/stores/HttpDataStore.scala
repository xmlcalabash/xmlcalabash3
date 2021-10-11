package com.xmlcalabash.util.stores

import com.jafpl.messages.Message
import com.xmlcalabash.config.{DocumentRequest, XMLCalabashConfig}
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XProcXPathExpression}
import com.xmlcalabash.util.{InternetProtocolRequest, MediaType}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmNode, XdmValue}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, IOException}
import java.net.URI
import scala.collection.mutable

class HttpDataStore(config: XMLCalabashConfig, fallback: DataStore) extends DataStore {
  override def writeEntry(href: String, baseURI: URI, media: String, handler: DataWriter): URI = {
    val uri = baseURI.resolve(href)
    if ("http".equalsIgnoreCase(uri.getScheme) || "https".equalsIgnoreCase(uri.getScheme)) {
      val request = new InternetProtocolRequest(config, uri)
      val baos = new ByteArrayOutputStream()
      handler.store(baos)
      val meta = new XProcMetadata(MediaType.parse(media))
      request.addSource(baos.toByteArray, meta)
      val response = request.execute("PUT")
      if (response.statusCode.isEmpty || response.statusCode.get >= 400) {
        throw new RuntimeException("Failed to PUT resource " + uri.toString)
      }
      response.finalURI
    } else {
      fallback.writeEntry(href, baseURI, media, handler)
    }
  }

  override def readEntry(href: String, baseURI: URI, accept: String, overrideContentType: Option[String], handler: DataReader): Unit = {
    val uri = baseURI.resolve(href)
    if ("http".equalsIgnoreCase(uri.getScheme) || "https".equalsIgnoreCase(uri.getScheme)) {
      val request = new InternetProtocolRequest(config, uri)
      val response = request.execute("GET")
      if (response.statusCode.isEmpty || response.statusCode.get >= 400) {
        throw new RuntimeException("Failed to GET resource " + uri.toString)
      }
      if (response.response.length > 1) {
        throw new RuntimeException("Multipart resources are not acceptable here: " + uri.toString)
      }

      val ctype = overrideContentType.getOrElse(response.responseMetadata.head.contentType.toString)

      if (response.response.isEmpty) {
        val in = new ByteArrayInputStream(new Array[Byte](0))
        try {
          handler.load(response.finalURI, ctype, in, Some(0L))
        } finally {
          in.close()
        }
      } else {
        val clen = response.responseMetadata.head.property("content-length")
        val len = if (clen.isDefined) {
          Some(clen.get.toString.toLong)
        } else {
          None
        }
        try {
          handler.load(response.finalURI, ctype, response.response.head, len)
        } finally {
          response.response.head.close()
        }
      }
    } else {
      fallback.readEntry(href, baseURI, accept, overrideContentType, handler)
    }
  }

  override def infoEntry(href: String, baseURI: URI, accept: String, handler: DataInfo): Unit = {
    val uri = baseURI.resolve(href)
    if ("http".equalsIgnoreCase(uri.getScheme) || "https".equalsIgnoreCase(uri.getScheme)) {
      val request = new InternetProtocolRequest(config, uri)
      val response = request.execute("HEAD")
      if (response.statusCode.isEmpty || response.statusCode.get >= 400) {
        throw new RuntimeException("Failed to GET resource " + uri.toString)
      }
      handler.list(response.finalURI, response.headers)
    } else {
      fallback.infoEntry(href, baseURI, accept, handler)
    }
  }

  override def listEachEntry(href: String, baseURI: URI, accept: String, handler: DataInfo): Unit = {
    val uri = baseURI.resolve(href)
    if ("http".equalsIgnoreCase(uri.getScheme) || "https".equalsIgnoreCase(uri.getScheme)) {
      val request = new InternetProtocolRequest(config, uri)

      // Do head first to avoid a big download that isn't going to work
      var response = request.execute("HEAD")
      if (response.statusCode.isEmpty || response.statusCode.get >= 400) {
        throw new RuntimeException("Failed to GET resource " + uri.toString)
      }
      if (response.mediaType.isEmpty || !response.mediaType.get.htmlContentType) {
        throw new RuntimeException("Cannot list entries for " + uri.toString)
      }

      // It's actually HTML, so get it
      response = request.execute("GET")
      if (response.statusCode.isEmpty || response.statusCode.get >= 400) {
        throw new RuntimeException("Failed to GET resource " + uri.toString)
      }
      if (response.mediaType.isEmpty || !response.mediaType.get.htmlContentType) {
        throw new RuntimeException("Cannot list entries for " + uri.toString)
      }

      val docreq = new DocumentRequest(response.finalURI, response.mediaType.get)
      val result = config.documentManager.parse(docreq, response.response.head)
      val staticContext = new StaticContext(config)
      val nsmap = mutable.HashMap.empty[String, String]
      nsmap.put("html", XProcConstants.ns_html)
      staticContext.nsBindings = nsmap.toMap

      val context = new XdmValueItemMessage(result.value, response.responseMetadata.head, staticContext)
      val expr = new XProcXPathExpression(staticContext, "//html:a")
      val anchors = config.expressionEvaluator.newInstance().value(expr, List(context), Map.empty[String, Message], None)

      val viter = anchors.item.iterator()
      while (viter.hasNext) {
        val node = viter.next().asInstanceOf[XdmNode];
        var href = node.getAttributeValue(XProcConstants._href)
        if (href != null) {
          var pos = href.indexOf("#")
          if (pos >= 0) {
            href = href.substring(0, pos)
          }
          pos = href.indexOf("/")
          if (href != "" && (pos < 0 || pos == href.length)) {
            val itemuri = uri.resolve(href)
            val irequest = new InternetProtocolRequest(config, itemuri)
            val iresponse = irequest.execute("HEAD")

            if (accept.contains("*/*")) {
              handler.list(itemuri, iresponse.headers)
            } else {
              val ctype = iresponse.mediaType.getOrElse(MediaType.OCTET_STREAM)
              val ctypestr = ctype.mediaType + "/" + ctype.mediaSubtype
              if (accept.contains(ctypestr) || accept.contains(ctype.mediaType + "/*")) {
                handler.list(itemuri, iresponse.headers)
              }
            }
          }
        }
      }
    } else {
      fallback.infoEntry(href, baseURI, accept, handler)
    }
  }

  override def createList(href: String, baseURI: URI): URI = {
    val uri = baseURI.resolve(href)
    if ("http".equalsIgnoreCase(uri.getScheme) || "https".equalsIgnoreCase(uri.getScheme)) {
      throw new IOException("Cannot create list " + uri)
    } else {
      fallback.createList(href, baseURI)
    }
  }

  override def deleteEntry(href: String, baseURI: URI): Unit = {
    val uri = baseURI.resolve(href)
    if ("http".equalsIgnoreCase(uri.getScheme) || "https".equalsIgnoreCase(uri.getScheme)) {
      val request = new InternetProtocolRequest(config, uri)
      val response = request.execute("DELETE")
      if (response.statusCode.isEmpty || response.statusCode.get >= 400) {
        throw new RuntimeException("Failed to DELETE resource " + uri.toString)
      }
    } else {
      fallback.deleteEntry(href, baseURI)
    }
  }

  private def stringMap(properties: Map[QName, XdmValue]): Map[String, XdmAtomicValue] = {
    val stringMap = mutable.HashMap.empty[String, XdmAtomicValue];
    for ((key, value) <- properties) {
      // I think this cast is safe; a header will always be atomic, I think...
      stringMap.put(key.getLocalName, value.asInstanceOf[XdmAtomicValue])
    }
    stringMap.toMap
  }
}
