package com.xmlcalabash.util

import net.sf.saxon.s9api.Location

class SysIdLocation (val sysId: String): Location {
    override fun getPublicId(): String? = null
    override fun getSystemId(): String? = sysId
    override fun getLineNumber(): Int = -1
    override fun getColumnNumber(): Int = -1
    override fun saveLocation(): Location = this
}