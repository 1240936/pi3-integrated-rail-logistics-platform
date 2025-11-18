.section .text
.global encrypt_data


# It receives as input
# a0 = in (string)
# in -> represents the input data that we are going to encrypt. ['A','Z']

# a1 = key (integer)
# key -> represents a shift amount, controls how the encryption works, [1, 26]

# It receives as output
# a2 = out (string)
# out -> represents a pointer to a string where the encrypted result will be stored.

# The function must encrypt the string in according to the Caesar Cipher
# return 1: if it succeeds
# return 0: if it not succeeds -> and it should set out as an empty string

# How Caesar Cipher works:
# Encryption:
# choose a shift value, in this case: the key
# replace each letter in the array in, with the letter that is key positions further down the alphabet


encrypt_data:
    addi sp, sp, -16                    # reserva espaço na stack (16 bytes)
    sw ra, 12(sp)                       # guarda o endereço de retorno
    sw s0, 8(sp)                        # guarda s0 (resultado)
    sw s1, 4(sp)                        # guarda s1 (índice i)
    sw s2, 0(sp)                        # guarda s2 (ponteiro)   

    mv s2, a0                           # s2 (ponteiro) = a0 (input pointer)
    mv s3, a2                           # s3 (ponteiro) = a2 (output pointer)
    addi s1, zero, 0                    # s1 (índice i) = 0    
    li s0, 1                            # s0 (resultado) = 1 -> assume sucesso

    li t0, 0
    blt a1, t0, fail                # if key < 0 -> fail
    li t1, 26
    bgt a1, t1, fail                # if key > 26 -> fail

loop:
    lb t4, 0(s2)                        # lê ponteiro s2 na posição atual
    beq t4, zero, end                   # se t4 (posição atua) = '\0', termina

    li t2, 'A'                          # t2 = A
    li t3, 'Z'                          # t3 = Z


    blt t4, t2, fail                    # se a0 < 'A' → não é maiúscula, logo falha
    bgt t4, t3, fail                    # se a0 > 'Z' → não é maiúscula, logo falha

    add t5, t4, a1                      # t5 = t4 + key(shift)
    ble t5, t3, store                   # se letra + key <= Z, guarda em s3 (a2)
    addi t5, t5, -26                    # se a letra + key > Z, dá a volta ao alfabeto (-26)

store:
    sb t5, 0(s3)                        # guarda t5 em s3 (a2)
    addi s2, s2, 1                      # incrementa s2
    addi s3, s3, 1                      # incrementa s3
    j loop

fail:
    li s0, 0                            # resultado = 0
    sb zero, 0(a2)                      # guarda o valor nulo em a2
    j end

end:
    sb zero, 0(s3)                      # escreve '\0', marca o fim da string
    mv a0, s0                           # a0 = s0 (1 ou 0)
    lw ra, 12(sp)                       # restaura os registradores
    lw s0, 8(sp)
    lw s1, 4(sp)
    lw s2, 0(sp)
    addi sp, sp, 16                     # libera os 16 bytes reservados na stack
    ret
