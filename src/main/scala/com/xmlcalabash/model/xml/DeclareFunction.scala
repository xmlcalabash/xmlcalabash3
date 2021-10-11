package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig

class DeclareFunction(override val config: XMLCalabashConfig) extends Artifact(config) {
  override protected[model] def makeStructureExplicit(): Unit = {
    for (child <- allChildren) {
      child.makeStructureExplicit()
    }
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child.validateStructure()
    }
  }

  override def toString: String = {
    s"p:declare-function $tumble_id"
  }
}
