package com.xmlcalabash.datamodel

import com.xmlcalabash.documents.XProcDocument
import net.sf.saxon.s9api.XdmNode
import java.net.URI

class Location(private val uri: URI?, lineNo: Int?, colNo: Int?) {
    companion object {
        val NULL = Location(null, null, null)
    }

    val baseUri: URI? = uri
    var _lineNumber: Int = lineNo ?: -1
    val lineNumber: Int = _lineNumber
    var _columnNumber: Int = colNo ?: -1
    val columnNumber: Int = _columnNumber

    constructor(uri: URI?): this(uri, null, null)
    constructor(loc: net.sf.saxon.s9api.Location): this(if (loc.systemId == null) null else URI(loc.systemId), loc.lineNumber, loc.columnNumber)
    constructor(node: XdmNode): this(node.baseURI, node.lineNumber, node.columnNumber)
    constructor(doc: XProcDocument): this(doc.baseURI) {
        if (doc.value is XdmNode) {
            _lineNumber = (doc.value as XdmNode).lineNumber
            _columnNumber = (doc.value as XdmNode).columnNumber
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(uri ?: "???")
        if (lineNumber >= 0) {
            sb.append(":${lineNumber}")
        }
        if (columnNumber >= 0) {
            sb.append(":${columnNumber}")
        }
        return sb.toString()
    }
}