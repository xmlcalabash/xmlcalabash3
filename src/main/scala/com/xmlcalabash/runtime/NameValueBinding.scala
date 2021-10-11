package com.xmlcalabash.runtime

import com.xmlcalabash.messages.XdmValueItemMessage
import net.sf.saxon.s9api.{QName, XdmValue}

class NameValueBinding(val name: QName, val value: XdmValue, val meta: XProcMetadata, val context: StaticContext) {
  def this(name: QName, message: XdmValueItemMessage) =
    this(name, message.item, message.metadata, message.context)
  def this(name: QName, value: XdmValue, message: XdmValueItemMessage) =
    this(name, value, message.metadata, message.context)
}
