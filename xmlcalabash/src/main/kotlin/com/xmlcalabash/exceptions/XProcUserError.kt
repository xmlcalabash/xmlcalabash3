package com.xmlcalabash.exceptions

import com.xmlcalabash.datamodel.Location
import com.xmlcalabash.documents.XProcDocument
import net.sf.saxon.s9api.QName

class XProcUserError(code: QName, location: Location, inputLocation: Location, vararg documents: XProcDocument)
    : XProcError(code, 1, location, inputLocation, *documents)
{
}