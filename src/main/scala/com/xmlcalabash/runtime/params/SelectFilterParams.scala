package com.xmlcalabash.runtime.params

import com.xmlcalabash.model.xxml.XStaticContext
import com.xmlcalabash.runtime.{ImplParams, XmlPortSpecification}

class SelectFilterParams(val context: XStaticContext, val select: String, val port: String, val sequence: Boolean) extends ImplParams {
}
