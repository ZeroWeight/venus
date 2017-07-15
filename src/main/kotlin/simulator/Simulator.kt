package venus.simulator

import venus.riscv.Instruction
import venus.riscv.InstructionField
import venus.riscv.MemorySegments
import venus.riscv.Program
import venus.simulator.diffs.* // ktlint-disable no-wildcard-imports

/** Right now, this is a loose wrapper around SimulatorState
    Eventually, it will support debugging. */
class Simulator(prog: Program) {
    private val state = SimulatorState()
    var maxpc = MemorySegments.TEXT_BEGIN
    var cycles = 0
    val MAX_CYCLES = 1000
    val history = History()
    val preInstruction = ArrayList<Diff>()
    val postInstruction = ArrayList<Diff>()

    init {
        state.pc = MemorySegments.TEXT_BEGIN
        for (inst in prog.insts) {
            /* TODO: abstract away instruction length */
            state.mem.storeWord(maxpc, inst.getField(InstructionField.ENTIRE))
            maxpc += inst.length
        }

        var dataOffset = MemorySegments.STATIC_BEGIN
        for (datum in prog.dataSegment) {
            state.mem.storeByte(dataOffset, datum.toInt())
            dataOffset++
        }

        state.setReg(2, MemorySegments.STACK_BEGIN)
        state.setReg(3, MemorySegments.STATIC_BEGIN)
    }

    fun isDone(): Boolean = state.pc >= maxpc || cycles > MAX_CYCLES

    fun run() {
        while (!isDone()) {
            step()
            cycles++
        }
    }

    fun step(): List<Diff> {
        preInstruction.clear()
        postInstruction.clear()
        /* TODO: abstract away instruction length */
        val inst: Instruction = Instruction(state.mem.loadWord(state.pc))
        val impl = InstructionDispatcher.dispatch(inst)
        /* TODO: throw an error here */
        if (impl == null) return listOf()
        impl(inst, this)
        history.add(preInstruction)
        return postInstruction.toList()
    }

    fun undo() {
        if (history.isEmpty()) return /* TODO: error here? */
        val beforeLast = history.pop()
        for (diff in beforeLast) {
            diff(state)
        }
    }
    fun getReg(id: Int) = state.getReg(id)

    fun setReg(id: Int, v: Int) {
        preInstruction.add(RegisterDiff(id, state.getReg(id)))
        state.setReg(id, v)
        postInstruction.add(RegisterDiff(id, state.getReg(id)))
    }

    fun getPC() = state.pc

    fun setPC(newPC: Int) {
        preInstruction.add(PCDiff(state.pc))
        state.pc = newPC
        postInstruction.add(PCDiff(state.pc))
    }

    fun incrementPC(inc: Int) {
        preInstruction.add(PCDiff(state.pc))
        state.pc += inc
        postInstruction.add(PCDiff(state.pc))
    }

    fun loadByte(addr: Int): Int = state.mem.loadByte(addr)
    fun loadHalfWord(addr: Int): Int = state.mem.loadHalfWord(addr)
    fun loadWord(addr: Int): Int = state.mem.loadWord(addr)
    fun storeByte(addr: Int, value: Int) = state.mem.storeByte(addr, value)
    fun storeHalfWord(addr: Int, value: Int) = state.mem.storeHalfWord(addr, value)
    fun storeWord(addr: Int, value: Int) = state.mem.storeWord(addr, value)
}