package com.xmlcalabash.io

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.config.XProcEnvironment
import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcBinaryDocument
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.spi.DocumentResolver
import com.xmlcalabash.tracing.TraceListener
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.lib.ModuleURIResolver
import net.sf.saxon.lib.ResourceResolver
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmValue
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.ext.EntityResolver2
import org.xmlresolver.ResolverConstants
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

class DocumentManager(val environment: XProcEnvironment): EntityResolver, EntityResolver2, ResourceResolver, ModuleURIResolver {
    constructor(manager: DocumentManager): this(manager.environment) {
        prefixMap.putAll(manager.prefixMap)
        prefixList.addAll(manager.prefixList)
        _cache.putAll(manager._cache)
        _resolver = manager._resolver
        _resolverConfiguration = manager._resolverConfiguration
    }

    private val prefixMap = mutableMapOf<String, DocumentResolver>()
    private val prefixList = mutableListOf<String>()
    private val _cache = mutableMapOf<URI, MutableMap<MediaType,XProcDocument>>()

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

    private fun tryResolvers(href: URI, stepConfig: StepConfiguration, loaded: XProcDocument? = null): XProcDocument? {
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

    fun lookup(href: URI, baseUri: URI? = null): URI {
        val req = if (baseUri == null) {
            resolver.getRequest("${href}")
        } else {
            resolver.getRequest("${href}", "${baseUri}")
        }
        val resp = resolver.lookup(req)
        if (resp.resolvedURI != null) {
            return resp.resolvedURI
        }
        return href
    }

    fun load(href: URI, stepConfig: StepConfiguration, properties: DocumentProperties = DocumentProperties(), parameters: Map<QName, XdmValue> = mapOf()): XProcDocument {
        val cached = getCached(href)
        if (cached != null) {
            val resolved = tryResolvers(href, stepConfig, cached)
            if (resolved != null) {
                trace(stepConfig, href, null, resolved=true, cached=false)
                return resolved
            }
            trace(stepConfig, href, null, resolved=false, cached=true)
            return cached
        }

        val resp = resolver.lookupUri(href.toString())
        val loadURI = if (resp.isResolved) {
            resp.resolvedURI
        } else {
            href
        }

        val resolved = tryResolvers(loadURI, stepConfig)
        if (resolved != null) {
            trace(stepConfig, loadURI, href, resolved=true, cached=false)
            return resolved
        }

        val originalUri = if (href == loadURI) {
            null
        } else {
            href
        }

        val newProperties = DocumentProperties()
        for ((key, value) in properties.asMap()) {
            if (key.namespaceUri == NsCx.namespace) {
                stepConfig.warn { "Attempt to set ${key} property ignored." }
            } else {
                newProperties[key] = value
            }
        }

        val loader = DocumentLoader(stepConfig, loadURI, properties, parameters, originalUri)
        return loader.load()
    }

    private fun trace(stepConfig: StepConfiguration, uri: URI, href: URI?, resolved: Boolean, cached: Boolean) {
        val traceHref = if (uri == href) {
            null
        } else {
            href
        }

        for (monitor in stepConfig.monitors) {
            if (monitor is TraceListener) {
                monitor.getResource(0, uri, traceHref, resolved, cached)
            }
        }
    }

    fun getCached(href: URI): XProcDocument? {
        val contentType = MediaType.parse(environment.mimeTypes.getContentType(href.toString()))
        return getCached(href, contentType)
    }

    fun getCached(href: URI, contentType: MediaType): XProcDocument? {
        synchronized(_cache) {
            val typeMap = _cache[href] ?: return null
            if (contentType in typeMap) {
                logger.debug { "Using cached: ${typeMap[contentType]!!.baseURI}/${contentType}"}
                return typeMap[contentType]!!
            }
            var randomDoc: XProcDocument? = null
            val typeClass = contentType.classification()
            for ((type, doc) in typeMap) {
                randomDoc = randomDoc ?: doc
                if (type.classification() == typeClass) {
                    logger.debug { "Using cached: ${doc.baseURI}/${type}"}
                    return doc
                }
            }
            return randomDoc
        }
    }

    fun cache(document: XProcDocument) {
        synchronized(_cache) {
            if (document.baseURI == null) {
                throw XProcError.xiBaseUriRequiredToCache().exception()
            }
            val contentType = document.contentType ?: MediaType.OCTET_STREAM
            if (_cache[document.baseURI] == null) {
                logger.debug { "Caching: ${document.baseURI}/${contentType}"}
                _cache[document.baseURI!!] = mutableMapOf()
                _cache[document.baseURI]!![contentType] = document
                return
            }

            val typeMap = _cache[document.baseURI]!!
            if (contentType in typeMap) {
                logger.debug { "Replacing cache entry: ${document.baseURI}/${contentType}"}
            } else {
                logger.debug { "Caching: ${document.baseURI}.${contentType}"}
            }
            typeMap[contentType] = document
        }
    }

    fun uncache(document: XProcDocument) {
        uncache(document, MediaType.ANY)
    }

    fun uncache(document: XProcDocument, contentType: MediaType) {
        synchronized(_cache) {
            val typeMap = _cache[document.baseURI]
            if (typeMap == null) {
                logger.debug { "Was not cached: ${document.baseURI}" }
                return
            }

            val doc = typeMap[contentType]
            if (doc != null) {
                typeMap.remove(contentType)
                logger.debug { "Removed from cache: ${document.baseURI}/${contentType}" }
                return
            }

            var removed = false
            for ((type, doc) in typeMap) {
                if (contentType.mediaType == "*"
                    || (contentType.mediaType == type.mediaType && contentType.mediaSubtype == "*")) {
                    typeMap.remove(type)
                    logger.debug { "Removed from cache: ${document.baseURI}/${type}" }
                    removed = true
                }
            }

            if (!removed) {
                logger.debug { "Was not cached: ${document.baseURI}/${contentType}" }
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
        request.entityName = saxonRequest.entityName
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
                UriUtils.resolve(URI(baseURI), moduleURI)!!
            }
        }

        val inputSource = inputFromCache(href)
        if (inputSource == null) {
            val request = ResourceRequestImpl(resolverConfiguration, ResolverConstants.SCHEMA_NATURE, ResolverConstants.VALIDATION_PURPOSE)
            request.uri = moduleURI
            request.baseURI = baseURI

            val resp = resolver.resolve(request)
            if (resp.inputStream != null) {
                val source = StreamSource(resp.inputStream, href.toString())
                return arrayOf(source)
            }

            return null
        }

        val source = StreamSource(inputSource.byteStream, href.toString())
        return arrayOf(source)
    }
}