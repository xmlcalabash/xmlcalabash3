package com.xmlcalabash.runtime.params

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.xxml.XStaticContext
import com.xmlcalabash.runtime.ImplParams
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.QName

class ContentTypeCheckerParams(val port: String,
                               val contentTypes: List[MediaType],
                               val context: XStaticContext,
                               val select: Option[String],
                               val errCode: QName,
                               val inputPort: Boolean,
                               val sequence: Boolean) extends ImplParams {
  def this(port: String, contentTypes: List[MediaType], context: XStaticContext, select: Option[String], inputPort: Boolean, sequence: Boolean) = {
    this(port, contentTypes, context, select, XProcException.err_xd0038, inputPort, sequence)
  }
}
