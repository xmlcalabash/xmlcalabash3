package com.xmlcalabash.runtime.steps

import com.xmlcalabash.config.SaxonConfiguration
import com.xmlcalabash.config.XmlCalabash
import com.xmlcalabash.datamodel.DeclareStepInstruction
import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.net.URI

interface RuntimeStepStaticContext {
    val xmlCalabash: XmlCalabash

    val saxonConfig: SaxonConfiguration
    val processor: Processor

    val location: Location
    val baseUri: URI?

    val inscopeNamespaces: Map<String, NamespaceUri>
    val inscopeStepTypes: Map<QName, DeclareStepInstruction>

    fun addVisibleStepType(decl: DeclareStepInstruction)
    fun stepDeclaration(name: QName): DeclareStepInstruction?
    fun stepAvailable(name: QName): Boolean

    fun updateWith(node: XdmNode)
    fun updateWith(baseUri: URI)
    fun updateNamespaces(nsmap: Map<String, NamespaceUri>)
    fun addNamespace(prefix: String, uri: NamespaceUri)

    fun resolve(href: String): URI
    fun exception(error: XProcError): XProcException
    fun exception(error: XProcError, cause: Throwable): XProcException
}