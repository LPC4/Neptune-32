; Neptune Assembly Program - Half Green, Half Blue Screen Fill

START:
    ; Get VRAM information
    LOADI r0, 1          ; System call 1: Get VRAM info
    SYSCALL              ; r1=start, r2=size, r3=width, r4=height

    ; Store VRAM info for later use
    MOV r15, r1          ; r15 = VRAM start address
    MOV r14, r3          ; r14 = width (save for syscall parameter)
    MOV r13, r4          ; r13 = height (save for loop comparison)

    ; Calculate half width
    SHR r14, 1           ; r14 = width/2 (halfway point)

    ; Set up colors
    LOADI r5, 0xFF00FF00 ; Green color (ARGB: Alpha=FF, Red=00, Green=FF, Blue=00)
    LOADI r8, 0xFFFF0000 ; Blue color (ARGB: Alpha=FF, Red=00, Green=00, Blue=FF)

    ; Fill screen using pixel syscall
    CLR r6               ; r6 = y coordinate (row)
outer_loop:
    CLR r7               ; r7 = x coordinate (column)
    inner_loop:
        ; Set parameters for set_pixel_RGBA32 syscall
        LOADI r0, 0      ; r0 = syscall number (set_pixel_RGBA32)
        MOV r1, r7       ; r1 = x coordinate
        MOV r2, r6       ; r2 = y coordinate

        ; Choose color based on x position
        CMP r7, r14      ; Compare x with width/2
        JL green_half     ; If x < width/2, use green
        MOV r3, r8       ; Else use blue
        JMP set_pixel
    green_half:
        MOV r3, r5       ; Use green color

    set_pixel:
        MOV r4, r14      ; r4 = original width (restore it)
        SHL r4, 1        ; Multiply by 2 since we divided by 2 earlier

        ; Call set_pixel syscall
        SYSCALL

        ; Move to next column
        INC r7
        CMP r7, r4       ; Compare x with full width
        JL inner_loop

    ; Move to next row
    INC r6
    CMP r6, r13          ; Compare y with height
    JL outer_loop

    ; Halt the program
    HLT