; =============================================================================
; Neptune Plasma Wave Animation
; A mesmerizing animated plasma effect with rotating color waves
; =============================================================================

; Program entry point
main:
    ; Get VRAM information
    LOADI r0, 1              ; Syscall ID for get_neptune_vram_info
    SYSCALL
    ; r1 = VRAM base (139264), r2 = size, r3 = width (128), r4 = height (128)

    MOV r15, r1              ; Store VRAM base in r15 for pixel syscalls
    MOV r10, r3              ; Store screen width in r10
    MOV r11, r4              ; Store screen height in r11

    ; Initialize animation variables
    CLR r14                  ; r14 = time counter for animation

; Main animation loop
animation_loop:
    ; Clear screen with dark background
    LOADI r0, 2              ; Clear screen syscall
    MOVI r1, 0xFF001122     ; Dark blue-purple background
    MOV r4, r10              ; Screen width
    MOV r5, r11              ; Screen height
    SYSCALL

    ; Draw plasma wave pattern
    CLR r8                   ; r8 = y coordinate

y_loop:
    CLR r9                   ; r9 = x coordinate

x_loop:
    ; Calculate plasma effect using sin/cos approximation
    ; color = sin(x/16 + time) + cos(y/16 + time) + sin((x+y)/32 + time)

    ; Calculate wave1: based on x position and time
    MOV r5, r9               ; x coordinate
    DIVI r5, 8               ; x/8 for frequency
    ADD r5, r14              ; add time offset
    ANDI r5, 63              ; keep in 0-63 range for wave table

    ; Calculate wave2: based on y position and time
    MOV r6, r8               ; y coordinate
    DIVI r6, 8               ; y/8 for frequency
    ADD r6, r14              ; add time offset
    ANDI r6, 63              ; keep in range

    ; Calculate wave3: based on distance from center
    MOV r12, r9              ; x
    SUBI r12, 64             ; center x
    MOV r13, r8              ; y
    SUBI r13, 64             ; center y

    ; Simple distance approximation: abs(x) + abs(y)
    CMPI r12, 0
    JGE skip_neg_x
    NEG r12
skip_neg_x:
    CMPI r13, 0
    JGE skip_neg_y
    NEG r13
skip_neg_y:
    ADD r12, r13             ; distance approximation
    DIVI r12, 4              ; scale down
    ADD r12, r14             ; add time
    ANDI r12, 63             ; keep in range

    ; Combine waves (simple addition)
    ADD r5, r6
    ADD r5, r12
    ANDI r5, 255             ; keep in 0-255 range

    ; Convert wave value to RGB color
    ; Red component: wave value
    MOV r12, r5
    SHL r12, 16              ; shift to red position

    ; Green component: wave shifted by 85 (1/3 of 255)
    MOV r13, r5
    ADDI r13, 85
    ANDI r13, 255
    SHL r13, 8               ; shift to green position

    ; Blue component: wave shifted by 170 (2/3 of 255)
    MOV r3, r5
    ADDI r3, 170
    ANDI r3, 255             ; blue in low byte

    ; Combine RGB components (Alpha = 0xFF)
    OR r3, r13               ; combine blue and green
    OR r3, r12               ; combine with red
    ORI r3, 0xFF000000       ; set alpha channel

    ; Set pixel
    CLR r0                   ; set_pixel syscall (ID = 0)
    MOV r1, r9               ; x coordinate
    MOV r2, r8               ; y coordinate
    ; r3 already has color
    MOV r4, r10              ; screen width
    SYSCALL

    ; Next x coordinate
    INC r9
    CMP r9, r10
    JL x_loop

    ; Next y coordinate
    INC r8
    CMP r8, r11
    JL y_loop

    ; Add some sparkles/stars
    MOVI r7, 20             ; number of sparkles
sparkle_loop:
    ; Generate pseudo-random coordinates using time
    MOV r1, r14              ; use time as seed
    MUL r1, r7               ; multiply by sparkle counter
    ADDI r1, 17              ; add prime number
    MOD r1, r10              ; x = random % width

    MOV r2, r14              ; use time as seed
    MUL r2, r7               ; multiply by sparkle counter
    ADDI r2, 31              ; add different prime
    MOD r2, r11              ; y = random % height

    ; Bright white sparkle
    CLR r0                   ; set_pixel syscall (ID = 0)
    MOVI r3, 0xFFFFFFFF     ; white color
    MOV r4, r10              ; screen width
    SYSCALL

    DEC r7
    CMPI r7, 0
    JG sparkle_loop

    ; Add central rotating spiral
    MOVI r7, 32             ; number of spiral points
spiral_loop:
    ; Calculate spiral position
    MOV r12, r7              ; angle based on loop counter
    MUL r12, r14             ; multiply by time for rotation
    DIVI r12, 4              ; slow down rotation

    ; Calculate radius (grows from center)
    MOV r13, r7
    MULI r13, 2              ; radius grows with angle

    ; Convert polar to cartesian (simplified)
    ; x = center + radius * cos(angle) ≈ center + radius * (angle & 1 ? -1 : 1)
    MOVI r1, 64               ; center x
    TESTI r12, 1              ; check if angle is odd
    JZ spiral_pos_x
    SUB r1, r13              ; x = center - radius
    JMP spiral_y
spiral_pos_x:
    ADD r1, r13              ; x = center + radius

spiral_y:
    ; y = center + radius * sin(angle) ≈ center + radius * (angle & 2 ? -1 : 1)
    MOVI r2, 64               ; center y
    TESTI r12, 2              ; check if angle bit 1 is set
    JZ spiral_pos_y
    SUB r2, r13              ; y = center - radius
    JMP spiral_bounds
spiral_pos_y:
    ADD r2, r13              ; y = center + radius

spiral_bounds:
    ; Check bounds
    CMPI r1, 0
    JL spiral_next
    CMP r1, r10
    JGE spiral_next
    CMPI r2, 0
    JL spiral_next
    CMP r2, r11
    JGE spiral_next

    ; Draw spiral pixel with color based on angle
    CLR r0                   ; set_pixel syscall (ID = 0)
    MOV r3, r12              ; use angle for color
    SHL r3, 18               ; shift for interesting color
    ORI r3, 0xFF000000       ; set alpha
    MOV r4, r10              ; screen width
    SYSCALL

spiral_next:
    DEC r7
    CMPI r7, 0
    JG spiral_loop

    ; Animation delay (simple loop)
    MOVI r12, 5000          ; delay counter
delay_loop:
    DEC r12
    CMPI r12, 0
    JG delay_loop

    ; Increment time and continue animation
    INC r14
    ANDI r14, 1023           ; prevent overflow, cycle every 1024 frames

    ; Loop forever
    JMP animation_loop

; Program should never reach here, but just in case
    HLT