package com.xmlcalabash.model.xxml

import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}

import java.net.URI
import scala.collection.mutable

class XImport(config: XMLCalabash, val library: XLibrary) extends XArtifact(config) {
  private var _href: URI = _

  override protected[xxml] def checkAttributes(): Unit = {
    super.checkAttributes()
    try {
      if (attributes.contains(XProcConstants._href)) {
        _href = staticContext.baseURI.get.resolve(attr(XProcConstants._href).get)
      } else {
        error(XProcException.xsMissingRequiredAttribute(XProcConstants._href, None))
      }
    } catch {
      case ex: Exception =>
        error(ex)
    }
  }

  override protected[xxml] def validate(): Unit = {
    for (child <- allChildren) {
      child match {
        case _: XPipeinfo => ()
        case _: XDocumentation => ()
        case _ =>
          error(XProcException.xsElementNotAllowed(child.nodeName, None))
      }
    }
    allChildren = List()

    val cont = ancestor[XDeclContainer].get
    for (step <- library.inScopeSteps) {
      if (step.stepType.isDefined) {
        for (istep <- cont.inScopeSteps) {
          if (istep.stepType.isDefined && istep.stepType.get == step.stepType.get) {
            throw XProcException.xsDupStepType(step.stepType.get, istep.location)
          }
        }
      }
    }
    cont.addInScopeSteps(library.inScopeSteps)
  }

  override def dumpTree(sb: SaxonTreeBuilder): Unit = {
    val attr = mutable.HashMap.empty[String, Option[Any]]
    attr.put("href", Option(_href))
    dumpTree(sb, "p:import", attr.toMap)
  }
}
