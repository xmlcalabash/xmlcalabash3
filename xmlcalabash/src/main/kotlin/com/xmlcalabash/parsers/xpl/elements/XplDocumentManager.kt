package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.datamodel.PipelineBuilder
import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmNodeKind
import java.net.URI

class XplDocumentManager(val builder: PipelineBuilder) {
    val documents = mutableMapOf<URI, XplDocument>()

    fun load(xml: XdmNode): XplDocument {
        if (xml.nodeKind != XdmNodeKind.DOCUMENT) {
            throw XProcError.xiImpossible("Pipeline XML must be a document").exception()
        }
        if (xml.baseURI == null || !xml.baseURI.isAbsolute) {
            throw XProcError.xiImpossible("Pipeline XML must have an absolute base URI").exception()
        }
        return load(xml.baseURI!!, xml, UseWhenContext(builder))
    }

    fun load(uri: URI): XplDocument {
        return load(uri, UseWhenContext(builder))
    }

    fun load(uri: URI, context: UseWhenContext): XplDocument {
        if (!documents.containsKey(uri)) {
            val xml = builder.load(uri)
            val document = XplDocument(builder, xml.value as XdmNode)
            documents[uri] = document

            val localContext = context.copy()
            localContext.staticOptions.putAll(context.staticOptions)

            document.resolve(this, localContext)

            return document
        }

         return documents[uri]!!
    }


    fun load(uri: URI, xml: XdmNode, context: UseWhenContext): XplDocument {
        if (!documents.containsKey(uri)) {
            val document = XplDocument(builder, xml)
            documents[uri] = document

            val localContext = context.copy()
            document.resolve(this, localContext)
            return document
        }

        return documents[uri]!!
    }
}