package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.XProcPipeline
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.util.SchematronAssertions
import com.xmlcalabash.util.SchematronMonitor
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.XdmNode
import java.net.URI

open class LibraryInstruction(stepConfig: InstructionConfiguration): XProcInstruction(null, stepConfig, NsP.library), StepContainerInterface {
    internal val schematron = mutableMapOf<String, XdmNode>()

    override var psviRequired: Boolean? = null
        set(value) {
            checkOpen()
            field = value
        }

    override var xpathVersion: Double? = null
        set(value) {
            checkOpen()
            field = value
        }

    override var version: Double? = null
        set(value) {
            checkOpen()
            field = value
        }

    // ========================================================================================

    private val _imported = mutableListOf<StepContainerInterface>()
    private val _exportedSteps = mutableMapOf<QName, DeclareStepInstruction>()
    private val _exportedOptions = mutableMapOf<QName, OptionInstruction>()
    private var compiled = false

    internal val _staticOptions = mutableMapOf<QName, StaticOptionDetails>()
    val staticOptions: Map<QName, StaticOptionDetails>
        get() = _staticOptions

    val exportedSteps: Map<QName, DeclareStepInstruction>
        get() = _exportedSteps

    val exportedOptions: Map<QName, OptionInstruction>
        get() = _exportedOptions

    fun findDeclarations() {
        findDeclarations(emptyMap(), emptyMap(), emptyMap())
    }

    var checkedCoherent = false
    fun coherentStepDeclarations() {
        if (checkedCoherent) {
            return
        }
        checkedCoherent = true

        for (import in _imported) {
            when (import) {
                is DeclareStepInstruction -> {
                    import.coherentStepDeclarations()
                }
                is LibraryInstruction -> {
                    import.coherentStepDeclarations()
                }
                else -> throw stepConfig.exception(XProcError.xiImpossible("Unexpected imported type: ${import}"))
            }
        }

        for (child in children.filterIsInstance<DeclareStepInstruction>()) {
            child.coherentStepDeclarations()
        }
    }

    var foundDeclarations = false
    override fun findDeclarations(stepTypes: Map<QName, DeclareStepInstruction>, stepNames: Map<String, StepDeclaration>, bindings: Map<QName, VariableBindingContainer>) {
        // Libraries don't inherit anything...

        if (foundDeclarations) {
            return
        }
        foundDeclarations = true

        val newStepTypes = mutableMapOf<QName, DeclareStepInstruction>()

        // Sort out what we're exporting so that if there are recursive imports, the
        // importing containers will get the correct imports.
        for (option in children.filterIsInstance<OptionInstruction>().filter { it.visibility != Visibility.PRIVATE }) {
            _exportedOptions[option.name] = option
        }
        for (decl in children.filterIsInstance<DeclareStepInstruction>().filter {
            it.type != null && it.visibility != Visibility.PRIVATE
        }) {
            _exportedSteps[decl.type!!] = decl
        }

        val newBindings = mutableMapOf<QName, VariableBindingContainer>()

        for (import in _imported) {
            // Library exports are transitive
            when (import) {
                is DeclareStepInstruction -> {
                    import.findDeclarations(stepTypes, emptyMap(), bindings)
                    if (import.type != null && import.visibility != Visibility.PRIVATE) {
                        if (newStepTypes.containsKey(import.type)) {
                            throw stepConfig.exception(XProcError.xsDuplicateStepType(import.type!!))
                        }
                        newStepTypes[import.type!!] = import
                        _exportedSteps[import.type!!] = import
                    }
                }
                is LibraryInstruction -> {
                    import.findDeclarations(stepTypes, emptyMap(), bindings)
                    for ((name, opt) in import.exportedOptions) {
                        if (newBindings.containsKey(name)) {
                            throw stepConfig.exception(XProcError.xsDuplicateOption(name))
                        }
                        newBindings[name] = opt
                        if (opt.visibility != Visibility.PRIVATE) {
                            _exportedOptions[name] = opt
                        }
                    }

                    for ((type, decl) in import.exportedSteps) {
                        val existing = newStepTypes[type]
                        if (existing != null && existing.parent != decl.parent) {
                            throw stepConfig.exception(XProcError.xsDuplicateStepType(type))
                        }
                        newStepTypes[type] = decl
                        if (decl.visibility != Visibility.PRIVATE) {
                            _exportedSteps[type] = decl
                        }
                    }
                }
                else -> throw stepConfig.exception(XProcError.xiImpossible("Unexpected imported type: ${import}"))
            }
        }

        for (child in children.filterIsInstance<DeclareStepInstruction>()) {
            if (child.type != null) {
                val current = newStepTypes[child.type]
                if (current != null && current !== child) {
                    throw stepConfig.exception(XProcError.xsDuplicateStepType(child.type!!))
                }
                newStepTypes[child.type!!] = child
            }
        }

        updateStepConfig(newStepTypes, emptyMap(), bindings)

        val optnames = mutableSetOf<QName>()
        var lastChild: VariableBindingContainer? = null
        for (child in children) {
            if (lastChild != null) {
                newBindings[lastChild.name] = lastChild
            }

            lastChild = null

            when (child) {
                is DeclareStepInstruction -> {
                    child.findDeclarations(newStepTypes, emptyMap(), newBindings)
                }
                is OptionInstruction -> {
                    child.findDeclarations(newStepTypes, emptyMap(), newBindings)
                    if (optnames.contains(child.name)) {
                        throw stepConfig.exception(XProcError.xsDuplicateStaticOption(child.name))
                    }
                    optnames.add(child.name)
                    newBindings[child.name] = child
                    lastChild = child
                }
                else -> throw stepConfig.exception(XProcError.xsInvalidElement(child.instructionType))
            }
        }
    }

    override fun elaborateInstructions() {
        if (parent == null) {
            if (version == null) {
                throw stepConfig.exception(XProcError.xsMissingVersion())
            } else {
                if (version != 3.0 && version != 3.1) {
                    throw stepConfig.exception(XProcError.xsUnsupportedVersion(version!!.toString()))
                }
            }
        }

        if (stepConfig.xmlCalabash.xmlCalabashConfig.assertions != SchematronAssertions.IGNORE) {
            SchematronMonitor.parseFromPipeinfo(this)
        }

        for (import in _imported) {
            when (import) {
                is DeclareStepInstruction -> import.validate()
                is LibraryInstruction -> import.validate()
                else -> throw stepConfig.exception(XProcError.xiImpossible("Import not a library or declared step?"))
            }
        }

        for (child in children) {
            when (child) {
                is OptionInstruction -> {
                    child.elaborateInstructions()
                    if (child.static) {
                        _staticOptions[child.name] = builder.staticOptionsManager.get(child)
                    }
                }
                else -> child.elaborateInstructions()
            }
        }

        open = false
    }

    private var validated = false
    internal fun validate() {
        if (validated) {
            return
        }
        validated = true

        coherentStepDeclarations()
        findDeclarations(emptyMap(), emptyMap(), emptyMap())
        findDefaultReadablePort(null)
        elaborateInstructions()

        rewrite()
    }

    // ========================================================================================

    fun rewrite() {
        for (child in children.filterIsInstance<DeclareStepInstruction>()) {
            if (!child.isAtomic) {
                child.rewrite()
            }
        }
    }

    fun getPipeline(name: String?): DeclareStepInstruction {
        for (child in children.filterIsInstance<DeclareStepInstruction>()) {
            if (name == null || child.name == name) {
                return child
            }
        }
        throw stepConfig.exception(XProcError.xiImpossible("No step named ${name}"))
    }

    fun getRuntime(stepName: String?): XProcRuntime {
        synchronized(this) {
            if (!compiled) {
                validate()
                compiled = true
            }

            for (child in children.filterIsInstance<DeclareStepInstruction>()) {
                if (stepName == null || child.name == stepName) {
                    return child.runtime()
                }
            }

            throw stepConfig.exception(XProcError.xiImpossible("No step named ${stepName}"))
        }
    }

    fun getExecutable(stepName: String?): XProcPipeline {
        return getRuntime(stepName).executable()
    }


    /*
    fun compile(stepName: String?): XProcPipeline {
        synchronized(this) {
            validate()

            for (child in children.filterIsInstance<DeclareStepInstruction>()) {
                if (stepName == null || child.name == stepName) {
                    return child.compile()
                }
            }

            throw stepConfig.exception(XProcError.xiImpossible("No step named ${stepName}"))
        }
    }
     */

    /*
    fun addIOType(instruction: XProcInstruction) {
        if (instruction is OptionInstruction) {
            if (!instruction.static) {
                instruction.throw XProcError.xsInvalidElement(instruction.instructionType))
            }
        }

        val last = children.lastOrNull()
        if (last == null || last is ImportFunctionsInstruction) {
            _children.add(instruction)
            return
        }

        instruction.throw XProcError.xsInvalidElement(instruction.instructionType))
    }
*/

    override fun importFunctions(href: URI, contentType: MediaType?, namespace: String?): ImportFunctionsInstruction {
        val import = ImportFunctionsInstruction(this, stepConfig.copy(), href)
        import.contentType = contentType
        import.namespace = namespace
        val functionLibrary = import.prefetch()

        // Importing a function library changes our StepConfiguration so that future steps also
        // inherited these functions
        if (functionLibrary != null) {
            stepConfig.saxonConfig.addFunctionLibrary(import.href, functionLibrary)
        }

        return import
    }

    /*
    override fun addImportFunctions(import: ImportFunctionsInstruction) {
        if (children.filterIsInstance<OptionInstruction>().isNotEmpty()
            || children.filterIsInstance<DeclareStepInstruction>().isNotEmpty()) {
            throw stepConfig.exception(XProcError.xsInvalidElement(import.instructionType))
        }
        _children.add(import)
    }

     */

    override fun option(name: QName): OptionInstruction {
        if (name.namespaceUri == NsP.namespace) {
            throw stepConfig.exception(XProcError.xsOptionInXProcNamespace(name))
        }
        val option = OptionInstruction(this, name, stepConfig.copy())
        _children.add(option)
        return option
    }

    override fun option(name: QName, staticValue: XProcExpression): OptionInstruction {
        if (name.namespaceUri == NsP.namespace) {
            throw stepConfig.exception(XProcError.xsOptionInXProcNamespace(name))
        }
        val option = OptionInstruction(this, name, stepConfig.copy())
        option._select = staticValue
        _children.add(option)
        return option
    }

    override fun declareStep(): DeclareStepInstruction {
        val decl = DeclareStepInstruction(this, stepConfig.copy())
        _children.add(decl)
        return decl
    }

    override fun import(import: StepContainerInterface) {
        _imported.add(import)
    }

    /*
    override fun addDeclareStep(decl: DeclareStepInstruction) {
        if (decl.parent == null) {
            decl._parent = this
        } else {
            throw stepConfig.exception(XProcError.xiImpossible("Can't add a step to more than one container"))
        }
        if (!decl.open) {
            throw stepConfig.exception(XProcError.xiImpossible("Can't add a compiled step to a container"))
        }

        _children.add(decl)
        if (decl.type != null && decl.visibility != Visibility.PRIVATE) {
            _exported[decl.type!!] = decl
        }
    }

     */

    override fun toString(): String {
        return "${instructionType}/${id}: ${stepConfig.baseUri}"
    }
}