; Neptune Assembly Program - Pixel Subroutine and Screen Fill
; This program defines a subroutine to set pixel color at x,y coordinates
; and uses it to fill the screen with green

START:
    ; Get VRAM information
    LOADI r0, 1          ; System call 1: Get VRAM info
    SYSCALL              ; r1=start, r2=size, r3=width, r4=height

    ; Store VRAM info for later use
    MOV r15, r1          ; r15 = VRAM start address
    MOV r14, r3          ; r14 = width (save for syscall parameter)
    MOV r13, r4          ; r13 = height (save for loop comparison)

    ; Set up green color
    LOADI r5, 0xFF00FF00 ; Green color (ARGB: Alpha=FF, Red=00, Green=FF, Blue=00)

    ; Fill screen using pixel syscall
    CLR r6               ; r6 = y coordinate (row)
outer_loop:
    CLR r7               ; r7 = x coordinate (column)
    inner_loop:
        ; Set parameters for set_pixel_RGBA32 syscall
        ; Syscall parameters: r0=syscall_nr, r1=x, r2=y, r3=color, r4=width
        LOADI r0, 0      ; r0 = syscall number (assuming set_pixel_RGBA32 is syscall 0)
        MOV r1, r7       ; r1 = x coordinate
        MOV r2, r6       ; r2 = y coordinate
        MOV r3, r5       ; r3 = color
        MOV r4, r14      ; r4 = width

        ; Call set_pixel syscall
        SYSCALL

        ; Move to next column
        INC r7
        CMP r7, r14      ; Compare x with width (use r14 since r3 was overwritten)
        JL inner_loop

    ; Move to next row
    INC r6
    CMP r6, r13          ; Compare y with height (use r13 since r4 was overwritten)
    JL outer_loop

    ; Halt the program
    HLT