package com.xmlcalabash.util

import net.sf.saxon.s9api.Location

class VoidLocation private constructor(): Location {
    companion object {
        val instance = VoidLocation()
    }

    override fun getPublicId(): String? = null
    override fun getSystemId(): String? = null
    override fun getLineNumber(): Int = -1
    override fun getColumnNumber(): Int = -1
    override fun saveLocation(): Location = this
}