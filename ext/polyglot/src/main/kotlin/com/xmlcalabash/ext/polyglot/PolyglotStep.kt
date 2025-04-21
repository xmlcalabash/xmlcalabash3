package com.xmlcalabash.ext.polyglot

import com.xmlcalabash.documents.DocumentProperties
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.DocumentLoader
import com.xmlcalabash.io.DocumentWriter
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.*
import net.sf.saxon.value.*
import org.apache.logging.log4j.kotlin.logger
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Value
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class PolyglotStep(val stepLanguage: String): AbstractAtomicStep() {
    lateinit var jscontext: Context
    lateinit var language: String

    override fun run() {
        super.run()

        language = when (stepLanguage) {
            "javascript" -> "js"
            "dynamic" -> stringBinding(QName("language"))!!
            else -> stepLanguage
        }

        if ((queues["source"]?.size ?: 0) > 1) {
            throw stepConfig.exception(XProcError.xiTooManySources(queues["source"]!!.size))
        }

        val source = queues["source"]?.firstOrNull()
        val program = queues["program"]!!.first().value.underlyingValue.stringValue
        val variables = qnameMapBinding(Ns.parameters)

        val ctype = options[Ns.resultContentType]?.value
        val resultContentType = if (ctype == null || ctype === XdmEmptySequence.getInstance()) {
            null
        } else {
            mediaTypeBinding(Ns.resultContentType)
        }

        val args = mutableListOf<String>()
        args.add(stepConfig.baseUri?.toString() ?: "")
        if (options[Ns.args] != null) {
            val argsvalue = options[Ns.args]!!.value
            for (arg in options[Ns.args]!!.value) {
                args.add(arg.toString())
            }
        }

        val inputStream = if (source == null) {
            ByteArrayInputStream("".toByteArray(StandardCharsets.UTF_8))
        } else {
            val baos = ByteArrayOutputStream()
            DocumentWriter(source, baos).write()
            ByteArrayInputStream(baos.toByteArray())
        }
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()

        jscontext = Context.newBuilder(language)
            .allowHostClassLookup { true }
            .arguments(language, args.toTypedArray())
            .`in`(inputStream)
            .out(outputStream)
            .err(errorStream)
            .allowHostAccess(HostAccess.ALL).build()

        try {
            val jsbindings = jscontext.getBindings(language)
            for ((name, value) in variables) {
                if (name.namespaceUri == NamespaceUri.NULL) {
                    jsbindings.putMember(name.localName, convertToValue(value))
                } else {
                    stepConfig.warn { "Unexpected parameter: ${name} on ${stepParams.stepType}/${stepParams.stepName}" }
                }
            }

            val jsvalue = try {
                jscontext.parse(language, program)
            } catch (ex: Exception) {
                if (ex is PolyglotException) {
                    throw stepConfig.exception(XProcError.xdStepFailed("error interpreting ${language} input"))
                }
                throw ex
            }

            if (!jsvalue.canExecute()) {
                throw stepConfig.exception(XProcError.xdStepFailed("${language} input was not executable"))
            }

            if (resultContentType != null) {
                jsvalue.executeVoid()

                messages(String(errorStream.toByteArray()), true)

                val properties = DocumentProperties()
                properties[Ns.contentType] = resultContentType
                val loader = DocumentLoader(stepConfig, null, properties, emptyMap())
                val doc = loader.load(ByteArrayInputStream(outputStream.toByteArray()), resultContentType)
                receiver.output("result", doc)
            } else {
                val value = jsvalue.execute()

                messages(String(errorStream.toByteArray()), true)
                messages(String(outputStream.toByteArray()), false)

                logger.debug { "Polyglot return value is ${value}" }
                val result = convertFromValue(value)
                receiver.output("result", XProcDocument.ofValue(result, stepConfig, resultContentType))
            }
        } catch (ex: Exception) {
            when (ex) {
                is XProcException -> throw ex
                is PolyglotException -> throw stepConfig.exception(XProcError.xdStepFailed("${language} evaluation failed or signaled an error"))
                else -> throw stepConfig.exception(XProcError.xdStepFailed(ex.message ?: "(no explanation"), ex)
            }
        } finally {
            jscontext.close()
        }
    }

    override fun toString(): String {
        if (language == "js") {
            return "cx:javascript"
        } else {
            return "cx:${language}"
        }
    }

    private fun messages(text: String, warning: Boolean) {
        for (line in text.split("\n+".toRegex())) {
            if (line != "") {
                if (warning) {
                    stepConfig.warn { line }
                } else {
                    stepConfig.info { line }
                }
            }
        }

    }

    private fun convertToValue(xdmValue: XdmValue): Value {
        if (xdmValue === XdmEmptySequence.getInstance()) {
            return jscontext.asValue(null)
        }

        when (xdmValue) {
            is XdmAtomicValue -> {
                val uvalue = xdmValue.underlyingValue
                when (uvalue) {
                    is StringValue -> return jscontext.asValue(uvalue.stringValue)
                    is IntegerValue -> return jscontext.asValue(uvalue.longValue())
                    is DoubleValue -> return jscontext.asValue(uvalue.doubleValue)
                    is FloatValue -> return jscontext.asValue(uvalue.floatValue)
                    is BooleanValue -> return jscontext.asValue(uvalue.booleanValue)
                    is DecimalValue -> return jscontext.asValue(uvalue.decimalValue)
                    is DateTimeValue -> return jscontext.asValue(uvalue.toZonedDateTime())
                    is DateValue -> return jscontext.asValue(uvalue.toLocalDate())
                    is QNameValue -> return jscontext.asValue(uvalue.eqName)
                    else -> {
                        stepConfig.warn { "Unexpected atomic value: ${uvalue}" }
                        return jscontext.asValue(uvalue.toString())
                    }
                }
            }
            is XdmMap -> {
                val map = mutableMapOf<String, Value>()
                for ((key, xvalue) in xdmValue.entrySet()) {
                    val name = key.stringValue
                    val value = convertToValue(xvalue)
                    map[name] = value
                }
                return jscontext.asValue(map)
            }
            is XdmArray -> {
                return jscontext.asValue(xdmValue.asList().toTypedArray())
            }
            is XdmNode -> {
                val tempDoc = XProcDocument.ofXml(xdmValue, stepConfig)
                val baos = ByteArrayOutputStream()
                val writer = DocumentWriter(tempDoc, baos)
                writer[Ns.omitXmlDeclaration] = true
                writer.write()
                return jscontext.asValue(baos.toString(StandardCharsets.UTF_8))
            }
            else -> {
                stepConfig.warn { "Unexpected value: ${xdmValue}" }
                return jscontext.asValue(xdmValue.underlyingValue.stringValue)
            }
        }
    }

    private fun convertFromValue(value: Value, seen: MutableSet<Value> = mutableSetOf()): XdmValue {
        // The Gradle engine supports "isDuration()" and "isTime()" but I haven't worked
        // out how to get them back from the host language so they're unsupported at the moment.

        if (value.isBoolean) {
            return XdmAtomicValue(value.asBoolean())
        } else if (value.isInstant) {
            return XdmAtomicValue(value.asInstant())
        } else if (value.isDate) {
            return XdmAtomicValue(value.asDate())
        } else if (value.isException) {
            throw value.throwException()
        } else if (value.isNull) {
            return XdmEmptySequence.getInstance()
        } else if (value.isNumber) {
            if (value.fitsInLong()) {
                return XdmAtomicValue(value.asLong())
            } else {
                return XdmAtomicValue(value.asDouble())
            }
        } else if (value.isString) {
            return XdmAtomicValue(value.asString())
        }

        if (value.hasArrayElements()) {
            var array = XdmArray()
            for (index in 0 ..< value.arraySize) {
                val avalue = value.getArrayElement(index)
                if (avalue !in seen) {
                    seen.add(avalue)
                    array = array.addMember(convertFromValue(avalue, seen))
                }
            }
            return array
        }

        if (value.hasHashEntries()) {
            var map = XdmMap()
            val hashKeysIterator = value.hashKeysIterator
            while (hashKeysIterator.hasIteratorNextElement()) {
                val keyValue = hashKeysIterator.iteratorNextElement
                val entryValue = value.getHashValue(keyValue)
                if (keyValue !in seen && entryValue !in seen) {
                    seen.add(keyValue)
                    val key = convertFromValue(keyValue, seen)
                    if (key is XdmAtomicValue) {
                        seen.add(entryValue)
                        val entry = convertFromValue(entryValue, seen)
                        map = map.put(key, entry)
                    }
                }

            }
            return map
        }

        if (value.hasMembers()) {
            var map = XdmMap()
            for (key in value.memberKeys) {
                val kvalue = value.getMember(key)
                if (kvalue !in seen) {
                    seen.add(kvalue)
                    val xvalue = convertFromValue(kvalue, seen)
                    map = map.put(XdmAtomicValue(key), xvalue)
                }
            }
            return map
        }

        stepConfig.warn { "Unconvertable value: ${value}" }
        return XdmAtomicValue(value.toString())
    }
}