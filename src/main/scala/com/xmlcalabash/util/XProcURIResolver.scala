package com.xmlcalabash.util

import java.io.{IOException, Reader}
import java.net.{MalformedURLException, URI, URISyntaxException, URL}

import com.sun.org.apache.xml.internal.utils.URI.MalformedURIException
import com.xmlcalabash.config.XMLCalabashConfig
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.{Source, URIResolver}
import net.sf.saxon.Configuration
import net.sf.saxon.lib.{ModuleURIResolver, StandardModuleURIResolver, StandardUnparsedTextResolver, UnparsedTextURIResolver}
import net.sf.saxon.s9api.XdmNode
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.{EntityResolver, InputSource}

import scala.collection.mutable

class XProcURIResolver(config: XMLCalabashConfig) extends URIResolver with EntityResolver with ModuleURIResolver with UnparsedTextURIResolver {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var _uriResolver = Option.empty[URIResolver]
  protected var _entityResolver = Option.empty[EntityResolver]
  protected var _moduleUriResolver = Option.empty[ModuleURIResolver]
  protected var _unparsedTextUriResolver = Option.empty[UnparsedTextURIResolver]
  protected val cache = mutable.HashMap.empty[String,XdmNode]
  protected var useCache = true // FIXME: This is supposed to be temporary

  def uriResolver: Option[URIResolver] = _uriResolver
  def uriResolver_=(resolver: URIResolver): Unit = {
    _uriResolver = Some(resolver)
  }

  def entityResolver: Option[EntityResolver] = _entityResolver
  def entityResolver_=(resolver: EntityResolver): Unit = {
    _entityResolver = Some(resolver)
  }

  def moduleUriResolver: Option[ModuleURIResolver] = _moduleUriResolver
  def moduleUriResolver_=(resolver: ModuleURIResolver): Unit = {
    _moduleUriResolver = Some(resolver)
  }

  def unparsedTextUriResolver: Option[UnparsedTextURIResolver] = _unparsedTextUriResolver
  def unparsedTextUriResolver_=(resolver: UnparsedTextURIResolver): Unit = {
    _unparsedTextUriResolver = Some(resolver)
  }

  def addCatalogs(catalogs: List[String]): Unit = {
    if (uriResolver.isDefined) {
      uriResolver.get match {
        case res: org.xmlresolver.Resolver =>
          for (catalog <- catalogs) {
            try {
              logger.debug("Adding catalog to resolver: " + catalog)
              val cat = new URL(catalog)
              val source = new InputSource(cat.openStream())
              source.setSystemId(catalog)
              val catconfig = res.getConfiguration
              catconfig.addCatalog(cat.toURI, source)
            } catch {
              case e: MalformedURIException =>
                logger.info("Malformed catalog URI: " + catalog)
              case e: IOException =>
                logger.info("I/O error reading catalog: " + catalog)
              case t: Throwable => throw t
            }
          }
        case _ =>
          logger.debug("Not adding catalogs to resolver: URI Resolver is not an org.xmlresolver.Resolver")
      }
    } else {
      logger.debug("Not adding catalogs to resolver: URI Resolver is not defined")
    }
  }

  def cache(doc: XdmNode, baseURI: URI): Unit = {
    val root = S9Api.documentElement(doc)

    // Explicitly use the base URI of the root element so that if it has an xml:base
    // attribute, that becomes the base URI of the document.
    val docURI = if (root.isDefined) {
      baseURI.resolve(root.get.getBaseURI)
    } else {
      baseURI
    }

    if (useCache) {
      cache.put(docURI.toASCIIString, doc)
    }
  }

  override def resolve(href: String, base: String): Source = {
    logger.trace("URIResolver(" + href + "," + base + ")")

    var uri: String = null
    if (base == null) {
      try {
        val url = new URL(href)
        uri = url.toURI.toASCIIString
      } catch {
        case mue: MalformedURLException =>
          logger.trace("MalformedURLException on " + href)
        case use: URISyntaxException =>
          logger.trace("URISyntaxException on " + href)
      }
    } else {
      try {
        val baseURI = new URI(base)
        uri = baseURI.resolve(href).toASCIIString
      } catch {
        case use: URISyntaxException =>
          logger.trace("URISyntaxException resolving base and href: " + base + " : " + href)
      }
    }

    logger.trace("Resolved: " + uri)

    if (cache.contains(uri)) {
      logger.trace("Returning cached document.")
      return cache(uri).asSource
    }

    if (uriResolver.isDefined) {
      logger.trace("uriResolver.resolve(" + href + "," + base + ")")
      val resolved = uriResolver.get.resolve(href, base)
      // FIXME: This is a grotesque hack. This is wrong. Wrong. Wrong.
      // To support caching, XMLResolver (xmlresolver.org) returns a Source even when it hasn't
      // found the resource. Problem is, it doesn't setup the entity resolver correctly for that
      // resource. So we hack at it here...
      if (resolved != null && resolved.isInstanceOf[SAXSource]) {
        val ssource: SAXSource = resolved.asInstanceOf[SAXSource]
        var reader = ssource.getXMLReader
        if (reader == null) {
          val saxParserFactory: SAXParserFactory = SAXParserFactory.newInstance
          saxParserFactory.setNamespaceAware(true) // Must be namespace aware

          reader = saxParserFactory.newSAXParser.getXMLReader
          reader.setEntityResolver(this)
          ssource.setXMLReader(reader)
        }
      }
      return resolved
    }
    null
  }

  override def resolveEntity(publicId: String, systemId: String): InputSource = {
    logger.trace("ResolveEntity(" + publicId + "," + systemId + ")")

    if (systemId == null) {
      return null
    }

    var uri = ""
    try {
      uri = new URI(systemId).toASCIIString
    } catch {
      case use: URISyntaxException =>
        logger.trace("URISyntaxException resolving entityResolver systemId: " + systemId)
        uri = systemId
      case t: Throwable => throw t
    }

    if (cache.contains(uri)) {
      logger.trace("Returning cached document.")
      S9Api.xdmToInputSource(config, cache(uri))
    } else if (entityResolver.isDefined) {
      val r = entityResolver.get.resolveEntity(publicId, systemId)
      r
    } else {
      new InputSource(uri)
    }
  }

  override def resolve(moduleURI: String, baseURI: String, locations: Array[String]): Array[StreamSource] = {
    if (moduleUriResolver.isDefined) {
      moduleUriResolver.get.resolve(moduleURI, baseURI, locations)
    } else {
      val resolver = new StandardModuleURIResolver()
      resolver.resolve(moduleURI, baseURI, locations)
    }
  }

  override def resolve(absoluteURI: URI, encoding: String, config: Configuration): Reader = {
    if (unparsedTextUriResolver.isDefined) {
      unparsedTextUriResolver.get.resolve(absoluteURI, encoding, config)
    } else {
      val resolver = new StandardUnparsedTextResolver()
      resolver.resolve(absoluteURI, encoding, config)
    }
  }
}
