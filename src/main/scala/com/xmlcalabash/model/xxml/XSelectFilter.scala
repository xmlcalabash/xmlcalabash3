package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.params.{InlineLoaderParams, SelectFilterParams}
import com.xmlcalabash.runtime.{StepProxy, XMLCalabashRuntime}

class XSelectFilter(parentStep: XArtifact, filterStep: XStep, input: XPort) extends XAtomicStep(filterStep.config, XProcConstants.cx_select_filter) {
  staticContext = filterStep.staticContext
  _synthetic = true
  parent = parentStep

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val params = new SelectFilterParams(filterStep.staticContext, input.select.get, input.port, input.sequence)

    val start = parent.asInstanceOf[ContainerStart]
    val impl = stepImplementation
    impl.configure(config, stepType, name, Some(params))

    val proxy = new StepProxy(runtime, stepType, impl, staticContext)
    runtime.addNode(this, start.addAtomic(proxy, "cx:select-filter"))
  }
}
