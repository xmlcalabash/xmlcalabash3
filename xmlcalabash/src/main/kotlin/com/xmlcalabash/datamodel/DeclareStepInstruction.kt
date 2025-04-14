package com.xmlcalabash.datamodel

import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.io.MediaType
import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.XProcPipeline
import com.xmlcalabash.runtime.XProcRuntime
import com.xmlcalabash.util.AssertionsLevel
import com.xmlcalabash.util.AssertionsMonitor
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.ValidationMode
import net.sf.saxon.s9api.XdmNode
import java.net.URI

class DeclareStepInstruction(parent: XProcInstruction?, stepConfig: InstructionConfiguration): CompoundStepDeclaration(parent, stepConfig, NsP.declareStep), StepContainerInterface {
    internal constructor(builder: PipelineBuilder, type: QName): this(null, builder.stepConfig.copy()) {
        this.builder = builder
        this.standardStep = true
        this.type = type
    }
    constructor(parent: XProcInstruction): this(parent, parent.stepConfig.copyNew())
    constructor(parent: XProcInstruction, type: QName): this(parent, parent.stepConfig.copyNew()) {
        this.type = type
    }

    private var standardStep = false
    private var compiled = false
    override val contentModel = anySteps + mapOf(NsP.input to '*', NsP.output to '*', NsP.declareStep to '*', NsP.option to '*')
    internal val declaredSteps = mutableListOf<DeclareStepInstruction>()
    internal val assertions = mutableMapOf<String, XdmNode>()

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

    internal var _type: QName? = null
    var type: QName?
        get() = _type
        set(value) {
            checkOpen()
            if (value == null) {
                return
            }
            if (value.namespaceUri == NamespaceUri.NULL) {
                throw stepConfig.exception(XProcError.xsStepTypeInNoNamespace(value))
            }
            if (!this.standardStep && value.namespaceUri == NsP.namespace) {
                throw stepConfig.exception(XProcError.xsStepTypeNotAllowed(value))
            }
            _type = value
        }

    var visibility: Visibility? = null
        set(value) {
            checkOpen()
            field = value
        }

    val isAtomic: Boolean
        get() {
            for (child in children) {
                when (child) {
                    is InputInstruction, is OutputInstruction, is OptionInstruction -> Unit
                    else -> return false
                }
            }
            return true
        }

    // ========================================================================================

    private val _imported = mutableSetOf<StepContainerInterface>()

    private fun findStepTypes(stepTypes: Map<QName, DeclareStepInstruction>): Map<QName, DeclareStepInstruction> {
        val newStepTypes = mutableMapOf<QName, DeclareStepInstruction>()
        newStepTypes.putAll(stepTypes)
        for (child in children.filterIsInstance<DeclareStepInstruction>().filter { it.type != null }) {
            val existing = newStepTypes[child.type]
            if (existing == child) {
                continue
            }
            if (existing != null) {
                throw stepConfig.exception(XProcError.xsDuplicateStepType(child.type!!))
            }

            if (!child.isAtomic || stepConfig.atomicStepAvailable(child.type!!)) {
                child.type?.let { newStepTypes[it] = child }
                stepConfig.addVisibleStepType(child)
            }
        }
        return newStepTypes
    }

    var checkedCoherent = false
    fun coherentStepDeclarations() {
        if (checkedCoherent) {
            return
        }
        checkedCoherent = true

        // Is the externally visible structure of this declaration coherent?

        val portNames = mutableSetOf<String>()
        for (child in children.filterIsInstance<InputInstruction>() + children.filterIsInstance<OutputInstruction>()) {
            if (child.portDefined) {
                if (child.port in portNames) {
                    throw stepConfig.exception(XProcError.xsDuplicatePortName(child.port))
                }
                portNames.add(child.port)
            }
        }

        coherentPortDeclarations(children.filterIsInstance<InputInstruction>(), { port: String -> XProcError.xsMultiplePrimaryInputPorts(port) })
        coherentPortDeclarations(children.filterIsInstance<OutputInstruction>(), { port: String -> XProcError.xsMultiplePrimaryOutputPorts(port) })

        val optnames = mutableSetOf<QName>()
        for (option in _children.filterIsInstance<OptionInstruction>()) {
            if (optnames.contains(option.name)) {
                throw stepConfig.exception(XProcError.xsDuplicateOption(option.name))
            }
            optnames.add(option.name)
        }

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
        if (foundDeclarations) {
            return
        }
        foundDeclarations = true

        val newStepNames = mutableMapOf<String, StepDeclaration>()
        val newStepTypes = mutableMapOf<QName, DeclareStepInstruction>()

        val newBindings = mutableMapOf<QName, VariableBindingContainer>()
        newBindings.putAll(bindings)

        newStepTypes.putAll(stepTypes)
        // Step names aren't inherited, so don't add stepNames to newStepNames

        if (type != null) {
            val decl = stepConfig.inscopeStepTypes[type]
            if (decl != null) {
                if (decl !== this) {
                    throw stepConfig.exception(XProcError.xsDuplicateStepType(type!!))
                }
            }
            if (!isAtomic || stepConfig.atomicStepAvailable(type!!)) {
                stepConfig.addVisibleStepType(this)
                newStepTypes[type!!] = this
            }
        }

        if (!name.startsWith("!")) {
            if (stepConfig.inscopeStepNames.contains(name)) {
                throw stepConfig.exception(XProcError.xsDuplicateStepName(name))
            }
            newStepNames[name] = this
        }
        stepConfig.addVisibleStepName(this)

        for (import in _imported) {
            when (import) {
                is DeclareStepInstruction -> {
                    import.findDeclarations(stepTypes, emptyMap(), bindings)
                    if (import.type != null && import.visibility != Visibility.PRIVATE) {
                        if (newStepTypes.containsKey(import.type) && import !== newStepTypes[import.type]) {
                            throw stepConfig.exception(XProcError.xsDuplicateStepType(import.type!!))
                        }
                        newStepTypes[import.type!!] = import
                    }
                }
                is LibraryInstruction -> {
                    import.findDeclarations(stepTypes, emptyMap(), bindings)
                    for ((name, opt) in import.exportedOptions) {
                        if (newBindings.containsKey(name) && opt !== newBindings[name]) {
                            throw stepConfig.exception(XProcError.xsDuplicateOption(name))
                        }
                        newBindings[name] = opt
                    }

                    for ((type, decl) in import.exportedSteps) {
                        if (newStepTypes.containsKey(type) && decl !== newStepTypes[type]) {
                            throw stepConfig.exception(XProcError.xsDuplicateStepType(type))
                        }
                        newStepTypes[type] = decl
                    }
                }
                else -> throw stepConfig.exception(XProcError.xiImpossible("Unexpected imported type: ${import}"))
            }
        }

        newStepNames.putAll(findStepNames(newStepNames))
        newStepTypes.putAll(findStepTypes(newStepTypes))

        updateStepConfig(newStepTypes, emptyMap(), bindings)

        var lastChild: VariableBindingContainer? = null
        for (child in children) {
            if (lastChild != null) {
                newBindings[lastChild.name] = lastChild
            }

            lastChild = null
            if (child is DeclareStepInstruction) {
                child.findDeclarations(newStepTypes, emptyMap(), emptyMap())
            } else {
                child.findDeclarations(newStepTypes, newStepNames, newBindings)

                if (child is VariableBindingContainer) {
                    lastChild = child
                }
            }
        }
    }

    override fun checkInputBindings() {
        // they're allowed to be unbound here!
    }

    var elaboratedInstructions = false
    override fun elaborateInstructions() {
        if (elaboratedInstructions) {
            return
        }
        elaboratedInstructions = true

        if (parent == null) {
            if (version == null) {
                throw stepConfig.exception(XProcError.xsMissingVersion())
            } else {
                if (version != 3.0 && version != 3.1) {
                    throw stepConfig.exception(XProcError.xsUnsupportedVersion(version!!.toString()))
                }
            }
        }

        stepConfig.validationMode = stepConfig.xmlCalabashConfig.validationMode
        when (extensionAttributes[NsCx.validationMode]) {
            null -> Unit
            "strict" -> stepConfig.validationMode = ValidationMode.STRICT
            "lax" -> stepConfig.validationMode = ValidationMode.LAX
            else -> throw stepConfig.exception(XProcError.xiUnsupportedValidationMode(extensionAttributes[NsCx.validationMode]!!))
        }

        if (isAtomic) {
            for (output in children.filterIsInstance<OutputInstruction>()) {
                if (!output.children.isEmpty()) {
                    throw stepConfig.exception(XProcError.xsOutputConnectionForbidden(output.port))
                }
            }
            return
        }

        if (stepConfig.assertions != AssertionsLevel.IGNORE) {
            AssertionsMonitor.parseFromPipeinfo(this)
        }

        for (import in _imported) {
            when (import) {
                is DeclareStepInstruction -> {
                    import.validate()
                    registerPipelineFunction(import)
                }
                is LibraryInstruction -> {
                    import.validate()
                    for ((_, decl) in import.exportedSteps) {
                        registerPipelineFunction(decl)
                    }
                }
                else -> throw stepConfig.exception(XProcError.xiImpossible("Import not a library or declared step?"))
            }
        }

        val newChildren = mutableListOf<XProcInstruction>()
        for (child in children) {
            if (child is DeclareStepInstruction) {
                child.elaborateInstructions()
                declaredSteps.add(child)
                registerPipelineFunction(child)
            } else {
                newChildren.add(child)
            }
        }

        // For recursive use...
        registerPipelineFunction(this)

        _children.clear()
        _children.addAll(newChildren)

        super.elaborateInstructions()

        open = false
    }

    private fun registerPipelineFunction(decl: DeclareStepInstruction) {
        if (decl.type != null) {
            stepConfig.saxonConfig.declareFunction(decl)
        }
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

        for (decl in declaredSteps) {
            decl.validate()
        }

        if (!isAtomic) {
            rewrite()
        }

        sinkUnreadPorts()
    }

    // ========================================================================================

    override fun checkImplicitOutput(lastStep: StepDeclaration) {
        // "No."
    }

    fun runtime(): XProcRuntime {
        synchronized(this) {
            if (!compiled) {
                validate()
                compiled = true
            }

            return XProcRuntime.newInstance(this)
        }
    }

    fun getExecutable(): XProcPipeline {
        return runtime().executable()
    }

    private fun findUsedSteps(seen: MutableSet<DeclareStepInstruction> = mutableSetOf()): Set<DeclareStepInstruction> {
        if (seen.contains(this)) {
            return seen
        }
        seen.add(this)
        for (child in children.filterIsInstance<StepDeclaration>()) {
            findUsedSteps(seen, child)
        }
        return seen
    }

    private fun findUsedSteps(seen: MutableSet<DeclareStepInstruction>, step: StepDeclaration) {
        if (step is AtomicStepInstruction) {
            val decl = step.declaration()!!
            if (!decl.isAtomic) {
                decl.findUsedSteps(seen)
            }
            return
        }

        for (child in step.children.filterIsInstance<StepDeclaration>()) {
            findUsedSteps(seen, child)
        }
    }

    fun getInputs(): List<InputInstruction> {
        return children.filterIsInstance<InputInstruction>()
    }

    fun getPrimaryInput(): InputInstruction? {
        for (input in getInputs()) {
            if (input.primary == true) {
                return input
            }
        }
        return null
    }

    fun getInput(port: String): InputInstruction? {
        for (input in children.filterIsInstance<InputInstruction>()) {
            if (input.port == port) {
                return input
            }
        }
        return null
    }

    fun getOutputs(): List<OutputInstruction> {
        return children.filterIsInstance<OutputInstruction>()
    }

    fun getPrimaryOutput(): OutputInstruction? {
        for (output in getOutputs()) {
            if (output.primary == true) {
                return output
            }
        }
        return null
    }

    fun getOutput(port: String): OutputInstruction? {
        for (output in children.filterIsInstance<OutputInstruction>()) {
            if (output.port == port) {
                return output
            }
        }
        return null
    }

    fun getOption(name: QName): OptionInstruction? {
        for (opt in children.filterIsInstance<OptionInstruction>()) {
            if (opt.name == name) {
                return opt
            }
        }
        return null
    }

    override fun import(import: StepContainerInterface) {
        val last = children.lastOrNull()
        if (last == null || last is ImportFunctionsInstruction) {
            _imported.add(import)
            return
        }
        throw stepConfig.exception(XProcError.xsInvalidElement(NsP.import))
    }

    override fun importFunctions(href: URI, contentType: MediaType?, namespace: String?): ImportFunctionsInstruction {
        val last = children.lastOrNull()
        if (last == null || last is ImportFunctionsInstruction) {
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

        throw stepConfig.exception(XProcError.xsInvalidElement(NsP.importFunctions))
    }

    fun addIOType(instruction: XProcInstruction) {
        val last = children.lastOrNull()
        if (last == null || last is ImportFunctionsInstruction
            || last is InputInstruction || last is OutputInstruction || last is OptionInstruction) {
            _children.add(instruction)
            return
        }
        throw stepConfig.exception(XProcError.xsInvalidElement(instruction.instructionType))
    }

    fun input(): InputInstruction {
        val input = InputInstruction(this)
        addIOType(input)
        return input
    }

    fun input(port: String, primary: Boolean? = null, sequence: Boolean? = null): InputInstruction {
        val input = InputInstruction(this, port, primary, sequence)
        addIOType(input)
        return input
    }

    fun getOptions(): List<OptionInstruction> {
        return children.filterIsInstance<OptionInstruction>()
    }

    override fun option(name: QName): OptionInstruction {
        if (name.namespaceUri == NsP.namespace) {
            throw stepConfig.exception(XProcError.xsOptionInXProcNamespace(name))
        }
        val option = OptionInstruction(this, name, stepConfig.copy())
        _children.add(option)
        stepConfig.addVariable(option)
        return option
    }

    override fun option(name: QName, staticValue: XProcExpression): OptionInstruction {
        if (name.namespaceUri == NsP.namespace) {
            throw stepConfig.exception(XProcError.xsOptionInXProcNamespace(name))
        }
        val option = OptionInstruction(this, name, stepConfig.copy())
        option._select = staticValue
        _children.add(option)
        stepConfig.addVariable(option)
        return option
    }

    override fun declareStep(): DeclareStepInstruction {
        val last = children.lastOrNull()
        if (last != null && last !is ImportFunctionsInstruction
            && last !is InputInstruction && last !is OutputInstruction && last !is OptionInstruction
            && last !is DeclareStepInstruction) {
            throw stepConfig.exception(XProcError.xsInvalidElement(NsP.declareStep))
        }

        val decl = DeclareStepInstruction(this, stepConfig.copyNew())
        _children.add(decl)
        return decl
    }

    override fun toString(): String {
        if (type == null) {
            return name
        }
        return "${name}: ${type}"
    }
}