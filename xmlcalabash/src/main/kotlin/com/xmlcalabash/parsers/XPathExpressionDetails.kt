package com.xmlcalabash.parsers

import net.sf.saxon.s9api.QName

data class XPathExpressionDetails(
    val error: Exception?,
    val variableRefs: Set<QName>,
    val functionRefs: Set<Pair<QName,Int>>,
    val contextRef: Boolean,
    val alwaysDynamic: Boolean
)