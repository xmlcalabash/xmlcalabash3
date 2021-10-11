package com.xmlcalabash.messages

import com.xmlcalabash.runtime.{StaticContext, XProcMetadata}
import net.sf.saxon.s9api.XdmNode

class XdmNodeItemMessage(override val item: XdmNode,
                         override val metadata: XProcMetadata,
                         override val context: StaticContext)
  extends XdmValueItemMessage(item, metadata, context) {
}
