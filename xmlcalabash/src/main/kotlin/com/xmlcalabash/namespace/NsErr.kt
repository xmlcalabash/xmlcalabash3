package com.xmlcalabash.namespace

import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName

object NsErr {
    val namespace: NamespaceUri = NamespaceUri.of("http://www.w3.org/ns/xproc-error")

    private val staticErrors = mutableMapOf<Int, QName>()
    private val dynamicErrors = mutableMapOf<Int, QName>()
    private val stepErrors = mutableMapOf<Int, QName>()
    private val internalErrors = mutableMapOf<Int, QName>()

    val threadInterrupted = xi(3)

    fun xs(code: Int): QName {
        val err = staticErrors[code] ?: error(code, "XS")
        staticErrors[code] = err
        return err
    }

    fun xd(code: Int): QName {
        val err = dynamicErrors[code] ?: error(code, "XD")
        dynamicErrors[code] = err
        return err
    }

    fun xc(code: Int): QName {
        val err = stepErrors[code] ?: error(code, "XC")
        stepErrors[code] = err
        return err
    }

    fun xi(code: Int): QName {
        if (code in internalErrors) {
            return internalErrors[code]!!
        }
        val err = QName(NsCx.errorNamespace, "cxerr:XI${code.toString().padStart(4, '0')}")
        internalErrors[code] = err
        return err
    }

    private fun error(code: Int, type: String): QName {
        return QName(namespace, "err:${type}${code.toString().padStart(4, '0')}")
    }
}