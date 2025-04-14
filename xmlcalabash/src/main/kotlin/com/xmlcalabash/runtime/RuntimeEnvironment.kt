package com.xmlcalabash.runtime

import com.xmlcalabash.api.Monitor
import com.xmlcalabash.config.XProcEnvironment
import com.xmlcalabash.datamodel.CompileEnvironment
import java.util.*

class RuntimeEnvironment(override val episode: String, environment: CompileEnvironment): XProcEnvironment by environment {
    override val monitors = mutableListOf<Monitor>()

    companion object {
        fun newInstance(environment: CompileEnvironment): RuntimeEnvironment {
            val episode = "E-${UUID.randomUUID()}"
            return RuntimeEnvironment(episode, environment)
        }
    }
}