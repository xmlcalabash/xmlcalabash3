package com.xmlcalabash.steps

import com.xmlcalabash.api.XProcStep
import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.namespace.NsXmlns
import com.xmlcalabash.runtime.LazyValue
import com.xmlcalabash.runtime.ProcessMatch
import com.xmlcalabash.runtime.ProcessMatchingNodes
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.runtime.api.Receiver
import com.xmlcalabash.runtime.parameters.RuntimeStepParameters
import com.xmlcalabash.util.Urify
import net.sf.saxon.om.NamespaceMap
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.s9api.*
import net.sf.saxon.value.QNameValue
import net.sf.saxon.value.StringValue
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

abstract class AbstractAtomicStep(): XProcStep {
    companion object {
        private var _id: Long = 0
        private var vara = QName("a")
        private var varb = QName("b")
    }

    private lateinit var _stepConfig: XProcStepConfiguration
    private lateinit var _receiver: Receiver
    private lateinit var _stepParams: RuntimeStepParameters
    internal val _options = mutableMapOf<QName, LazyValue>()
    internal val _queues: ConcurrentMap<String, List<XProcDocument>>
    private var _nodeId: Long = -1

    init {
        _queues = ConcurrentHashMap()
    }

    val stepParams: RuntimeStepParameters
        get() = _stepParams
    val stepConfig: XProcStepConfiguration
        get() = _stepConfig
    val receiver: Receiver
        get() = _receiver
    val options: Map<QName,LazyValue>
        get() = _options
    val queues: Map<String, List<XProcDocument>>
        get() = _queues

    override fun setup(stepConfig: XProcStepConfiguration, receiver: Receiver, stepParams: RuntimeStepParameters) {
        this._stepConfig = stepConfig
        this._receiver = receiver
        this._stepParams = stepParams

        for (port in stepParams.inputs.keys.filter { !it.startsWith("Q{")}) {
            _queues.put(port, mutableListOf())
        }

        synchronized(Companion) {
            _nodeId = ++_id
        }
    }

    final override fun input(port: String, doc: XProcDocument) {
        synchronized(this) {
            stepConfig.debug { "RECVD ${this} input on $port" }
            if (!port.startsWith("Q{")) {
                val list = mutableListOf<XProcDocument>()
                list.addAll(_queues[port] ?: emptyList())
                list.add(doc)
                _queues[port] = list
            }
        }
    }

    override fun extensionAttributes(attributes: Map<QName, String>) {
        // nop
    }

    override fun teardown() {
        // nop
    }

    override fun reset() {
        _options.clear()
        for ((key, _) in _queues) {
            _queues[key] = emptyList()
        }
    }

    override fun option(name: QName, binding: LazyValue) {
        synchronized(this) {
            stepConfig.debug { "RECVD ${this} option $name" }
            _options[name] = binding
        }
    }

    override fun run() {
        stepConfig.debug { "  RUNNING ${this}" }
        for ((name, value) in options) {
            stepConfig.debug { "    OPT: ${name}" }
        }
        for ((name, docs) in queues) {
            stepConfig.debug { "    INP: ${name}: ${docs.size}" }
        }
    }

    override fun abort() {
        // nop?
    }

    internal fun forbidNamespaceAttribute(attName: QName) {
        if (attName.localName == "xmlns" || attName.prefix == "xmlns" || NsXmlns.namespace == attName.namespaceUri
            || (attName.prefix == "xml" && attName.namespaceUri != NsXml.namespace)
            || (attName.prefix != "xml" && attName.namespaceUri == NsXml.namespace)) {
            throw stepConfig.exception(XProcError.xcCannotAddNamespaces(attName))
        }
    }

    internal fun processMatcher(matchName: QName): ProcessMatch {
        val nsbindings = valueBinding(Ns.match).context.inscopeNamespaces
        val bindings = mutableMapOf<QName, XdmValue>()
        return ProcessMatch(stepConfig, this as ProcessMatchingNodes, nsbindings, bindings)
    }

    fun establishNamespaceBinding(prefix: String, ns: NamespaceUri, nsmap: NamespaceMap): Pair<String,Map<String, NamespaceUri>> {
        val mutableNS = mutableMapOf<String, NamespaceUri>()
        for (binding in nsmap.namespaceBindings) {
            mutableNS.put(binding.prefix, binding.namespaceUri)
        }
        return establishNamespaceBinding(prefix, ns, mutableNS)
    }

    fun establishNamespaceBinding(prefix: String, ns: NamespaceUri, nsmap: Map<String, NamespaceUri>): Pair<String,Map<String, NamespaceUri>> {
        if (nsmap[prefix] == ns) {
            return Pair(prefix, nsmap)
        }

        val newmap = mutableMapOf<String, NamespaceUri>()
        newmap.putAll(nsmap)

        if (newmap[prefix] == null) {
            newmap.put(prefix, ns)
            return Pair(prefix, newmap)
        }

        var px = 1
        var newpfx = "${prefix}_${px}"
        while (newmap[newpfx] != null && newmap[newpfx] != ns) {
            px++
            newpfx = "${prefix}_${px}"
        }
        newmap[newpfx] = ns
        return Pair(newpfx, newmap)
    }

    fun hasBinding(name: QName): Boolean {
        if (options.containsKey(name)) {
            val option = options[name]!!
            return option.value != XdmEmptySequence.getInstance()
        }
        return false
    }

    fun xmlBinding(name: QName): XProcDocument {
        val lazy = options[name]!!
        return XProcDocument.ofValue(lazy.value, lazy.context)
    }

    fun valueBinding(name: QName): XProcDocument {
        val lazy = options[name]!!
        return XProcDocument.ofValue(lazy.value, lazy.context)
    }

    fun stringBinding(name: QName): String? {
        val opt = options[name] ?: return null
        if (opt.value == XdmEmptySequence.getInstance()) {
            return null
        }
        return opt.value.underlyingValue.stringValue
    }

    fun uriBinding(name: QName): URI? {
        val opt = options[name] ?: return null
        if (opt.value == XdmEmptySequence.getInstance()) {
            return null
        }

        // If it raises a URISyntaxException, let that be thrown...except that...
        // If this is Windows, change  \ into / because reasons.
        val href = if (Urify.isWindows) {
            opt.value.underlyingValue.stringValue.replace("\\", "/")
        } else {
            opt.value.underlyingValue.stringValue
        }

        if (Urify.isWindows && href.length > 1 && href[1] == ':') {
            // And if this is Windows and it begins with a drive letter, don't treat that as the scheme!
            opt.context.resolve("file:/${href}")
        } else {
            opt.context.resolve(opt.value.underlyingValue.stringValue)
        }

        val resolved = if (opt.context.baseUri == null) {
            Urify.urify(opt.value.underlyingValue.stringValue)
        } else {
            Urify.urify(opt.value.underlyingValue.stringValue, opt.context.baseUri!!)
        }

        // Normalize file:/// back to file:/ for consistency
        val result = if (!resolved.startsWith("file:////") && resolved.startsWith("file:///")) {
            URI("file:/${resolved.substring(8)}")
        } else {
            URI(resolved)
        }

        return result
    }

    fun integerBinding(name: QName): Int? {
        val opt = options[name] ?: return null
        if (opt.value == XdmEmptySequence.getInstance()) {
            return null
        }
        return opt.value.underlyingValue.stringValue.toInt()
    }

    fun booleanBinding(name: QName): Boolean? {
        val opt = options[name] ?: return null
        if (opt.value == XdmEmptySequence.getInstance()) {
            return null
        }
        val value = opt.value.underlyingValue.stringValue
        return value == "true"
    }

    fun qnameMapBinding(name: QName): Map<QName, XdmValue> {
        if (options[name] == null) {
            return mapOf()
        }
        val value = options[name]!!.value
        if (value == XdmEmptySequence.getInstance()) {
            return mapOf()
        }
        return stepConfig.typeUtils.asMap(stepConfig.typeUtils.forceQNameKeys(value as XdmMap))
    }

    fun qnameBinding(name: QName): QName? {
        val ovalue = options[name]!!.value
        if (ovalue == XdmEmptySequence.getInstance()) {
            return null
        }
        val value = ovalue.underlyingValue
        val qname = when (value) {
            is QName -> value
            is QNameValue -> QName(value.structuredQName)
            is StringValue -> stepConfig.typeUtils.parseQName(value.stringValue)
            is NodeInfo -> stepConfig.typeUtils.parseQName(value.stringValue)
            else -> throw RuntimeException("bang")
        }
        return qname
    }

    fun mediaTypeBinding(name: QName, default: MediaType = MediaType.ANY): MediaType {
        val value = options[name]!!.value
        if (value == XdmEmptySequence.getInstance()) {
            return default
        }
        return MediaType.parse(value.underlyingValue.stringValue)
    }

    fun wildcardMediaTypeBinding(name: QName, default: MediaType = MediaType.ANY): MediaType {
        val value = options[name]!!.value
        if (value == XdmEmptySequence.getInstance()) {
            return default
        }
        return MediaType.parseWildcard(value.underlyingValue.stringValue)
    }

    fun stringMapBinding(name: QName): Map<String,String> {
        if (options[name] == null) {
            return mapOf()
        }
        val value = options[name]!!.value
        if (value == XdmEmptySequence.getInstance()) {
            return mapOf()
        }
        val map = mutableMapOf<String,String>()
        for (key in (value as XdmMap).keySet()) {
            map.put(key.stringValue, value.get(key).toString())
        }
        return map
    }

    fun overrideContentTypes(value: XdmValue): List<Pair<String,MediaType>> {
        if (value == XdmEmptySequence.getInstance()) {
            return emptyList()
        }
        if (value !is XdmArray) {
            throw stepConfig.exception(XProcError.xcBadOverrideContentTypesType())
        }
        val pairs = mutableListOf<Pair<String,MediaType>>()
        for (index in 0 ..< value.arrayLength()) {
            val item = value.get(index)
            if (item !is XdmArray) {
                throw stepConfig.exception(XProcError.xcBadOverrideContentTypesMemberType(index))
            }
            if (item.arrayLength() != 2) {
                throw stepConfig.exception(XProcError.xcBadOverrideContentTypesMemberTypeLength(index))
            }

            val regex = item.get(0).underlyingValue.stringValue
            val ctstr = item.get(1).underlyingValue.stringValue

            try {
                val compiler = stepConfig.newXPathCompiler()
                compiler.declareVariable(vara)
                compiler.declareVariable(varb)
                val exec = compiler.compile("matches(\$a,\$b)")
                val selector = exec.load()
                selector.setVariable(vara, XdmAtomicValue("test"))
                selector.setVariable(varb, XdmAtomicValue(regex))
                selector.evaluate()
            } catch (ex: SaxonApiException) {
                throw stepConfig.exception(XProcError.xcInvalidRegex(regex), ex)
            }

            pairs.add(Pair(regex, MediaType.parse(ctstr)))
        }

        return pairs
    }
}
