package com.xmlcalabash.model.xml

import java.net.URI

import com.xmlcalabash.config.{DocumentRequest, XMLCalabashConfig}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.XdmNode

class Import(override val config: XMLCalabashConfig) extends Artifact(config) {
  private var _href: URI = _

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (attributes.contains(XProcConstants._href)) {
      _href = node.getBaseURI.resolve(attr(XProcConstants._href).get)
    } else {
      throw XProcException.xsMissingRequiredAttribute(XProcConstants._href, location)
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  protected[model] def loadImports(): DeclContainer = {
    if (config.importedURI(_href).isEmpty) {
      var root: XdmNode = null
      try {
        val request = new DocumentRequest(_href, MediaType.XML)
        val response = config.documentManager.parse(request)
        root = S9Api.documentElement(response.value.asInstanceOf[XdmNode]).get
      } catch {
        case ex: XProcException =>
          if (ex.code == XProcException.xd0011) {
            throw XProcException.xsImportFailed(_href, location)
          } else {
            throw ex
          }
      }

      val parser = new Parser(config)

      val declContainer = root.getNodeName match {
        case XProcConstants.p_declare_step =>
          parser.parseDeclareStep(root)
        case XProcConstants.p_library =>
          parser.parseLibrary(root)
        case _ =>
          throw XProcException.xsBadImport(root.getNodeName, location)
      }

      declContainer match {
        case decl: DeclareStep =>
          if (decl.stepType.isEmpty) {
            throw XProcException.xsStepTypeRequired(location)
          }
        case _ => ()
      }

      config.addImportedURI(_href, declContainer)

      declContainer.loadImports()
      declContainer
    } else {
      config.importedURI(_href).get
    }
  }

  override def toString: String = {
    s"p:import $tumble_id ${_href}"
  }
}
