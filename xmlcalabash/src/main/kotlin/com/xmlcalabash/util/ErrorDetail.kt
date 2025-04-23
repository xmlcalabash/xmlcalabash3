package com.xmlcalabash.util

import net.sf.saxon.s9api.QName

data class ErrorDetail(val message: String, val extra: Map<QName,String>) {
}