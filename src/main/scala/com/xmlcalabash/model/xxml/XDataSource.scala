package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.QName

import scala.collection.mutable.ListBuffer

abstract class XDataSource(config: XMLCalabash) extends XArtifact(config) {
  protected var _drp: Option[XPort] = None

  protected[xxml] def drp: Option[XPort] = _drp

  protected def parseContentType(): Option[MediaType] = {
    try {
      val contentType = staticContext.parseSingleContentType(attr(XProcConstants._content_type))
      if (contentType.isDefined) {
        contentType.get.assertValid
        return contentType
      }
    } catch {
      case ex: XProcException =>
        error(ex)
    }

    None
  }

  protected def resolveBindings(contextDependent: Boolean, refs: Set[QName], context: XNameBindingContext): Unit = {
    if (!contextDependent) {
      _drp = None
    }

    val namepipe = ListBuffer.empty[XNameBinding]
    for (ref <- refs) {
      val cbind = context.inScopeConstants.get(ref)
      val dbind = context.inScopeDynamics.get(ref)
      if (cbind.isDefined) {
        // ok
      } else if (dbind.isDefined) {
        dbind.get match {
          case v: XVariable =>
            namepipe += v
          case o: XOption =>
            namepipe += o
          case _ =>
            error(XProcException.xiThisCantHappen(s"Unexpected name binding: ${dbind.get}"))
        }
      } else {
        error(XProcException.xsNoBindingInExpression(ref, None))
      }
    }

    if (namepipe.nonEmpty) {
      val xwi = new XWithInput(this, "#bindings", false, true, MediaType.MATCH_ANY)
      for (binding <- namepipe) {
        val xstep = context.inScopeDynamics.get(binding.name)
        val pipe = new XPipe(xwi, xstep.get.tumble_id, "result")
        xwi.addChild(pipe)
      }
      addChild(xwi)
    }
  }

  override protected[xxml] def elaborateDefaultReadablePort(initial: Option[XPort]): Option[XPort] = {
    _drp = initial
    super.elaborateDefaultReadablePort(initial)
  }

  override protected[xxml] def elaboratePortConnections(): Unit = {
    if (drp.isDefined && children[XDataSource].isEmpty) {
      addChild(new XPipe(drp.get))
    }
  }
}
