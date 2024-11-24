package com.xmlcalabash.util

import com.xmlcalabash.documents.XProcDocument
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.CollectionFinder
import net.sf.saxon.lib.Resource
import net.sf.saxon.lib.ResourceCollection
import net.sf.saxon.om.Item
import net.sf.saxon.s9api.XdmNode

class XProcCollectionFinder(val coll: List<XProcDocument>, val chain: CollectionFinder): CollectionFinder {
    companion object {
        val DEFAULT = "https://xmlcalabash.com/default/collection"
    }

    override fun findCollection(context: XPathContext?, collectionUri: String?): ResourceCollection {
        if (collectionUri == DEFAULT) {
            val resources = XProcResourceCollection(coll)
            return resources
        } else {
            return chain.findCollection(context, collectionUri)
        }
    }

    inner class XProcResourceCollection(val coll: List<XProcDocument>): ResourceCollection {
        override fun getCollectionURI(): String {
            return DEFAULT
        }

        override fun getResourceURIs(context: XPathContext?): MutableIterator<String> {
            val uris = mutableListOf<String>()
            for (doc in coll) {
                uris.add(doc.baseURI.toString())
            }
            return uris.iterator()
        }

        override fun getResources(context: XPathContext?): MutableIterator<Resource> {
            val resources = mutableListOf<XProcResource>()
            for (doc in coll) {
                resources.add(XProcResource(doc))
            }
            return resources.iterator()
        }

        override fun isStable(context: XPathContext?): Boolean {
            return true
        }
    }

    inner class XProcResource(val doc: XProcDocument): Resource {
        override fun getResourceURI(): String {
            return doc.baseURI.toString()
        }

        override fun getItem(): Item {
            val item = doc.value
            when (item) {
                is XdmNode -> return item.underlyingValue
                else -> return item as Item
            }
        }

        override fun getContentType(): String {
            return doc.contentType.toString()
        }
    }
}