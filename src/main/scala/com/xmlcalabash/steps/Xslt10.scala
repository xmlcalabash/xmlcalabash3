package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.util.Xslt10Source

import java.io.OutputStream

class Xslt10(loader: ClassLoader) {
  def transform(document: Xslt10Source, stylesheet: Xslt10Source, stream: OutputStream, params: Map[String, String]): Unit = {
    val factoryClass = loader.loadClass("com.icl.saxon.TransformerFactoryImpl")
    val streamResultClass = loader.loadClass("javax.xml.transform.stream.StreamResult")
    val sourceClass = loader.loadClass("javax.xml.transform.Source")
    val resultClass = loader.loadClass("javax.xml.transform.Result")

    val factory = factoryClass.getDeclaredConstructor().newInstance()

    try {
      val newTransformerMethod = factory.getClass.getMethod("newTransformer", sourceClass)
      val transformer = newTransformerMethod.invoke(factory, stylesheet.source)

      val setParameterMethod = transformer.getClass.getMethod("setParameter", classOf[String], classOf[Any])
      for ((key, value) <- params) {
        setParameterMethod.invoke(transformer, key, value)
      }

      val streamResult = streamResultClass.getDeclaredConstructor(classOf[OutputStream]).newInstance(stream)

      val transformMethod = transformer.getClass.getMethod("transform", sourceClass, resultClass)

      transformMethod.invoke(transformer, document.source, streamResult)
    } catch {
      case ex: Exception =>
        val cause = ex.getCause
        if (Option(cause).isDefined) {
          val message = Option(cause.getMessage).getOrElse("")
          if (message.contains("Processing terminated by xsl:message")) {
            throw XProcException.xcXsltUserTermination(cause.getMessage, None)
          }
          if (message.contains("Failed to compile")) {
            throw XProcException.xcXsltCompileError(message, cause.asInstanceOf[Exception], None)
          }
        }

        throw ex
    }
  }
}
