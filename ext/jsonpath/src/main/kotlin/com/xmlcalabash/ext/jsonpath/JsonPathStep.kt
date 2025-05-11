package com.xmlcalabash.ext.jsonpath

import com.jayway.jsonpath.Option
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.steps.AbstractAtomicStep
import net.sf.saxon.s9api.QName

class JsonPathStep(): AbstractAtomicStep() {
    companion object {
        private val _query = QName("query")
        private val _asPathList = QName("as-path-list")
        private val _defaultPathLeafToNull = QName("default-path-leaf-to-null")
        private val _alwaysReturnList = QName("always-return-list")
        private val _suppressExceptions = QName("suppress-exceptions")
        private val _requireProperties = QName("require-properties")
    }

    override fun run() {
        super.run()

        val source = queues["source"]!!.first()
        val path = stringBinding(_query)!!
        val parameters = qnameMapBinding(Ns.parameters)

        val options = mutableSetOf<Option>()
        for ((key, value) in parameters) {
            if (value.underlyingValue.effectiveBooleanValue()) {
                when (key) {
                    _asPathList -> options.add(Option.AS_PATH_LIST)
                    _defaultPathLeafToNull -> options.add(Option.DEFAULT_PATH_LEAF_TO_NULL)
                    _alwaysReturnList -> options.add(Option.ALWAYS_RETURN_LIST)
                    _suppressExceptions -> options.add(Option.SUPPRESS_EXCEPTIONS)
                    _requireProperties -> options.add(Option.REQUIRE_PROPERTIES)
                    else -> throw stepConfig.exception(XProcError.xcxUnrecognizedJsonPathOption(key))
                }
            }
        }

        val doc = JsonPath.evaluate(stepConfig, source, path, options)
        receiver.output("result", doc)
    }

    override fun toString(): String = "cx:jsonpath"
}