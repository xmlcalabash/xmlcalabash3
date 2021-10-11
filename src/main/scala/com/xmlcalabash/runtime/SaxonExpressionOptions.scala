package com.xmlcalabash.runtime

import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.s9api.QName

import scala.collection.mutable

class SaxonExpressionOptions private(map: Option[Map[String,Any]]) {
  private val opts = mutable.HashMap.empty[String, Any]

  if (map.isDefined) {
    for (key <- map.get.keySet) {
      val value = map.get(key)
      key match {
        case "collection" => opts.put(key, checkBoolean(key, value))
        case "optiondecl" => opts.put(key, checkBoolean(key, value))
        case "inj.elapsed" => opts.put(key, checkDouble(key, value))
        case "inj.id" => opts.put(key, value.toString)
        case "inj.name" => opts.put(key, value.toString)
        case "inj.type" => opts.put(key, checkQName(key, value))
      }
    }
  }

  def this() = {
    this(None)
  }

  def this(map: Map[String,Any]) = {
    this(Some(map))
  }

  def contextCollection: Option[Boolean] = optBoolean("collection")
  def contextCollection_=(coll: Boolean): Unit = {
    opts.put("collection", coll)
  }

  def optionDecl: Option[Boolean] = optBoolean("optiondecl")
  def optionDecl_=(decl: Boolean): Unit = {
    opts.put("optiondecl", decl)
  }

  def inj_elapsed: Option[Double] = optDouble("inj.elapsed")
  def inj_elapsed_=(ms: Double): Unit = {
    opts.put("inj.elapsed", ms)
  }

  def inj_id: Option[String] = optString("inj.id")
  def inj_id_=(id: String): Unit = {
    opts.put("inj.id", id)
  }

  def inj_name: Option[String] = optString("inj.name")
  def inj_name_=(name: String): Unit = {
    opts.put("inj.name", name)
  }

  def inj_type: Option[QName] = optQName("inj.type")
  def inj_type_=(stepType: QName): Unit = {
    opts.put("inj.type", stepType)
  }

  // ========================================================================================================

  private def optString(key: String): Option[String] = {
    if (opts.contains(key)) {
      Some(opts(key).asInstanceOf[String])
    } else {
      None
    }
  }

  private def optBoolean(key: String): Option[Boolean] = {
    if (opts.contains(key)) {
      Some(opts(key).asInstanceOf[Boolean])
    } else {
      None
    }
  }

  private def checkBoolean(key: String, value: Any): Boolean = {
    value match {
      case b: Boolean => b
      case _ => throw XProcException.xdBadValue(value.toString, "boolean", None)
    }
  }

  private def optDouble(key: String): Option[Double] = {
    if (opts.contains(key)) {
      Some(opts(key).asInstanceOf[Double])
    } else {
      None
    }
  }

  private def checkDouble(key: String, value: Any): Double = {
    value match {
      case d: Double => d
      case _ => throw XProcException.xdBadValue(value.toString, "double", None)
    }
  }

  private def optQName(key: String): Option[QName] = {
    if (opts.contains(key)) {
      Some(opts(key).asInstanceOf[QName])
    } else {
      None
    }
  }

  private def checkQName(key: String, value: Any): QName = {
    value match {
      case q: QName => q
      case _ => throw XProcException.xdBadValue(value.toString, "QName", None)
    }
  }
}
