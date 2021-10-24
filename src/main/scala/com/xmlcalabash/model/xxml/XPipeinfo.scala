package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.model.util.SaxonTreeBuilder
import net.sf.saxon.s9api.XdmNode

class XPipeinfo(config: XMLCalabash, val content: XdmNode) extends XArtifact(config) {
  override protected[xxml] def validate(): Unit = {
    // nop
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    dumpTree(sb, "p:pipeinfo", Map(), "« content elided »")
  }
}
