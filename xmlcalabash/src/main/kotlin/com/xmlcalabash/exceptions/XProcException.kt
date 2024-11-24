package com.xmlcalabash.exceptions


class XProcException(val error: XProcError, cause: Throwable? = null): RuntimeException(error.toString(), cause) {
}