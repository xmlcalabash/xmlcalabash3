package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.params.ContentTypeCheckerParams
import com.xmlcalabash.runtime.{StepProxy, XMLCalabashRuntime}

class XContentTypeChecker(parentStep: XArtifact, xport: XPort) extends XAtomicStep(parentStep.config, XProcConstants.cx_content_type_checker) {
  staticContext = parentStep.staticContext
  _synthetic = true
  parent = parentStep

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val inputPort = xport match {
      case _: XInput => true
      case _: XWithInput => true
      case _ => false
    }

    val params = new ContentTypeCheckerParams(xport.port, xport.contentTypes, parentStep.staticContext,
      xport.select, inputPort, xport.sequence)

    val start = parent.asInstanceOf[ContainerStart]
    val impl = stepImplementation
    impl.configure(config, stepType, name, Some(params))

    val proxy = new StepProxy(runtime, stepType, impl, staticContext)
    runtime.addNode(this, start.addAtomic(proxy, "cx:content-type-checker"))
  }
}
