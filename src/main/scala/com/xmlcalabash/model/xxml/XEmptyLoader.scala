package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StepProxy, XMLCalabashRuntime}
import com.xmlcalabash.runtime.params.EmptyLoaderParams

class XEmptyLoader(parentStep: XArtifact) extends XAtomicStep(parentStep.config, XProcConstants.cx_empty_loader) {
  staticContext = parentStep.staticContext
  _synthetic = true
  parent = parentStep

  val output = new XWithOutput(this, "result")
  output.validate()
  addChild(output)

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val params = new EmptyLoaderParams(staticContext)
    val start = parent.asInstanceOf[ContainerStart]
    val impl = stepImplementation
    impl.configure(config, stepType, name, Some(params))

    val proxy = new StepProxy(runtime, stepType, impl, staticContext)
    runtime.addNode(this, start.addAtomic(proxy, "p:empty"))
  }
}
