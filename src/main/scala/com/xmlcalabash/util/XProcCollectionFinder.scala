package com.xmlcalabash.util

import java.net.URI
import com.xmlcalabash.config.XMLCalabashConfig
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.{CollectionFinder, Resource, ResourceCollection}
import net.sf.saxon.om.{Item, SpaceStrippingRule}
import net.sf.saxon.s9api.XdmNode
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.IteratorHasAsJava

object XProcCollectionFinder {
  val DEFAULT = "http://xmlcalabash.com/saxon-default-collection"
}

class XProcCollectionFinder(config: XMLCalabashConfig, docs: List[XdmNode], chainedFinder: CollectionFinder)
  extends CollectionFinder {

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def findCollection(context: XPathContext, collectionURI: String): ResourceCollection = {
    logger.trace("Collection: " + collectionURI)

    if (collectionURI == null || collectionURI == XProcCollectionFinder.DEFAULT) {
      new DocumentResourceCollection(collectionURI, docs)
    } else {
      val cURI = new URI(collectionURI)
      val docs = config.collection(cURI)
      if (docs.isEmpty) {
        chainedFinder.findCollection(context, collectionURI)
      } else {
        new DocumentResourceCollection(collectionURI, docs)
      }
    }
  }

  private class DocumentResourceCollection(val collectionURI: String, val docs: List[XdmNode]) extends ResourceCollection {
    private val uris = ListBuffer.empty[String]
    private var rsrcs = ListBuffer.empty[Resource]

    override def getCollectionURI: String = collectionURI

    def getResourceURIs(context: XPathContext): java.util.Iterator[String] = {
      if (uris.isEmpty) {
        for (doc <- docs) {
          uris += doc.getBaseURI.toASCIIString
        }
      }
      uris.iterator.asJava
    }

    def getResources(context: XPathContext): java.util.Iterator[Resource] = {
      if (rsrcs.isEmpty) {
        for (doc <- docs) {
          rsrcs += new DocumentResource(doc)
        }
      }
      rsrcs.iterator.asJava
    }

    def isStable(context: XPathContext) = true

    override def stripWhitespace(rules: SpaceStrippingRule): Boolean = false
  }

  private class DocumentResource(val doc: XdmNode) extends Resource {
    override def getResourceURI: String = doc.getBaseURI.toASCIIString

    override def getItem(context: XPathContext): Item = doc.getUnderlyingValue.head

    override def getContentType: String = null
  }


}
