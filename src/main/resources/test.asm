; Neptune Assembly - Smooth Fade from Blue to Green

START:
    ; Get VRAM info
    LOADI r0, 1
    SYSCALL              ; r1 = VRAM start, r2 = size, r3 = width, r4 = height

    MOV r15, r1          ; VRAM base
    MOV r14, r3          ; width
    MOV r13, r4          ; height

    LOADI r5, 0xFFFF0000 ; Start color (blueish)
    LOADI r6, 0xFF00FF00 ; Target color (green), for reference

main_loop:
    ; Fill screen with current color in r5
    CLR r8               ; y = 0
outer_loop:
    CLR r9               ; x = 0
inner_loop:
    LOADI r0, 0          ; syscall: set_pixel_RGBA32
    MOV r1, r9           ; x
    MOV r2, r8           ; y
    MOV r3, r5           ; color
    SYSCALL

    INC r9
    CMP r9, r14
    JL inner_loop

    INC r8
    CMP r8, r13
    JL outer_loop

    ; ==== Fade step ====
    ; Extract R and G components from ARGB
    MOV r10, r5          ; Copy color
    SHR r10, 16          ; r10 = R component
    ANDI r10, 0xFF       ; Mask to 8 bits

    MOV r11, r5
    SHR r11, 8           ; r11 = G component
    ANDI r11, 0xFF       ; Mask to 8 bits

    ; Update R (decrease) and G (increase)
    CMPI r10, 0
    JZ skip_red_decrease
    DEC r10              ; decrease R
skip_red_decrease:

    CMPI r11, 255
    JG skip_green_increase
    INC r11              ; increase G
skip_green_increase:

    ; Rebuild ARGB
    LOADI r12, 0xFF000000 ; Alpha channel

    SHL r10, 16          ; Shift R into position
    SHL r11, 8           ; Shift G into position

    OR r12, r10
    OR r12, r11          ; B stays zero

    MOV r5, r12          ; Store new color

    JMP main_loop
