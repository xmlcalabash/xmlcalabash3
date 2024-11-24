package com.xmlcalabash.util

import com.xmlcalabash.datamodel.MediaType
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.value.StringValue

class ValueUtils {
    companion object {
        fun contentClassification(value: XdmValue): MediaType? {
            when (value) {
                is XdmNode -> {
                    if (S9Api.isTextDocument(value)) {
                        if (value.stringValue.isEmpty()) {
                            return MediaType.XML
                        }
                        return MediaType.TEXT
                    }
                    val root = S9Api.firstElement(value)
                    if (root != null && root.nodeName.namespaceUri == NamespaceUri.of("http://www.w3.org/1999/xhtml")) {
                        return MediaType.HTML
                    }
                    return MediaType.XML
                }
                is XdmMap -> return MediaType.JSON
                is XdmArray -> return MediaType.JSON
                is XdmAtomicValue -> {
                    if (value.underlyingValue is StringValue) {
                        return MediaType.TEXT
                    }
                    return MediaType.JSON
                }
                else -> {
                    if (value == XdmEmptySequence.getInstance()) {
                        return null
                    }

                    var allAtomic = true
                    for (item in value.iterator()) {
                        if (item !is XdmAtomicValue) {
                            allAtomic = false
                            break
                        }
                    }

                    if (allAtomic) {
                        return MediaType.JSON
                    }

                    return null
                }
            }
        }

        fun inscopeNamespaces(node: XdmNode): Map<String, NamespaceUri> {
            val namespaces = mutableMapOf<String, NamespaceUri>()
            for (ns in node.axisIterator(Axis.NAMESPACE)) {
                if (node.nodeName.localName != "xml") {
                    if (ns.nodeName == null) {
                        namespaces[""] = NamespaceUri.of(ns.stringValue)
                    } else {
                        namespaces[ns.nodeName.localName] = NamespaceUri.of(ns.stringValue)
                    }
                }
            }
            return namespaces
        }

        fun parseClarkName(name: String, pfx: String? = null): QName {
            // FIXME: Better error handling for ClarkName parsing
            if (name.startsWith("{")) {
                val pos = name.indexOf("}")
                val uri = name.substring(1, pos)
                val local = name.substring(pos + 1)
                if (pfx != null) {
                    return QName(pfx, uri, local)
                } else {
                    return QName(uri, local)
                }
            } else {
                return QName(name)
            }
        }

        fun isTrue(value: Any?): Boolean {
            if (value == null) {
                return false
            }

            when (value) {
                is String -> return listOf("1", "true", "yes").contains(value)
                is XdmAtomicValue -> return listOf("1", "true", "yes").contains(value.stringValue)
                else -> return false
            }
        }
    }
}