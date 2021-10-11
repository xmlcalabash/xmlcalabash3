package com.xmlcalabash.runtime.params

import com.xmlcalabash.runtime.{ImplParams, StaticContext}
import com.xmlcalabash.util.MediaType

class DocumentLoaderParams(val hrefAvt: List[String],
                           val content_type: Option[MediaType],
                           val parameters: Option[String],
                           val document_properties: Option[String],
                           val context_provided: Boolean,
                           val context: StaticContext) extends ImplParams {

}
