package com.xmlcalabash.util

import com.xmlcalabash.config.StepConfiguration
import com.xmlcalabash.exceptions.XProcError
import com.xmlcalabash.runtime.XProcStepConfiguration

/*
This class parses value templates. Value templates have the form "string{expr}s{{t}}ring{expr}string" etc.
The "string" portions can be empty. The tricky bit is that the "expr" portions are XPath expressions
and can contain { and }. Less tricky, is that outside an expr, {{ represents { and }} represents }.
So, the example above is "string", "expr", "s{t}ring", "expr", "string".

Inside an expr, { and } are ignored inside strings or comments. An unbalanced } ends the expression.
(I don't think a balanced pair of { } is valid in XPath 3.1, but let's look towards the future.)
*/

class ValueTemplateParser private constructor(private val stepConfig: StepConfiguration?, val expr: String) {
    companion object {
        private const val DOUBLEQUOTE = "\""
        private const val SINGLEQUOTE ="'"

        internal fun testParse(expr: String): ValueTemplate {
            val parser = ValueTemplateParser(null, expr)
            return parser.template()
        }

        fun parse(config: StepConfiguration, expr: String): ValueTemplate {
            val parser = ValueTemplateParser(config, expr)
            return parser.template()
        }
    }

    private var pos = 0;

    private val more: Boolean
        get() = pos < expr.length

    private val current: String
        get() = if (more) {
            expr.substring(pos, pos+1)
        } else {
            ""
        }

    private fun peek(next: String): Boolean {
        if (pos+2 > expr.length) {
            return false
        }
        return more && next == expr.subSequence(pos+1,pos+2)
    }

    fun template(): ValueTemplate {
        val template: MutableList<String> = mutableListOf()
        while (more) {
            val strlit = parseString()
            template.add(strlit)
            if (more) {
                val strexpr = parseExpr()
                template.add(strexpr)
            }
        }
        return ValueTemplate(template)
    }

    private fun parseString(): String {
        val acc = StringBuilder()
        var moreString = more
        while (moreString) {
            when (current) {
                "{" ->
                    if (peek("{")) {
                        pos += 1
                        acc.append(current)
                        pos += 1
                        moreString = more
                    } else {
                        pos += 1
                        moreString = false
                        if (current == "") {
                            if (stepConfig == null) {
                                throw XProcError.xsInvalidAVT("Unmatched open brace at end of string").exception()
                            }
                            throw stepConfig.exception(XProcError.xsInvalidAVT("Unmatched open brace at end of string"))
                        }
                    }
                "}" ->
                    if (peek("}")) {
                        pos += 1
                        acc.append(current)
                        pos += 1
                        moreString = more
                    } else {
                        if (stepConfig == null) {
                            throw XProcError.xsInvalidAVT("Unescaped closing brace in string").exception()
                        }
                        throw stepConfig.exception(XProcError.xsInvalidAVT("Unescaped closing brace in string"))
                    }
                else -> {
                    acc.append(current)
                    pos += 1
                    moreString = more
                }
            }
        }
        return acc.toString()
    }

    private fun parseExpr(): String {
        val acc = StringBuilder()
        var moreString = more
        var commentDepth = 0
        var braceDepth = 1
        var state = State.ORDINARY

        while (moreString) {
            when (state) {
                State.ORDINARY -> {
                    when (current) {
                        "(" -> {
                            if (peek(":")) {
                                state = State.COMMENT
                                commentDepth += 1
                            }
                            acc.append(current)
                            pos += 1
                            moreString = more
                        }
                        "\"" -> {
                            state = State.DSTRING
                            acc.append(current)
                            pos += 1
                            moreString = more
                        }
                        "'" -> {
                            state = State.SSTRING
                            acc.append(current)
                            pos += 1
                            moreString = more
                        }
                        "{" -> {
                            braceDepth += 1
                            acc.append(current)
                            pos += 1
                            moreString = more
                        }
                        "}" -> {
                            braceDepth -= 1
                            if (braceDepth > 0) {
                                acc.append(current)
                                moreString = more
                            } else {
                                moreString = false
                            }
                            pos += 1
                        }
                        else -> {
                            acc.append(current)
                            pos += 1
                            moreString = more
                        }
                    }
                }

                State.SSTRING, State.DSTRING -> {
                    when (current) {
                        DOUBLEQUOTE -> {
                            if (state == State.DSTRING) {
                                if (peek(DOUBLEQUOTE)) {
                                    acc.append("\"\"")
                                    pos += 2
                                } else {
                                    acc.append(current)
                                    pos += 1
                                    state = State.ORDINARY
                                }
                            } else {
                                acc.append(current)
                                pos += 1
                            }
                            moreString = more
                        }
                        SINGLEQUOTE -> {
                            if (state == State.SSTRING) {
                                if (peek(SINGLEQUOTE)) {
                                    acc.append("''")
                                    pos += 2
                                } else {
                                    acc.append(current)
                                    pos += 1
                                    state = State.ORDINARY
                                }
                            } else {
                                acc.append(current)
                                pos += 1
                            }
                            moreString = more
                        }
                        else -> {
                            acc.append(current)
                            pos += 1
                            moreString = more
                        }
                    }
                }
                State.COMMENT -> {
                    when (current) {
                        "(" -> {
                            if (peek(":")) {
                                commentDepth += 1
                            }
                            acc.append(current)
                            pos += 1
                            moreString = more
                        }
                        ":" -> {
                            if (peek(")")) {
                                commentDepth -= 1
                                acc.append(":)")
                                pos += 2
                                if (commentDepth == 0) {
                                    state = State.ORDINARY
                                }
                            } else {
                                acc.append(current)
                                pos += 1
                            }
                            moreString = more
                        }
                        else -> {
                            acc.append(current)
                            pos += 1
                            moreString = more
                        }
                    }
                }
            }
        }

        if (state != State.ORDINARY) {
            if (stepConfig == null) {
                throw XProcError.xsInvalidAVT("Ended in quoted string or comment").exception()
            }
            throw stepConfig.exception(XProcError.xsInvalidAVT("Ended in quoted string or comment"))
        }

        if (braceDepth != 0) {
            if (stepConfig == null) {
                throw XProcError.xsInvalidAVT("Unmatched opening brace").exception()
            }
            throw stepConfig.exception(XProcError.xsInvalidAVT("Unmatched opening brace"))
        }

        return acc.toString()
    }

    private enum class State {
        ORDINARY, COMMENT, SSTRING, DSTRING
    }
}