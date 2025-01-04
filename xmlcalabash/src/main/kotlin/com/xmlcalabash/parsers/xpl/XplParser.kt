package com.xmlcalabash.parsers.xpl

import com.xmlcalabash.datamodel.*
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.Ns
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsErr
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.namespace.NsXml
import com.xmlcalabash.parsers.xpl.elements.*
import com.xmlcalabash.util.*
import net.sf.saxon.om.*
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.type.Untyped
import java.net.URI

class XplParser internal constructor(val builder: PipelineBuilder) {
    private val manager = XplDocumentManager(builder)
    private val parsedUris = mutableMapOf<URI, StepContainerInterface>()
    private val errors = mutableListOf<XProcException>()

    // Multiple methods instead of defaulting the second parameter makes the Java API cleaner

    fun parse(filename: String): DeclareStepInstruction {
        return parse(filename, null)
    }

    fun parse(filename: String, stepName: String?): DeclareStepInstruction {
        val uri = UriUtils.cwdAsUri().resolve(filename)
        return parse(uri, stepName)
    }

    fun parse(uri: URI): DeclareStepInstruction {
        return parse(uri, null)
    }

    fun parse(xml: XdmNode): DeclareStepInstruction {
        return parse(xml, null)
    }

    fun parse(xml: XdmNode, stepName: String?): DeclareStepInstruction {
        val document = manager.load(xml)
        val uri = xml.baseURI
        val stepContainer = if (document.rootNode is LibraryNode) {
            val library = builder.newLibrary()
            library.stepConfig.updateWith(document.rootNode.node)
            parsedUris[uri] = library
            parseLibrary(document.rootNode as LibraryNode, library)
        } else {
            val decl = builder.newDeclareStep()
            decl.stepConfig.updateWith(document.rootNode.node)
            parsedUris[uri] = decl
            parseDeclareStep(null, document.rootNode as DeclareStepNode, decl)
        }

        parsedUris[uri] = stepContainer

        if (errors.isNotEmpty()) {
            throw errors.first()
        }

        return pipelineForContainer(stepContainer, stepName)
    }

    fun parse(uri: URI, stepName: String?): DeclareStepInstruction {
        val stepContainer = parseUri(uri)

        if (errors.isNotEmpty()) {
            throw errors.first()
        }

        return pipelineForContainer(stepContainer, stepName)
    }

    private fun pipelineForContainer(stepContainer: StepContainerInterface, stepName: String?): DeclareStepInstruction {
        if (stepContainer is DeclareStepInstruction) {
            return stepContainer
        } else {
            val library = stepContainer as LibraryInstruction
            val decl = if (stepName == null) {
                library.children.filterIsInstance<DeclareStepInstruction>().firstOrNull()
            } else {
                library.children.filterIsInstance<DeclareStepInstruction>().firstOrNull { it.name == stepName }
            }
            if (decl == null) {
                if (stepName == null) {
                    throw XProcError.xiNoPipelineInLibrary(stepContainer.stepConfig.baseUri?.toString() ?: "-").exception()
                }
                throw XProcError.xiNoPipelineInLibrary(stepName, stepContainer.stepConfig.baseUri?.toString() ?: "-").exception()
            }
            return decl
        }
    }

    private fun parseUri(uri: URI): StepContainerInterface {
        if (parsedUris.containsKey(uri)) {
            return parsedUris[uri]!!
        }

        val document = manager.load(uri)
        val stepContainer = if (document.rootNode is LibraryNode) {
            val library = builder.newLibrary()
            library.stepConfig.updateWith(document.rootNode.node)
            parsedUris[uri] = library
            parseLibrary(document.rootNode as LibraryNode, library)
        } else {
            val decl = builder.newDeclareStep()
            decl.stepConfig.updateWith(document.rootNode.node)
            parsedUris[uri] = decl
            parseDeclareStep(null, document.rootNode as DeclareStepNode, decl)
        }

        return stepContainer
    }

    private fun parseLibrary(node: LibraryNode, library: LibraryInstruction): StepContainerInterface {
        val stepConfig = library.stepConfig

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.psviRequired to { value -> library.psviRequired = stepConfig.parseBoolean(value) },
            Ns.xpathVersion to { value -> library.xpathVersion = value.toDouble() },
            Ns.excludeInlinePrefixes to { value -> stepConfig.parseExcludeInlinePrefixes(value) },
            Ns.version to { value -> library.version = parseVersion(node, value) },
        )

        processAttributes(node, library, attributeMapping)

        val elementMapping = mapOf<QName, (ElementNode) -> Unit>(
            NsP.import to { child -> parseImport(library as StepContainerInterface, child) },
            NsP.importFunctions to { child -> parseImportFunctions(library as StepContainerInterface, child) },
            NsP.option to { child -> parseLibraryOption(library, child) },
            NsP.declareStep to { child -> parseNestedDeclareStep(library, child as DeclareStepNode) },
        )

        processElements(node, library, elementMapping)

        return library
    }

    private fun parseLibraryOption(library: LibraryInstruction, node: ElementNode) {
        if (node is OptionNode) {
            val stepConfig = library.stepConfig
            val option = library.option(node.name)
            option.stepConfig.updateWith(node.node)

            val attributeMapping = mapOf<QName, (String) -> Unit>(
                Ns.name to { _ -> },
                Ns.asType to { value -> option.asType = stepConfig.parseSequenceType(value) },
                Ns.values to { value -> option.values = stepConfig.parseValues(value) },
                Ns.static to { value -> option.static = stepConfig.parseBoolean(value) },
                Ns.required to { value -> option.required = stepConfig.parseBoolean(value) },
                Ns.select to { value -> option.select = XProcExpression.select(stepConfig, value) },
                Ns.visibility to { value -> option.visibility = stepConfig.parseVisibility(value) }
            )

            processAttributes(node, option, attributeMapping)
            processElements(node, option, emptyMap())

            if (!option.static) {
                throw stepConfig.exception(XProcError.xsLibraryOptionsMustBeStatic(node.name))
            }
        } else {
            if (Ns.name !in node.attributes) {
                errors.add(library.stepConfig.exception(XProcError.xsMissingRequiredAttribute(Ns.name)))
            }
            if (Ns.select !in node.attributes) {
                errors.add(library.stepConfig.exception(XProcError.xsMissingRequiredAttribute(Ns.select)))
            }
            // If we got here, it's an option that isn't static... (it would
            // be an OptionNode if it was static...)
            throw library.stepConfig.exception(XProcError.xsLibraryOptionsMustBeStatic(node.node.nodeName))
        }
    }

    private fun subpipelineElementMapping(decl: CompoundStepDeclaration): Map<QName, (ElementNode) -> Unit> {
        return mapOf<QName, (ElementNode) -> Unit>(
            NsP.variable to { child -> parseVariable(decl, child) },
            NsP.choose to { child -> parseChoose(decl, child) },
            NsP.`if` to { child -> parseIf(decl, child) },
            NsP.forEach to { child -> parseForEach(decl, child) },
            NsP.viewport to { child -> parseViewport(decl, child) },
            NsP.group to { child -> parseGroup(decl, child) },
            NsP.`try` to { child -> parseTry(decl, child) },
            NsP.run to { child -> parseRun(decl, child) },
            NsCx.defaultElement to { child -> parseAtomicStep(decl, child) }
        )
    }

    private fun parseDeclareStep(container: StepContainerInterface?, node: DeclareStepNode, decl: DeclareStepInstruction): StepContainerInterface {
        val stepConfig = decl.stepConfig.copyNew()

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { value -> decl.name = stepConfig.parseNCName(value) },
            Ns.type to { value -> decl.type = stepConfig.parseQName(value) },
            Ns.psviRequired to { value -> decl.psviRequired = stepConfig.parseBoolean(value) },
            Ns.xpathVersion to { value -> decl.xpathVersion = value.toDouble() },
            Ns.excludeInlinePrefixes to { value -> stepConfig.parseExcludeInlinePrefixes(value) },
            Ns.version to { value -> decl.version = parseVersion(node, value) },
            Ns.visibility to { value -> decl.visibility = stepConfig.parseVisibility(value) },
        )

        processAttributes(node, decl, attributeMapping)

        val elementMapping = mutableMapOf<QName, (ElementNode) -> Unit>(
            NsP.import to { child -> parseImport(decl as StepContainerInterface, child) },
            NsP.importFunctions to { child -> parseImportFunctions(decl as StepContainerInterface, child) },
            NsP.input to { child -> parseInput(decl, child) },
            NsP.output to { child -> parseOutput(decl, child) },
            NsP.option to { child -> parseOption(decl, child) },
            NsP.declareStep to { child -> parseNestedDeclareStep(decl, child as DeclareStepNode) }
        )
        elementMapping.putAll(subpipelineElementMapping(decl))

        processElements(node, decl, elementMapping)
        return decl
    }

    private fun parseNestedDeclareStep(container: StepContainerInterface, node: DeclareStepNode): StepContainerInterface {
        val decl = container.declareStep()
        decl.stepConfig.updateWith(node.node)
        return parseDeclareStep(container, node, decl)
    }

    private fun parseInput(decl: DeclareStepInstruction, node: ElementNode) {
        val input = decl.input()
        input.stepConfig.updateWith(node.node)
        val stepConfig = input.stepConfig

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.port to { value -> input.port = stepConfig.parseNCName(value) },
            Ns.sequence to { value -> input.sequence = stepConfig.parseBoolean(value) },
            Ns.primary to { value -> input.primary = stepConfig.parseBoolean(value) },
            Ns.select to { value -> input.select = XProcExpression.select(stepConfig, value) },
            Ns.contentTypes to { value -> input.contentTypes = stepConfig.parseContentTypes(value) },
            Ns.href to { value -> input.href = XProcExpression.avt(stepConfig, value) },
            Ns.excludeInlinePrefixes to { value -> stepConfig.parseExcludeInlinePrefixes(value) },
        )

        processAttributes(node, input, attributeMapping)

        val elementMapping = mapOf<QName, (ElementNode) -> Unit>(
            NsP.empty to { child -> parseEmpty(input, child) },
            NsP.document to { child -> parseDocument(input, child) },
            NsP.inline to { child -> parseInline(input, child) },
            NsCx.defaultElement to { child -> parseImplicitInline(input, child) }
        )

        processElements(node, input, elementMapping)

        // Later, in PortBindingContainer, we'll check if this element has an href
        // and children. But because of [expletive] default inputs, we've secreted those
        // children away in defaultInputs, so we have to do those tests here too...
        if (input.href != null) {
            if (input.defaultBindings.isNotEmpty()) {
                throw stepConfig.exception(XProcError.xsHrefAndChildren())
            }
            val doc = DocumentInstruction(input, input.href!!)
            input.defaultBindings.add(doc)
            input.href = null
        }
    }

    private fun parseOutput(decl: StepDeclaration, node: ElementNode) {
        val output = if (decl is RunInstruction) {
            decl.output()
        } else {
            (decl as CompoundStepDeclaration).output()
        }
        output.stepConfig.updateWith(node.node)
        val stepConfig = output.stepConfig

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.port to { value -> output.port = stepConfig.parseNCName(value) },
            Ns.sequence to { value -> output.sequence = stepConfig.parseBoolean(value) },
            Ns.primary to { value -> output.primary = stepConfig.parseBoolean(value) },
            Ns.contentTypes to { value -> output.contentTypes = stepConfig.parseContentTypes(value) },
            Ns.href to { value -> output.href = XProcExpression.avt(stepConfig, value) },
            Ns.pipe to { value -> output.pipe = value },
            Ns.excludeInlinePrefixes to { value -> stepConfig.parseExcludeInlinePrefixes(value) },
            Ns.serialization to { value -> output.serialization = XProcExpression.select(stepConfig, value) },
        )

        processAttributes(node, output, attributeMapping)

        val elementMapping = mapOf<QName, (ElementNode) -> Unit>(
            NsP.empty to { child -> parseEmpty(output, child) },
            NsP.document to { child -> parseDocument(output, child) },
            NsP.pipe to { child -> parsePipe(output, child) },
            NsP.inline to { child -> parseInline(output, child) },
            NsCx.defaultElement to { child -> parseImplicitInline(output, child) }
        )

        processElements(node, output, elementMapping)
    }

    private fun parseOption(decl: DeclareStepInstruction, node: ElementNode) {
        val name = try {
            node.attributes[Ns.name]?.let { decl.stepConfig.parseQName(it) }
        } catch (ex: XProcException) {
            when (ex.error.code) {
                NsErr.xd(69) -> throw decl.stepConfig.exception(XProcError.xsUnboundPrefix(node.attributes[Ns.name]!!))
                NsErr.xd(36) -> throw decl.stepConfig.exception(XProcError.xsValueDoesNotSatisfyType(node.attributes[Ns.name]!!, "xs:QName"))
                else -> throw ex
            }
        }
        if (name == null) {
            throw decl.stepConfig.exception(XProcError.xsMissingRequiredAttribute(Ns.name))
        }

        val option = decl.option(name)
        val stepConfig = option.stepConfig
        stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { _ -> },
            Ns.asType to { value -> option.asType = stepConfig.parseSequenceType(value) },
            Ns.values to { value -> option.values = stepConfig.parseValues(value) },
            Ns.static to { value -> option.static = stepConfig.parseBoolean(value) },
            Ns.required to { value -> option.required = stepConfig.parseBoolean(value) },
            Ns.select to { value -> option.select = XProcExpression.select(stepConfig, value) },
            Ns.visibility to { value -> option.visibility = stepConfig.parseVisibility(value) }
        )

        processAttributes(node, option, attributeMapping)
        processElements(node, option, emptyMap())
    }

    private fun parseVariable(decl: CompoundStepDeclaration, node: ElementNode) {
        val name = try {
            node.attributes[Ns.name]?.let { decl.stepConfig.parseQName(it) }
        } catch (ex: XProcException) {
            throw ex.error.asStatic().exception()
        }
        if (name == null) {
            throw decl.stepConfig.exception(XProcError.xsMissingRequiredAttribute(Ns.name))
        }

        val variable = decl.variable(name)
        val stepConfig = variable.stepConfig
        stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { _ -> },
            Ns.asType to { value -> variable.asType = stepConfig.parseSequenceType(value) },
            Ns.select to { _ -> }, // see below
            Ns.collection to { value -> variable.collection = stepConfig.parseBoolean(value) },
            Ns.href to { value -> variable.href = XProcExpression.avt(stepConfig, value) },
            Ns.pipe to { value -> variable.pipe = value },
            Ns.excludeInlinePrefixes to { value -> stepConfig.parseExcludeInlinePrefixes(value) }
        )

        processAttributes(node, variable, attributeMapping)

        if (node.attributes.containsKey(Ns.select)) {
            // The collection and as values are actually associated with the expression...
            val collection = variable.collection == true
            val asType = variable.asType ?: SequenceType.ANY
            val select = node.attributes[Ns.select]!!
            variable.select = XProcExpression.select(stepConfig, select, asType, collection, emptyList())
        }

        val elementMapping = mapOf<QName, (ElementNode) -> Unit>(
            NsP.empty to { child -> parseEmpty(variable, child) },
            NsP.document to { child -> parseDocument(variable, child) },
            NsP.pipe to { child -> parsePipe(variable, child) },
            NsP.inline to { child -> parseInline(variable, child) },
            NsCx.defaultElement to { child -> parseImplicitInline(variable, child) }
        )

        processElements(node, variable, elementMapping)
    }

    private fun parseWithOption(decl: StepDeclaration, node: ElementNode) {
        val name = node.attributes[Ns.name]?.let { decl.stepConfig.parseQName(it) }
        if (name == null) {
            throw decl.stepConfig.exception(XProcError.xsMissingRequiredAttribute(Ns.name))
        }

        val withOption = if (decl is RunInstruction) {
            decl.runOption(name)
        } else {
            (decl as AtomicStepInstruction).withOption(name)
        }
        val stepConfig = withOption.stepConfig
        withOption.stepConfig.updateWith(node.node)

        val attributeMapping = mutableMapOf<QName, (String) -> Unit>(
            Ns.name to { _ -> },
            Ns.asType to { value -> withOption.asType = stepConfig.parseSequenceType(value) },
            Ns.select to { value -> withOption.select = XProcExpression.select(stepConfig, value) },
            Ns.collection to { value -> withOption.collection = stepConfig.parseBoolean(value) },
            Ns.href to { value -> withOption.href = XProcExpression.avt(stepConfig, value) },
            Ns.pipe to { value -> withOption.pipe = value },
            Ns.excludeInlinePrefixes to { value -> stepConfig.parseExcludeInlinePrefixes(value) }
        )

        if (node.node.nodeName == NsP.runOption) {
            attributeMapping[Ns.static] = { value -> (withOption as RunOptionInstruction).static = stepConfig.parseBoolean(value) }
        }

        processAttributes(node, withOption, attributeMapping)

        val elementMapping = mapOf<QName, (ElementNode) -> Unit>(
            NsP.empty to { child -> parseEmpty(withOption, child) },
            NsP.document to { child -> parseDocument(withOption, child) },
            NsP.pipe to { child -> parsePipe(withOption, child) },
            NsP.inline to { child -> parseInline(withOption, child) },
            NsCx.defaultElement to { child -> parseImplicitInline(withOption, child) }
        )

        processElements(node, withOption, elementMapping)
    }

    private fun parseEmpty(container: BindingContainer, node: ElementNode) {
        val empty = container.empty()
        processAttributes(node, empty, emptyMap())
        processElements(node, empty, emptyMap())
    }

    private fun parseDocument(container: BindingContainer, node: ElementNode) {
        val href = node.attributes[Ns.href] ?: throw container.stepConfig.exception(XProcError.xsMissingRequiredAttribute(Ns.href))

        val dconfig = container.stepConfig.with(Location(node.node))
        val document = container.document(XProcExpression.avt(dconfig, href))
        val stepConfig = document.stepConfig

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.href to { _ -> },
            Ns.contentType to { value -> document.contentType = MediaType.parse(value) },
            Ns.documentProperties to { value -> document.documentProperties = XProcExpression.select(stepConfig, value) },
            Ns.parameters to { value -> document.parameters = XProcExpression.select(stepConfig, value) }
        )

        processAttributes(node, document, attributeMapping)
        processElements(node, document, emptyMap())
    }

    private fun parsePipe(container: BindingContainer, node: ElementNode) {
        val pipe = container.pipe()

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.step to { value -> pipe.step = value },
            Ns.port to { value -> pipe.port = value }
        )

        processAttributes(node, pipe, attributeMapping)
        processElements(node, pipe, emptyMap())
    }

    private fun parseInline(container: BindingContainer, node: ElementNode) {
        val xml = inlineXml(container.stepConfig, node, node.children.filter { it.useWhen == true })
        val inline = container.inline(xml)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.documentProperties to { value -> inline.documentProperties = XProcExpression.select(inline.stepConfig, value) },
            Ns.contentType to { value -> inline.contentType = MediaType.parse(value) },
            Ns.encoding to { value -> inline.encoding = value},
            Ns.excludeInlinePrefixes to { value -> inline.stepConfig.parseExcludeInlinePrefixes(value) }
        )

        processAttributes(node, inline, attributeMapping)
    }

    private fun parseImplicitInline(container: BindingContainer, node: ElementNode) {
        val xml = inlineXml(container.stepConfig, node, listOf(node))

        val documentElement = S9Api.firstElement(xml)
        if (documentElement != null && documentElement.nodeName.namespaceUri == NsP.namespace) {
            throw container.stepConfig.exception(XProcError.xsInvalidElement(documentElement.nodeName))
        }

        container.inline(xml)
    }

    private fun parseImport(instruction: StepContainerInterface, node: ElementNode) {
        if (node !is ImportNode) {
            if (Ns.href !in node.attributes) {
                errors.add(XProcError.xsMissingRequiredAttribute(Ns.href).at(node.node).exception())
            } else {
                throw XProcError.xiImpossible("p:import is not an ImportNode?").at(node.node).exception()
            }
            return
        }

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.href to { _ -> },
        )

        processAttributes(node, instruction as XProcInstruction, attributeMapping)
        processElements(node, instruction as XProcInstruction, emptyMap())

        val container = parseUri(node.href)

        if (container is DeclareStepInstruction) {
            // Visibility is irrelevant if the parent isn't a p:library
            container.visibility = Visibility.PUBLIC
        }

        instruction.import(container)
    }

    private fun parseImportFunctions(instruction: StepContainerInterface, node: ElementNode) {
        val href = node.attributes[Ns.href] ?: throw XProcError.xsMissingRequiredAttribute(Ns.href).at(node.node).exception()
        val contentType = node.attributes[Ns.contentType]?.let { MediaType.parse(it) }
        val namespace = node.attributes[Ns.namespace]

        val stepConfig = (instruction as XProcInstruction).stepConfig
        val uri = stepConfig.resolve(href)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.href to { _ -> Unit },
            Ns.contentType to { _ -> Unit },
            Ns.namespace to { _ -> Unit },
        )

        val import = instruction.importFunctions(uri, contentType, namespace)

        processAttributes(node, import, attributeMapping)
        processElements(node, import, emptyMap())
    }

    private fun parseWithInput(decl: StepDeclaration, node: ElementNode) {
        val input = if (decl is RunInstruction) {
            if (node.node.nodeName == NsP.runInput) {
                decl.runInput()
            } else {
                decl.withInput()
            }
        } else {
            decl.withInput()
        }
        val stepConfig = input.stepConfig
        stepConfig.updateWith(node.node)

        val attributeMapping = mutableMapOf<QName, (String) -> Unit>(
            Ns.port to { value -> input.port = stepConfig.parseNCName(value) },
            Ns.select to { value -> input.select = XProcExpression.select(stepConfig, value) },
            Ns.href to { value -> input.href = XProcExpression.avt(stepConfig, value) },
            Ns.pipe to { value -> input.pipe = value },
            Ns.excludeInlinePrefixes to { value -> stepConfig.parseExcludeInlinePrefixes(value) },
        )

        if (decl is RunInstruction) {
            attributeMapping[Ns.primary] = { value -> input.primary = stepConfig.parseBoolean(value) }
        }

        processAttributes(node, input, attributeMapping)

        val elementMapping = mapOf<QName, (ElementNode) -> Unit>(
            NsP.empty to { child -> parseEmpty(input, child) },
            NsP.document to { child -> parseDocument(input, child) },
            NsP.inline to { child -> parseInline(input, child) },
            NsP.pipe to { child -> parsePipe(input, child) },
            NsCx.defaultElement to { child -> parseImplicitInline(input, child) }
        )

        processElements(node, input, elementMapping)
    }

    private fun parseChoose(step: CompoundStepDeclaration, node: ElementNode) {
        val choose = step.choose()
        choose.stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { value -> choose.name = value },
        )

        processAttributes(node, choose, attributeMapping)

        val elementMapping = mapOf<QName, (ElementNode) -> Unit>(
            NsP.withInput to { child -> parseWithInput(choose, child) },
            NsP.`when` to { child -> parseWhen(choose, child) },
            NsP.otherwise to { child -> parseOtherwise(choose, child) },
        )

        processElements(node, choose, elementMapping)
    }

    private fun parseWhen(choose: ChooseInstruction, node: ElementNode) {
        val whenInstr = choose.whenInstruction()
        val stepConfig = whenInstr.stepConfig
        stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { value -> whenInstr.name = value },
            Ns.test to { value -> whenInstr.test = value },
            Ns.collection to { value -> whenInstr.collection = parseStaticBoolean(stepConfig, value) }
        )

        processAttributes(node, whenInstr, attributeMapping)

        val elementMapping = mutableMapOf<QName, (ElementNode) -> Unit>(
            NsP.withInput to { child -> parseWithInput(whenInstr, child) },
            NsP.output to { child -> parseOutput(whenInstr, child) },
        )
        elementMapping.putAll(subpipelineElementMapping(whenInstr))

        processElements(node, whenInstr, elementMapping)
    }

    private fun parseStaticBoolean(stepConfig: InstructionConfiguration, value: String): Boolean {
        try {
            return stepConfig.parseBoolean(value)
        } catch (ex: XProcException) {
            throw ex.error.asStatic().exception()
        }
    }

    private fun parseOtherwise(choose: ChooseInstruction, node: ElementNode) {
        val otherwise = choose.otherwise()
        otherwise.stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { value -> otherwise.name = value },
        )

        processAttributes(node, otherwise, attributeMapping)

        val elementMapping = mutableMapOf<QName, (ElementNode) -> Unit>(
            NsP.output to { child -> parseOutput(otherwise, child) },
        )
        elementMapping.putAll(subpipelineElementMapping(otherwise))

        processElements(node, otherwise, elementMapping)
    }

    private fun parseIf(step: CompoundStepDeclaration, node: ElementNode) {
        val ifInstr = step.ifInstruction()
        val stepConfig = ifInstr.stepConfig
        stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { value -> ifInstr.name = value },
            Ns.test to { value -> ifInstr.test = value },
            Ns.collection to { value -> ifInstr.collection = stepConfig.parseBoolean(value) }
        )

        processAttributes(node, ifInstr, attributeMapping)

        val elementMapping = mutableMapOf<QName, (ElementNode) -> Unit>(
            NsP.withInput to { child -> parseWithInput(ifInstr, child) },
            NsP.output to { child -> parseOutput(ifInstr, child) },
        )
        elementMapping.putAll(subpipelineElementMapping(ifInstr))

        processElements(node, ifInstr, elementMapping)
    }

    private fun parseForEach(step: CompoundStepDeclaration, node: ElementNode) {
        val forEach = step.forEach()
        forEach.stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { value -> forEach.name = value },
        )

        processAttributes(node, forEach, attributeMapping)

        val elementMapping = mutableMapOf<QName, (ElementNode) -> Unit>(
            NsP.withInput to { child -> parseWithInput(forEach, child) },
            NsP.output to { child -> parseOutput(forEach, child) },
        )
        elementMapping.putAll(subpipelineElementMapping(forEach))

        processElements(node, forEach, elementMapping)
    }

    private fun parseViewport(step: CompoundStepDeclaration, node: ElementNode) {
        val viewport = step.viewport()
        viewport.stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { value -> viewport.name = value },
            Ns.match to { value -> viewport.match = XProcExpression.match(viewport.stepConfig, value) }
        )

        processAttributes(node, viewport, attributeMapping)

        val elementMapping = mutableMapOf<QName, (ElementNode) -> Unit>(
            NsP.withInput to { child -> parseWithInput(viewport, child) },
            NsP.output to { child -> parseOutput(viewport, child) },
        )
        elementMapping.putAll(subpipelineElementMapping(viewport))

        processElements(node, viewport, elementMapping)
    }

    private fun parseGroup(step: CompoundStepDeclaration, node: ElementNode) {
        val group = step.group()
        group.stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { value -> group.name = value },
        )

        processAttributes(node, group, attributeMapping)

        val elementMapping = mutableMapOf<QName, (ElementNode) -> Unit>(
            NsP.output to { child -> parseOutput(group, child) },
        )
        elementMapping.putAll(subpipelineElementMapping(group))

        processElements(node, group, elementMapping)
    }

    private fun parseTry(step: CompoundStepDeclaration, node: ElementNode) {
        val tryInstr = step.tryInstruction()
        tryInstr.stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { value -> tryInstr.name = value },
        )

        processAttributes(node, tryInstr, attributeMapping)

        val elementMapping = mutableMapOf<QName, (ElementNode) -> Unit>(
            NsP.output to { child -> parseOutput(tryInstr, child) },
            NsP.catch to { child -> parseCatch(tryInstr, child) },
            NsP.finally to { child -> parseFinally(tryInstr, child) },
        )
        elementMapping.putAll(subpipelineElementMapping(tryInstr))

        processElements(node, tryInstr, elementMapping)
    }

    private fun parseCatch(step: TryInstruction, node: ElementNode) {
        val catch = step.catch()
        catch.stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { value -> catch.name = value },
            Ns.code to { value -> catch.code = parseCodes(catch.stepConfig, value) }
        )

        processAttributes(node, catch, attributeMapping)

        val elementMapping = mutableMapOf<QName, (ElementNode) -> Unit>(
            NsP.output to { child -> parseOutput(catch, child) },
        )
        elementMapping.putAll(subpipelineElementMapping(catch))

        processElements(node, catch, elementMapping)
    }

    private fun parseFinally(step: TryInstruction, node: ElementNode) {
        val finally = step.finally()
        finally.stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { value -> finally.name = value },
        )

        processAttributes(node, finally, attributeMapping)

        val elementMapping = mutableMapOf<QName, (ElementNode) -> Unit>(
            NsP.output to { child -> parseOutput(finally, child) },
        )
        elementMapping.putAll(subpipelineElementMapping(finally))

        processElements(node, finally, elementMapping)
    }

    private fun parseAtomicStep(step: CompoundStepDeclaration, node: ElementNode) {
        val atomic = step.atomicStep(node.node.nodeName)
        atomic.stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { value -> atomic.name = value },
            NsCx.defaultElement to { _ -> }
        )

        processAtomicAttributes(node, atomic, attributeMapping)

        val elementMapping = mapOf<QName, (ElementNode) -> Unit>(
            NsP.withInput to { child -> parseWithInput(atomic, child) },
            NsP.withOption to { child -> parseWithOption(atomic, child) },
        )

        processElements(node, atomic, elementMapping)
    }

    private fun parseRun(step: CompoundStepDeclaration, node: ElementNode) {
        val runStep = step.runStep()
        runStep.stepConfig.updateWith(node.node)

        val attributeMapping = mapOf<QName, (String) -> Unit>(
            Ns.name to { value -> runStep.name = value },
            NsCx.defaultElement to { _ -> }
        )

        processAttributes(node, runStep, attributeMapping)

        val elementMapping = mapOf<QName, (ElementNode) -> Unit>(
            NsP.withInput to { child -> parseWithInput(runStep, child) },
            NsP.runInput to { child -> parseWithInput(runStep, child) },
            NsP.runOption to { child -> parseWithOption(runStep, child) },
            NsP.output to { child -> parseOutput(runStep, child) },
        )

        processElements(node, runStep, elementMapping)
    }

    private fun processAttributes(node: ElementNode, instruction: XProcInstruction, attributeMapping: Map<QName, (String) -> Unit>) {
        for ((name, value) in node.attributes) {
            val mapping = attributeMapping[name] ?: attributeMapping[NsCx.defaultAttribute]
            if (mapping != null) {
                try {
                    mapping(value)
                } catch (ex: XProcException) {
                    errors.add(ex.error.asStatic().exception())
                }
            } else {
                if (name == Ns.expandText || name == NsP.expandText) {
                    if (name == Ns.expandText) {
                        if (node.node.nodeName.namespaceUri != NsP.namespace) {
                            errors.add(XProcError.xsAttributeForbidden(name).at(node.node).exception())
                        } else {
                            instruction.expandText = parseStaticBoolean(instruction.stepConfig, value)
                        }
                    } else {
                        if (node.node.nodeName.namespaceUri == NsP.namespace) {
                            errors.add(XProcError.xsAttributeNotAllowed(name).at(node.node).exception())
                        } else {
                            instruction.expandText = parseStaticBoolean(instruction.stepConfig, value)
                        }
                    }
                } else if (instruction is CompoundStepDeclaration && (name == Ns.message || name == NsP.message)) {
                    if (name == Ns.message) {
                        if (node.node.nodeName.namespaceUri != NsP.namespace) {
                            errors.add(XProcError.xsAttributeForbidden(name).at(node.node).exception())
                        } else {
                            instruction.message(XProcExpression.avt(instruction.stepConfig, value))
                        }
                    } else {
                        if (node.node.nodeName.namespaceUri == NsP.namespace) {
                            errors.add(XProcError.xsAttributeNotAllowed(name).at(node.node).exception())
                        } else {
                            instruction.message(XProcExpression.avt(instruction.stepConfig, value))
                        }
                    }
                } else {
                    if (instruction is StepDeclaration) {
                        when (name) {
                            Ns.depends -> {
                                if (node.node.nodeName.namespaceUri == NsP.namespace) {
                                    instruction.depends(value)
                                } else {
                                    errors.add(XProcError.xsAttributeForbidden(name).at(node.node).exception())
                                }
                            }
                            NsP.depends -> {
                                if (node.node.nodeName.namespaceUri == NsP.namespace) {
                                    throw XProcError.xsAttributeNotAllowed(name).at(node.node).exception()
                                }
                                instruction.depends(value)
                            }
                            Ns.timeout -> {
                                if (node.node.nodeName.namespaceUri == NsP.namespace) {
                                    instruction.timeout(value)
                                } else {
                                    errors.add(XProcError.xsAttributeForbidden(name).at(node.node).exception())
                                }
                            }
                            NsP.timeout -> {
                                if (node.node.nodeName.namespaceUri == NsP.namespace) {
                                    throw XProcError.xsAttributeNotAllowed(name).at(node.node).exception()
                                }
                                instruction.depends(value)
                            }
                            else -> {
                                if (name.namespaceUri != NamespaceUri.NULL) {
                                    instruction.setExtensionAttribute(name, value)
                                } else {
                                    errors.add(XProcError.xsAttributeForbidden(name).at(node.node).exception())
                                }
                            }
                        }
                    } else {
                        if (name.namespaceUri != NamespaceUri.NULL) {
                            instruction.setExtensionAttribute(name, value)
                        } else {
                            errors.add(XProcError.xsAttributeForbidden(name).at(node.node).exception())
                        }
                    }
                }
            }
        }
    }

    private fun processAtomicAttributes(node: ElementNode, atomic: AtomicStepInstruction, attributeMapping: Map<QName, (String) -> Unit>) {
        for ((name, value) in node.attributes) {
            val mapping = attributeMapping[name]
            if (mapping != null) {
                try {
                    mapping(value)
                } catch (ex: XProcException) {
                    errors.add(ex)
                }
            } else {
                when (name) {
                    Ns.depends -> {
                        if (node.node.nodeName.namespaceUri == NsP.namespace) {
                            atomic.depends(value)
                        } else {
                            atomic.withOption(name, XProcExpression.avt(atomic.stepConfig, value))
                        }
                    }
                    NsP.depends -> {
                        if (node.node.nodeName.namespaceUri == NsP.namespace) {
                            throw XProcError.xsAttributeNotAllowed(name).at(node.node).exception()
                        }
                        atomic.depends(value)
                    }
                    Ns.expandText -> {
                        if (node.node.nodeName.namespaceUri != NsP.namespace) {
                            errors.add(XProcError.xsAttributeForbidden(name).at(node.node).exception())
                        } else {
                            try {
                                atomic.expandText = parseStaticBoolean(atomic.stepConfig, value)
                            } catch (ex: XProcException) {
                                throw XProcError.xsInvalidExpandText(value).exception(ex)
                            }
                        }
                    }
                    NsP.expandText -> {
                        if (node.node.nodeName.namespaceUri == NsP.namespace) {
                            errors.add(XProcError.xsAttributeForbidden(name).at(node.node).exception())
                        } else {
                            try {
                                atomic.expandText = parseStaticBoolean(atomic.stepConfig, value)
                            } catch (ex: XProcException) {
                                throw XProcError.xsInvalidExpandText(value).exception(ex)
                            }
                        }
                    }
                    NsP.inlineExpandText -> {
                        throw XProcError.xsNoSuchOption(NsP.inlineExpandText).at(node.node).exception()
                    }
                    Ns.timeout -> {
                        if (node.node.nodeName.namespaceUri == NsP.namespace) {
                            atomic.timeout(value)
                        } else {
                            errors.add(XProcError.xsAttributeForbidden(name).at(node.node).exception())
                        }
                    }
                    NsP.timeout -> {
                        if (node.node.nodeName.namespaceUri == NsP.namespace) {
                            throw XProcError.xsAttributeNotAllowed(name).at(node.node).exception()
                        }
                        atomic.depends(value)
                    }
                    else -> {
                        if ((atomic.instructionType.namespaceUri == NsP.namespace && name == Ns.message)
                            || (atomic.instructionType.namespaceUri != NsP.namespace && name == NsP.message)) {
                            atomic.withOption(name, XProcExpression.avt(atomic.stepConfig, value))
                        } else {
                            if (name.namespaceUri != NamespaceUri.NULL) {
                                atomic.setExtensionAttribute(name, value)
                            } else {
                                if (atomic.expandText == true) {
                                    atomic.withOption(name, XProcExpression.shortcut(atomic.stepConfig, value))
                                } else {
                                    atomic.withOption(name, XProcExpression.constant(atomic.stepConfig, XdmAtomicValue(value)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // The instruction parameter is unused, but parallel ot the processAttributes() parameters sequence
    private fun processElements(node: ElementNode, instruction: XProcInstruction, elementMapping: Map<QName, (ElementNode) -> Unit>) {
        var implicitInline: QName? = null
        var explicitBinding = false
        var commentOrPi = false
        for (child in node.children.filter { it.useWhen == true }) {
            try {
                when (child) {
                    is ElementNode -> {
                        if (child.node.nodeName == NsP.documentation) {
                            continue
                        }
                        if (child.node.nodeName == NsP.pipeinfo) {
                            if (child.attributes.contains(NsCx.href)) {
                                for (gchild in child.node.axisIterator(Axis.CHILD)) {
                                    throw instruction.stepConfig.exception(XProcError.xiPipeinfoMustBeEmpty())
                                }
                                val uri = child.node.baseURI.resolve(child.attributes[NsCx.href]!!)
                                val info = try {
                                    builder.pipelineContext.documentManager.load(uri, instruction.stepConfig)
                                } catch (ex: Exception) {
                                    throw instruction.stepConfig.exception(XProcError.xiPipeinfoMustExist(ex.message ?: "???"))
                                }
                                if (info.value !is XdmNode) {
                                    throw instruction.stepConfig.exception(XProcError.xiPipeinfoMustBePipeinfo("Document is not XML."))
                                }
                                val root = S9Api.documentElement(info.value as XdmNode)
                                if (root.nodeName != NsP.pipeinfo) {
                                    throw instruction.stepConfig.exception(XProcError.xiPipeinfoMustBePipeinfo(root.nodeName))
                                }
                                instruction._pipeinfo.add(info.value as XdmNode)
                            } else {
                                val xml = inlineXml(instruction.stepConfig, child, listOf(child))
                                instruction._pipeinfo.add(xml)
                            }
                            continue
                        }

                        explicitBinding = explicitBinding || (child.node.nodeName == NsP.inline
                                || child.node.nodeName == NsP.document
                                || child.node.nodeName == NsP.pipe)

                        val mapping = if (child.node.nodeName in elementMapping) {
                            elementMapping[child.node.nodeName]
                        } else {
                            // Hack, this doesn't *have* to be why, but it is why
                            if (NsCx.defaultElement in elementMapping) {
                                implicitInline = child.node.nodeName
                                elementMapping[NsCx.defaultElement]
                            } else {
                                null
                            }
                        }

                        if (instruction is PortBindingContainer) {
                            if (explicitBinding && implicitInline != null) {
                                throw XProcError.xsInvalidImplicitInline(implicitInline).at(node.node).exception()
                            }

                            if (commentOrPi && implicitInline != null) {
                                throw XProcError.xsInvalidImplicitInlineSiblings().at(node.node).exception()
                            }
                        }

                        if (mapping != null) {
                            mapping(child)
                        } else {
                            errors.add(XProcError.xsInvalidElement(child.node.nodeName).at(node.node).exception())
                        }
                    }
                    is TextNode -> {
                        if (child.node.stringValue.trim().isNotEmpty()) {
                            errors.add(XProcError.xsTextNotAllowed(child.node.stringValue).at(node.node).exception())
                        }
                    }
                    else -> {
                        commentOrPi = true
                        if (instruction is PortBindingContainer && implicitInline != null) {
                            throw XProcError.xsInvalidImplicitInlineSiblings().at(node.node).exception()
                        }
                    }
                }
            } catch (ex: XProcException) {
                errors.add(ex)
            }
        }
    }

    private fun parseCodes(stepConfig: InstructionConfiguration, value: String): List<QName> {
        if (value.trim().isEmpty()) {
            throw stepConfig.exception(XProcError.xsInvalidAttribute(Ns.code))
        }

        val codes = mutableListOf<QName>()
        for (codeValue in value.split("\\s+".toRegex())) {
            try {
                codes.add(stepConfig.parseQName(codeValue))
            } catch (ex: Exception) {
                if (ex is XProcException) {
                    throw stepConfig.exception(XProcError.xsCatchCodesNotEQName(codeValue))
                }
                throw ex
            }
        }

        return codes.toList()

    }

    private fun parseVersion(node: ElementNode, version: String): Double {
        try {
            version.toDouble()
        } catch (ex: NumberFormatException) {
            throw XProcError.xsVersionMustBeDecimal(version).at(node.node).exception()
        }

        val pos = version.indexOf(".")
        if (pos == -1) {
            if (version.toInt() == 3) {
                return 3.0
            }
            throw XProcError.xsUnsupportedVersion(version).at(node.node).exception()
        } else {
            val units = version.substring(0, pos).toInt()
            val frac = version.substring(pos + 1).toInt()
            if (units == 3) {
                when (frac) {
                    0 -> return 3.0
                    1 -> return 3.1
                    else -> Unit
                }
            }
            throw XProcError.xsUnsupportedVersion(version).at(node.node).exception()
        }
    }

    private fun inlineXml(stepConfig: InstructionConfiguration, node: ElementNode, nodes: List<AnyNode>): XdmNode {
        val builder = SaxonTreeBuilder(stepConfig)
        builder.startDocument(node.baseUri)

        // Will only apply if p:inline was used because nodes is only ever one element node for implicits
        val inlineTrimWhitespace = stepConfig.xmlCalabash.xmlCalabashConfig.inlineTrimWhitespace

        for ((index, child) in nodes.withIndex()) {
            if (inlineTrimWhitespace && child is TextNode) {
                if (index == 0 || index+1 == nodes.size) {
                    val text = child.node.stringValue.trim()
                    if (text.isEmpty()) {
                        continue
                    }
                }
            }

            val excludeNamespaceUris = mutableSetOf<NamespaceUri>()
            excludeNamespaceUris.add(NsP.namespace)
            excludeNamespaces(stepConfig, excludeNamespaceUris, child)
            filterXml(builder, stepConfig, child, excludeNamespaceUris, stepConfig.baseUri)
        }

        builder.endDocument()
        return builder.result
    }

    private fun filterXml(builder: SaxonTreeBuilder, stepConfig: InstructionConfiguration, node: AnyNode, excludeNamespaceUris: Set<NamespaceUri>, baseUri: URI?) {
        when (node) {
            is ElementNode -> {
                val includeNS = mutableMapOf<String, NamespaceUri>()
                var nsMap = NamespaceMap.emptyMap()
                for ((prefix, uri) in ValueUtils.inscopeNamespaces(node.node)) {
                    if (!excludeNamespaceUris.contains(uri)) {
                        includeNS[prefix] = uri
                        nsMap = nsMap.put(prefix, uri)
                    }
                }
                var adjBaseUri = baseUri
                val attrMap = mutableMapOf<QName,String>()
                for (attr in node.node.axisIterator(Axis.ATTRIBUTE)) {
                    if ((node.node.nodeName.namespaceUri == NsP.namespace && attr.nodeName == Ns.useWhen)
                        || (node.node.nodeName.namespaceUri != NsP.namespace && attr.nodeName == NsP.useWhen)) {
                        continue
                    }

                    if (attr.nodeName.namespaceUri == NamespaceUri.NULL) {
                        attrMap[attr.nodeName] = attr.stringValue
                    } else {
                        if (!includeNS.containsKey(attr.nodeName.prefix)) {
                            nsMap = nsMap.put(attr.nodeName.prefix, attr.nodeName.namespaceUri)
                            includeNS[attr.nodeName.prefix] = attr.nodeName.namespaceUri
                        }

                        if ((node.node.nodeName.namespaceUri == NsP.namespace && attr.nodeName != Ns.useWhen)
                            || (node.node.nodeName.namespaceUri != NsP.namespace && attr.nodeName != NsP.useWhen)) {
                            attrMap[attr.nodeName] = attr.stringValue
                        }
                    }
                    if (attr.nodeName == NsXml.base) {
                        if (adjBaseUri == null) {
                            adjBaseUri = URI(attr.stringValue)
                        } else {
                            adjBaseUri = adjBaseUri.resolve(attr.stringValue)
                        }
                    }
                }

                val attMap = stepConfig.attributeMap(attrMap)
                val elemName = FingerprintedQName(node.node.nodeName.prefix, node.node.nodeName.namespaceUri, node.node.nodeName.localName)
                builder.location = BuilderLocation(node.node)
                builder.addStartElement(elemName, attMap, Untyped.getInstance(), nsMap, adjBaseUri)

                for (child in node.children.filter { it.useWhen == true }) {
                    filterXml(builder, stepConfig, child, excludeNamespaceUris, child.baseUri)
                }

                builder.addEndElement()
            }
            else -> builder.addSubtree(node.node)
        }
    }

    private fun excludeNamespaces(stepConfig: InstructionConfiguration, excludeNamespaceUris: MutableSet<NamespaceUri>, child: AnyNode) {
        if (child is ElementNode && child.node.nodeName.namespaceUri == NsP.namespace
            && child.attributes.contains(Ns.excludeInlinePrefixes)) {
            val prefixes = child.attributes[Ns.excludeInlinePrefixes]!!
            excludeNamespaceUris.addAll(stepConfig.parseExcludeInlinePrefixes(prefixes))
        }
        if (child.parent is ElementNode) {
            excludeNamespaces(stepConfig, excludeNamespaceUris, child.parent)
        }
    }
}