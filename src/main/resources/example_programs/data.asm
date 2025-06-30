.const COUT 0x00112010

.data
; String data
string message = "Hello World!\n"
string name = "CPU Emulator\n"
string newline = "\n"
string hex_prefix = "0x"
string dec_prefix = ""

; Integer data
int magic_number = 0x12345678
int counter = 42
int zero_value = 0

; Byte data
byte status_flag = 0xAB
byte test_byte = 255

; Array data
array numbers[5] = 10, 20, 30, 40, 50
array matrix[4] = 0x1111, 0x2222, 0x3333, 0x4444

; Buffer for dynamic data
buffer workspace[16]

.code

main:
    ; Print message string
    MOVI r1, message
    CALL print_string

    ; Print name string
    MOVI r1, name
    CALL print_string

    ; Print magic number
    MOVI r1, magic_number
    LOAD r2, r1
    MOVI r1, hex_prefix
    CALL print_string
    CALL print_hex
    MOVI r1, newline
    CALL print_string

    ; Print counter
    MOVI r1, counter
    LOAD r2, r1
    CALL print_decimal
    MOVI r1, newline
    CALL print_string

    ; Print zero_value
    MOVI r1, zero_value
    LOAD r2, r1
    CALL print_decimal
    MOVI r1, newline
    CALL print_string

    ; Print status_flag
    MOVI r1, status_flag
    LOAD r2, r1
    MOVI r1, hex_prefix
    CALL print_string
    CALL print_hex_byte
    MOVI r1, newline
    CALL print_string

    ; Print test_byte
    MOVI r1, test_byte
    LOAD r2, r1
    CALL print_decimal_byte
    MOVI r1, newline
    CALL print_string

    ; Print numbers array
    MOVI r1, numbers
    MOVI r3, 5          ; array length
    CALL print_array

    ; Print matrix array
    MOVI r1, matrix
    MOVI r3, 4          ; array length
    CALL print_array_hex

infloop:
    jmp infloop

; Subroutine to print null-terminated string
; Input: r1 = string address
print_string:
    PUSH r2
    PUSH r3
    MOVI r3, COUT

    print_string_loop:
        LOAD r2, r1         ; Load character
        CMPI r2, 0          ; Check for null terminator
        JE print_string_done
        STORE r2, r3        ; Print character
        INC r1              ; Next character
        JMP print_string_loop

    print_string_done:
    POP r3
    POP r2
    RET

; Subroutine to print 32-bit hex value
; Input: r2 = value to print
print_hex:
    PUSH r1
    PUSH r3
    PUSH r4
    MOVI r3, COUT
    MOVI r4, 8              ; 8 hex digits

    print_hex_loop:
        MOVI r1, 0xF0000000
        AND r1, r2          ; Get top nibble
        SHL r2, 4           ; Shift next nibble into position

        ; Convert nibble to ASCII
        SHR r1, 28          ; Shift nibble to bottom
        CMPI r1, 10
        JB print_hex_digit
        ADDI r1, 55         ; Adjust for A-F (55 = 'A' - 10)
        JMP print_hex_store

    print_hex_digit:
        ADDI r1, 48         ; '0' = 48

    print_hex_store:
        STORE r1, r3        ; Print digit
        DEC r4
        JNZ print_hex_loop

    POP r4
    POP r3
    POP r1
    RET

; Subroutine to print 32-bit decimal value
; Input: r2 = value to print
print_decimal:
    PUSH r1
    PUSH r3
    PUSH r4
    PUSH r5
    MOVI r3, COUT
    MOVI r4, 10             ; Base 10
    MOVI r5, 0              ; Digit counter

    ; Handle zero case
    CMPI r2, 0
    JNE print_decimal_nonzero
    MOVI r1, 48
    STORE r1, r3
    JMP print_decimal_done

print_decimal_nonzero:
    ; Push digits onto stack
    print_decimal_divloop:
        MOV r1, r2
        DIV r1, r4          ; r1 = r1 / 10
        MUL r1, r4          ; r1 = r1 * 10
        SUB r1, r2          ; r1 = remainder (digit)
        NEG r1              ; Make positive
        PUSH r1             ; Save digit
        INC r5              ; Count digits
        DIV r2, r4          ; r2 = r2 / 10
        CMPI r2, 0
        JNZ print_decimal_divloop

    ; Pop and print digits
    print_decimal_printloop:
        POP r1
        ADDI r1, 48
        STORE r1, r3
        DEC r5
        JNZ print_decimal_printloop

print_decimal_done:
    POP r5
    POP r4
    POP r3
    POP r1
    RET

; Subroutine to print 8-bit hex value
; Input: r2 = value to print (lower byte)
print_hex_byte:
    PUSH r1
    PUSH r3
    MOVI r3, COUT

    ; Print first nibble
    MOV r1, r2
    SHR r1, 4
    ANDI r1, 0xF
    CMPI r1, 10
    JB print_hex_byte_digit1
    ADDI r1, 55         ; Adjust for A-F
    JMP print_hex_byte_store1

print_hex_byte_digit1:
    ADDI r1, 48         ; '0'

print_hex_byte_store1:
    STORE r1, r3

    ; Print second nibble
    MOV r1, r2
    ANDI r1, 0xF
    CMPI r1, 10
    JB print_hex_byte_digit2
    ADDI r1, 55         ; Adjust for A-F
    JMP print_hex_byte_store2

print_hex_byte_digit2:
    ADDI r1, 48         ; '0'

print_hex_byte_store2:
    STORE r1, r3

    POP r3
    POP r1
    RET

; Subroutine to print 8-bit decimal value
; Input: r2 = value to print (lower byte)
print_decimal_byte:
    PUSH r1
    PUSH r3
    PUSH r4
    PUSH r5
    MOVI r3, COUT
    MOVI r4, 10             ; Base 10
    MOVI r5, 0              ; Digit counter
    ANDI r2, 0xFF           ; Ensure we only use lower byte

    ; Handle zero case
    CMPI r2, 0
    JNE print_decimal_byte_nonzero
    MOVI r1, 48
    STORE r1, r3
    JMP print_decimal_byte_done

print_decimal_byte_nonzero:
    ; Push digits onto stack
    print_decimal_byte_divloop:
        MOV r1, r2
        DIV r1, r4          ; r1 = r1 / 10
        MUL r1, r4          ; r1 = r1 * 10
        SUB r1, r2          ; r1 = remainder (digit)
        NEG r1              ; Make positive
        PUSH r1             ; Save digit
        INC r5              ; Count digits
        DIV r2, r4          ; r2 = r2 / 10
        CMPI r2, 0
        JNZ print_decimal_byte_divloop

    ; Pop and print digits
    print_decimal_byte_printloop:
        POP r1
        ADDI r1, 48
        STORE r1, r3
        DEC r5
        JNZ print_decimal_byte_printloop

print_decimal_byte_done:
    POP r5
    POP r4
    POP r3
    POP r1
    RET

; Subroutine to print array of decimal values
; Input: r1 = array address, r3 = length
print_array:
    PUSH r2
    PUSH r4
    MOVI r4, 0              ; Counter

    print_array_loop:
        CMP r4, r3
        JE print_array_done
        LOAD r2, r1         ; Load array element
        CALL print_decimal
        MOVI r1, newline
        CALL print_string
        MOVI r1, numbers     ; Reload array base (since r1 was modified)
        ADD r1, r4           ; Add offset
        INC r1              ; Next element
        INC r4              ; Increment counter
        JMP print_array_loop

    print_array_done:
    POP r4
    POP r2
    RET

; Subroutine to print array of hex values
; Input: r1 = array address, r3 = length
print_array_hex:
    PUSH r2
    PUSH r4
    MOVI r4, 0              ; Counter

    print_array_hex_loop:
        CMP r4, r3
        JE print_array_hex_done
        LOAD r2, r1         ; Load array element
        MOVI r1, hex_prefix
        CALL print_string
        CALL print_hex
        MOVI r1, newline
        CALL print_string
        MOVI r1, matrix      ; Reload array base
        ADD r1, r4           ; Add offset
        INC r1              ; Next element
        INC r4              ; Increment counter
        JMP print_array_hex_loop

    print_array_hex_done:
    POP r4
    POP r2
    RET