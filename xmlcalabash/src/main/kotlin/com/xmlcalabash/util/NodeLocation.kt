package com.xmlcalabash.util

import net.sf.saxon.s9api.Location
import net.sf.saxon.s9api.XdmNode

class NodeLocation(val node: XdmNode): Location {
    override fun getPublicId(): String? = null
    override fun getSystemId(): String? = node.baseURI?.toString()
    override fun getLineNumber(): Int = node.lineNumber
    override fun getColumnNumber(): Int = node.columnNumber
    override fun saveLocation(): Location = this
}