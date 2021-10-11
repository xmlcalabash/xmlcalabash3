package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}

class PipeInfo(override val config: XMLCalabashConfig, val info: XdmNode) extends Port(config) {
  override protected[model] def validateStructure(): Unit = {
    // nop
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    var root = Option.empty[QName]
    val iter = info.axisIterator(Axis.CHILD)
    while (root.isEmpty && iter.hasNext) {
      val item = iter.next()
      if (item.getNodeKind == XdmNodeKind.ELEMENT) {
        root = Some(item.getNodeName)
      }
    }

    xml.startPipeInfo(tumble_id, root)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endPipeInfo()
  }
  override def toString: String = {
    s"p:pipeinfo $tumble_id"
  }
}
