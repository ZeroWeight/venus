package venus.assembler

import venus.riscv.MemorySegments
import venus.riscv.Program
import venus.linker.RelocationInfo

typealias LineTokens = List<String>

object Assembler {
    fun assemble(text: String): Program {
        return AssemblyFile(text).assemble()
    }

    private class AssemblyFile(val text: String) {
        val prog = Program()
        private var currentTextOffset = MemorySegments.TEXT_BEGIN
        private var currentDataOffset = MemorySegments.STATIC_BEGIN
        private var inTextSegment = true
        private val TALInstructions = ArrayList<LineTokens>()
        private val symbolTable = HashMap<String, Int>()
        private val relocationTable = ArrayList<RelocationInfo>()

        fun assemble(): Program {
            passOne()
            passTwo()
            return prog
        }

        private fun passOne() {
            for (line in text.split('\n')) {
                val offset = getOffset()

                val (label, args) = Lexer.lexLine(line)
                if (label != "" && isGlobalLabel(label)) {
                    symbolTable.put(label, offset)
                }

                if (args.size == 0 || args[0] == "") continue; // empty line

                if (isAssemblerDirective(args[0])) {
                    parseAssemblerDirective(args, line)
                } else {
                    val expandedInsts = replacePseudoInstructions(args)
                    TALInstructions.addAll(expandedInsts)
                    currentTextOffset += 4 * expandedInsts.size
                }
            }

            for ((label, offset) in symbolTable) {
                prog.addLabel(label, offset)
            }

            for ((label, offset) in relocationTable) {
                prog.addRelocation(label, offset)
            }
        }

        private fun passTwo() {
            for (inst in TALInstructions) {
                addInstruction(inst)
            }
        }

        private fun addInstruction(tokens: LineTokens) {
            if (tokens.size < 1 || tokens[0] == "") return
            val cmd = getInstruction(tokens)
            val disp: WriterDispatcher = try {
                WriterDispatcher.valueOf(cmd)
            } catch (e: IllegalStateException) {
                throw AssemblerError("no such instruction ${cmd}")
            }
            disp.writer(prog, disp.iform, tokens.drop(1))
        }

        private fun replacePseudoInstructions(tokens: LineTokens): List<LineTokens> {
            try {
                val cmd = getInstruction(tokens)
                val pw = PseudoDispatcher.valueOf(cmd).pw
                return pw(tokens)
            } catch (t: Throwable) {
                /* TODO: don't use throwable here */
                /* not a pseudoinstruction, or expansion failure */
                return listOf(tokens)
            }
        }

        private fun parseAssemblerDirective(args: LineTokens, line: String) {
            val directive = args[0]
            when (directive) {
                ".data" -> inTextSegment = false
                ".text" -> inTextSegment = true

                ".byte" -> {
                    for (arg in args.drop(1)) {
                        prog.addToData(arg.toByte())
                        currentDataOffset++
                    }
                }

                ".asciiz" -> {
                    val asciiString = Lexer.lexAsciizDirective(line)

                    if (asciiString == null) {
                        throw AssemblerError("expected a quoted string: ${line}")
                    }

                    for (c in asciiString) {
                        if (c.toInt() !in 0..127) {
                            throw AssemblerError("unexpected non-ascii character: ${c}")
                        }
                        prog.addToData(c.toByte())
                        currentDataOffset++
                    }

                    prog.addToData(0)
                    currentDataOffset++
                }

                else -> throw AssemblerError("unknown assembler directive ${directive}")
            }
        }

        private fun getOffset() = if (inTextSegment) currentTextOffset else currentDataOffset
        private fun isAssemblerDirective(cmd: String) = cmd.startsWith(".")
        private fun getInstruction(tokens: LineTokens) = tokens[0].toLowerCase()
        private fun isGlobalLabel(label: String) = !label.startsWith("_")
    }
    /* TODO: add actual parser */
}