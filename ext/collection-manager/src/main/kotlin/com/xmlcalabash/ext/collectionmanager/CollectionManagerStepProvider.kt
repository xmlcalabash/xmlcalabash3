package com.xmlcalabash.ext.collectionmanager

import com.xmlcalabash.namespace.NsCx
import com.xmlcalabash.util.SimpleStepProvider
import net.sf.saxon.s9api.QName

class CollectionManagerStepProvider: SimpleStepProvider(
    QName(NsCx.namespace, "cx:collection-manager"),
    { -> CollectionManagerStep() })