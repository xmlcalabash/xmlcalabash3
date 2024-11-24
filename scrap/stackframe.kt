    private class ParserState() {
        val stack = Stack<StackFrame>()
        init {
            stack.push(StackFrame(null, "root()"))
        }

        val nt: String
            get() {
                return stack.peek().nt
            }

        val context: String
            get() {
                return stack.peek().context
            }

        fun clear() {
            stack.clear()
            stack.push(StackFrame(null, "root()"))
        }

        fun push(nt: String) {
            val frame = StackFrame(stack.peek(), nt)
            if (nt == "VarRef" || nt == "Predicate") {
                frame.context = nt
            }
            stack.push(frame)
        }

        fun pop(nt: String) {
            if (this.nt != nt) {
                throw RuntimeException("Attempt to pop ${nt} when ${this.nt} was the top of the stack")
            }
            stack.pop()
        }

        fun inScopeDecl(name: QName): Boolean {
            for (index in stack.indices.reversed()) {
                if (stack[index].names.contains(name)) {
                    return true
                }
            }
            return false
        }

        fun position(ancestors: List<String>): Boolean {
            return find(ancestors, ancestors.size-1, stack.size-1)
        }

        private fun find(ancestors: List<String>, index: Int, stackPos: Int): Boolean {
            if (stack[stackPos].nt == ancestors[index]) {
                if (index == 0) {
                    return true
                }
                return find(ancestors, index - 1, stackPos - 1)
            }
            if (stackPos < index) {
                return false
            }
            return find(ancestors, index, stackPos - 1)
        }

        fun addQuantifiedDeclaration(name: QName) {
            for (index in stack.indices.reversed()) {
                if (stack[index].nt == "QuantifiedExpr") {
                    stack[index].names.add(name)
                    return
                }
            }
        }

        fun parameterDecl(): Boolean {
            return stack.size > 3
                    && stack[stack.size - 1].nt == "QName"
                    && stack[stack.size - 2].nt == "Param"
                    && stack[stack.size - 3].nt == "ParamList"
        }

    }

    private class StackFrame(frame: StackFrame?, val nt: String) {
        val names = mutableSetOf<QName>()
        var context = "root()"

        init {
            if (frame != null) {
                names.addAll(frame.names)
                context = frame.context
            }
        }

        override fun toString(): String {
            return "${nt}: $names"
        }
    }
