package com.xmlcalabash.runtime.params

import com.xmlcalabash.runtime.{ImplParams, StaticContext, XmlPortSpecification}

class SelectFilterParams(val context: StaticContext, val select: String, val port: String, val ispec: XmlPortSpecification) extends ImplParams {
}
