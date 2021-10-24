package com.xmlcalabash.model.xxml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XMLCalabashRuntime
import net.sf.saxon.s9api.QName

class XForLoop(config: XMLCalabash) extends XLoopingStep(config) {
  private val _from = new QName("from")
  private val _to = new QName("to")
  private val _by = new QName("by")
  private var countFrom = 1L
  private var countTo = 0L
  private var countBy = 1L

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()

    if (attributes.contains(_from)) {
      countFrom = attr(_from).get.toLong
    }

    if (attributes.contains(_to)) {
      countTo = attr(_to).get.toLong
    } else {
      throw new RuntimeException("to is required")
    }

    if (attributes.contains(_by)) {
      countBy = attr(_by).get.toLong
    }

    if (countBy == 0) {
      throw XProcException.xiAttempToCountByZero(location)
    }

    if ((countFrom > countTo && countBy > 0)
      || (countFrom < countTo && countBy < 0)) {
      logger.debug(s"Counting from $countFrom to $countTo by $countBy will never execute")
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    val node = start.addFor(stepName, countFrom, countTo, countBy, containerManifold)
    runtime.addNode(this, node)
    super.graphNodes(runtime, node)
  }
}
