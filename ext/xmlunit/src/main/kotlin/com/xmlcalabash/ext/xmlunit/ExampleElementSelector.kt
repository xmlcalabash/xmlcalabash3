package com.xmlcalabash.ext.xmlunit

import org.w3c.dom.Element
import org.xmlunit.diff.ElementSelector
import org.xmlunit.util.Nodes

class ExampleElementSelector: ElementSelector {
    override fun canBeCompared(controlElement: Element?, testElement: Element?): Boolean {
        if (controlElement == null || testElement == null) {
            return false // can't actually happen, but keep the API happy
        }

        return Nodes.getQName(controlElement) == Nodes.getQName(testElement)
    }
}