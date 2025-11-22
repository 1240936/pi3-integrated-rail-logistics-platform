.section .text
.global dequeue_value


# This function removes the oldest element (tail) from the buffer
# The output is the tail in the value integer pointer

# The head pointer -> points to the location where we will insert the next element
# The tail pointer -> points to the location of the oldest element in the buffer

# Return 1: in case of success
# Return 0: otherwise

# a0 = buffer (array)
# a1 = length (capacidade do array)
# a2 = *nelem (quantidade de elementos atuais)
# a3 = *tail (indica onde está o elemento mais antigo no buffer)
# a4 = *head (indica onde o próximo elemento será inserido)
# a5 = value (valor a ser inserido no array)


dequeue_value:  
    lw t0, 0(a2)                                                        # load a2 (nelem)

    beq t0, zero, fail                                                  # se nelem == 0, signifca que não há nada para remover

remove_oldest:
    lw t1, 0(a3)                                                        # load a3 (tail)
    li t2, 4                                                            # t2 = 4

    mul t3, t1, t2                                                      # t3(deslocamento) =  tail *4
    add t3, a0, t3                                                      # t3 = buffer + deslocamento                                   

    lw t4, 0(t3)                                                        # load buffer[tail] 
    sw t4, 0(a5)                                                        # guarda t4 (elemento mais antigo) em value

    addi t1, t1, 1                                                      # avança o ponteiro tail para descartar o mais antigo
    rem t1, t1, a1                                                      # garante que o ponteiro nunca ultrapassa os limites do array
    sw t1, 0(a3)                                                        # guarda tail atualizado

    addi t0, t0, -1                                                     # nelem = nelem - 1
    sw t0, 0(a2)                                                        # guarda nelem atualizado

    li a0, 1                                                            # retorna 1
    ret

fail:
    li a0, 0                                                            # retorna 0
    ret