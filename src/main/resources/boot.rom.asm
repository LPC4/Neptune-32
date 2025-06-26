; =============================================================================
; SYSCALL: set_pixel_RGBA32 (ID: 0)
; =============================================================================
; Description: Sets a single pixel color at specified coordinates in VRAM
;
; Input Parameters:
;   r0 = Syscall ID (must be 0 for this syscall)
;   r1 = X coordinate (0 to width-1)
;   r2 = Y coordinate (0 to height-1)
;   r3 = Color value in RGBA32 format (0xAARRGGBB)
;   r4 = Screen width in pixels
;
; Output Parameters:
;   None (all input registers preserved except temporaries)
;
; Registers Modified:
;   r12, r13 (used as temporaries, restored before return)
;
; Prerequisites:
;   - VRAM base address must be stored in r15
;   - Coordinates must be within screen bounds
;   - Color format: Alpha(8) Red(8) Green(8) Blue(8)
;
; Memory Access:
;   Writes 4 bytes to VRAM at calculated pixel address
;
; Error Conditions:
;   - No bounds checking performed
;   - Invalid coordinates may corrupt memory
; =============================================================================
syscall 0 set_pixel_RGBA32:
    ; Preserve registers that will be modified
    PUSH r12                 ; Save temporary register 1
    PUSH r13                 ; Save temporary register 2

    ; Calculate pixel memory address using formula:
    ; pixel_addr = VRAM_base + ((y * width) + x) * bytes_per_pixel
    ; Where bytes_per_pixel = 4 for RGBA32 format

    MOV r12, r2              ; r12 = y coordinate
    MUL r12, r4              ; r12 = y * screen_width
    ADD r12, r1              ; r12 = (y * width) + x = pixel_index
    MULI r12, 4              ; r12 = pixel_index * 4 bytes = byte_offset

    ; Convert byte offset to absolute VRAM address
    ADD r12, r15             ; r12 = VRAM_base + byte_offset = final_address

    ; Write the 32-bit color value to the calculated address
    STORE r3, r12            ; Memory[pixel_address] = color_value

    ; Restore modified registers in reverse order
    POP r13                  ; Restore temporary register 2
    POP r12                  ; Restore temporary register 1

    ; Return to caller
    RET


; =============================================================================
; SYSCALL: get_neptune_vram_info (ID: 1)
; =============================================================================
; Description: Returns Neptune system VRAM configuration information
;
; Input Parameters:
;   r0 = Syscall ID (must be 100 for this syscall)
;
; Output Parameters:
;   r1 = VRAM base address (0x0000A000)
;   r2 = VRAM total size in bytes (65,536 bytes = 64KB)
;   r3 = Screen width in pixels (128)
;   r4 = Screen height in pixels (128)
;
; Registers Modified:
;   r1, r2, r3, r4 (output parameters)
;   All other registers preserved
;
; Memory Access:
;   None (read-only syscall)
;
; Notes:
;   - VRAM format is RGBA32 (4 bytes per pixel)
;   - Total pixels = width * height = 128 * 128 = 16,384
;   - Memory layout: 16,384 pixels * 4 bytes = 65,536 bytes
; =============================================================================
syscall 1 get_neptune_vram_info:
    ; Load Neptune VRAM configuration constants
    LOADI r1, 139264         ; VRAM base address (from memory map)
    LOADI r2, 65536          ; VRAM size: 128*128*4 bytes = 64KB
    LOADI r3, 128            ; Screen width in pixels
    LOADI r4, 128            ; Screen height in pixels

    ; Return to caller with VRAM info in r1-r4
    RET