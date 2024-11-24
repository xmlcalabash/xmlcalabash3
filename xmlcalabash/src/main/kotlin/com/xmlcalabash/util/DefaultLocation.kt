package com.xmlcalabash.util

import net.sf.saxon.s9api.Location

class DefaultLocation (val href: String? = null, val lineNumber: Int? = null, val columnNumber: Int? = null): Location {
    override fun getPublicId(): String? = null
    override fun getSystemId(): String? = href
    override fun getLineNumber(): Int = lineNumber ?: -1
    override fun getColumnNumber(): Int = columnNumber ?: -1
    override fun saveLocation(): Location = this
    override fun toString(): String = "${href?:""}:${lineNumber?:""}:${columnNumber?:""}"
}