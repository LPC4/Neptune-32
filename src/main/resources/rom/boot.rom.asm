; =============================================================================
; SYSCALL: set_pixel_RGBA32 (ID: 0)
; =============================================================================
; Description:
;   Sets a single pixel at (x, y) to a specified RGBA32 color in VRAM.
;
; Input Parameters:
;   r0 = Syscall ID (0)
;   r1 = X coordinate (0 to width - 1)
;   r2 = Y coordinate (0 to height - 1)
;   r3 = Color value (0xAARRGGBB)
;   r4 = Screen width
;   r15 = VRAM base address
;
; Output Parameters:
;   None
;
; Registers Modified:
;   r12, r13 (temporaries, preserved)
;
; Memory Access:
;   Writes 4 bytes (1 pixel) to VRAM at calculated address
;
; Notes:
;   - No bounds checking performed
;   - Invalid coordinates may corrupt memory
; =============================================================================
syscall 0 set_pixel_RGBA32:
    PUSH r12
    PUSH r13

    MOV r12, r2
    MUL r12, r4
    ADD r12, r1
    MULI r12, 4
    ADD r12, r15

    STORE r3, r12

    POP r13
    POP r12

    RET


; =============================================================================
; SYSCALL: get_neptune_vram_info (ID: 1)
; =============================================================================
; Description:
;   Retrieves Neptune VRAM configuration information.
;
; Input Parameters:
;   r0 = Syscall ID (1)
;
; Output Parameters:
;   r1 = VRAM base address (0x00022000 / 139264)
;   r2 = VRAM size in bytes (65536 bytes = 64KB)
;   r3 = Screen width (128 pixels)
;   r4 = Screen height (128 pixels)
;
; Registers Modified:
;   r1, r2, r3, r4 (output values)
;
; Memory Access:
;   None
;
; Notes:
;   - VRAM uses RGBA32 format (4 bytes per pixel)
;   - Pixel count = width * height = 16,384 pixels
; =============================================================================
syscall 1 get_neptune_vram_info:
    MOVI r1, 139264    ; VRAM base address
    MOVI r2, 65536     ; VRAM size (128 * 128 * 4)
    MOVI r3, 128       ; screen width
    MOVI r4, 128       ; screen height
    RET


; =============================================================================
; SYSCALL: clear_screen_RGBA32 (ID: 2)
; =============================================================================
; Description:
;   Fills the entire VRAM screen with a single RGBA32 color.
;
; Input Parameters:
;   r0 = Syscall ID (2)
;   r1 = Fill color (0xAARRGGBB)
;   r4 = Screen width
;   r5 = Screen height
;   r15 = VRAM base address
;
; Output Parameters:
;   None
;
; Registers Modified:
;   r12, r13, r1 (temporaries, preserved)
;
; Memory Access:
;   Writes width * height * 4 bytes to VRAM
;
; Notes:
;   - No bounds checking
; =============================================================================
syscall 2 clear_screen_RGBA32:
    PUSH r12
    PUSH r13
    PUSH r1

    ; Calculate total pixels (words) to clear
    MOV r12, r4         ; width
    MUL r12, r5         ; width * height = total pixels
    MOV r1, r12         ; r1 = count for MSET

    ; Set up destination and value
    MOV r13, r15        ; r13 = VRAM base address
    MOV r2, r1          ; r2 = color (from input r1)

    ; Fill VRAM with color
    MSET r13, r2        ; fill VRAM (r13=address, r2=color, r1=count)

    POP r1
    POP r13
    POP r12
    RET

; =============================================================================
; SYSCALL: copy_vram (ID: 3)
; =============================================================================
; Description:
;   Copies the entire VRAM to a specified destination in RAM.
;
; Input Parameters:
;   r0 = Syscall ID (3)
;   r4 = Screen width
;   r5 = Screen height
;   r14 = Destination address
;   r15 = VRAM source base address
;
; Output Parameters:
;   None
;
; Registers Modified:
;   r12, r13, r1 (temporaries, preserved)
;
; Memory Access:
;   Reads width * height * 4 bytes from VRAM
;   Writes same amount to destination memory
;
; Notes:
;   - Destination must have enough allocated space
; =============================================================================
syscall 3 copy_vram:
    PUSH r12
    PUSH r13
    PUSH r1

    MOV r12, r4
    MUL r12, r5         ; r12 = total pixels

    MOV r1, r12         ; r1 = count
    MOV r13, r14        ; destination address
    MOV r14, r15        ; source address (VRAM base)

    MCPY r13, r14       ; copy VRAM to destination

    POP r1
    POP r13
    POP r12

    RET


