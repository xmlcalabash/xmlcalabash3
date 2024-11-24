package com.xmlcalabash.steps.validation

import net.sf.saxon.lib.ErrorReporter
import net.sf.saxon.lib.Invalidity
import net.sf.saxon.lib.InvalidityHandler
import net.sf.saxon.om.Sequence
import net.sf.saxon.s9api.XdmEmptySequence
import net.sf.saxon.s9api.XmlProcessingError
import net.sf.saxon.type.ValidationException
import net.sf.saxon.value.EmptySequence
import org.apache.logging.log4j.kotlin.logger
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException
import javax.xml.transform.ErrorListener
import javax.xml.transform.TransformerException
import java.lang.Exception

class CachingErrorListener(val errors: Errors, val invalidityHandler: InvalidityHandler?): ErrorHandler, ErrorListener, ErrorReporter, InvalidityHandler {
    private val _exceptions = mutableListOf<Exception>()
    var listener: ErrorListener? = null
    var reporter: ErrorReporter? = null
    var handler: ErrorHandler? = null

    constructor(errors: Errors): this(errors, null)

    private var showErrors = false

    constructor(errors: Errors, listener: ErrorListener): this(errors) {
        this.listener = listener
    }

    constructor(errors: Errors, reporter: ErrorReporter): this(errors) {
        this.reporter = reporter
    }

    constructor(errors: Errors, handler: ErrorHandler): this(errors) {
        this.handler = handler
    }

    val exceptions: List<Exception>
        get() = _exceptions

    override fun warning(exception: SAXParseException?) {
        if (exception == null) {
            logger.debug { "Call to error listener with null exception!?"}
            return
        }

        if (handler != null) {
            handler!!.warning(exception)
        }
        if (showErrors) {
            println(exception.toString())
        }
        report(exception)
    }

    override fun error(exception: SAXParseException?) {
        if (exception == null) {
            logger.debug { "Call to error listener with null exception!?"}
            return
        }

        if (handler != null) {
            handler!!.error(exception)
        }
        if (showErrors) {
            println(exception)
        }
        report(exception)
        _exceptions += exception
    }

    override fun fatalError(exception: SAXParseException?) {
        if (exception == null) {
            logger.debug { "Call to error listener with null exception!?"}
            return
        }

        if (handler != null) {
            handler!!.fatalError(exception)
        }
        if (showErrors) {
            println(exception)
        }
        report(exception)
        _exceptions += exception
    }

    override fun warning(exception: TransformerException?) {
        if (exception == null) {
            logger.debug { "Call to error listener with null exception!?"}
            return
        }

        if (listener != null) {
            listener!!.warning(exception)
        }
        if (showErrors) {
            println(exception.toString())
        }
        report(exception)
    }

    override fun error(exception: TransformerException?) {
        if (exception == null) {
            logger.debug { "Call to error listener with null exception!?"}
            return
        }

        if (listener != null) {
            listener!!.error(exception)
        }
        if (showErrors) {
            println(exception.toString())
        }
        report(exception)
        _exceptions.add(exception)
    }

    override fun fatalError(exception: TransformerException?) {
        if (exception == null) {
            logger.debug { "Call to error listener with null exception!?"}
            return
        }

        if (listener != null) {
            listener!!.fatalError(exception)
        }
        if (showErrors) {
            println(exception.toString())
        }
        report(exception)
        _exceptions.add(exception)
    }

    override fun report(xmlProcessingError: XmlProcessingError?) {
        if (showErrors) {
            println(xmlProcessingError)
        }
    }

    private fun report(exception: Exception) {
        when (exception) {
            is ValidationException -> {
                val msg = exception.message!!
                val fail = exception.validationFailure
                errors.xsdValidationError(msg, fail)
            }
            else ->
                errors.xsdValidationError(exception.localizedMessage!!)
        }
    }

    override fun startReporting(systemId: String?) {
        invalidityHandler?.startReporting(systemId)
    }

    override fun reportInvalidity(failure: Invalidity?) {
        invalidityHandler?.reportInvalidity(failure)

        if (failure == null) {
            return
        }

        if (showErrors) {
            println(failure.message)
        }
    }

    override fun endReporting(): Sequence {
        return invalidityHandler?.endReporting() ?: EmptySequence.getInstance()
    }
}