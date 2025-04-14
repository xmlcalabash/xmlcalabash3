package com.xmlcalabash.testdriver

import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.io.File

class TestStatus(val testFile: File, val status: String, val error: XProcError? = null) {
    val failedAssertions = mutableListOf<XdmNode>()
    val expectedCodes = mutableListOf<QName>()
    var message: String? = null
    var stdOutput: String? = null
    var stdError: String? = null
    var elapsed: Double = -1.0
    var messagesXml: XdmNode? = null

    constructor(testFile: File, status: String, message: String): this(testFile, status, null) {
        this.message = message
    }

    constructor(testFile: File, status: String, errors: List<XdmNode>): this(testFile, status, null) {
        failedAssertions.addAll(errors)
    }

    constructor(testFile: File, status: String, error: XProcError, codes: List<QName>): this(testFile, status, error) {
        expectedCodes.addAll(codes)
    }

    override fun toString(): String {
        if (error == null) {
            return status
        }
        return "${status}: ${error.code}"
    }
}