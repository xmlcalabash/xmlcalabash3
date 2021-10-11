package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}

class Documentation(override val config: XMLCalabashConfig, val docs: XdmNode) extends Artifact(config) {
  override protected[model] def validateStructure(): Unit = {
    // nop
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    var root = Option.empty[QName]
    val iter = docs.axisIterator(Axis.CHILD)
    while (root.isEmpty && iter.hasNext) {
      val item = iter.next()
      if (item.getNodeKind == XdmNodeKind.ELEMENT) {
        root = Some(item.getNodeName)
      }
    }

    xml.startDocumentation(tumble_id, root)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endDocumentation()
  }

  override def toString: String = {
    s"p:documentation $tumble_id"
  }
}
