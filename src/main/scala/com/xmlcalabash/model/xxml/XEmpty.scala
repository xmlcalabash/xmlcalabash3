package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.SaxonTreeBuilder

class XEmpty(config: XMLCalabash) extends XDataSource(config) {
  def this(parent: XArtifact) = {
    this(parent.config)
    staticContext = parent.staticContext
    this.parent = parent
    _synthetic = true
  }

  override protected[xxml] def validate(): Unit = {
    checkAttributes()
    checkEmptyAttributes()

    for (child <- allChildren) {
      child.validate()
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }
    allChildren = List()
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    dumpTree(sb, "p:empty", Map())
  }
}
