package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StepProxy, XMLCalabashRuntime}

class XDocumentLoader(doc: XDocument, parentStep: XArtifact) extends XAtomicStep(doc.config, if (doc.contextDependent) { XProcConstants.cx_document_loader_vt } else { XProcConstants.cx_document_loader }) {
  staticContext = doc.staticContext
  _synthetic = true
  parent = parentStep

  allChildren = doc.allChildren
  if (doc.contextDependent) {
    val xwi = new XWithInput(this, "source")
    xwi.drp = doc.drp
    doc.allChildren = List()
    addChild(xwi)
  }

  val output = new XWithOutput(this, "result")
  output.validate()
  addChild(output)

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    val impl = stepImplementation
    val params = Some(doc.loaderParams)
    impl.configure(config, stepType, name, params)

    val proxy = new StepProxy(runtime, stepType, impl, staticContext)
    runtime.addNode(this, start.addAtomic(proxy, stepType.toString))
  }
}
