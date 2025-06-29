# Neptune CPU Emulator Documentation

## Table of Contents

1. [Overview](#overview)
2. [Memory Map](#memory-map)
3. [Register Set](#register-set)
4. [Instruction Encoding](#instruction-encoding)
5. [Assembler Syntax](#assembler-syntax)
6. [IO Devices](#io-devices)
7. [VRAM Layout](#vram-layout)
8. [Syscall Mechanism](#syscall-mechanism)
9. [Instruction Set](#instruction-set)
10. [Development Notes](#development-notes)
11. [License](#license)
12. [Author](#author)
13. [Contributions](#contributions)

---

## Overview

Neptune is a custom-built 32-bit CPU emulator with an integrated assembler and assembly language. It is designed for educational purposes, experimentation, and as a foundation for building simple operating systems or games. The emulator simulates RAM, ROM (used for syscalls), VRAM for graphical output, a stack, a heap, and memory-mapped IO devices. It uses little-endian word addressing and has fixed-size instructions (single or double word).

---

## Memory Map

### Layout Summary

| Region          | Address Range           | Size   | Description                               |
| --------------- | ----------------------- | ------ | ----------------------------------------- |
| ROM             | 0x00000000 - 0x00001FFF | 8 KB   | Boot ROM (contains syscall table & code)  |
| - Syscall Table | 0x00000010 - 0x0000010F | 256 B  | Maps 64 syscalls to handler addresses (4 bytes each) |
| - Syscall Code  | 0x00000110 - 0x0000090F | 2 KB   | Syscall handler implementations           |
| RAM             | 0x00002000 - 0x00021FFF | 128 KB | Main RAM for programs                     |
| - Heap          | 0x00004000 - dynamic    | ~      | Grows upwards from 8KB into RAM          |
| - Stack         | 0x00021FFC - downwards  | ~      | Grows downwards from end of RAM           |
| VRAM            | 0x00022000 - 0x00031FFF | 64 KB  | 128x128 RGBA32 framebuffer                |
| IO              | 0x00032000 - 0x00032FFF | 4 KB   | Memory-mapped input/output devices        |

### Memory Details

* **ROM Start:** 0x00000000 (8 KB total)
* **RAM Start:** 0x00002000 (128 KB total)
* **Heap Start:** 0x00004000 (8 KB into RAM)
* **Stack Start:** 0x00021FFC (grows downward from end of RAM)
* **VRAM Start:** 0x00022000 (64 KB total)
* **IO Start:** 0x00032000 (4 KB total)

### Notes

* Memory is byte-addressable but operates on 32-bit words
* All addresses are 32-bit
* Stack collisions with heap result in runtime errors
* Words are 32-bit (4 bytes) and affect instruction encoding

---

## Register Set

Neptune has 32 general-purpose registers (configurable in constructor) plus special registers:

### General-Purpose Registers
| Register | Description                                  |
| -------- | -------------------------------------------- |
| r0 - r31 | General-purpose registers (32 total, default) |

### Special Registers
| Register | Description                                  |
| -------- | -------------------------------------------- |
| PC       | Program Counter, tracks next instruction     |
| SP       | Stack Pointer, grows downward                |
| HP       | Heap Pointer, grows upward                   |
| FLAGS    | Contains four boolean flags                  |

### Flag Definitions

The FLAGS register contains four boolean flags:

* **Z (Zero):** Set if result is zero
* **N (Negative):** Set if result is negative (signed)
* **C (Carry):** Set if unsigned carry occurs
* **V (Overflow):** Set if signed overflow occurs

---

## Instruction Encoding

### Single Word Instructions (Register-Register)

```
| 31-24: rDest | 23-16: rSrc | 15-8: Reserved | 7-0: Opcode |
```

* Example: `ADD r1, r2` → r1 = r1 + r2 (r1 is both source and destination)

### Double Word Instructions (Immediate)

```
Word 1: | 31-24: rDest | 23-16: 0 | 15-8: Reserved | 7-0: Opcode |
Word 2: Immediate value (32-bit literal/address)
```

* Example: `MOVI r1, 42` → r1 = 42

### Jump/Call Instructions (Double Word)

* Use the same immediate encoding format, where Word 2 is the absolute 32-bit address

---

## Assembler Syntax

* **Labels:** `label:` (used for jumps and calls)
* **Comments:** `;` starts a comment line
* **Instructions:** Case-insensitive (recommended uppercase)
* **Immediate literals:**
    * Decimal: `42`
    * Hexadecimal: `0x2A`
* **No directives** like `.org` or `.word` are currently supported

---

## IO Devices

### Keyboard Input Device

| Offset | Name          | Type | Description                               |
| ------ | ------------- | ---- | ----------------------------------------- |
| +0x00  | FIRST_CHAR    | RO   | ASCII code of the oldest char in buffer   |
| +0x04  | BUFFER_READY  | RO   | 1 if buffer has >=2 chars, else 0         |
| +0x08  | CURRENT_CHAR  | RO   | Most recent char pressed                  |
| +0x0C  | CONTROL       | WO   | 1=consume oldest, 2=clear buffer, 3=reset |

### Console Output Device

* Write ASCII values to the output register at offset +0x00
* CPU sends bytes to display as console output

### Timer Device

* Provides tick counts or can be expanded for interrupts
* Offset mappings:
    * +0x00: Current tick count (RO)
    * +0x04: Control (WO)

---

## VRAM Layout

* **Resolution:** 128x128 pixels
* **Format:** RGBA32 (4 bytes per pixel)
* **Total Size:** 64 KB
* **Address formula:**

```
address = VRAM_BASE + (y * 128 + x) * 4
```

* **Pixel layout:** Row-major order (left-to-right, top-to-bottom)

Each pixel is stored as 4 consecutive bytes:

```
| +0: Red | +1: Green | +2: Blue | +3: Alpha |
```

* Write to VRAM to update the framebuffer

---

## Syscall Mechanism

* Invoke syscalls using the `SYSCALL` instruction
* Syscall number is placed in register `r0`
* The syscall handler address is looked up in ROM's syscall table (64 possible syscalls)
* The CPU pushes PC onto the stack and jumps to the syscall handler
* Use `RET` at the end of the syscall handler to return

---

## Instruction Set

### Arithmetic Instructions

| Instruction | Description |
|-------------|-------------|
| `ADD rDest, rSrc` | Add rSrc to rDest, store in rDest, update flags |
| `ADDI rDest, imm` | Add immediate to rDest, store in rDest, update flags |
| `SUB rDest, rSrc` | Subtract rSrc from rDest, store in rDest, update flags |
| `SUBI rDest, imm` | Subtract immediate from rDest, store in rDest, update flags |
| `MUL rDest, rSrc` | Multiply rDest by rSrc, store in rDest, update flags |
| `MULI rDest, imm` | Multiply rDest by immediate, store in rDest, update flags |
| `DIV rDest, rSrc` | Divide rDest by rSrc, store in rDest, update flags (throws if divide by zero) |
| `DIVI rDest, imm` | Divide rDest by immediate, store in rDest, update flags (throws if divide by zero) |
| `MOD rDest, rSrc` | Modulo rDest by rSrc, store in rDest, update flags (throws if modulo by zero) |
| `MODI rDest, imm` | Modulo rDest by immediate, store in rDest, update flags (throws if modulo by zero) |
| `INC rDest` | Increment rDest by 1, update flags |
| `DEC rDest` | Decrement rDest by 1, update flags |
| `NEG rDest` | Negate rDest, update flags |

### Logical Instructions

| Instruction | Description |
|-------------|-------------|
| `AND rDest, rSrc` | Bitwise AND rDest with rSrc, store in rDest, update flags |
| `ANDI rDest, imm` | Bitwise AND rDest with immediate, store in rDest, update flags |
| `OR rDest, rSrc` | Bitwise OR rDest with rSrc, store in rDest, update flags |
| `ORI rDest, imm` | Bitwise OR rDest with immediate, store in rDest, update flags |
| `XOR rDest, rSrc` | Bitwise XOR rDest with rSrc, store in rDest, update flags |
| `XORI rDest, imm` | Bitwise XOR rDest with immediate, store in rDest, update flags |
| `NOT rDest` | Bitwise NOT of rDest, store in rDest, update flags |

### Shift Instructions

| Instruction | Description |
|-------------|-------------|
| `SHL rDest, shift` | Shift rDest left by shift bits, store in rDest, update flags |
| `SHR rDest, shift` | Logical shift rDest right by shift bits, store in rDest, update flags |

### Memory Instructions

| Instruction | Description |
|-------------|-------------|
| `LOAD rDest, rAddr` | Load word from memory at rAddr into rDest, update flags |
| `STORE rSrc, rAddr` | Store word from rSrc into memory at rAddr |
| `LOADI rDest, immAddr` | Load word from memory at immediate address into rDest, update flags |
| `STORI rSrc, immAddr` | Store word into memory at immediate address |
| `MSET rAddr, rValue` | Set r1 words starting at rAddr to rValue |
| `MCPY rDest, rSrc` | Copy r1 words from rSrc to rDest |

### Control Flow Instructions

#### Unconditional Jumps
| Instruction | Description |
|-------------|-------------|
| `JMP address` | Jump unconditionally |
| `CALL address` | Push PC to stack, jump to address (2-word instruction) |
| `RET` | Pop PC from stack and jump back |

#### Conditional Jumps (Equality/Zero)
| Instruction | Description |
|-------------|-------------|
| `JZ address` | Jump if zero flag is set (==) |
| `JE address` | Jump if zero flag is set (==) |
| `JNZ address` | Jump if zero flag is not set (!=) |

#### Conditional Jumps (Signed Comparison)
| Instruction | Description |
|-------------|-------------|
| `JN address` | Jump if negative flag is set (signed < 0) |
| `JP address` | Jump if negative flag is not set (signed >= 0) |
| `JG address` | Jump if greater (signed >) - zero flag not set AND negative flag not set |
| `JGE address` | Jump if greater or equal (>=) - negative flag not set |
| `JL address` | Jump if less (signed <) - negative flag set |
| `JLE address` | Jump if less or equal (<=) - negative flag set OR zero flag set |

#### Conditional Jumps (Unsigned Comparison)
| Instruction | Description |
|-------------|-------------|
| `JC address` | Jump if carry flag is set (unsigned <) |
| `JNC address` | Jump if carry flag is not set (unsigned >=) |
| `JA address` | Jump if above (unsigned >) - carry flag not set AND zero flag not set |
| `JAE address` | Jump if above or equal (>=) - carry flag not set |
| `JB address` | Jump if below (unsigned <) - carry flag set |
| `JBE address` | Jump if below or equal (<=) - carry flag set OR zero flag set |

### Stack Instructions

| Instruction | Description |
|-------------|-------------|
| `PUSH rSrc` | Push register rSrc onto stack |
| `POP rDest` | Pop from stack into rDest, update flags |

### Data Movement Instructions

| Instruction | Description |
|-------------|-------------|
| `MOV rDest, rSrc` | Copy value from rSrc into rDest, update flags |
| `MOVI rDest, imm` | Load immediate into rDest, update flags (2-word instruction) |
| `CLR rDest` | Clear rDest (set to 0), update flags |

### Comparison Instructions

| Instruction | Description |
|-------------|-------------|
| `CMP rA, rB` | Compare rA with rB (rA - rB), update flags (no register change) |
| `CMPI rA, imm` | Compare rA with immediate, update flags (no register change) |
| `TEST rA, rB` | Bitwise AND of rA and rB, update flags (no register change) |
| `TESTI rA, imm` | Bitwise AND of rA and immediate, update flags (no register change) |

### System Instructions

| Instruction | Description |
|-------------|-------------|
| `SYSCALL` | Execute system call specified by r0 |
| `NOP` | No operation |
| `HLT` | Halt the CPU |

### Flag Behavior Reference

| Instruction Category | Z | N | C | V |
|---------------------|---|---|---|---|
| Arithmetic          | ✔ | ✔ | ✔ | ✔ |
| Logic               | ✔ | ✔ | ✖ | ✖ |
| CMP/TEST            | ✔ | ✔ | ✖ | ✖ |
| MOV/MOVI            | ✔ | ✔ | ✖ | ✖ |

---

## Development Notes

* PC starts at `main:` label
* ROM is reserved for syscall handlers and the syscall table
* Heap grows upward from HEAP_START (0x00004000)
* Stack grows downward from STACK_START (0x00021FFC)
* Heap-stack collision triggers a runtime panic
* VRAM is read-write memory for external graphics engines
* Register count is configurable in constructor (default: r0-r31)
* Words are 32-bit (4 bytes)

### Future Extensions

* Directives in assembler (`.data`, `.org`, etc.)
* Interrupt support
* Floating-point instructions
* More IO devices (audio, mouse, network)

---

## License

MIT License or a license of your choosing.

---

## Contributions

Contributions are welcome. Submit pull requests to add IO devices, instructions, syscall implementations, or assembler features.