package com.xmlcalabash.util

import com.xmlcalabash.api.MessageReporter
import com.xmlcalabash.namespace.Ns
import net.sf.saxon.s9api.QName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BufferingMessageReporter(val maxsize: Int, nextReporter: MessageReporter): NopMessageReporter(nextReporter) {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'X'")
    }

    private val _reports = mutableListOf<Report>()

    fun messages(threshold: Verbosity): List<Report> {
        return _reports.filter { it.severity >= threshold }
    }

    fun clear() {
        synchronized(_reports) {
            _reports.clear()
        }
    }

    override fun report(severity: Verbosity, report: () -> Report) {
        val reified = report()
        reified.addDetail(Ns.date, LocalDateTime.now().format(formatter))

        while (_reports.size >= maxsize) {
            _reports.removeAt(0)
        }
        _reports.add(reified)

        nextReporter?.report(severity) { reified }
    }
}