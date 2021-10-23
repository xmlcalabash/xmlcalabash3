package com.xmlcalabash.model.xml

import com.xmlcalabash.XMLCalabash

abstract class ForContainer(override val config: XMLCalabash) extends Container(config) {

  protected def setupLoopInputs(primary: Option[Boolean]): Unit = {
    val first = firstChild

    if (primary.isDefined) {
      if (firstWithInput.isEmpty) {
        val input = new WithInput(config)
        input.port = "source"
        input.primary = primary.get
        addChild(input, first)
      }
    }

    val current = new DeclareInput(config)
    current.port = "current"
    current.primary = true
    addChild(current, first)

    makeContainerStructureExplicit()
  }

}
