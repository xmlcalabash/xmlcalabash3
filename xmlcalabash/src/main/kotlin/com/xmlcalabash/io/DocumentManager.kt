package com.xmlcalabash.io

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.spi.DocumentResolver
import net.sf.saxon.lib.ModuleURIResolver
import net.sf.saxon.lib.ResourceResolver
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.ext.EntityResolver2
import org.xmlresolver.ResourceRequestImpl
import org.xmlresolver.XMLResolver
import org.xmlresolver.XMLResolverConfiguration
import org.xmlresolver.sources.ResolverSAXSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.xml.transform.Source
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamSource

class DocumentManager(): EntityResolver, EntityResolver2, ResourceResolver, ModuleURIResolver {
    constructor(manager: DocumentManager): this() {
        prefixMap.putAll(manager.prefixMap)
        prefixList.addAll(manager.prefixList)
        _cache.putAll(manager._cache)
        _resolver = manager._resolver
        _resolverConfiguration = manager._resolverConfiguration
    }

    private val prefixMap = mutableMapOf<String, DocumentResolver>()
    private val prefixList = mutableListOf<String>()
    private val _cache = mutableMapOf<URI, XProcDocument>()

    private var _resolver: XMLResolver? = null
    private var _resolverConfiguration = XMLResolverConfiguration()

    val resolverConfiguration: XMLResolverConfiguration
        get() = _resolverConfiguration

    val resolver: XMLResolver
        get() {
            if (_resolver == null) {
                _resolver = XMLResolver(resolverConfiguration)
            }
            return _resolver!!
        }

    fun registerPrefix(prefix: String, resolver: DocumentResolver) {
        prefixMap[prefix] = resolver
        prefixList.add(prefix)
        prefixList.sort()
    }

    fun unregisterPrefix(prefix: String) {
        prefixList.remove(prefix)
        prefixMap.remove(prefix)
    }

    private fun tryResolvers(href: URI, stepConfig: XProcStepConfiguration, loaded: XProcDocument? = null): XProcDocument? {
        var current = loaded
        // FIXME: use a binary search
        val uristr = href.toString()
        for (prefix in prefixList) {
            if (uristr.startsWith(prefix)) {
                current = prefixMap[prefix]!!.resolve(stepConfig, href, current) ?: current
            }
        }
        return current
    }

    fun lookup(href: URI): URI {
        val req = resolver.getRequest("${href}")
        val resp = resolver.lookup(req)
        if (resp.resolvedURI != null) {
            return resp.resolvedURI
        }
        return href
    }

    fun load(href: URI, stepConfig: XProcStepConfiguration, properties: DocumentProperties = DocumentProperties(), parameters: Map<QName, XdmValue> = mapOf()): XProcDocument {
        val cached = getCached(href)
        if (cached != null) {
            val resolved = tryResolvers(href, stepConfig, cached)
            return resolved ?: cached
        }

        val resp = resolver.lookupUri(href.toString())
        val loadURI = if (resp.isResolved) {
            resp.resolvedURI
        } else {
            href
        }

        val resolved = tryResolvers(loadURI, stepConfig)
        if (resolved == null) {
            val loader = DocumentLoader(stepConfig, loadURI, properties, parameters)
            return loader.load()
        }
        return resolved
    }

    fun getCached(href: URI): XProcDocument? {
        synchronized(_cache) {
            val doc = _cache[href]
            if (doc != null) {
                logger.debug { "Using cached: ${doc.baseURI}"}
            }
            return doc
        }
    }

    fun cache(document: XProcDocument) {
        synchronized(_cache) {
            if (document.baseURI == null) {
                throw XProcError.xiBaseUriRequiredToCache().exception()
            }
            if (_cache[document.baseURI] != null) {
                logger.debug { "Replacing cache entry: ${document.baseURI}"}
            } else {
                logger.debug { "Caching: ${document.baseURI}"}
            }
            _cache[document.baseURI!!] = document
        }
    }

    fun uncache(document: XProcDocument) {
        synchronized(_cache) {
            if (document.baseURI != null) {
                val doc = _cache.remove(document.baseURI!!)
                if (doc == null) {
                    logger.debug { "Was not cached: ${document.baseURI}" }
                } else {
                    logger.debug { "Removed from cache: ${document.baseURI}" }
                }
            }
        }
    }

    fun clearCache() {
        synchronized(_cache) {
            logger.debug("Clearing cache")
            _cache.clear()
        }
    }

    private fun inputFromCache(href: URI): InputSource? {
        val doc = getCached(href)
        if (doc != null) {
            val byteStream = if (doc is XProcBinaryDocument) {
                doc.binaryValue.inputStream()
            } else {
                val byteStream = ByteArrayOutputStream()
                DocumentWriter(doc, byteStream).write()
                ByteArrayInputStream(byteStream.toByteArray())
            }
            val source = InputSource(byteStream)
            source.systemId = doc.baseURI?.toString()
            return source
        }
        return null
    }

    override fun resolveEntity(name: String?, publicId: String?, baseURI: String?, systemId: String?): InputSource {
        return resolver.entityResolver2.resolveEntity(name, publicId, baseURI, systemId)
    }

    override fun resolveEntity(publicId: String?, systemId: String?): InputSource {
        if (systemId != null) {
            val source = inputFromCache(URI(systemId))
            if (source != null) {
                return source
            }
        }

        return resolver.entityResolver2.resolveEntity(publicId, systemId)
    }

    override fun getExternalSubset(name: String?, baseURI: String?): InputSource {
        return resolver.entityResolver2.getExternalSubset(name, baseURI)
    }

    override fun resolve(saxonRequest: net.sf.saxon.lib.ResourceRequest?): Source? {
        if (saxonRequest == null) {
            return null
        }

        if (saxonRequest.uri != null) {
            val inputSource = inputFromCache(URI(saxonRequest.uri))
            if (inputSource != null) {
                val source = SAXSource(inputSource)
                source.systemId = saxonRequest.uri
                return source
            }
        }

        val request = ResourceRequestImpl(resolverConfiguration, saxonRequest.nature, saxonRequest.purpose)
        request.uri = saxonRequest.uri
        request.baseURI = saxonRequest.baseUri
        request.encoding = saxonRequest.entityName
        request.publicId = saxonRequest.publicId

        val resp = resolver.resolve(request)
        if (resp.inputStream != null) {
            return ResolverSAXSource(resp)
        }

        return null
    }

    override fun resolve(moduleURI: String?, baseURI: String?, locations: Array<out String>?): Array<StreamSource>? {
        var href = if (moduleURI == null) {
            if (baseURI == null) {
                return null
            }
            URI(baseURI)
        } else {
            if (baseURI == null) {
                URI(moduleURI)
            } else {
                URI(baseURI).resolve(moduleURI)
            }
        }

        val inputSource = inputFromCache(href)
        if (inputSource == null) {
            return null
        }

        val source = StreamSource(inputSource.byteStream, href.toString())
        return arrayOf(source)
    }
}