; =============================================================================
; VRAM Constants
; =============================================================================
.const VRAM_BASE 0x102000
.const VRAM_SIZE 65536
.const VRAM_WIDTH 128
.const VRAM_HEIGHT 128
; =============================================================================
; SYSCALL: set_pixel_RGBA32 (ID: 0)
; =============================================================================
; Description:
;   Sets a single pixel at (x, y) to a specified RGBA32 color in VRAM.
;   Uses standard pixel addressing: address = (y * width + x) * 4 + vram_base
;
; Input Parameters:
;   r0 = Syscall ID (0)
;   r1 = X coordinate (0 to width - 1)
;   r2 = Y coordinate (0 to height - 1)
;   r3 = Color value (0xAABBGGRR format)
;   r4 = Screen width (pixels)
;   r15 = VRAM base address
;
; Output Parameters:
;   None
;
; Registers Modified:
;   r12, r13 (temporaries, preserved via stack)
;
; Memory Access:
;   Writes 4 bytes (1 pixel) to VRAM at calculated address
;
; Algorithm:
;   1. Calculate pixel offset: y * width + x
;   2. Convert to byte address: offset * 4 (RGBA32 = 4 bytes/pixel)
;   3. Add VRAM base address
;   4. Store color value at final address
;
; Performance:
;   - 8 instructions (excluding stack operations)
;   - 1 memory write operation
;   - O(1) constant time
;
; Error Conditions:
;   - No bounds checking performed
;   - Invalid coordinates may corrupt memory outside VRAM
;   - Coordinates >= screen dimensions will write beyond VRAM
;
; Example Usage:
;   ; Set pixel at (10, 20) to red
;   MOVI r0, 0          ; syscall ID
;   MOVI r1, 10         ; x coordinate
;   MOVI r2, 20         ; y coordinate
;   MOVI r3, 0xFFFF0000 ; red color (full alpha, full red)
;   MOVI r4, 128        ; screen width
;   MOVI r15, 139264    ; VRAM base
;   SYSCALL
; =============================================================================
syscall 0 set_pixel_RGBA32:
    PUSH r12
    PUSH r13

    ; Calculate pixel address: (y * width + x) * 4 + vram_base
    MOV r12, r2         ; r12 = y coordinate
    MUL r12, r4         ; r12 = y * width
    ADD r12, r1         ; r12 = y * width + x (pixel offset)
    MULI r12, 4         ; r12 = pixel_offset * 4 (byte offset)
    ADD r12, r15        ; r12 = byte_offset + vram_base (final address)

    ; Store color at calculated address
    STORE r3, r12       ; store color value to VRAM

    POP r13
    POP r12
    RET


; =============================================================================
; SYSCALL: get_neptune_vram_info (ID: 1)
; =============================================================================
; Description:
;   Retrieves Neptune GPU VRAM configuration and display parameters.
;   Returns hardware-specific constants for the Neptune graphics subsystem.
;
; Input Parameters:
;   r0 = Syscall ID (1)
;
; Output Parameters:
;   r1 = VRAM base address (139264 / 0x22000)
;   r2 = VRAM size in bytes (65536 bytes = 64KB)
;   r3 = Screen width in pixels (128)
;   r4 = Screen height in pixels (128)
;
; Registers Modified:
;   r1, r2, r3, r4 (output values only)
;
; Memory Access:
;   None - returns only constant values
;
; Algorithm:
;   Direct assignment of Neptune GPU hardware constants
;
; Performance:
;   - 4 instructions
;   - No memory operations
;   - O(1) constant time
;
; Technical Details:
;   - VRAM format: RGBA32 (4 bytes per pixel)
;   - Total pixels: width × height = 128 × 128 = 16,384 pixels
;   - Memory requirement: 16,384 × 4 = 65,536 bytes
;   - Address space: 0x22000 to 0x31FFF (inclusive)
;
; Usage Pattern:
;   Call this syscall once at program initialization to obtain
;   display parameters for use with other graphics syscalls
;
; Example Usage:
;   ; Get VRAM info for graphics initialization
;   MOVI r0, 1          ; syscall ID
;   SYSCALL
;   ; Now r1=vram_base, r2=vram_size, r3=width, r4=height
;   MOV r15, r1         ; save VRAM base for other syscalls
; =============================================================================
syscall 1 get_neptune_vram_info:
    MOVI r1, VRAM_BASE   ; VRAM base address
    MOVI r2, VRAM_SIZE   ; VRAM size: 128 × 128 × 4 bytes
    MOVI r3, VRAM_WIDTH  ; screen width in pixels
    MOVI r4, VRAM_HEIGHT ; screen height in pixels
    RET


; =============================================================================
; SYSCALL: clear_screen_RGBA32 (ID: 2)
; =============================================================================
; Description:
;   Fills the entire VRAM framebuffer with a single RGBA32 color value.
;   Efficiently clears or fills the screen using bulk memory operations.
;
; Input Parameters:
;   r0 = Syscall ID (2)
;   r1 = Fill color (0xAABBGGRR format)
;   r4 = Screen width in pixels
;   r5 = Screen height in pixels
;   r15 = VRAM base address
;
; Output Parameters:
;   None
;
; Registers Modified:
;   r12, r13, r14 (temporaries, preserved via stack)
;   r1 (overwritten during execution, original value not preserved)
;
; Memory Access:
;   Writes (width × height × 4) bytes to VRAM using MSET instruction
;
; Algorithm:
;   1. Preserve fill color (r1 gets overwritten by MSET requirements)
;   2. Calculate total pixel count: width × height
;   3. Use MSET to fill entire VRAM with specified color
;
; Performance:
;   - ~10 instructions (excluding stack operations)
;   - 1 bulk memory operation (MSET)
;   - O(1) execution time (hardware-accelerated fill)
;
; Technical Details:
;   - MSET requires count in r1, destination in r12, value in r13
;   - For 128×128 screen: writes 65,536 bytes (16,384 pixels)
;   - More efficient than pixel-by-pixel clearing
;
; Error Conditions:
;   - No bounds checking on VRAM access
;   - Invalid screen dimensions may corrupt adjacent memory
;   - Zero dimensions will result in no operation
;
; Example Usage:
;   ; Clear screen to black
;   MOVI r0, 2          ; syscall ID
;   MOVI r1, 0xFF000000 ; black with full alpha
;   MOVI r4, 128        ; screen width
;   MOVI r5, 128        ; screen height
;   MOVI r15, 139264    ; VRAM base
;   SYSCALL
; =============================================================================
syscall 2 clear_screen_RGBA32:
    PUSH r12
    PUSH r13
    PUSH r14

    ; Preserve fill color early (r1 will be overwritten for MSET)
    MOV r14, r1         ; r14 = fill color (preserved)

    ; Calculate total pixel count
    MOV r12, r4         ; r12 = width
    MUL r12, r5         ; r12 = width × height (pixel count)
    MOV r1, r12         ; r1 = count (required by MSET)

    ; Setup MSET parameters
    MOV r12, r15        ; r12 = destination (VRAM base address)
    MOV r13, r14        ; r13 = value (fill color)

    ; Perform bulk memory fill
    MSET r12, r13       ; fill VRAM: MSET(destination, value) with count in r1

    POP r14
    POP r13
    POP r12
    RET


; =============================================================================
; SYSCALL: copy_vram (ID: 3)
; =============================================================================
; Description:
;   Copies the entire VRAM framebuffer to a specified destination in RAM.
;   Useful for framebuffer backup, double buffering, or image processing.
;
; Input Parameters:
;   r0 = Syscall ID (3)
;   r4 = Screen width in pixels
;   r5 = Screen height in pixels
;   r14 = Destination address (must be valid RAM location)
;   r15 = VRAM source base address
;
; Output Parameters:
;   None
;
; Registers Modified:
;   r12, r13 (temporaries, preserved via stack)
;   r1 (overwritten during execution, original value not preserved)
;
; Memory Access:
;   Reads (width × height × 4) bytes from VRAM
;   Writes same amount to destination memory using MCPY instruction
;
; Algorithm:
;   1. Calculate total pixel count: width × height
;   2. Setup source (VRAM) and destination addresses
;   3. Use MCPY for efficient bulk memory copy
;
; Performance:
;   - ~8 instructions (excluding stack operations)
;   - 1 bulk memory operation (MCPY)
;   - O(1) execution time (hardware-accelerated copy)
;
; Technical Details:
;   - MCPY requires count in r1, destination in r12, source in r13
;   - For 128×128 screen: copies 65,536 bytes (16,384 pixels)
;   - Destination must have sufficient allocated space
;   - Source and destination may not overlap safely
;
; Memory Requirements:
;   - Destination must have at least (width × height × 4) bytes available
;   - For standard 128×128 screen: requires 65,536 bytes (64KB)
;
; Error Conditions:
;   - No bounds checking on source or destination
;   - Insufficient destination space causes memory corruption
;   - Invalid addresses may cause system fault
;   - Overlapping source/destination regions undefined behavior
;
; Use Cases:
;   - Save current screen state before drawing
;   - Implement double buffering for smooth animation
;   - Copy framebuffer for post-processing effects
;   - Create screenshots or image captures
;
; Example Usage:
;   ; Copy VRAM to backup buffer at address 0x50000
;   MOVI r0, 3          ; syscall ID
;   MOVI r4, 128        ; screen width
;   MOVI r5, 128        ; screen height
;   MOVI r14, 0x50000   ; destination address
;   MOVI r15, 139264    ; VRAM base
;   SYSCALL
; =============================================================================
syscall 3 copy_vram:
    PUSH r12
    PUSH r13

    ; Calculate total pixel count
    MOV r12, r4         ; r12 = width
    MUL r12, r5         ; r12 = width × height (pixel count)
    MOV r1, r12         ; r1 = count (required by MCPY)

    ; Setup MCPY parameters
    MOV r12, r14        ; r12 = destination address
    MOV r13, r15        ; r13 = source address (VRAM base)

    ; Perform bulk memory copy
    MCPY r12, r13       ; copy VRAM: MCPY(dest, src) with count in r1

    POP r13
    POP r12
    RET


; =============================================================================
; SYSCALL: draw_line_RGBA32 (ID: 4)
; =============================================================================
; Description:
;   Draws a line from (x1, y1) to (x2, y2) using Bresenham's line algorithm.
;
; Input Parameters:
;   r0 = Syscall ID (4)
;   r1 = X1 coordinate (start point)
;   r2 = Y1 coordinate (start point)
;   r3 = X2 coordinate (end point)
;   r4 = Y2 coordinate (end point)
;   r5 = Color value (0xAABBGGRR)
;   r6 = Screen width
;   r15 = VRAM base address
;
; Output Parameters:
;   None
;
; Registers Modified:
;   r7-r14 (temporaries, preserved)
;
; Memory Access:
;   Writes pixels to VRAM at calculated addresses
;
; Notes:
;   - Uses Bresenham's line algorithm for pixel-perfect lines
;   - No bounds checking performed
;   - Invalid coordinates may corrupt memory
; =============================================================================
syscall 4 draw_line_RGBA32:
    PUSH r7
    PUSH r8
    PUSH r9
    PUSH r10
    PUSH r11
    PUSH r12
    PUSH r13
    PUSH r14

    ; Calculate dx = abs(x2 - x1)
    MOV r7, r3          ; r7 = x2
    SUB r7, r1          ; r7 = x2 - x1
    MOV r8, r7          ; r8 = dx
    CMPI r8, 0
    JGE .dx_positive
    NEG r8              ; r8 = abs(dx)
.dx_positive:

    ; Calculate dy = abs(y2 - y1)
    MOV r9, r4          ; r9 = y2
    SUB r9, r2          ; r9 = y2 - y1
    MOV r10, r9         ; r10 = dy
    CMPI r10, 0
    JGE .dy_positive
    NEG r10             ; r10 = abs(dy)
.dy_positive:

    ; Determine step directions
    MOVI r11, 1         ; r11 = sx (x step direction)
    CMPI r7, 0           ; compare original dx
    JGE .sx_positive
    MOVI r11, -1
.sx_positive:

    MOVI r12, 1         ; r12 = sy (y step direction)
    CMPI r9, 0           ; compare original dy
    JGE .sy_positive
    MOVI r12, -1
.sy_positive:

    ; Calculate initial error: err = dx - dy
    MOV r13, r8         ; r13 = dx
    SUB r13, r10        ; r13 = dx - dy (error)

    ; Initialize current position
    MOV r7, r1          ; r7 = current x
    MOV r9, r2          ; r9 = current y

.line_loop:
    ; Draw pixel at current position (r7, r9)
    ; Calculate address: (y * width + x) * 4 + vram_base
    MOV r14, r9         ; r14 = y
    MUL r14, r6         ; r14 = y * width
    ADD r14, r7         ; r14 = y * width + x
    MULI r14, 4         ; r14 = (y * width + x) * 4
    ADD r14, r15        ; r14 = address in VRAM
    STORE r5, r14       ; store color at address

    ; Check if we've reached the end point
    CMP r7, r3          ; compare current x with x2
    JNZ .continue_line
    CMP r9, r4          ; compare current y with y2
    JZ .line_done       ; if both x and y match, we're done

.continue_line:
    ; Calculate 2 * error
    MOV r14, r13        ; r14 = err
    MULI r14, 2         ; r14 = 2 * err

    ; Check if 2*err > -dy (should step in x direction)
    MOV r11, r10        ; temp = dy
    NEG r11             ; temp = -dy
    CMP r14, r11        ; compare 2*err with -dy
    JLE .check_y_step

    ; Step in x direction
    SUB r13, r10        ; err -= dy
    MOVI r11, 1          ; restore sx
    CMP r3, r1          ; compare x2 with x1 (original)
    JGE .sx_pos_x
    MOVI r11, -1
.sx_pos_x:
    ADD r7, r11         ; x += sx

.check_y_step:
    ; Check if 2*err < dx (should step in y direction)
    CMP r14, r8         ; compare 2*err with dx
    JGE .line_loop

    ; Step in y direction
    ADD r13, r8         ; err += dx
    MOVI r12, 1          ; restore sy
    CMP r4, r2          ; compare y2 with y1 (original)
    JGE .sy_pos_y
    MOVI r12, -1
.sy_pos_y:
    ADD r9, r12         ; y += sy

    JMP .line_loop

.line_done:
    POP r14
    POP r13
    POP r12
    POP r11
    POP r10
    POP r9
    POP r8
    POP r7
    RET


; =============================================================================
; Memory Management Constants
; =============================================================================
.const HEAP_START          0x00082000      ; Start of heap area
.const HEAP_END            0x00101FFC      ; End of heap area (just before stack)
.const HEAP_SIZE           0x0007DFFC      ; Total heap size (512 KB - 4 bytes for stack)

; Global variables (stored at start of heap)
.const heap_initialized    0x82000      ; 1 byte: 0=uninitialized, 1=initialized
.const free_list_head      0x82001      ; 4 bytes: pointer to first free block
.const current_bump_ptr    0x82005      ; 4 bytes: current position for bump allocation
.const heap_data_start     0x82009      ; Start of actual allocatable memory

; Free block structure (stored in freed memory):
; +0: next_ptr (4 bytes) - pointer to next free block
; +4: size (4 bytes) - size of this free block (including header)

; =============================================================================
; SYSCALL: malloc (ID: 5)
; =============================================================================
; Description:
;   Allocates a block of memory from the heap using a hybrid bump/free-list
;   allocator. First tries to find a suitable free block, then falls back
;   to bump allocation.
;
; Input Parameters:
;   r0 = Syscall ID (5)
;   r1 = Size in bytes to allocate (must be > 0)
;
; Output Parameters:
;   r1 = Pointer to allocated memory (0 if allocation failed)
;
; Registers Modified:
;   r1 (return value), r12-r14 (temporaries, preserved)
;
; Memory Layout:
;   - Heap metadata stored at beginning of heap
;   - Free blocks form intrusive linked list
;   - Each free block has 8-byte header (next_ptr, size)
;
; Algorithm:
;   1. Initialize heap on first call
;   2. Search free list for suitable block
;   3. If found, remove from list and return
;   4. Otherwise, try bump allocation
;   5. Return 0 if out of memory
;
; Error Conditions:
;   - Returns 0 if size is 0 or allocation fails
;   - No bounds checking on returned pointer usage
; =============================================================================
syscall 5 malloc:
    PUSH r12
    PUSH r13
    PUSH r14

    ; Check if size is valid (non-zero)
    CMPI r1, 0
    JZ .malloc_fail

    ; Round size up to word boundary (multiple of 4)
    ADDI r1, 3              ; Add 3 for rounding
    ANDI r1, 0xFFFFFFFC     ; Clear bottom 2 bits

    ; Check if heap is initialized
    LOADI r12, heap_initialized
    CMPI r12, 0
    JNZ .heap_ready

    ; Initialize heap on first call
    MOVI r12, 1
    STORI r12, heap_initialized

    MOVI r12, 0             ; No free blocks initially
    STORI r12, free_list_head

    MOVI r12, heap_data_start ; Initialize bump pointer
    STORI r12, current_bump_ptr

.heap_ready:
    ; Try to find a suitable free block
    LOADI r12, free_list_head   ; r12 = current block pointer
    MOVI r13, free_list_head    ; r13 = pointer to previous next field

.find_block_loop:
    CMPI r12, 0             ; Check if we've reached end of list
    JZ .try_bump_alloc

    ; Load block size
    MOV r14, r12
    ADDI r14, 4
    LOAD r14, r14           ; r14 = block size

    ; Check if block is large enough
    CMP r14, r1             ; Compare block size with requested size
    JGE .found_suitable_block

    ; Move to next block
    MOV r13, r12            ; Previous = current
    LOAD r12, r12           ; Load next pointer from current block
    JMP .find_block_loop

.found_suitable_block:
    ; Remove block from free list
    LOAD r14, r12           ; Load next pointer from found block
    STORE r14, r13          ; Update previous->next = found->next

    ; Return the block (skip the header we used for free list)
    MOV r1, r12             ; Return pointer to block
    JMP .malloc_success

.try_bump_alloc:
    ; Try bump allocation
    LOADI r12, current_bump_ptr ; r12 = current bump pointer
    MOV r13, r12            ; r13 = allocation address
    ADD r12, r1             ; r12 = new bump pointer

    ; Check if we have enough space
    CMPI r12, HEAP_END
    JG .malloc_fail

    ; Update bump pointer
    STORI r12, current_bump_ptr

    ; Return allocated address
    MOV r1, r13
    JMP .malloc_success

.malloc_fail:
    MOVI r1, 0              ; Return NULL on failure

.malloc_success:
    POP r14
    POP r13
    POP r12
    RET


; =============================================================================
; SYSCALL: free (ID: 6)
; =============================================================================
; Description:
;   Frees a previously allocated memory block by adding it to the free list.
;   Uses intrusive linked list stored directly in the freed memory.
;
; Input Parameters:
;   r0 = Syscall ID (6)
;   r1 = Pointer to memory block to free (must be valid malloc'd pointer)
;   r2 = Size of the block being freed (must match original malloc size)
;
; Output Parameters:
;   None
;
; Registers Modified:
;   r12, r13 (temporaries, preserved)
;
; Memory Access:
;   Writes 8 bytes to the freed block (next pointer + size)
;   Updates free list head pointer
;
; Algorithm:
;   1. Validate inputs (non-null pointer, non-zero size)
;   2. Write free block header into freed memory
;   3. Add block to front of free list
;   4. Update free list head
;
; Error Conditions:
;   - Silently ignores null pointers or zero sizes
;   - No validation that pointer was actually malloc'd
;   - Double-free will corrupt free list
;
; Block Format:
;   Freed blocks store metadata in first 8 bytes:
;   [+0] next_ptr: Pointer to next free block (0 if last)
;   [+4] size: Size of this block in bytes
; =============================================================================
syscall 6 free:
    PUSH r12
    PUSH r13

    ; Validate inputs
    CMPI r1, 0              ; Check for null pointer
    JZ .free_done
    CMPI r2, 0              ; Check for zero size
    JZ .free_done

    ; Check if heap is initialized (shouldn't free before any malloc)
    LOADI r12, heap_initialized
    CMPI r12, 0
    JZ .free_done

    ; Setup free block header in the freed memory
    ; First, get current head of free list
    LOADI r12, free_list_head

    ; Store next pointer in freed block
    STORE r12, r1           ; freed_block->next = old_head

    ; Store size in freed block
    MOV r13, r1
    ADDI r13, 4
    STORE r2, r13           ; freed_block->size = size

    ; Update free list head to point to this block
    STORI r1, free_list_head

.free_done:
    POP r13
    POP r12
    RET


; =============================================================================
; SYSCALL: get_heap_info (ID: 7)
; =============================================================================
; Description:
;   Returns information about heap usage and configuration.
;   Useful for debugging and memory management monitoring.
;
; Input Parameters:
;   r0 = Syscall ID (7)
;
; Output Parameters:
;   r1 = Heap start address (0x4000)
;   r2 = Heap end address (0x21FFC)
;   r3 = Current bump pointer position
;   r4 = Free list head pointer (0 if no free blocks)
;
; Registers Modified:
;   r1, r2, r3, r4 (output values only)
;
; Example Usage:
;   ; Get heap information
;   MOVI r0, 7
;   SYSCALL
;   ; Now r1=heap_start, r2=heap_end, r3=bump_ptr, r4=free_head
; =============================================================================
syscall 7 get_heap_info:
    MOVI r1, HEAP_START         ; Heap start address
    MOVI r2, HEAP_END           ; Heap end address

    ; Load current bump pointer (might be 0 if uninitialized)
    LOADI r3, current_bump_ptr

    ; Load free list head (might be 0 if no free blocks)
    LOADI r4, free_list_head

    RET