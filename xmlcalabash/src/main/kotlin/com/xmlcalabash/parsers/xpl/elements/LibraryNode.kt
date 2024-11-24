package com.xmlcalabash.parsers.xpl.elements

import net.sf.saxon.s9api.XdmNode

class LibraryNode(parent: AnyNode, node: XdmNode): RootNode(parent, parent.stepConfig, node) {
}