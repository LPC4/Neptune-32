main:
; ---------------------------------
; Get heap info (optional)
MOVI r0, 7
SYSCALL
; r1=heap_start, r2=heap_end, r3=bump_ptr, r4=free_head

; ---------------------------------
; Allocate 8 bytes (2 words)
MOVI r0, 5
MOVI r1, 8
SYSCALL
MOV r10, r1          ; store pointer to heap in r10

; ---------------------------------
; Write data into heap
MOVI r2, 0x41414141  ; 'AAAA'
STORE r2, r10

MOVI r3, 0x42424242  ; 'BBBB'

MOV r11, r10
MOVI r12, 4
ADD r11, r12         ; r11 = r10 + 4

STORE r3, r11

; ---------------------------------
; Read back and print 'AAAA'
LOAD r4, r10
MOV r12, r4

; Print byte 1
MOV r13, r12
ANDI r13, 0xFF
STORI r13, 0x32010

; Print byte 2
SHR r12, 8
MOV r13, r12
ANDI r13, 0xFF
STORI r13, 0x32010

; Print byte 3
SHR r12, 8
MOV r13, r12
ANDI r13, 0xFF
STORI r13, 0x32010

; Print byte 4
SHR r12, 8
MOV r13, r12
ANDI r13, 0xFF
STORI r13, 0x32010

; ---------------------------------
; Read back and print 'BBBB'
LOAD r4, r11
MOV r12, r4

; Print byte 1
MOV r13, r12
ANDI r13, 0xFF
STORI r13, 0x32010

; Print byte 2
SHR r12, 8
MOV r13, r12
ANDI r13, 0xFF
STORI r13, 0x32010

; Print byte 3
SHR r12, 8
MOV r13, r12
ANDI r13, 0xFF
STORI r13, 0x32010

; Print byte 4
SHR r12, 8
MOV r13, r12
ANDI r13, 0xFF
STORI r13, 0x32010

; Free the memory
MOVI r0, 6
MOV r1, r10
MOVI r2, 8
SYSCALL

; ---------------------------------
; Done - Loop forever
loop:
JMP loop
