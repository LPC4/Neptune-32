; Neptune Assembly Program - Pixel Subroutine and Screen Fill
; This program defines a subroutine to set pixel color at x,y coordinates
; and uses it to fill the screen with green

START:
; Get VRAM information
LOADI r0, 1          ; System call 1: Get VRAM info
SYSCALL              ; r1=start, r2=size, r3=width, r4=height

; Store VRAM start in r15 for subroutine use
MOV r15, r1          ; r15 = VRAM start address

; Set up green color
LOADI r5, 0xFF00FF00 ; Green color (ARGB: Alpha=FF, Red=00, Green=FF, Blue=00)

; Fill screen using pixel subroutine
CLR r6               ; r6 = y coordinate (row)
outer_loop:
    CLR r7           ; r7 = x coordinate (column)
    inner_loop:
        ; Set parameters for set_pixel subroutine (standard convention)
        MOV r0, r7   ; r0 = x coordinate
        MOV r1, r6   ; r1 = y coordinate
        MOV r2, r5   ; r2 = color

        ; Call set_pixel subroutine
        CALL set_pixel_RGBA32

        ; Move to next column
        INC r7
        CMP r7, r3   ; Compare x with width
        JL inner_loop

    ; Move to next row
    INC r6
    CMP r6, r4       ; Compare y with height
    JL outer_loop

; Halt the program
HLT

; Subroutine: set_pixel
; Parameters: r0=x, r1=y, r2=color, r3=width
; Preserves all registers except temporaries
; Uses: r12, r13 (high registers for safe temporaries)
set_pixel_RGBA32:
    ; Save registers we'll modify
    PUSH r12
    PUSH r13

    ; Calculate pixel address: VRAM_start + (y * width + x) * 4
    ; We have width in r3
    MOV r12, r1      ; r12 = y
    MUL r12, r3      ; r12 = y * width
    ADD r12, r0      ; r12 = y * width + x
    MULI r12, 4      ; r12 = (y * width + x) * 4 bytes

    ; Add VRAM base (we stored it in r15 from main)
    ADD r12, r15     ; r12 = final pixel address

    ; Store the color
    STORE r2, r12

    ; Restore registers
    POP r13
    POP r12

    RET