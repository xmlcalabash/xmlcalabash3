package com.xmlcalabash.ext.pipelinemessages

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class PipelineMessagesStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:pipeline-messages"),
    { -> PipelineMessagesStep() })