package com.xmlcalabash.runtime.params

import com.xmlcalabash.runtime.ImplParams
import com.xmlcalabash.util.{MediaType, MinimalStaticContext}

class DocumentLoaderParams(val hrefAvt: List[String],
                           val content_type: Option[MediaType],
                           val parameters: Option[String],
                           val document_properties: Option[String],
                           val context_provided: Boolean,
                           val context: MinimalStaticContext) extends ImplParams {

}
