package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.runtime.{ImplParams, XMLCalabashRuntime}

class AtomicLoader(override val config: XMLCalabash, params: Option[ImplParams]) extends AtomicStep(config, params) {

  def this(config: XMLCalabash, params: ImplParams, context: Artifact) = {
    this(config, Some(params))
    _inScopeStatics = context._inScopeStatics
    _inScopeDynamics = context._inScopeDynamics
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    super.graphEdges(runtime, parent)

    for ((name, np) <- _inScopeDynamics) {
      val binding = findInScopeOption(name)
      if (binding.isDefined) {
        // Can be undefined if it's on an atomic step
        val pipe = new NamePipe(config, name, this.tumble_id, binding.get)
        pipe.graphEdges(runtime, _graphNode.get)
      }
    }
  }
}
