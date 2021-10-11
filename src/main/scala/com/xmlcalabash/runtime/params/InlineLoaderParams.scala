package com.xmlcalabash.runtime.params

import com.xmlcalabash.runtime.{ImplParams, StaticContext}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.XdmNode

class InlineLoaderParams(val document: XdmNode,
                         val content_type: Option[MediaType],
                         val document_properties: Option[String],
                         val encoding: Option[String],
                         val exclude_inline_prefixes: Option[String],
                         val expand_text: Boolean,
                         val context_provided: Boolean,
                         val context: StaticContext) extends ImplParams {
}
