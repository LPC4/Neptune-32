package org.lpc.instructions;

import org.lpc.CPU;
import org.lpc.memory.Memory;
import org.lpc.memory.MemoryHandler;
import org.lpc.memory.io.IODeviceManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.lpc.instructions.InstructionUtils;

/*
 * NeptuneInstructionSet - Complete Instruction Reference
 *
 * Format: [instruction] [arguments] - Description
 *
 * =====================================================
 * Arithmetic Instructions:
 * =====================================================
 * ADD   rDest, rSrc      - Add rSrc to rDest, store in rDest, update flags
 * ADDI  rDest, imm       - Add immediate to rDest, store in rDest, update flags
 * SUB   rDest, rSrc      - Subtract rSrc from rDest, store in rDest, update flags
 * SUBI  rDest, imm       - Subtract immediate from rDest, store in rDest, update flags
 * MUL   rDest, rSrc      - Multiply rDest by rSrc, store in rDest, update flags
 * MULI  rDest, imm       - Multiply rDest by immediate, store in rDest, update flags
 * DIV   rDest, rSrc      - Divide rDest by rSrc, store in rDest, update flags (throws if divide by zero)
 * DIVI  rDest, imm       - Divide rDest by immediate, store in rDest, update flags (throws if divide by zero)
 * MOD   rDest, rSrc      - Modulo rDest by rSrc, store in rDest, update flags (throws if modulo by zero)
 * MODI  rDest, imm       - Modulo rDest by immediate, store in rDest, update flags (throws if modulo by zero)
 * INC   rDest            - Increment rDest by 1, update flags
 * DEC   rDest            - Decrement rDest by 1, update flags
 * NEG   rDest            - Negate rDest, update flags
 *
 * =====================================================
 * Logical Instructions:
 * =====================================================
 * AND   rDest, rSrc      - Bitwise AND rDest with rSrc, store in rDest, update flags
 * ANDI  rDest, imm       - Bitwise AND rDest with immediate, store in rDest, update flags
 * OR    rDest, rSrc      - Bitwise OR rDest with rSrc, store in rDest, update flags
 * ORI   rDest, imm       - Bitwise OR rDest with immediate, store in rDest, update flags
 * XOR   rDest, rSrc      - Bitwise XOR rDest with rSrc, store in rDest, update flags
 * XORI  rDest, imm       - Bitwise XOR rDest with immediate, store in rDest, update flags
 * NOT   rDest            - Bitwise NOT of rDest, store in rDest, update flags
 *
 * =====================================================
 * Shift Instructions:
 * =====================================================
 * SHL   rDest, shift     - Shift rDest left by shift bits, store in rDest, update flags
 * SHR   rDest, shift     - Logical shift rDest right by shift bits, store in rDest, update flags
 *
 * =====================================================
 * Memory Instructions:
 * =====================================================
 * LOAD  rDest, rAddr     - Load word from memory at rAddr into rDest, update flags
 * STORE rSrc, rAddr      - Store word from rSrc into memory at rAddr
 * LOADI rDest, immA      - Load word from memory at imm into rDest, update flags
 * STORI rSrc, imm        - Store word into memory at imm
 * MSET  rAddr, rValue    - Set r1 words starting at rAddr to rValue
 * MCPY  rDest, rSrc      - Copy r1 words from rSrc to rDest
 *
 * =====================================================
 * Control Flow Instructions:
 * =====================================================
 * JMP   address          - Jump unconditionally
 * JZ    address          - Jump if zero flag is set (==)
 * JE    address          - Jump if zero flag is set (==)
 * JNZ   address          - Jump if zero flag is not set (!=)
 * JN    address          - Jump if negative flag is set (signed < 0)
 * JP    address          - Jump if negative flag is not set (signed >= 0)
 *
 * JG    address          - Jump if greater (signed >)     : zero flag not set AND negative flag not set
 * JGE   address          - Jump if greater or equal (>=)  : negative flag not set
 * JL    address          - Jump if less (signed <)        : negative flag set
 * JLE   address          - Jump if less or equal (<=)     : negative flag set OR zero flag set
 *
 * JC    address          - Jump if carry flag is set (unsigned <)
 * JNC   address          - Jump if carry flag is not set (unsigned >=)
 * JA    address          - Jump if above (unsigned >)     : carry flag not set AND zero flag not set
 * JAE   address          - Jump if above or equal (>=)    : carry flag not set
 * JB    address          - Jump if below (unsigned <)     : carry flag set
 * JBE   address          - Jump if below or equal (<=)    : carry flag set OR zero flag set
 *
 * CALL  address          - Push PC to stack, jump to address (2-word instruction)
 * RET                    - Pop PC from stack and jump back
 *
 * =====================================================
 * Stack Instructions:
 * =====================================================
 * PUSH  rSrc             - Push register rSrc onto stack
 * POP   rDest            - Pop from stack into rDest, update flags
 *
 * =====================================================
 * Data Movement Instructions:
 * =====================================================
 * MOV   rDest, rSrc      - Copy value from rSrc into rDest, update flags
 * MOVI  rDest, imm       - Load immediate imm into rDest, update flags (2-word instruction)
 * MSET  rDest, rSrc      - Set r1=amount, set rDest - rDest + amount to rSrc (word)
 * MCPY  rDest, rSrc      - Set r1=amount, copy range from rSrc - rSrc + amount to rDest (addr)
 * CLR   rDest            - Clear rDest (set to 0), update flags
 *
 * =====================================================
 * Comparison Instructions:
 * =====================================================
 * CMP   rA, rB           - Compare rA with rB (rA - rB), update flags (no register change)
 * CMPI  rA, imm          - Compare rA with immediate, update flags (no register change)
 * TEST  rA, rB           - Bitwise AND of rA and rB, update flags (no register change)
 * TESTI rA, imm          - Bitwise AND of rA and immediate, update flags (no register change)
 *
 * =====================================================
 * System Instructions:
 * =====================================================
 * SYSCALL               - Executes system call specified by r0:
 * NOP                   - No operation
 * HLT                   - Halt the CPU
 *
 * =====================================================
 * Immediate Values:
 * Can be specified as:
 * - Decimal: 42
 * - Hexadecimal: 0x2A
 *
 * =====================================================
 * Flag Behavior:
 * - Zero flag (Z): Set when result is zero
 * - Negative flag (N): Set when result is negative
 * - Overflow flag (V): Set when arithmetic overflow occurs
 * - Carry flag (C): Set when carry occurs
 *
 * =====================================================
 * Memory Access:
 * - All memory operations use word (32-bit) addressing
 * - Memory is byte-addressable but accessed in words
 */

public class NeptuneInstructionSet implements InstructionSet {
    private final Map<Byte, Instruction> instructionMap = new HashMap<>();
    private final Map<String, Byte> nameToOpcode = new HashMap<>();
    private final Map<Byte, String> opcodeToName = new HashMap<>();
    private byte nextOpcode = 1;

    public NeptuneInstructionSet() {
        registerOpcodes();
        //logInstructions();
    }

    private void registerOpcodes() {
        registerArithmeticInstructions();
        registerLogicalInstructions();
        registerShiftInstructions();
        registerMemoryInstructions();
        registerControlFlowInstructions();
        registerStackInstructions();
        registerDataMovementInstructions();
        registerComparisonInstructions();
        registerSystemInstructions();
    }

    // Helper method for splitting arguments
    private String[] splitArgs(String args, int expectedCount) {
        String[] parts = args.split(",");
        if (parts.length != expectedCount) {
            throw new IllegalArgumentException("Invalid arguments: " + args);
        }
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    private void registerArithmeticInstructions() {
        // Register-register operations
        registerBinaryOp("ADD", Integer::sum, true);
        registerBinaryOp("SUB", (a, b) -> a - b, true);
        registerBinaryOp("MUL", (a, b) -> a * b, false);
        registerBinaryOp("DIV", (a, b) -> {
            if (b == 0) throw new ArithmeticException("Division by zero");
            return a / b;
        }, false);
        registerBinaryOp("MOD", (a, b) -> {
            if (b == 0) throw new ArithmeticException("Modulo by zero");
            return a % b;
        }, false);

        // Register-immediate operations
        registerBinaryImmediateOp("ADDI", Integer::sum, true);
        registerBinaryImmediateOp("SUBI", (a, b) -> a - b, true);
        registerBinaryImmediateOp("MULI", (a, b) -> a * b, false);
        registerBinaryImmediateOp("DIVI", (a, b) -> {
            if (b == 0) throw new ArithmeticException("Division by zero");
            return a / b;
        }, false);
        registerBinaryImmediateOp("MODI", (a, b) -> {
            if (b == 0) throw new ArithmeticException("Modulo by zero");
            return a % b;
        }, false);

        register("INC", new UnaryRegisterInstruction("INC") {
            @Override
            protected int calculate(int value) {
                return value + 1;
            }
        });

        register("DEC", new UnaryRegisterInstruction("DEC") {
            @Override
            protected int calculate(int value) {
                return value - 1;
            }
        });

        register("NEG", new UnaryRegisterInstruction("NEG") {
            @Override
            protected int calculate(int value) {
                return -value;
            }
        });
    }

    private void registerLogicalInstructions() {
        // Register-register operations
        registerBinaryOp("AND", (a, b) -> a & b, false);
        registerBinaryOp("OR", (a, b) -> a | b, false);
        registerBinaryOp("XOR", (a, b) -> a ^ b, false);

        // Register-immediate operations
        registerBinaryImmediateOp("ANDI", (a, b) -> a & b, false);
        registerBinaryImmediateOp("ORI", (a, b) -> a | b, false);
        registerBinaryImmediateOp("XORI", (a, b) -> a ^ b, false);

        register("NOT", new UnaryRegisterInstruction("NOT") {
            @Override
            protected int calculate(int value) {
                return ~value;
            }
        });
    }

    private void registerShiftInstructions() {
        register("SHL", createShiftInstruction("SHL", (a, s) -> a << s));
        register("SHR", createShiftInstruction("SHR", (a, s) -> a >>> s));
    }

    private void registerMemoryInstructions() {
        register("LOAD", createMemoryInstruction("LOAD", true));
        register("STORE", createMemoryInstruction("STORE", false));
        // LOADI implementation
        register("LOADI", new ImmediateInstruction("LOADI") {
            @Override
            protected void executeImmediate(CPU cpu, int rDest, int immediate) {
                cpu.setRegister(rDest, immediate);
                cpu.getFlags().update(immediate);  // Update flags based on the loaded value
            }
        });

        // STOREI implementation
        register("STORI", new ImmediateInstruction("STORI") {
            @Override
            protected void executeImmediate(CPU cpu, int rDest, int immediate) {
                int value = cpu.getRegister(rDest);
                cpu.getMemory().writeWord(immediate, value);
            }
        });

        register("MSET", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rAddr = InstructionUtils.extractRegister(words[0], 16);
                int rValue = InstructionUtils.extractRegister(words[0], 8);
                int addr = cpu.getRegister(rAddr);
                int value = cpu.getRegister(rValue);
                int count = cpu.getRegister(1); // r1 holds count

                for (int i = 0; i < count; i++) {
                    cpu.getMemory().writeWord(addr + i * 4, value);
                }
            }

            @Override
            public int[] encode(String args) {
                var split = splitArgs(args, 2);
                int rAddr = parseRegister(split[0]);
                int rValue = parseRegister(split[1]);
                return new int[]{InstructionUtils.encodeInstruction(rAddr, rValue, getOpcode("MSET"))};
            }
        });

        register("MCPY", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rDest = InstructionUtils.extractRegister(words[0], 16);
                int rSrc = InstructionUtils.extractRegister(words[0], 8);
                int destAddr = cpu.getRegister(rDest);
                int srcAddr = cpu.getRegister(rSrc);
                int count = cpu.getRegister(1); // r1 holds count

                // Handle overlapping regions by copying backward if dest > src
                if (destAddr > srcAddr && destAddr < srcAddr + count * 4) {
                    for (int i = count - 1; i >= 0; i--) {
                        int val = cpu.getMemory().readWord(srcAddr + i * 4);
                        cpu.getMemory().writeWord(destAddr + i * 4, val);
                    }
                } else {
                    for (int i = 0; i < count; i++) {
                        int val = cpu.getMemory().readWord(srcAddr + i * 4);
                        cpu.getMemory().writeWord(destAddr + i * 4, val);
                    }
                }
            }

            @Override
            public int[] encode(String args) {
                var split = splitArgs(args, 2);
                int rDest = parseRegister(split[0]);
                int rSrc = parseRegister(split[1]);
                return new int[]{InstructionUtils.encodeInstruction(rDest, rSrc, getOpcode("MCPY"))};
            }

        });
    }

    private void registerControlFlowInstructions() {
        register("JMP", createJumpInstruction("JMP", cpu -> true));

        register("JZ", createJumpInstruction("JZ", cpu -> cpu.getFlags().isZero()));
        register("JE", createJumpInstruction("JE", cpu -> cpu.getFlags().isZero()));
        register("JNZ", createJumpInstruction("JNZ", cpu -> !cpu.getFlags().isZero()));
        register("JNE", createJumpInstruction("JNE", cpu -> !cpu.getFlags().isZero()));

        register("JN", createJumpInstruction("JN", cpu -> cpu.getFlags().isNegative()));
        register("JP", createJumpInstruction("JP", cpu -> !cpu.getFlags().isNegative()));

        // Signed comparisons (after CMP a, b)
        register("JG", createJumpInstruction("JG", cpu -> !cpu.getFlags().isZero() && !cpu.getFlags().isNegative())); // a > b
        register("JGE", createJumpInstruction("JGE", cpu -> !cpu.getFlags().isNegative()));                           // a >= b
        register("JL", createJumpInstruction("JL", cpu -> cpu.getFlags().isNegative()));                              // a < b
        register("JLE", createJumpInstruction("JLE", cpu -> cpu.getFlags().isNegative() || cpu.getFlags().isZero()));  // a <= b

        // Unsigned comparisons (if you support unsigned CMP or SUB with carry/borrow flag)
        register("JC", createJumpInstruction("JC", cpu -> cpu.getFlags().isCarry()));       // Jump if carry (unsigned <)
        register("JNC", createJumpInstruction("JNC", cpu -> !cpu.getFlags().isCarry()));    // Jump if not carry (unsigned >=)

        register("JA", createJumpInstruction("JA", cpu -> !cpu.getFlags().isCarry() && !cpu.getFlags().isZero())); // unsigned >
        register("JAE", createJumpInstruction("JAE", cpu -> !cpu.getFlags().isCarry()));                         // unsigned >=

        register("JB", createJumpInstruction("JB", cpu -> cpu.getFlags().isCarry()));                             // unsigned <
        register("JBE", createJumpInstruction("JBE", cpu -> cpu.getFlags().isCarry() || cpu.getFlags().isZero())); // unsigned <=


        register("CALL", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int addr = words[1];
                cpu.push(cpu.getProgramCounter());
                cpu.jump(addr);
            }

            @Override
            public int[] encode(String args) {
                int addr = parseImmediate(args);
                return new int[]{InstructionUtils.encodeInstruction(0, 0, getOpcode("CALL")), addr};
            }

            @Override
            public int getWordCount() { return 2; }
        });

        register("RET", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int retAddr = cpu.pop();
                cpu.jump(retAddr);
            }

            @Override
            public int[] encode(String args) {
                return new int[]{InstructionUtils.encodeInstruction(0, 0, getOpcode("RET"))};
            }
        });
    }

    private void registerStackInstructions() {
        register("PUSH", new StackInstruction("PUSH") {
            @Override
            public void executeOperation(CPU cpu, int value) {
                cpu.push(value);
            }
        });

        register("POP", new StackInstruction("POP") {
            @Override
            public void executeOperation(CPU cpu, int value) {
                cpu.setRegister(register, value);
                cpu.getFlags().update(value);
            }
        });
    }

    private void registerDataMovementInstructions() {
        register("MOVI", new ImmediateInstruction("MOVI") {
            @Override
            protected void executeImmediate(CPU cpu, int rDest, int immediate) {
                cpu.setRegister(rDest, immediate);
                cpu.getFlags().update(immediate);
            }
        });

        register("MOV", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rDest = InstructionUtils.extractRegister(words[0], 16);
                int rSrc = InstructionUtils.extractRegister(words[0], 8);
                int val = cpu.getRegister(rSrc);
                cpu.setRegister(rDest, val);
                cpu.getFlags().update(val);
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rDest = parseRegister(parts[0]);
                int rSrc = parseRegister(parts[1]);
                return new int[]{InstructionUtils.encodeInstruction(rDest, rSrc, getOpcode("MOV"))};
            }
        });

        register("CLR", new UnaryRegisterInstruction("CLR") {
            @Override
            protected int calculate(int value) {
                return 0;
            }
        });
    }

    private void registerComparisonInstructions() {
        // Register-register comparisons
        register("CMP", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rA = InstructionUtils.extractRegister(words[0], 16);
                int rB = InstructionUtils.extractRegister(words[0], 8);
                int a = cpu.getRegister(rA);
                int b = cpu.getRegister(rB);
                cpu.getFlags().updateSub(a, b, a - b);
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rA = parseRegister(parts[0]);
                int rB = parseRegister(parts[1]);
                return new int[]{InstructionUtils.encodeInstruction(rA, rB, getOpcode("CMP"))};
            }
        });

        // Register-immediate comparison
        register("CMPI", new ImmediateInstruction("CMPI") {
            @Override
            protected void executeImmediate(CPU cpu, int rA, int immediate) {
                int a = cpu.getRegister(rA);
                cpu.getFlags().updateSub(a, immediate, a - immediate);
            }
        });

        // Register-register test
        register("TEST", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rA = InstructionUtils.extractRegister(words[0], 16);
                int rB = InstructionUtils.extractRegister(words[0], 8);
                int val = cpu.getRegister(rA) & cpu.getRegister(rB);
                cpu.getFlags().update(val);
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rA = parseRegister(parts[0]);
                int rB = parseRegister(parts[1]);
                return new int[]{InstructionUtils.encodeInstruction(rA, rB, getOpcode("TEST"))};
            }
        });

        // Register-immediate test
        register("TESTI", new ImmediateInstruction("TESTI") {
            @Override
            protected void executeImmediate(CPU cpu, int rA, int immediate) {
                int val = cpu.getRegister(rA) & immediate;
                cpu.getFlags().update(val);
            }
        });
    }

    private void registerSystemInstructions() {
        register("SYSCALL", new Instruction() {
            public void execute(CPU cpu, int[] words) {
                int syscallNumber = cpu.getRegister(0);

                // use the ROM syscall table
                int syscallTableAddr = cpu.getMemoryMap().getSyscallTableStart();
                Memory rom = cpu.getMemory().getRom();

                // Calculate the address of the syscall entry in the table
                int tableEntryAddr = syscallTableAddr + (syscallNumber * 4);

                // Verify the table entry address is within ROM bounds
                if (!isAddressInMemory(tableEntryAddr, rom)) {
                    throw new IllegalStateException("Syscall number " + syscallNumber + " is out of range");
                }

                // Read the target address from the syscall table
                int targetAddress;
                try {
                    targetAddress = rom.readWord(tableEntryAddr);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to read syscall table entry for syscall " + syscallNumber, e);
                }

                // Verify the target address is valid (non-zero and within bounds)
                if (targetAddress == 0) {
                    throw new IllegalStateException("Syscall " + syscallNumber + " is not implemented (target address is 0)");
                }

                // Determine which memory region contains the target address
                MemoryHandler targetMemory = resolveMemoryForAddress(cpu, targetAddress);
                if (targetMemory == null) {
                    throw new IllegalStateException("Syscall " + syscallNumber + " target address 0x" +
                            Integer.toHexString(targetAddress) + " is not in any valid memory region");
                }

                // push the current PC and jump to the syscall handler
                int returnAddress = cpu.getProgramCounter();
                cpu.push(returnAddress);

                // Jump to the syscall handler
                cpu.setProgramCounter(targetAddress);
            }

            private boolean isAddressInMemory(int address, MemoryHandler memory) {
                return address >= memory.getBaseAddress() &&
                        address < memory.getBaseAddress() + memory.getSize();
            }

            private MemoryHandler resolveMemoryForAddress(CPU cpu, int address) {
                Memory rom = cpu.getMemory().getRom();
                Memory ram = cpu.getMemory().getRam();
                Memory vram = cpu.getMemory().getVram();
                IODeviceManager io = cpu.getMemory().getIo();

                if (isAddressInMemory(address, rom)) return rom;
                if (isAddressInMemory(address, ram)) return ram;
                if (isAddressInMemory(address, vram)) return vram;
                if (isAddressInMemory(address, io)) return io;

                return null; // Address not in any memory region
            }

            public int[] encode(String args) {
                return new int[] { InstructionUtils.encodeInstruction(0, 0, getOpcode("SYSCALL")) };
            }
        });

        register("NOP", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) { /* no op */ }

            @Override
            public int[] encode(String args) {
                return new int[]{InstructionUtils.encodeInstruction(0, 0, getOpcode("NOP"))};
            }
        });

        register("HLT", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) { cpu.setHalt(true); }

            @Override
            public int[] encode(String args) {
                return new int[]{InstructionUtils.encodeInstruction(0, 0, getOpcode("HLT"))};
            }
        });
    }

    // Helper classes for common instruction patterns
    private abstract class UnaryRegisterInstruction implements Instruction {
        private final String name;

        UnaryRegisterInstruction(String name) {
            this.name = name;
        }

        @Override
        public void execute(CPU cpu, int[] words) {
            int rDest = InstructionUtils.extractRegister(words[0], 16);
            int value = cpu.getRegister(rDest);
            int result = calculate(value);
            cpu.setRegister(rDest, result);
            cpu.getFlags().update(result);
        }

        @Override
        public int[] encode(String args) {
            int rDest = parseRegister(args);
            return new int[]{InstructionUtils.encodeInstruction(rDest, 0, getOpcode(name))};
        }

        protected abstract int calculate(int value);
    }

    private abstract class ImmediateInstruction implements Instruction {
        private final String name;

        ImmediateInstruction(String name) {
            this.name = name;
        }

        @Override
        public void execute(CPU cpu, int[] words) {
            int rDest = InstructionUtils.extractRegister(words[0], 16);
            executeImmediate(cpu, rDest, words[1]);
        }

        @Override
        public int[] encode(String args) {
            String[] parts = splitArgs(args, 2);
            int rDest = parseRegister(parts[0]);
            int imm = parseImmediate(parts[1]);
            return new int[]{
                    InstructionUtils.encodeInstruction(rDest, 0, getOpcode(name)),
                    imm
            };
        }

        @Override
        public int getWordCount() {
            return 2;
        }

        protected abstract void executeImmediate(CPU cpu, int rDest, int immediate);
    }

    private abstract class StackInstruction implements Instruction {
        protected int register;
        private final String name;

        StackInstruction(String name) {
            this.name = name;
        }

        @Override
        public void execute(CPU cpu, int[] words) {
            register = InstructionUtils.extractRegister(words[0], 16);
            int value = name.equals("PUSH") ?
                    cpu.getRegister(register) : cpu.pop();
            executeOperation(cpu, value);
        }

        @Override
        public int[] encode(String args) {
            int reg = parseRegister(args);
            return new int[]{InstructionUtils.encodeInstruction(reg, 0, getOpcode(name))};
        }

        public abstract void executeOperation(CPU cpu, int value);
    }

    // Helper methods for instruction creation
    private Instruction createJumpInstruction(String name, Predicate<CPU> condition) {
        return new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                if (condition.test(cpu)) cpu.jump(words[1]);
            }

            @Override
            public int[] encode(String args) {
                int addr = parseImmediate(args);
                return new int[]{
                        InstructionUtils.encodeInstruction(0, 0, getOpcode(name)),
                        addr
                };
            }

            @Override
            public int getWordCount() {
                return 2;
            }
        };
    }

    private Instruction createMemoryInstruction(String name, boolean load) {
        return new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rA = InstructionUtils.extractRegister(words[0], 16);
                int rB = InstructionUtils.extractRegister(words[0], 8);
                if (load) {
                    int value = cpu.getMemory().readWord(cpu.getRegister(rB));
                    cpu.setRegister(rA, value);
                    cpu.getFlags().update(value);
                } else {
                    cpu.getMemory().writeWord(cpu.getRegister(rB), cpu.getRegister(rA));
                }
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rA = parseRegister(parts[0]);
                int rB = parseRegister(parts[1]);
                return new int[]{InstructionUtils.encodeInstruction(rA, rB, getOpcode(name))};
            }
        };
    }

    private Instruction createShiftInstruction(String name, BiFunction<Integer, Integer, Integer> op) {
        return new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rDest = InstructionUtils.extractRegister(words[0], 16);
                int shift = InstructionUtils.extractRegister(words[0], 8);
                int result = op.apply(cpu.getRegister(rDest), shift);
                cpu.setRegister(rDest, result);
                cpu.getFlags().update(result);
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rDest = parseRegister(parts[0]);
                int shift = parseImmediate(parts[1]);
                return new int[]{InstructionUtils.encodeInstruction(rDest, shift, getOpcode(name))};
            }
        };
    }

    private void registerBinaryOp(String name, BiFunction<Integer, Integer, Integer> op, boolean updateAddFlags) {
        register(name, new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rDest = InstructionUtils.extractRegister(words[0], 16);
                int rSrc = InstructionUtils.extractRegister(words[0], 8);
                int a = cpu.getRegister(rDest);
                int b = cpu.getRegister(rSrc);
                int result = op.apply(a, b);
                cpu.setRegister(rDest, result);
                if (updateAddFlags) cpu.getFlags().updateAdd(a, b, result);
                else cpu.getFlags().update(result);
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rDest = parseRegister(parts[0]);
                int rSrc = parseRegister(parts[1]);
                return new int[]{InstructionUtils.encodeInstruction(rDest, rSrc, getOpcode(name))};
            }
        });
    }

    private void registerBinaryImmediateOp(String name, BiFunction<Integer, Integer, Integer> op, boolean updateAddFlags) {
        register(name, new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rDest = InstructionUtils.extractRegister(words[0], 16);
                int a = cpu.getRegister(rDest);
                int b = words[1];
                int result = op.apply(a, b);
                cpu.setRegister(rDest, result);
                if (updateAddFlags) cpu.getFlags().updateAdd(a, b, result);
                else cpu.getFlags().update(result);
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rDest = parseRegister(parts[0]);
                int imm = parseImmediate(parts[1]);
                return new int[]{
                        InstructionUtils.encodeInstruction(rDest, 0, getOpcode(name)),
                        imm
                };
            }

            @Override
            public int getWordCount() {
                return 2;
            }
        });
    }

    // Utility methods
    private int parseImmediate(String token) {
        token = token.trim().toLowerCase();
        if (token.startsWith("0x")) {
            return (int) Long.parseLong(token.substring(2), 16);
        }
        return Integer.parseInt(token);
    }

    private int parseRegister(String token) {
        token = token.trim().toLowerCase();

        return switch (token) {
            case "pc" -> 252;
            case "sp" -> 253;
            case "hp" -> 254;
            default -> {
                if (!token.startsWith("r")) {
                    throw new IllegalArgumentException("Invalid register: " + token);
                }
                try {
                    yield Integer.parseInt(token.substring(1));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid register number: " + token);
                }
            }
        };
    }


    @Override
    public Instruction getInstruction(int instructionWord) {
        byte opcode = InstructionUtils.decodeOpcode(instructionWord);
        return instructionMap.get(opcode);
    }

    @Override
    public void register(String name, Instruction instruction) {
        byte opcode = nextOpcode++;
        instructionMap.put(opcode, instruction);
        nameToOpcode.put(name, opcode);
        opcodeToName.put(opcode, name);
    }

    @Override
    public Byte getOpcode(String name) {
        return nameToOpcode.get(name);
    }

    @Override
    public String getName(Byte opcode) {
        return opcodeToName.get(opcode);
    }

    private void logInstructions() {
        System.out.println("Registered instructions:");
        nameToOpcode.forEach((name, opcode) ->
                System.out.printf("  %s -> 0x%02X%n", name, opcode)
        );
    }
}