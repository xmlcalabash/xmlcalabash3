package com.xmlcalabash.test

import com.xmlcalabash.namespace.NsXs

open class Expressions {
    protected fun type(name: String): String {
        return "Q{${NsXs.namespace}}${name}"
    }
}