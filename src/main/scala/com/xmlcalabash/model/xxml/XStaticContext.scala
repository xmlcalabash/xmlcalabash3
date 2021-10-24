package com.xmlcalabash.model.xxml

import com.jafpl.graph.Location
import com.jafpl.messages.Message
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.messages.{XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.util.{MinimalStaticContext, URIUtils, Urify, VoidLocation}
import net.sf.saxon.s9api.{QName, XdmValue}

import java.net.URI
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XStaticContext() extends MinimalStaticContext() {
  protected var _config: XMLCalabash = _
  protected var _baseURI: Option[URI] = None
  protected var _location: Option[Location] = None
  protected val _inscopeNamespaces: mutable.HashMap[String, String] = mutable.HashMap.empty[String, String]
  protected var _inscopeConstants: ListBuffer[XNameBinding] = ListBuffer.empty[XNameBinding]

  def this(location: Option[Location], inscopeNamespaces: Map[String,String]) = {
    this()
    _location = location
    _inscopeNamespaces ++= inscopeNamespaces
  }

  def this(location: Location, inscopeNamespaces: Map[String,String]) = this(Some(location), inscopeNamespaces)

  def this(baseURI: URI, inscopeNamespaces: Map[String,String]) = {
    this()
    _baseURI = Some(baseURI)
    _inscopeNamespaces ++= inscopeNamespaces
  }

  def baseURI: Option[URI] = {
    if (_baseURI.isDefined) {
      _baseURI
    } else {
      if (location.isDefined && location.get.uri.isDefined) {
        Some(new URI(Urify.urify(location.get.uri.get)))
      } else {
        Some(URIUtils.cwdAsURI)
      }
    }
  }
  protected[xxml] def baseURI_=(base: URI): Unit = {
    _baseURI = Some(base)
  }

  def location: Option[Location] = _location
  protected[xxml] def location_=(loc: Location): Unit = {
    _location = Some(loc)
  }

  override def inscopeNamespaces: Map[String, String] = _inscopeNamespaces.toMap

  override def inscopeConstants: Map[QName, XNameBinding] = {
    val map = mutable.HashMap.empty[QName, XNameBinding]
    for (opt <- _inscopeConstants) {
      map.put(opt.name, opt)
    }
    map.toMap
  }

  def inscopeConstantBindings: Map[String, Message] = {
    val bindings = mutable.HashMap.empty[String,Message]
    for ((name,value) <- inscopeConstants) {
      bindings.put(name.getClarkName, value.constantValue.get)
    }
    bindings.toMap
  }

  def inscopeConstantValues: Map[QName, XdmValue] = {
    val statics = mutable.HashMap.empty[QName,XdmValue]
    for ((name, value) <- inscopeConstantBindings) {
      val qname = parseClarkName(name)
      value match {
        case msg: XdmNodeItemMessage =>
          statics.put(qname,msg.item)
        case msg: XdmValueItemMessage =>
          statics.put(qname,msg.item)
      }
    }
    statics.toMap
  }
}
