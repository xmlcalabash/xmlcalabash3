package com.xmlcalabash.ext.sendmail

import com.xmlcalabash.namespace.NsP
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class SendMailStepProvider: SimpleStepProvider(
    QName(NsP.namespace, "p:send-mail"),
    { -> SendMailStep() })