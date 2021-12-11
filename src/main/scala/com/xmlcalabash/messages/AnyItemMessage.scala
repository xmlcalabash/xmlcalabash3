package com.xmlcalabash.messages

import com.xmlcalabash.runtime.{BinaryNode, XProcMetadata}
import com.xmlcalabash.util.MinimalStaticContext
import net.sf.saxon.s9api.XdmNode
import org.slf4j.{Logger, LoggerFactory}

class AnyItemMessage(override val item: XdmNode,
                     private val binary: BinaryNode,
                     override val metadata: XProcMetadata,
                     override val context: MinimalStaticContext) extends XProcItemMessage(item, metadata, context) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def shadow: BinaryNode = {
    binary
  }
}
