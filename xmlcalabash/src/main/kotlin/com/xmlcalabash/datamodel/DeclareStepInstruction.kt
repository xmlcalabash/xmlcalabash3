package com.xmlcalabash.datamodel

import com.xmlcalabash.documents.XProcDocument
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.runtime.XProcPipeline
import com.xmlcalabash.runtime.XProcRuntime
import net.sf.saxon.om.NamespaceUri
import net.sf.saxon.s9api.QName
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
    var eagerEval = false

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
                throw XProcError.xsStepTypeInNoNamespace(value).exception()
            }
            if (!this.standardStep && value.namespaceUri == NsP.namespace) {
                throw XProcError.xsStepTypeNotAllowed(value).exception()
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

    var debugPipelineBefore: String? = null
    var debugPipelineAfter: String? = null
    var debugPipelineGraph: String? = null

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
                throw XProcError.xsDuplicateStepType(child.type!!).exception()
            }

            if (!child.isAtomic || stepConfig.environment.commonEnvironment.atomicStepAvailable(child.type!!)) {
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
                    throw XProcError.xsDuplicatePortName(child.port).exception()
                }
                portNames.add(child.port)
            }
        }

        coherentPortDeclarations(children.filterIsInstance<InputInstruction>(), { port: String -> XProcError.xsMultiplePrimaryInputPorts(port) })
        coherentPortDeclarations(children.filterIsInstance<OutputInstruction>(), { port: String -> XProcError.xsMultiplePrimaryOutputPorts(port) })

        val optnames = mutableSetOf<QName>()
        for (option in _children.filterIsInstance<OptionInstruction>()) {
            if (optnames.contains(option.name)) {
                throw XProcError.xsDuplicateOption(option.name).exception()
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
                else -> throw XProcError.xiImpossible("Unexpected imported type: ${import}").exception()
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
                    throw XProcError.xsDuplicateStepType(type!!).exception()
                }
            }
            if (!isAtomic || stepConfig.environment.commonEnvironment.atomicStepAvailable(type!!)) {
                stepConfig.addVisibleStepType(this)
                newStepTypes[type!!] = this
            }
        }

        if (!name.startsWith("!")) {
            if (stepConfig.inscopeStepNames.contains(name)) {
                throw XProcError.xsDuplicateStepName(name).exception()
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
                            throw XProcError.xsDuplicateStepType(import.type!!).exception()
                        }
                        newStepTypes[import.type!!] = import
                    }
                }
                is LibraryInstruction -> {
                    import.findDeclarations(stepTypes, emptyMap(), bindings)
                    for ((name, opt) in import.exportedOptions) {
                        if (newBindings.containsKey(name) && opt !== newBindings[name]) {
                            throw XProcError.xsDuplicateOption(name).exception()
                        }
                        newBindings[name] = opt
                    }

                    for ((type, decl) in import.exportedSteps) {
                        if (newStepTypes.containsKey(type) && decl !== newStepTypes[type]) {
                            throw XProcError.xsDuplicateStepType(type).exception()
                        }
                        newStepTypes[type] = decl
                    }
                }
                else -> throw XProcError.xiImpossible("Unexpected imported type: ${import}").exception()
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
                throw XProcError.xsMissingVersion().exception()
            } else {
                if (version != 3.0 && version != 3.1) {
                    throw XProcError.xsUnsupportedVersion(version!!.toString()).exception()
                }
            }
        }

        if (isAtomic) {
            return
        }

        for (import in _imported) {
            when (import) {
                is DeclareStepInstruction -> import.validate()
                is LibraryInstruction -> import.validate()
                else -> throw XProcError.xiImpossible("Import not a library or declared step?").exception()
            }
        }

        val newChildren = mutableListOf<XProcInstruction>()
        for (child in children) {
            if (child is DeclareStepInstruction) {
                child.elaborateInstructions()
                declaredSteps.add(child)
            } else {
                newChildren.add(child)
            }
        }

        _children.clear()
        _children.addAll(newChildren)

        super.elaborateInstructions()

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

    fun description(): XdmNode {
        return PipelineVisualization.build(this)
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

    fun addIOType(instruction: XProcInstruction) {
        val last = children.lastOrNull()
        if (last == null || last is ImportFunctionsInstruction
            || last is InputInstruction || last is OutputInstruction || last is OptionInstruction) {
            _children.add(instruction)
            return
        }
        throw XProcError.xsInvalidElement(instruction.instructionType).exception()
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

    override fun option(name: QName): OptionInstruction {
        if (name.namespaceUri == NsP.namespace) {
            throw XProcError.xsOptionInXProcNamespace(name).exception()
        }
        val option = OptionInstruction(this, name, stepConfig.copy())
        _children.add(option)
        stepConfig.addVariable(option)
        return option
    }

    override fun option(name: QName, staticValue: XProcExpression): OptionInstruction {
        if (name.namespaceUri == NsP.namespace) {
            throw XProcError.xsOptionInXProcNamespace(name).exception()
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
            throw XProcError.xsInvalidElement(NsP.declareStep).exception()
        }

        val decl = DeclareStepInstruction(this, stepConfig.copyNew())
        _children.add(decl)
        return decl
    }

    override fun import(import: StepContainerInterface) {
        _imported.add(import)
    }

    override fun toString(): String {
        if (type == null) {
            if (name.startsWith("!")) {
                return "${instructionType}/${id}"
            }
            return "${instructionType}/${id} \"${name}\""
        }
        if (name.startsWith("!")) {
            return "${instructionType}/${id}: ${type}"
        }
        return "${instructionType}/${id}: ${type} \"${name}\""
    }
}