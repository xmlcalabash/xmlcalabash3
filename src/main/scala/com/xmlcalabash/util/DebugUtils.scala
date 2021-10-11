package com.xmlcalabash.util

import net.sf.saxon.s9api.{Axis, XdmAtomicValue, XdmNode, XdmNodeKind, XdmValue}

object DebugUtils {
  def dumpTreeLocations(node: XdmNode): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        val diter = node.axisIterator(Axis.CHILD)
        while (diter.hasNext) {
          dumpTreeLocations(diter.next())
        }
      case XdmNodeKind.ELEMENT =>
        println(s"${node.getNodeName}: ${node.getBaseURI}:${node.getLineNumber}")
        val diter = node.axisIterator(Axis.CHILD)
        while (diter.hasNext) {
          dumpTreeLocations(diter.next())
        }
      case _ => ()
    }
  }

}
