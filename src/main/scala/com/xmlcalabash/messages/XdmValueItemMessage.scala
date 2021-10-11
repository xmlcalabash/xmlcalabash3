package com.xmlcalabash.messages

import com.xmlcalabash.runtime.{StaticContext, XProcMetadata}
import net.sf.saxon.s9api.XdmValue

class XdmValueItemMessage(override val item: XdmValue,
                          override val metadata: XProcMetadata,
                          override val context: StaticContext)
  extends XProcItemMessage(item, metadata, context) {
}
