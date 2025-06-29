const KEYBOARD_BASE 0x32000
const CONSOLE_OUT 0x32010
const FIRST_CHAR 0x00
const CURRENT_CHAR 0x08
const CONTROL 0x0C
const ENTER_KEY 10
const CONSUME 1

main:
    MOVI r0, KEYBOARD_BASE   ; Keyboard base address
    MOVI r6, CONSOLE_OUT     ; Console output address

    ; Set up register pointers
    MOVI r1, FIRST_CHAR
    ADD r1, r0               ; r1 = FIRST_CHAR pointer
    MOVI r3, CURRENT_CHAR
    ADD r3, r0               ; r3 = CURRENT_CHAR pointer
    MOVI r4, CONTROL
    ADD r4, r0               ; r4 = CONTROL pointer

    MOVI r7, CONSUME         ; r7 = 1 (consume command)

wait_for_enter:
    LOAD r5, r3              ; Load CURRENT_CHAR
    MOVI r8, ENTER_KEY
    CMP r5, r8               ; Check for Enter key
    JNE wait_for_enter       ; Keep waiting if not Enter

    ; Enter pressed, process entire buffer
process_buffer:
    LOAD r5, r1              ; Load FIRST_CHAR
    CMPI r5, 0               ; Check if buffer is empty
    JE restart               ; If empty, restart

    STORI r5, CONSOLE_OUT    ; Print character to console
    STORE r7, r4             ; Consume character (write 1 to CONTROL)

    ; Short delay to ensure device updates
    MOVI r8, 10
delay_loop:
    SUBI r8, 1
    JNE delay_loop

    JMP process_buffer       ; Process next character

restart:
    JMP main