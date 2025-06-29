.const KEYBOARD_BASE 0x32000
.const CONSOLE_OUT 0x32010

.const FIRST_CHAR 0x00
.const NOT_LAST_CHAR 0x04
.const CURRENT_CHAR 0x08
.const CONTROL 0x0C

.const ENTER_KEY 10
.const CONSUME 1

.const SYSCALL_SET_PIXEL 0
.const SYSCALL_GET_VRAM_INFO 1
.const SYSCALL_CLEAR_SCREEN 2

.const BLACK_COLOR 0xFF000000
.const RED_COLOR 0xFFFF0000

.const CHAR_A 97
.const CHAR_Z 122

main:
    MOVI r0, SYSCALL_GET_VRAM_INFO
    SYSCALL
    MOV r10, r1       ; VRAM base
    MOV r11, r3       ; screen width
    MOV r12, r4       ; screen height

    MOVI r0, SYSCALL_CLEAR_SCREEN
    MOVI r1, BLACK_COLOR
    MOV r4, r11
    MOV r5, r12
    MOV r15, r10
    SYSCALL

    MOVI r0, KEYBOARD_BASE
    MOVI r1, FIRST_CHAR
    ADD r1, r0        ; r1 = &FIRST_CHAR
    MOVI r2, NOT_LAST_CHAR
    ADD r2, r0        ; r2 = &NOT_LAST_CHAR
    MOVI r3, CURRENT_CHAR
    ADD r3, r0        ; r3 = &CURRENT_CHAR
    MOVI r4, CONTROL
    ADD r4, r0        ; r4 = &CONTROL

    MOVI r6, CONSOLE_OUT
    MOVI r7, CONSUME

wait_for_enter:
    LOAD r5, r3
    MOVI r8, ENTER_KEY
    CMP r5, r8
    JNE wait_for_enter

process_buffer:
    LOAD r5, r2        ; Load NOT_LAST_CHAR flag
    CMPI r5, 0
    JE restart         ; No more chars to process, restart main

    LOAD r5, r1        ; Load char from FIRST_CHAR
    STORE r5, r6       ; Output char to console
    STORE r7, r4       ; Write consume command to CONTROL (consume char)

    MOV r8, r5         ; Copy char for comparison
    MOVI r9, CHAR_A
    CMP r8, r9
    JL skip_pixel      ; If char < 'a', skip pixel drawing

    MOVI r9, CHAR_Z
    CMP r8, r9
    JG skip_pixel      ; If char > 'z', skip pixel drawing

    MOV r9, r5
    SUBI r9, CHAR_A    ; char index: 0 for 'a', 1 for 'b', etc.

    MOVI r0, SYSCALL_SET_PIXEL
    MOV r1, r9         ; X coord
    MOV r2, r9         ; Y coord
    MOVI r3, RED_COLOR
    MOV r4, r11        ; screen width
    MOV r15, r10       ; VRAM base address
    SYSCALL

skip_pixel:
    JMP process_buffer

restart:
    JMP main
