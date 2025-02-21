package com.xmlcalabash.ext.xmlunit

import org.w3c.dom.Node
import org.xmlunit.diff.DefaultNodeMatcher
import org.xmlunit.diff.NodeMatcher

class ExampleNodeMatcher(): NodeMatcher {
    val matcher = DefaultNodeMatcher()

    override fun match(
        controlNodes: Iterable<Node?>?,
        testNodes: Iterable<Node?>?
    ): Iterable<Map.Entry<Node?, Node?>?>? {
        return matcher.match(controlNodes, testNodes)
    }
}