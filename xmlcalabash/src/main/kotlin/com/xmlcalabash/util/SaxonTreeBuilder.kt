package com.xmlcalabash.util

import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.documents.DocumentContext
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.xslt.XsltStylesheet
import com.xmlcalabash.xslt.stylesheet
import net.sf.saxon.Controller
import net.sf.saxon.event.NamespaceReducer
import net.sf.saxon.om.*
import net.sf.saxon.s9api.*
import net.sf.saxon.serialize.SerializationProperties
import net.sf.saxon.type.SchemaType
import net.sf.saxon.type.Untyped
import java.net.URI

/* N.B. There's a fundamental problem in here somewhere. In order to preserve base URIs correctly
   when, for example, @xml:base attributes have been deleted. The tree walker has to reset the
   systemIdentifier in the receiver several times. This must have something to do with getting
   the right base URIs on the constructed nodes.

   Conversely, in the case where, for example, the file is coming from a p:http-request, the URI
   of the document entity received over the net is supposed to be the base URI of the document.
   But this "resetting" that takes place undoes the value set on the document node. I'm not sure
   how.

   So there's a hacked compromise in here: if the "overrideBaseURI" is the empty string, we ignore
   it. That seems to cover both cases.

   But I am not very confident.
 */

open class SaxonTreeBuilder(val processor: Processor) {
    constructor (config: DocumentContext): this(config.processor)
    constructor (config: XProcStepConfiguration): this(config.processor)

    protected val excludedNamespaces = mutableSetOf<NamespaceUri>()
    protected val controller = Controller(processor.underlyingConfiguration)
    protected var destination = XdmDestination()
    protected var pipe = controller.makePipelineConfiguration()
    protected var receiver = destination.getReceiver(pipe, SerializationProperties())
    private val emptyAttributeMap = EmptyAttributeMap.getInstance()
    var location: BuilderLocation? = null

    var isNamespaceReducing = true

    val result: XdmNode
        get() = destination.xdmNode

    var inDocument = false
    var seenRoot = false

    fun excludeNamespaces(uris: Set<NamespaceUri>) {
        excludedNamespaces.addAll(uris)
    }

    open fun startDocument(baseURI: URI?) {
        destination = XdmDestination()
        pipe = controller.makePipelineConfiguration()
        // Make sure line numbers get preserved
        pipe.configuration.setLineNumbering(true);
        receiver = destination.getReceiver(pipe, SerializationProperties())

        if (isNamespaceReducing) {
            receiver = NamespaceReducer(receiver)
        }

        receiver.setPipelineConfiguration(pipe)

        if (baseURI != null) {
            receiver.setSystemId(baseURI.toASCIIString())
        }
        location = null

        receiver.open()
        receiver.startDocument(0)
    }

    fun endDocument() {
        receiver.endDocument()
        receiver.close()
    }

    fun addSubtree(value: XdmValue) {
        when (value) {
            is XdmNode -> addSubtreeNode(value)
            else -> addText(S9Api.valuesToString(value))
        }
    }

    private fun addSubtreeNode(node: XdmNode) {
        if (excludedNamespaces.isNotEmpty()) {
            addSubtreeNodeByParts(node)
        } else {
            location = BuilderLocation(node)
            try {
                receiver.append(node.underlyingNode)
            } catch (ex: UnsupportedOperationException) {
                // Okay, do it the hard way
                addSubtreeNodeByParts(node)
            }
        }
    }

    private fun addSubtreeNodeByParts(node: XdmNode) {
        // Okay, do it the hard way
        location = BuilderLocation(node)
        when (node.nodeKind) {
            XdmNodeKind.DOCUMENT -> writeChildren(node)
            XdmNodeKind.ELEMENT -> {
                addStartElement(node)
                writeChildren(node)
                addEndElement()
            }
            XdmNodeKind.COMMENT -> addComment(node.stringValue)
            XdmNodeKind.TEXT -> addText(node.stringValue)
            XdmNodeKind.PROCESSING_INSTRUCTION ->
                addPI(node.nodeName.localName, node.stringValue)
            else ->
                throw RuntimeException("Unexpected node kind")
        }
    }

    protected fun writeChildren(node: XdmNode) {
        location = BuilderLocation(node)
        val iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext()) {
            addSubtree(iter.next())
        }
    }

    fun addStartElement(nodeName: QName) {
        addStartElement(nodeName, EmptyAttributeMap.getInstance())
    }

    fun addStartElement(node: XdmNode) {
        location = BuilderLocation(node)
        addStartElement(node, node.nodeName, node.baseURI)
    }

    fun addStartElement(node: XdmNode, overrideBaseURI: URI) {
        location = BuilderLocation(node, overrideBaseURI)
        addStartElement(node, node.nodeName, overrideBaseURI)
    }

    fun addStartElement(node: XdmNode, newName: QName) {
        location = BuilderLocation(node)
        addStartElement(node, newName, node.baseURI)
    }

    fun addStartElement(node: XdmNode, newName: QName, overrideBaseURI: URI) {
        location = BuilderLocation(node, overrideBaseURI)
        val attrs = node.underlyingNode.attributes()
        addStartElement(node, newName, overrideBaseURI, attrs)
    }

    fun addStartElement(node: XdmNode, attrs: AttributeMap) {
        location = BuilderLocation(node)
        val inode = node.underlyingNode

        val baseURI = try {
            node.baseURI
        } catch (ex: IllegalStateException) {
            throw RuntimeException("bang") // XProcError.xdInvalidUri(node.underlyingNode.baseURI).exception()
        }

        addStartElement(NameOfNode.makeName(inode), attrs, inode.schemaType, filteredNamespaceMap(inode.allNamespaces), baseURI)
    }

    fun addStartElement(node: XdmNode, newName: QName, overrideBaseURI: URI, attrs: AttributeMap) {
        location = BuilderLocation(node, overrideBaseURI)
        val inode = node.underlyingNode

        var inscopeNS = if (seenRoot) {
            val nslist: MutableList<NamespaceBinding> = mutableListOf()
            inode.getDeclaredNamespaces(null).forEach { ns ->
                nslist.add(ns)
            }
            NamespaceMap(nslist)
        } else {
            inode.allNamespaces
        }
        inscopeNS = filteredNamespaceMap(inscopeNS)

        // If the newName has no prefix, then make sure we don't pass along some other
        // binding for the default namespace...
        if (newName.prefix == "" && inscopeNS.defaultNamespace.isEmpty) {
            inscopeNS = inscopeNS.remove("")
        }

        // Hack. See comment at top of file
        if (overrideBaseURI.toASCIIString() != "") {
            receiver.setSystemId(overrideBaseURI.toASCIIString())
        }

        val newNameOfNode = FingerprintedQName(newName.prefix, newName.namespaceUri, newName.localName)
        addStartElement(newNameOfNode, attrs, inode.schemaType, inscopeNS)
    }

    fun addStartElement(newName: QName, attrs: AttributeMap) {
        var nsmap = NamespaceMap.emptyMap()
        if (!newName.namespaceUri.isEmpty) {
            nsmap = nsmap.put(newName.prefix, newName.namespaceUri)
        }
        for (index in 0 until attrs.size()) {
            val attr = attrs.itemAt(index)
            if (attr.nodeName.namespaceUri != NamespaceUri.NULL) {
                nsmap = nsmap.put(attr.nodeName.prefix, attr.nodeName.namespaceUri)
            }
        }
        addStartElement(newName, attrs, nsmap)
    }

    fun addStartElement(newName: QName, attrs: AttributeMap, nsmap: NamespaceMap) {
        val elemName = FingerprintedQName(newName.prefix, newName.namespaceUri, newName.localName)
        addStartElement(elemName, attrs, Untyped.getInstance(), nsmap)
    }

    fun addStartElement(elemName: NodeName, typeCode: SchemaType) {
        addStartElement(elemName, emptyAttributeMap, typeCode, NamespaceMap.emptyMap())
    }

    fun addStartElement(elemName: NodeName, typeCode: SchemaType, nsmap: NamespaceMap) {
        addStartElement(elemName, emptyAttributeMap, typeCode, nsmap)
    }

    fun addStartElement(elemName: NodeName, attrs: AttributeMap, typeCode: SchemaType, nsmap: NamespaceMap, overrideBaseURI: URI?) {
        // Hack. See comment at top of file
        if (overrideBaseURI != null && overrideBaseURI.toASCIIString() != "") {
            receiver.setSystemId(overrideBaseURI.toASCIIString())
        }
        addStartElement(elemName, attrs, typeCode, nsmap)
    }

    fun addStartElement(elemName: NodeName, attrs: AttributeMap, typeCode: SchemaType, nsmap: NamespaceMap) {
        // Sort out the namespaces...
        var newmap = updateMap(nsmap, elemName.prefix, elemName.uri)
        attrs.asList().forEach { attr ->
            if (!attr.nodeName.namespaceUri.isEmpty) {
                newmap = updateMap(newmap, attr.nodeName.prefix, attr.nodeName.uri)
            }
        }

        val loc = if (location != null) {
            location
        } else if (receiver.systemId == null) {
            VoidLocation.instance
        } else {
            SysIdLocation(receiver.systemId)
        }

        receiver.startElement(elemName, typeCode, attrs, nsmap, loc, 0)
        location = null
    }

    private fun filteredNamespaceMap(nsmap: NamespaceMap): NamespaceMap {
        if (excludedNamespaces.isEmpty()) {
            return nsmap
        }

        var nmap = NamespaceMap.emptyMap()
        for (ns in nsmap.namespaceBindings) {
            if (!excludedNamespaces.contains(ns.namespaceUri)) {
                nmap = nmap.put(ns.prefix, ns.namespaceUri)
            }
        }
        return nmap
    }

    private fun updateMap(nsmap: NamespaceMap, prefix: String?, uri: String?): NamespaceMap {
        if (uri == null || "" == uri) {
            return nsmap
        }

        if (prefix == null || "" == prefix) {
            if (uri != nsmap.defaultNamespace.toString()) {
                return nsmap.put("", NamespaceUri.of(uri))
            }
        }

        val curNS = nsmap.getNamespaceUri(prefix)
        if (curNS == null) {
            return nsmap.put(prefix, NamespaceUri.of(uri))
        } else if (curNS == NamespaceUri.of(uri)) {
            return nsmap
        }

        throw XProcError.xiImpossible("Unresolvable conflicting namespace bindings for ${prefix}").exception()
    }

    fun addEndElement() {
        receiver.endElement()
    }

    fun addComment(comment: String) {
        val loc = DefaultLocation(receiver.systemId)
        receiver.comment(S9Api.makeUnicodeString(comment), loc, 0)
    }

    fun addText(text: String) {
        val loc = DefaultLocation(receiver.systemId)
        receiver.characters(S9Api.makeUnicodeString(text), loc, 0)
    }

    fun addPI(target: String, data: String, baseURI: String) {
        val loc = DefaultLocation(baseURI)
        receiver.processingInstruction(target, S9Api.makeUnicodeString(data), loc, 0)
    }

    fun addPI(target: String, data: String) {
        addPI(target, data, receiver.systemId)
    }

    fun xslt(documentUri: String? = null,
             id: String? = null,
             version: String = "3.0",
             defaultMode: String? = null,
             defaultValidation: String? = null,
             inputTypeAnnotations: String? = null,
             defaultCollation: String? = null,
             extensionElementPrefixes: String? = null,
             excludeResultPrefixes: String? = null,
             expandText: Boolean? = null,
             useWhen: String? = null,
             xpathDefaultNamespace: String? = null,
             attributes: Map<QName,String> = mapOf(),
             vocabularyPrefix: String = "xsl",
             vocabularNamespace: String = "http://www.w3.org/1999/XSL/Transform",
             ns: Map<String,String> = mapOf(),
             init: XsltStylesheet.() -> Unit): XdmNode {
        isNamespaceReducing = false
        return stylesheet(this,
            documentUri = documentUri,
            vocabularyPrefix = vocabularyPrefix,
            vocabularyNamespace = vocabularNamespace,
            id = id,
            version = version,
            defaultMode = defaultMode,
            defaultValidation = defaultValidation,
            inputTypeAnnotations = inputTypeAnnotations,
            defaultCollation = defaultCollation,
            extensionElementPrefixes = extensionElementPrefixes,
            excludeResultPrefixes = excludeResultPrefixes,
            expandText = expandText,
            useWhen = useWhen,
            xpathDefaultNamespace = xpathDefaultNamespace,
            attributes = attributes,
            ns = ns,
            init = init)
    }
}