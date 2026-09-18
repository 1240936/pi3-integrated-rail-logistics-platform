.section .text
.global decrypt_data


# It receives as input
# a0 = in (string)
# in -> represents the input data that we are going to encrypt. ['A','Z']

# a1 = key (integer)
# key -> represents a shift amount, controls how the encryption works, [1, 26]

# It receives as output
# a2 = out (string)
# out -> represents a pointer to a string where the encrypted result will be stored.

# The function must decrypt the string in according to the Caesar Cipher
# return 1: if it succeeds
# return 0: if it not succeeds -> and it should set out as an empty string

# How Caesar Cipher works:
# Decryption:
# choose a shift value, in this case: the key
# replace each letter in the array in, with the letter that is key positions earlier in the alphabet.


decrypt_data:
    addi    sp, sp, -20                         # reserva 20 bytes na stack
    sw      ra, 16(sp)                          # salva ra
    sw      s0, 12(sp)                          # salva s0 (resultado)
    sw      s1, 8(sp)                           # salva s1 (índice)
    sw      s2, 4(sp)                           # salva s2 (ponteiro in)
    sw      s3, 0(sp)                           # salva s3 (ponteiro out) 

    mv s2, a0                                   # s2(ponteiro) = a0 (in)
    mv s3, a2                                   # s3(ponteiro) = a2 (out)
    li s0, 1                                    # s0(resultado) = 1 -> assume sucesso

    li t5, 1
    blt a1, t5, fail                            # if key < 1 -> fail
    li t2, 26
    bgt a1, t2, fail                            # if key > 26 -> fail

loop:
    li t0, 'A'                                  # t0 = A 
    li t1, 'Z'                                  # t1 = Z

    lbu t3, 0(s2)                               # lê o ponteiro s2 (in)
    beq t3, zero, end                           # se t3 == \0, termina
    blt t3, t0, fail                            # se t3 < A -> letra minúscula, falha
    bgt t3, t1, fail                            # se t3 > Z -> letra minúscula, falha

    sub t4, t3, a1                              # t4 = t3 - key(shift)
    blt t4, t0, adjust_range                    # se letra + key < A, ajustamos o intervalo
    j store

adjust_range:
    addi t4, t4, 26                             # se a letra + key < A, dá a volta ao alfabeto (26)

store:  
    sb t4, 0(s3)                                # guarda t4 em s3 (out)
    addi s2, s2, 1                              # incrementa s2
    addi s3, s3, 1                              # incrementa s3
    j loop

fail:
    li s0, 0                                    # s0(resultado) = 0
    sb zero, 0(s3) 
    j end

end:
    sb zero, 0(s3)                              # faz com que s3 termine em \0
    mv a0, s0                                   # a0 = s0(1 ou 0)
    lw      ra, 16(sp)                          # restaura os registradores
    lw      s0, 12(sp)
    lw      s1, 8(sp)
    lw      s2, 4(sp)
    lw      s3, 0(sp)
    addi    sp, sp, 20                          # liberta a stack
    ret