package com.xmlcalabash.steps.extension

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.InvalidPathException
import com.jayway.jsonpath.JsonPath.using
import com.jayway.jsonpath.Option
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.runtime.XProcStepConfiguration
import net.minidev.json.JSONAwareEx
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class JsonPath {
    companion object {
        fun evaluate(stepConfig: XProcStepConfiguration, source: XProcDocument, jsonPath: String, options: Set<Option>): XProcDocument {
            // TODO: can this be made more efficient than going through strings?
            val baos = ByteArrayOutputStream()
            val ser = mutableMapOf<QName, XdmValue>()
            ser[Ns.method] = XdmAtomicValue("json")
            ser[Ns.indent] = XdmAtomicValue(true)
            val writer = DocumentWriter(source, baos, ser)
            writer.write()

            val conf = Configuration.builder().options(options).build()
            val json = using(conf).parse(ByteArrayInputStream(baos.toByteArray()))

            try {
                val bais = try {
                    val result = json.read(jsonPath, JSONAwareEx::class.java)
                    if (result == null) {
                        ByteArrayInputStream("null".toByteArray(StandardCharsets.UTF_8))
                    } else {
                        ByteArrayInputStream(result.toJSONString().toByteArray(StandardCharsets.UTF_8))
                    }
                } catch (_: ClassCastException) {
                    // Flipping heck, this is annoying
                    val result = json.read(jsonPath, Object::class.java)
                    when (result) {
                        is Integer, is Double -> ByteArrayInputStream(result.toString().toByteArray(StandardCharsets.UTF_8))
                        is String -> {
                            val quoted = "\"${result.replace("\"", "\\\"")}\""
                            ByteArrayInputStream(quoted.toString().toByteArray(StandardCharsets.UTF_8))
                        }
                        else -> throw stepConfig.exception(XProcError.xcxUnparsableJsonPathResult())
                    }
                }

                val loader = DocumentLoader(stepConfig, null)
                return loader.load(bais, MediaType.JSON, StandardCharsets.UTF_8)
            } catch (ex: Exception) {
                if (ex is InvalidPathException) {
                    throw stepConfig.exception(XProcError.xcxBadJsonPath(jsonPath), ex)
                }
                throw ex
            }
        }
    }
}