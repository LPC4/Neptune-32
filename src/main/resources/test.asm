    LOADI r0, 1       ; syscall number 1: get VRAM info
    SYSCALL            ; after syscall: r1 = VRAM start address, r2 = VRAM size in bytes

    SHR r2, 1          ; r2 = half of VRAM size (in bytes)

    MOV r4, r1         ; r4 = current VRAM write pointer (start at VRAM base)
    LOADI r5, 0xFF000000 ; opaque black pixel color
    LOADI r10, 4      ; constant 4

fill_loop:
    STORE r5, r4       ; store black pixel at address in r4
    ADDI r4, 4          ; move pointer to next pixel (4 bytes per pixel)
    SUBI r2, 4          ; decrement byte counter
    JNZ fill_loop      ; loop until half VRAM filled
