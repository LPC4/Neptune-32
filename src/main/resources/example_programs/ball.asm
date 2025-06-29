main:
    MOVI r0, 1
    SYSCALL                  ; r1=VRAM base, r2=VRAM size, r3=width, r4=height

    MOV r15, r1              ; VRAM base
    MOV r14, r3              ; width
    MOV r13, r4              ; height

    MOVI r8, 64             ; ball_x
    MOVI r9, 64             ; ball_y
    MOVI r10, 2             ; velocity_x
    MOVI r11, 3             ; velocity_y

    MOVI r5, 0xFFFF0000     ; ball color (red with alpha)

main_loop:
    ; Clear screen
    MOVI r0, 2
    MOVI r1, 0xFF000000
    MOV r4, r14
    MOV r5, r13
    SYSCALL

    ; --- Draw circle ---
    MOVI r6, 0              ; y_offset

draw_outer_loop:
    CMPI r6, 6
    JG draw_done

    ; Calculate x_offset = sqrt(25 - y_offset^2)
    MOVI r7, 25             ; r^2
    MOV r12, r6
    MUL r12, r6              ; y_offset^2
    SUB r7, r12              ; r^2 - y^2

    CLR r12                  ; x_offset = 0
sqrt_loop:
    CMPI r12, 6
    JG sqrt_done

    MOV r13, r12
    MUL r13, r13
    CMP r13, r7
    JG sqrt_done

    INC r12
    JMP sqrt_loop

sqrt_done:
    DEC r12                  ; x_offset = last valid

    ; === Draw line at y + offset ===
    MOV r1, r8
    SUB r1, r12              ; start x
    MOV r2, r8
    ADD r2, r12              ; end x
    MOV r3, r9
    ADD r3, r6               ; y + offset

draw_line_loop:
    CMP r1, r2
    JG draw_line_done

    ; Check bounds (x=r1, y=r3)
    CMPI r1, 0
    JL skip_pixel
    CMP r1, r14
    JG skip_pixel
    CMP r1, r14
    JZ skip_pixel

    CMPI r3, 0
    JL skip_pixel
    CMP r3, r13
    JG skip_pixel
    CMP r3, r13
    JZ skip_pixel

    MOVI r0, 0
    MOV r4, r14
    SYSCALL

skip_pixel:
    INC r1
    JMP draw_line_loop

draw_line_done:

    ; === Symmetrical line y - offset ===
    CMPI r6, 0
    JZ skip_symmetry

    MOV r1, r8
    SUB r1, r12
    MOV r2, r8
    ADD r2, r12
    MOV r3, r9
    SUB r3, r6

draw_sym_line_loop:
    CMP r1, r2
    JG draw_sym_line_done

    CMPI r1, 0
    JL skip_sym_pixel
    CMP r1, r14
    JG skip_sym_pixel
    CMP r1, r14
    JZ skip_sym_pixel

    CMPI r3, 0
    JL skip_sym_pixel
    CMP r3, r13
    JG skip_sym_pixel
    CMP r3, r13
    JZ skip_sym_pixel

    MOVI r0, 0
    MOV r4, r14
    SYSCALL

skip_sym_pixel:
    INC r1
    JMP draw_sym_line_loop

draw_sym_line_done:

skip_symmetry:
    INC r6
    JMP draw_outer_loop

draw_done:

    ; Debug position
    PRINT r8
    PRINT r9

    ; === Move ball ===
    ADD r8, r10
    ADD r9, r11

    ; Bounce X
    CMPI r8, 5
    JL bounce_x

    MOVI r7, 5
    MOV r12, r14
    SUB r12, r7              ; width - 5
    CMP r8, r12
    JG bounce_x
    CMP r8, r12
    JZ bounce_x
    JMP check_bounce_y

bounce_x:
    NEG r10
    ADD r8, r10

check_bounce_y:
    CMPI r9, 5
    JL bounce_y

    MOVI r7, 5
    MOV r12, r13
    SUB r12, r7              ; height -5
    CMP r9, r12
    JG bounce_y
    CMP r9, r12
    JZ bounce_y
    JMP next_frame

bounce_y:
    NEG r11
    ADD r9, r11

next_frame:
    MOVI r0, 100000
delay_loop:
    DEC r0
    JNZ delay_loop

    JMP main_loop
