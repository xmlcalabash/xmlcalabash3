package com.xmlcalabash.messages

import com.jafpl.messages.ItemMessage
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata}

class XProcItemMessage(override val item: Any,
                       override val metadata: XProcMetadata,
                       val context: StaticContext)
  extends ItemMessage(item, metadata)  {
}
