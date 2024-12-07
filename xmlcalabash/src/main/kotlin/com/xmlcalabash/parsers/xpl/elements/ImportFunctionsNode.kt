package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.datamodel.ImportFunctionsInstruction
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.XdmNode

class ImportFunctionsNode(parent: AnyNode, node: XdmNode): ElementNode(parent, node) {
    val href = node.baseURI.resolve(node.getAttributeValue(Ns.href)!!)
    val contentType = node.getAttributeValue(Ns.contentType)
    val namespace = node.getAttributeValue(Ns.namespace)

    init {
        val import = ImportFunctionsInstruction(null, parent.stepConfig, href)
        contentType?.let { import.contentType = MediaType.parse(it) }
        namespace?.let { import.namespace = it }
        import.prefetch()
    }
}