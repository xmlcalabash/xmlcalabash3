package com.xmlcalabash.messages

import com.xmlcalabash.model.xxml.XStaticContext
import com.xmlcalabash.runtime.XProcMetadata
import com.xmlcalabash.util.MinimalStaticContext
import net.sf.saxon.s9api.XdmValue

class XdmValueItemMessage(override val item: XdmValue,
                          override val metadata: XProcMetadata,
                          override val context: MinimalStaticContext)
  extends XProcItemMessage(item, metadata, context) {
}
