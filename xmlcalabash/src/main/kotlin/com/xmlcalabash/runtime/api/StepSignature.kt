package com.xmlcalabash.runtime.api

import com.xmlcalabash.config.XmlCalabashConfiguration
import net.sf.saxon.s9api.QName

class StepSignature(
    val xmlCalabashConfiguration: XmlCalabashConfiguration,
    val inputs: Map<String, RuntimePort>,
    val outputs: Map<String, RuntimePort>,
    val options: Map<QName, RuntimeOption>)
