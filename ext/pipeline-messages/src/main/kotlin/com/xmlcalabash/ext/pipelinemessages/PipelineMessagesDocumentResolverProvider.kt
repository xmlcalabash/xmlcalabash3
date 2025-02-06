package com.xmlcalabash.ext.pipelinemessages

import com.xmlcalabash.util.SimpleDocumentResolverProvider
import java.net.URI

class PipelineMessagesDocumentResolverProvider: SimpleDocumentResolverProvider(
    URI("https://xmlcalabash.com/ext/library/pipeline-messages.xpl"),
    "/com/xmlcalabash/ext/pipeline-messages.xpl")