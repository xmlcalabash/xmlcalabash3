package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.params.InlineLoaderParams
import com.xmlcalabash.runtime.{StepProxy, XMLCalabashRuntime}

class XInlineLoader(inline: XInline, parentStep: XArtifact)
  extends XAtomicStep(inline.config, if (inline.contextDependent) { XProcConstants.cx_inline_loader_vt } else { XProcConstants.cx_inline_loader }) {
  staticContext = inline.staticContext
  _synthetic = true
  parent = parentStep

  allChildren = inline.allChildren
  if (inline.contextDependent) {
    val xwi = new XWithInput(this, "source")
    xwi.drp = inline.drp
    inline.allChildren = List()
    addChild(xwi)
  }

  val output = new XWithOutput(this, "result")
  output.validate()
  addChild(output)

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val xwi = children[XWithInput] find { _.port == "source" }
    val pipe = if (xwi.isDefined) {
      xwi.get.children[XPipe].headOption
    } else {
      None
    }

    val params = new InlineLoaderParams(inline.content, inline.contentType, inline.documentProperties,
      inline.encoding, inline.excludeURIs, inline.expandText, pipe.isDefined, staticContext)

    val start = parent.asInstanceOf[ContainerStart]
    val impl = stepImplementation
    impl.configure(config, stepType, name, Some(params))

    val proxy = new StepProxy(runtime, stepType, impl, staticContext)
    runtime.addNode(this, start.addAtomic(proxy, "p:inline"))
  }
}
