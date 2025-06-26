; Neptune Assembly Program - Line demo

START:
    ; Get VRAM information (same as before)
    LOADI r0, 1          ; System call 1: Get VRAM info
    SYSCALL              ; r1=start, r2=size, r3=width, r4=height

    ; Store VRAM info for later use
    MOV r15, r1          ; r15 = VRAM start address
    MOV r14, r3          ; r14 = width
    MOV r13, r4          ; r13 = height

    ; Now draw some lines as demonstration
    LOADI r5, 0xFFFFFFFF ; White color for lines

    ; Line 1: Diagonal from (10,10) to (100,100)
    LOADI r1, 10         ; x1
    LOADI r2, 10         ; y1
    LOADI r3, 100        ; x2
    LOADI r4, 100        ; y2
    CALL draw_line

    ; Line 2: Horizontal line
    LOADI r1, 50         ; x1
    LOADI r2, 150        ; y1
    LOADI r3, 200        ; x2
    MOV r4, r2           ; y2 = y1 (horizontal)
    CALL draw_line

    ; Line 3: Vertical line
    LOADI r1, 300        ; x1
    LOADI r2, 50         ; y1
    MOV r3, r1           ; x2 = x1 (vertical)
    LOADI r4, 200        ; y2
    CALL draw_line

    ; Halt the program
    HLT

; Draw Line Subroutine
; Parameters:
;   r1 = x1
;   r2 = y1
;   r3 = x2
;   r2 = y2
;   r5 = color (ARGB format)
; Uses: r6-r12 for calculations
; Preserves: r13-r15 (VRAM info)
draw_line:
    MOV r6, r3        ; Copy r3 to r6 first
    SUB r6, r1         ; r6 = r6 - r1 (now r6 = r3 - r1)
    MOV r7, r4        ; Copy r4 to r7 first
    SUB r7, r2         ; r7 = r7 - r2 (now r7 = r4 - r2)

    ; Determine step direction for x (sx)
    LOADI r8, 1        ; r8 = sx = 1 (default positive)
    CMPI r6, 0
    JG x_positive
    JZ x_positive
    NEG r6            ; dx = -dx
    LOADI r8, -1       ; sx = -1
x_positive:

    ; Determine step direction for y (sy)
    LOADI r9, 1        ; r9 = sy = 1 (default positive)
    CMPI r7, 0
    JG y_positive
    JZ y_positive
    NEG r7            ; dy = -dy
    LOADI r9, -1       ; sy = -1
y_positive:

    ; Initialize decision variables
    MOV r10, r6       ; r10 = dx
    SUB r10, r7       ; r10 = dx - dy (decision parameter)

    ; Current position
    MOV r11, r1       ; r11 = current x
    MOV r12, r2       ; r12 = current y

line_loop:
    ; Set pixel at current position
    LOADI r0, 0       ; set_pixel syscall
    MOV r1, r11       ; x
    MOV r2, r12       ; y
    MOV r3, r5        ; color
    MOV r4, r14       ; width (should be preserved from main program)
    SYSCALL

    ; Check if we've reached the end point
    CMP r11, r3
    JNZ continue_line
    CMP r12, r4
    JZ line_done

continue_line:
    ; Calculate next step
    SHL r10, 1        ; Multiply decision by 2
    CMPI r10, 0
    JG decision_ge_zero
    JZ decision_ge_zero

    ; Decision < 0 case
    ADD r10, r6       ; decision += dx
    ADD r12, r9       ; y += sy
    JMP next_pixel

decision_ge_zero:
    ; Decision >= 0 case
    SUB r10, r7       ; decision -= dy
    ADD r11, r8       ; x += sx

next_pixel:
    JMP line_loop

line_done:
    RET