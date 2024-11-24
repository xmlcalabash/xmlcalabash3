package com.xmlcalabash.testdriver

import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode

class TestStatus(val status: String, val error: XProcError? = null) {
    val failedAssertions = mutableListOf<XdmNode>()
    val expectedCodes = mutableListOf<QName>()
    var message: String? = null

    constructor(status: String, message: String): this(status, null) {
        this.message = message
    }

    constructor(status: String, errors: List<XdmNode>): this(status, null) {
        failedAssertions.addAll(errors)
    }

    constructor(status: String, error: XProcError, codes: List<QName>): this(status, error) {
        expectedCodes.addAll(codes)
    }

    override fun toString(): String {
        if (error == null) {
            return status
        }
        return "${status}: ${error.code}"
    }
}