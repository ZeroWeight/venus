package venus.riscv

/** Masks to get specific fields from RV32 instruction formats */
enum class InstructionFormat(val mask: Int) {
    /* requires .toInt() as per KT-4749 */
    OPCODE              (0b00000000000000000000000001111111.toInt()),
    RD                  (0b00000000000000000000111110000000.toInt()),
    FUNCT3              (0b00000000000000000111000000000000.toInt()),
    RS1                 (0b00000000000011111000000000000000.toInt()),
    RS2                 (0b00000001111100000000000000000000.toInt()),
    FUNCT7              (0b11111110000000000000000000000000.toInt()),
    IMM_11_0            (0b11111111111100000000000000000000.toInt()),
    IMM_4_0             (0b00000000000000000000111110000000.toInt()),
    IMM_11_5            (0b11111110000000000000000000000000.toInt()),
    IMM_11_B            (0b00000000000000000000000010000000.toInt()),
    IMM_4_1             (0b00000000000000000000111100000000.toInt()),
    IMM_31_12           (0b11111111111111111111000000000000.toInt()),
    IMM_19_12           (0b00000000000011111111000000000000.toInt()),
    IMM_11_J            (0b00000000000100000000000000000000.toInt()),
    IMM_10_0            (0b01111111111000000000000000000000.toInt()),
    IMM_20              (0b10000000000000000000000000000000.toInt()),
}