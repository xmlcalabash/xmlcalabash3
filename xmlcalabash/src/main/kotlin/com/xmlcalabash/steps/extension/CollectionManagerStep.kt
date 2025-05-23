package com.xmlcalabash.steps.extension

import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.InternetProtocolRequest
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import com.xmlcalabash.util.DurationUtils
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.Resource
import net.sf.saxon.lib.ResourceCollection
import net.sf.saxon.om.Item
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmFunctionItem
import net.sf.saxon.s9api.XdmItem
import net.sf.saxon.value.DateTimeValue
import org.apache.logging.log4j.kotlin.contextName
import java.io.File
import java.net.URI
import java.time.Duration

class CollectionManagerStep(): AbstractAtomicStep() {
    companion object {
        private val _source = QName(NamespaceUri.NULL, "source")
        private val _stable = QName(NamespaceUri.NULL, "stable")
    }

    override fun run() {
        super.run()
        val href = uriBinding(_source)!!.toString()
        val stable = booleanBinding(_stable) ?: true

        val currColl = stepConfig.processor.underlyingConfiguration.getRegisteredCollection(href)
        if (currColl is Collection && currColl.stable) {
            throw stepConfig.exception(XProcError.xcxCannotModifyStableCollection(href))
        }

        val documents = mutableMapOf<String, Resource>()
        for (doc in queues["source"]!!) {
            if (doc.baseURI == null) {
                throw stepConfig.exception(XProcError.xcxResourceWithoutUri())
            }
            val uristr = "${doc.baseURI}"
            if (documents.containsKey(uristr)) {
                throw stepConfig.exception(XProcError.xcxResourceWithDuplicateUri(doc.baseURI!!))
            }
            val item = if (doc.value is XdmItem) {
                doc.value as XdmItem
            } else {
                throw stepConfig.exception(XProcError.xcxResourceWithoutValue(doc.baseURI!!))
            }
            documents[uristr] = Resource(doc.baseURI!!, item.underlyingValue, doc.contentType)
        }

        val coll = Collection(href, documents, stable)
        stepConfig.processor.underlyingConfiguration.registerCollection(href, coll)

        for (doc in queues["source"]!!) {
            receiver.output("result", doc)
        }
    }

    override fun toString(): String = "cx:collection-manager"

    class Resource(val uri: URI, val resourceItem: Item, val contentType: MediaType?): net.sf.saxon.lib.Resource {
        override fun getResourceURI(): String? {
            return uri.toString()
        }

        override fun getItem(): Item? {
            return resourceItem
        }

        override fun getContentType(): String? {
            return contentType?.toString()
        }
    }

    inner class Collection(val href: String, val documents: Map<String, Resource>, val stable: Boolean): ResourceCollection {
        override fun getCollectionURI(): String? {
            return href
        }

        override fun getResourceURIs(context: XPathContext?): Iterator<String?>? {
            return documents.keys.iterator()
        }

        override fun getResources(context: XPathContext?): Iterator<Resource?>? {
            return documents.values.iterator()
        }

        override fun isStable(context: XPathContext?): Boolean {
            return stable
        }

    }

}