package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.datamodel.ImportFunctionsInstruction
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.util.UriUtils
import net.sf.saxon.s9api.XdmNode

class ImportFunctionsNode(parent: AnyNode, node: XdmNode): ElementNode(parent, node) {
    val href = UriUtils.resolve(node.baseURI, node.getAttributeValue(Ns.href))!!
    val contentType = node.getAttributeValue(Ns.contentType)
    val namespace = node.getAttributeValue(Ns.namespace)

    init {
        val import = ImportFunctionsInstruction(null, parent.stepConfig, href)
        contentType?.let { import.contentType = MediaType.parse(it) }
        namespace?.let { import.namespace = it }
        import.prefetch()
    }
}