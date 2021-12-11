package com.xmlcalabash.messages

import com.xmlcalabash.model.xxml.XStaticContext
import com.xmlcalabash.runtime.XProcMetadata
import com.xmlcalabash.util.MinimalStaticContext
import net.sf.saxon.s9api.XdmNode

class XdmNodeItemMessage(override val item: XdmNode,
                         override val metadata: XProcMetadata,
                         override val context: MinimalStaticContext)
  extends XdmValueItemMessage(item, metadata, context) {
}
