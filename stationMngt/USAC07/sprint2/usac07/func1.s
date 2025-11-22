.section .text
.global move_n_to_array


# A circular buffer is an array of constant length
# It is required to know the length of the array as well as the number of elements in it.

# It is used to store data in a continuous loop
# It is also known as a ring buffer because it stores the data circularly. 

# The circular buffer has two pointers:
# one for the head of the buffer and
# another for the tail

# The head pointer -> points to the location where we will insert the next element
# The tail pointer -> points to the location of the oldest element in the buffer

# Return 1: if it succeeds
# Return 0: (if there is no n elements to move)

# a0 = buffer (array)
# a1 = length (capacidade do array)
# a2 = *nelem (quantidade de elementos atuais)
# a3 = *tail (indica onde está o elemento mais antigo no buffer)
# a4 = *head (indica onde o próximo elemento será inserido)
# a5 = n (os n mais velhos elementos de buffer)
# a6 = array (array que recebe os mais velhos elementos de buffer) 

move_n_to_array:
    lw t0, 0(a2)                                                                  # load a2 (nelem) 
    lw t1, 0(a3)                                                                  # load a3 (tail)
    mv t2, a6                                                                     # t2 = a6 (array)

    blt t0, a5, fail                                                              # se nelem < n -> fail

    mv t3, a5                                                                     # t3 = contador (n)

loop_n:
    li t4, 4                                                                      # t4 = 4
    mul t4, t1, t4                                                                # t4(deslocamento) = tail *4
    add t4, a0, t4                                                                # t4 = buffer + deslocamento
    lw t5, 0(t4)                                                                  # load buffer[tail]

    sw t5, 0(t2)                                                                  # guarda valor do buffer em t2 (array)

    addi t2, t2, 4                                                                # avança ponteiro array
    addi t1, t1, 1                                                                # tail++
    rem t1, t1, a1                                                                # garante que o ponteiro nunca ultrapassa os limites do array
    sw t1, 0(a3)                                                                  # guarda tail atualizado

    addi t0, t0, -1                                                               # nelem = nelem - 1
    sw t0, 0(a2)                                                                  # guarda nelem atualizado

    addi t3, t3, -1                                                               # contador n--
    bnez t3, loop_n                                                               # se n != 0 -> loop_n

    li a0, 1                                                                      # retorna 1
    ret   

fail:
    li a0, 0                                                                      # retorna 0
    ret


 