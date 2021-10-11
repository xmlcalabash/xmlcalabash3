package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{XdmAtomicValue, XdmMap, XdmValue}

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{IteratorHasAsJava, SetHasAsScala}

class WwwFormUrlEncode() extends DefaultXmlStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.TEXTRESULT

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val parameters = mapBinding(XProcConstants._parameters)
    val encoded = new StringBuilder()
    var sep = ""

    for (xdmkey <- parameters.keySet().asScala) {
      val name = encode(xdmkey.toString)
      val value = parameters.get(xdmkey)

      encoded.append(sep)
      sep = "&"

      if (value.size() == 0) {
        encoded.append(name)
        encoded.append("=")
      } else if (value.size() == 1) {
        encoded.append(name)
        encoded.append("=")
        encoded.append(encode(value.toString))
      } else {
        var first = true
        val iter = value.iterator()
        while (iter.hasNext) {
          if (!first) {
            encoded.append(sep)
          }
          first = false
          encoded.append(name)
          encoded.append("=")
          encoded.append(encode(iter.next().toString))
        }
      }
    }

    consumer.get.receive("result", encoded.toString(), new XProcMetadata(MediaType.TEXT))
  }

  private def encode(value: String): String = {
    val genDelims = ":/?#[]@"
    val subDelims = "!$'()*,;=" // N.B. no "&" and no "+" !
    val unreserved = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~"
    val okChars = genDelims + subDelims + unreserved

    val encoded = new StringBuilder()
    val bytes = value.getBytes(StandardCharsets.UTF_8)
    for (aByte <- bytes) {
      if (okChars.indexOf(aByte) >= 0) {
        encoded.append(aByte.toChar)
      } else {
        if (aByte == ' ') {
          encoded.append("+")
        } else {
          encoded.append(String.format("%%%02X", aByte))
        }
      }
    }

    encoded.toString
  }
}
