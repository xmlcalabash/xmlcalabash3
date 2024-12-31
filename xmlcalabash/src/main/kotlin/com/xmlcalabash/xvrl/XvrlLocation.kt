package com.xmlcalabash.xvrl

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsXvrl
import com.xmlcalabash.runtime.XProcStepConfiguration
import com.xmlcalabash.util.SaxonTreeBuilder
import net.sf.saxon.s9api.QName
import java.net.URI

open class XvrlLocation protected constructor(stepConfig: XProcStepConfiguration): XvrlElement(stepConfig) {
    companion object {
        private val _href = QName("href")
        private val _xpath = QName("xpath")
        private val _jsonpointer = QName("jsonpointer")
        private val _jsonpath = QName("jsonpath")
        private val _line = QName("line")
        private val _column = QName("column")
        private val _octetPosition = QName("octet-position")

        fun newInstance(stepConfig: XProcStepConfiguration, href: URI?, attr: Map<QName,String?> = emptyMap()): XvrlLocation {
            val loc = XvrlLocation(stepConfig)
            loc.setAttributes(attr)
            href?.let { loc.setAttribute(_href, "${it}") }
            return loc
        }

        fun newInstance(stepConfig: XProcStepConfiguration, href: URI?, line: Int, column: Int = 0, attr: Map<QName,String?> = emptyMap()): XvrlLocation {
            val loc = XvrlLocation(stepConfig)
            loc.setAttributes(attr)
            href?.let { loc.setAttribute(_href, "${it}") }
            if (line > 0) {
                loc.setAttribute(_line, "${line}")
            }
            if (column > 0) {
                loc.setAttribute(_column, "${column}")
            }
            return loc
        }

        fun newInstance(stepConfig: XProcStepConfiguration, href: URI?, offset: Int, attr: Map<QName,String?> = emptyMap()): XvrlLocation {
            val loc = XvrlLocation(stepConfig)
            loc.setAttributes(attr)
            href?.let { loc.setAttribute(_href, "${it}") }
            if (offset > 0) {
                loc.setAttribute(_octetPosition, "${offset}")
            }
            return loc
        }
    }

    var href: URI?
        get() {
            val href = attributes[Ns.href]
            if (href != null) {
                return URI.create(href)
            }
            return null
        }
        set(value) {
            if (value == null) {
                attributes.remove(Ns.href)
            } else {
               setAttribute(Ns.href, "${value}")
            }
        }

    var xpath: String?
        get() {
            return attributes[_xpath]
        }
        set(value) {
            if (value == null) {
                attributes.remove(_xpath)
            } else {
               setAttribute(_xpath, value)
            }
        }

    var jsonpointer: String?
        get() {
            return attributes[_jsonpointer]
        }
        set(value) {
            if (value == null) {
                attributes.remove(_jsonpointer)
            } else {
                setAttribute(_jsonpointer, value)
            }
        }

    var jsonpath: String?
        get() {
            return attributes[_jsonpath]
        }
        set(value) {
            if (value == null) {
                attributes.remove(_jsonpath)
            } else {
                setAttribute(_jsonpath, value)
            }
        }

    var line: Int
        get() {
            val line = attributes[_line]?.toInt()
            if (line != null && line > 0) {
                return line
            }
            return -1
        }
        set(value) {
            if (value <= 0) {
                attributes.remove(_line)
            } else {
                setAttribute(_line, "${value}")
            }
        }

    var column: Int
        get() {
            val col = attributes[_column]?.toInt()
            if (col != null && col > 0) {
                return col
            }
            return -1
        }
        set(value) {
            if (value <= 0) {
                attributes.remove(_column)
            } else {
                setAttribute(_column, "${value}")
            }
        }

    var octetPosition: Int
        get() {
            val pos = attributes[_octetPosition]?.toInt()
            if (pos != null && pos > 0) {
                return pos
            }
            return -1
        }
        set(value) {
            if (value <= 0) {
                attributes.remove(_octetPosition)
            } else {
                setAttribute(_octetPosition, "${value}")
            }
        }

    override fun setAttribute(name: QName, value: String) {
        if (name == _line || name == _column || name == _octetPosition) {
            if (value.toInt() <= 0) {
                throw XProcError.xiXvrlInvalidValue(name, value).exception()
            }
        }
        setElementAttribute(name, value )
    }

    override fun serialize(builder: SaxonTreeBuilder) {
        builder.addStartElement(NsXvrl.location, stepConfig.attributeMap(attributes))
        builder.addEndElement()
    }
}