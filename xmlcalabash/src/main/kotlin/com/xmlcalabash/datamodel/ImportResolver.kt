package com.xmlcalabash.datamodel

class ImportResolver() {
    companion object {
        fun resolve(container: StepContainerInterface) {
            directlyExported(container, mutableSetOf())
            visibleInside(container, mutableSetOf())
        }

        private fun directlyExported(container: StepContainerInterface, visited: MutableSet<StepContainerInterface>) {
            /*
            if (visited.contains(container)) {
                return
            }

            //println("Computing directly exported for ${container}")
            visited.add(container)

            container.clearExports()
            when (container) {
                is DeclareStepInstruction -> {
                    if (container.type != null && container.visibility != Visibility.PRIVATE) {
                        container.addExport(container.type!!, container)
                    }
                    for (child in container.containedSteps()) {
                        directlyExported(child, visited)
                    }
                }
                is LibraryInstruction -> {
                    for (child in container.containedSteps()) {
                        directlyExported(child, visited)
                        if (child.type != null && child.visibility != Visibility.PRIVATE) {
                            container.addExport(child.type!!, child)
                        }
                    }
                }
                else -> throw IllegalStateException("Unsupported container: ${container}")
            }

            for (import in container.imported()) {
                directlyExported(import, visited)
            }

             */
        }

        private fun visibleInside(container: StepContainerInterface, visited: MutableSet<StepContainerInterface>) {
            /*
            if (visited.contains(container)) {
                return
            }

            //println("Computing visible inside for ${container}")
            visited.add(container)

            val stepConfig = (container as XProcInstruction).stepConfig
            stepConfig.inscopeStepTypes = mapOf()
            if (container is DeclareStepInstruction) {
                container.type?.let { stepConfig.addVisibleStepType(container) }
            }

            for (step in container.containedSteps().filter { it.type != null }) {
                stepConfig.addVisibleStepType(step)
            }

            for (import in container.imported()) {
                traverse(container, setOf(), import)
            }

            for (step in container.containedSteps()) {
                if (container is LibraryInstruction) {
                    step.stepConfig.inscopeStepTypes = stepConfig.inscopeStepTypes
                }
                visibleInside(step, visited)
            }

            for (import in container.imported()) {
                visibleInside(import, visited)
            }
             */
        }

        private fun traverse(container: StepContainerInterface, visited: Set<StepContainerInterface>, import: StepContainerInterface) {
            /*
            if (visited.contains(import)) {
                return
            }

            val stepConfig = (container as XProcInstruction).stepConfig
            for ((name, decl) in import.exports()) {
                stepConfig.addVisibleStepType(decl)
            }

            for (nested in container.imported()) {
                traverse(container, visited + import, nested)
            }
             */
        }
    }
}
