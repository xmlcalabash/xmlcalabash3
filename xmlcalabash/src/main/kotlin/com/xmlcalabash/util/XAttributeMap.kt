package com.xmlcalabash.util

import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.AttributeInfo
import net.sf.saxon.om.AttributeMap
import net.sf.saxon.om.EmptyAttributeMap
import net.sf.saxon.om.FingerprintedQName
import net.sf.saxon.s9api.Location
import net.sf.saxon.s9api.QName
import net.sf.saxon.type.BuiltInAtomicType

class XAttributeMap() {
    private var _map: AttributeMap = EmptyAttributeMap.getInstance()
    val attributes: AttributeMap
        get() {
            return _map
        }

    fun set(name: QName, value: String, location: Location) {
        val info = AttributeInfo(fqName(name), BuiltInAtomicType.UNTYPED_ATOMIC, value, location, ReceiverOption.NONE)
        _map = _map.put(info)
    }

    operator fun set(name: QName, value: String?) {
        if (value != null) {
            val info = AttributeInfo(fqName(name), BuiltInAtomicType.UNTYPED_ATOMIC, value, null, ReceiverOption.NONE)
            _map = _map.put(info)
        }
    }

    private fun fqName(name: QName): FingerprintedQName = FingerprintedQName(name.prefix, name.namespaceUri, name.localName)
}