; Example program to draw a rectangle outline


main:
    ; Get VRAM info
    MOVI r0, 1
    SYSCALL
    ; r1 = VRAM addr, r2 = VRAM size, r3 = width, r4 = height

    MOV r15, r1          ; VRAM base address
    MOV r6, r3           ; screen width

    ; Define color
    MOVI r5, 0xFF0000FF  ; Red (AABBGGRR)

    ; Draw top edge
    MOVI r1, 10           ; x1
    MOVI r2, 10          ; y1
    MOVI r3, 100         ; x2
    MOVI r4, 10          ; y2
    MOVI r0, 4           ; syscall ID for draw_line_RGBA32
    SYSCALL

    ; Draw right edge
    MOVI r1, 100
    MOVI r2, 10
    MOVI r3, 100
    MOVI r4, 100
    MOVI r0, 4
    SYSCALL

    ; Draw bottom edge
    MOVI r1, 100
    MOVI r2, 100
    MOVI r3, 10
    MOVI r4, 100
    MOVI r0, 4
    SYSCALL

    ; Draw left edge
    MOVI r1, 10
    MOVI r2, 100
    MOVI r3, 10
    MOVI r4, 10
    MOVI r0, 4
    SYSCALL

    JMP .main
