package com.xmlcalabash.model.xxml

import com.jafpl.graph.Node
import com.xmlcalabash.runtime.XMLCalabashRuntime

trait XGraphableArtifact {
  def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit
}
