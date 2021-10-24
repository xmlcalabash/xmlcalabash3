package com.xmlcalabash.messages

import com.jafpl.messages.ItemMessage
import com.xmlcalabash.runtime.XProcMetadata
import com.xmlcalabash.util.MinimalStaticContext

class XProcItemMessage(override val item: Any,
                       override val metadata: XProcMetadata,
                       val context: MinimalStaticContext)
  extends ItemMessage(item, metadata)  {
}
