main:
    ; Get VRAM info
    MOVI r0, 1
    SYSCALL                  ; r1 = VRAM start, r2 = size, r3 = width, r4 = height

    MOV r15, r1              ; VRAM base
    MOV r14, r3              ; width
    MOV r13, r4              ; height

    CLR r12                  ; r12 = time/frame counter

main_loop:
    CLR r8                   ; y = 0
outer_loop:
    CLR r9                   ; x = 0
inner_loop:
    ; ==== Compute color ====
    MOV r10, r8              ; r10 = y
    ADD r10, r9              ; r10 = x + y
    ADD r10, r12             ; add time offset

    SHL r10, 3               ; amplify
    ANDI r10, 0xFF           ; clamp to 8 bits

    ; Split into red and blue components
    MOV r11, r10
    SHL r11, 16              ; r11 = red component (shift to R)

    MOV r5, r10              ; r5 = blue component (already in position)

    OR r5, r11               ; combine R and B

    MOVI r6, 0xFF000000     ; alpha
    OR r5, r6                ; apply alpha

    ; ==== Draw pixel ====
    MOVI r0, 0              ; syscall: set_pixel_RGBA32
    MOV r1, r9               ; x
    MOV r2, r8               ; y
    MOV r3, r5               ; color
    SYSCALL

    INC r9
    CMP r9, r14
    JL inner_loop

    INC r8
    CMP r8, r13
    JL outer_loop

    ; ==== Advance time ====
    INC r12
    ANDI r12, 0xFF

    JMP main_loop
