package com.xmlcalabash.parsers.xpl.elements

import com.xmlcalabash.datamodel.ImportFunctionsInstruction
import com.xmlcalabash.datamodel.MediaType
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.XdmNode
import org.apache.logging.log4j.kotlin.logger

class ImportFunctionsNode(parent: AnyNode, node: XdmNode): ElementNode(parent, node) {
    val href = node.baseURI.resolve(node.getAttributeValue(Ns.href)!!)
    val contentType = node.getAttributeValue(Ns.contentType)
    val namespace = node.getAttributeValue(Ns.namespace)

    init {
        try {
            val import = ImportFunctionsInstruction(null, parent.stepConfig, href)
            contentType?.let { import.contentType = MediaType.parse(it) }
            namespace?.let { import.namespace = NamespaceUri.of(it) }
            import.prefetch()
        } catch (ex: Exception) {
            logger.debug { "Prefetch of p:import-functions in XPL parse failed: ${ex.message}" }
        }
    }
}