package com.xmlcalabash.model.xml

import java.net.URI
import com.jafpl.graph.Location
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.StaticContext
import com.xmlcalabash.util.{MediaType, TypeUtils, ValueTemplateParser}
import net.sf.saxon.expr.parser.ExpressionTool
import net.sf.saxon.s9api.{ItemType, QName, SaxonApiException, SequenceType, XdmAtomicValue}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object XMLContext {
  private val contextFunctions =
    List(XProcConstants.p_iteration_size, XProcConstants.p_iteration_position,
      XProcConstants.fn_collection, XProcConstants._collection)
}

class XMLContext(override val config: XMLCalabashConfig, override val artifact: Option[Artifact]) extends StaticContext(config, artifact) {
  def this(config: XMLCalabashConfig, artifact: Artifact) = {
    this(config, Some(artifact))
  }

  def this(config: XMLCalabashConfig) = {
    this(config, None)
  }

  def this(config: XMLCalabashConfig, artifact: Artifact, baseURI: Option[URI], ns: Map[String,String], location: Option[Location]) = {
    this(config, Some(artifact))
    _baseURI = baseURI
    _inScopeNS = ns
    _location = location
  }

  def this(config: XMLCalabashConfig, baseURI: Option[URI], ns: Map[String,String], location: Option[Location]) = {
    this(config, None)
    _baseURI = baseURI
    _inScopeNS = ns
    _location = location
  }

  val typeUtils = new TypeUtils(config, this)

  def parseBoolean(value: Option[String]): Option[Boolean] = {
    if (value.isDefined) {
      if (value.get == "true" || value.get == "false") {
        Some(value.get == "true")
      } else {
        throw XProcException.xsBadTypeValue(value.get, "boolean", location)
      }
    } else {
      None
    }
  }

  def parseQName(name: String): QName = {
    parseQName(Some(name)).get
  }

  def parseQName(name: Option[String]): Option[QName] = {
    if (name.isDefined) {
      val eqname = "^Q\\{(.*)\\}(\\S+)$".r
      val qname = name.get match {
        case eqname(uri,local) => Some(new QName(uri, local))
        case _ =>
          if (name.get.contains(":")) {
            val pos = name.get.indexOf(':')
            val prefix = name.get.substring(0, pos)
            val local = name.get.substring(pos+1)
            if (nsBindings.contains(prefix)) {
              Some(new QName(prefix, nsBindings(prefix), local))
            } else {
              throw XProcException.xdCannotResolveQName(name.get, location)
            }
          } else {
            Some(new QName("", name.get))
          }
      }

      val prefix = qname.get.getPrefix
      val local = qname.get.getLocalName
      if (prefix != null && !"".equals(prefix)) {
        if (parseNCName(Some(prefix)).isDefined && parseNCName(Some(local)).isDefined) {
          return qname
        }
      } else {
        if (parseNCName(Some(local)).isDefined) {
          return qname
        }
      }
      // This will have already happened in the calls to parseNCName above, but
      // putting it here satisfies the compiler.
      throw XProcException.xsBadTypeValue(name.get, "NCName", location)
    } else {
      None
    }
  }

  def parseNCName(name: Option[String]): Option[String] = {
    if (name.isDefined) {
      try {
        val typeUtils = new TypeUtils(config)
        val ncname = typeUtils.castAtomicAs(new XdmAtomicValue(name.get), ItemType.NCNAME, null)
        Some(ncname.getStringValue)
      } catch {
        case _: SaxonApiException =>
          throw XProcException.xsBadTypeValue(name.get, "NCName", location)
        case e: Exception =>
          throw e
      }
    } else {
      None
    }
  }

  def parseContentTypes(ctypes: Option[String]): List[MediaType] = {
    if (ctypes.isDefined) {
      try {
        MediaType.parseList(ctypes.get).toList
      } catch {
        case ex: XProcException =>
          if (ex.code == XProcException.xc0070) {
            // Map to the static error...
            throw XProcException.xsUnrecognizedContentTypeShortcut(ex.details.head.toString, ex.location)
          } else {
            throw ex
          }
      }
    } else {
      List.empty[MediaType]
    }
  }

  def parseSequenceType(seqType: Option[String]): Option[SequenceType] = {
    typeUtils.parseSequenceType(seqType)
  }

  def parseAvt(value: String): List[String] = {
    val parser = new ValueTemplateParser(value)
    parser.template()
  }

  def findVariableRefsInAvt(list: List[String]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    var avt = false
    for (substr <- list) {
      if (avt) {
        variableRefs ++= findVariableRefsInString(substr)
      }
      avt = !avt
    }

    variableRefs.toSet
  }

  def findVariableRefsInString(text: String): Set[QName] = {
    findVariableRefsInString(Some(text))
  }

  def findVariableRefsInString(text: Option[String]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    if (text.isDefined) {
      val parser = config.expressionParser
      parser.parse(text.get)
      for (ref <- parser.variableRefs) {
        val qname = ValueParser.parseQName(ref, this)
        variableRefs += qname
      }
    }

    variableRefs.toSet
  }

  def findFunctionRefsInAvt(list: List[String]): Set[QName] = {
    val functionRefs = mutable.HashSet.empty[QName]

    var avt = false
    for (substr <- list) {
      if (avt) {
        functionRefs ++= findFunctionRefsInString(substr)
      }
      avt = !avt
    }

    functionRefs.toSet
  }

  def findFunctionRefsInString(text: String): Set[QName] = {
    findFunctionRefsInString(Some(text))
  }

  def findFunctionRefsInString(text: Option[String]): Set[QName] = {
    val functionRefs = mutable.HashSet.empty[QName]

    if (text.isDefined) {
      val parser = config.expressionParser
      parser.parse(text.get)
      for (ref <- parser.functionRefs) {
        val qname = ValueParser.parseQName(ref, this)
        functionRefs += qname
      }
    }

    functionRefs.toSet
  }

  def dependsOnContextAvt(list: List[String]): Boolean = {
    var depends = false

    var avt = false
    for (substr <- list) {
      if (avt) {
        depends = depends || dependsOnContextString(substr)
      }
      avt = !avt
    }

    depends
  }

  def dependsOnContextString(expr: String): Boolean = {
    var depends = false
    for (func <- findFunctionRefsInString(expr)) {
      depends = depends || XMLContext.contextFunctions.contains(func)
    }
    val vars = findVariableRefsInString(expr)

    if (!depends) {
      val xcomp = config.processor.newXPathCompiler()
      for ((prefix, uri) <- nsBindings) {
        xcomp.declareNamespace(prefix, uri)
      }
      for (name <- vars) {
        xcomp.declareVariable(name)
      }
      val xexec = xcomp.compile(expr)
      val xexpr = xexec.getUnderlyingExpression.getInternalExpression
      ExpressionTool.dependsOnFocus(xexpr)
    } else {
      true
    }
  }

  object StateChange {
    val STRING = 0
    val EXPR = 1
    val SQUOTE = 2
    val DQUOTE = 3
  }
}
