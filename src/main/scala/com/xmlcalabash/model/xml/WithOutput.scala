package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.XdmNode

class WithOutput(override val config: XMLCalabashConfig) extends Port(config) {
  override def parse(node: XdmNode): Unit = {
    throw new RuntimeException("This is a purely synthetic element")
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    // nop
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startWithOutput(tumble_id, tumble_id, port, sequence)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endWithOutput()
  }

  override def toString: String = {
    s"p:with-output $port${if (sequence) "*" else ""}"
  }
}
