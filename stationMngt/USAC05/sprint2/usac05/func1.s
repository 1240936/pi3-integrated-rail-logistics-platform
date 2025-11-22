.section .text
.global enqueue_value

# This function inserts:
# The value into buffer

# A circular buffer is an array of constant length
# It is required to know the length of the array as well as the number of elements in it.

# It is used to store data in a continuous loop
# It is also known as a ring buffer because it stores the data circularly. 

# The circular buffer has two pointers:
# one for the head of the buffer and
# another for the tail

# The head pointer -> points to the location where we will insert the next element
# The tail pointer -> points to the location of the oldest element in the buffer

# If buffer = full -> oldest element should be removed to insert the new one
# Return 1: if after insertion the buffer is full,
# Return 0: otherwise

# a0 = buffer (array)
# a1 = length (capacidade do array)
# a2 = *nelem (quantidade de elementos atuais)
# a3 = *tail (indica onde está o elemento mais antigo no buffer)
# a4 = *head (indica onde o próximo elemento será inserido)
# a5 = value (valor a ser inserido no array)

enqueue_value:
    lw t0, 0(a2)                                                        # load a2 (nelem)

    beq t0, a1, buffer_is_full                                          # se a1 == a2 (capacidade do array == número de elementos) -> buffer_is_full

buffer_is_not_full:
    j insert_value

buffer_is_full:
    lw t1, 0(a3)                                                        # load a3 (tail)
    addi t1, t1, 1                                                      # avança o ponteiro tail para descartar o mais antigo
    rem t1, t1, a1                                                      # garante que o ponteiro nunca ultrapassa os limites do array
    sw t1, 0(a3)                                                        # guarda tail atualizado

    addi t0, t0, -1                                                     # nelem = nelem - 1
    sw t0, 0(a2)                                                        # guarda nelem atualizado

insert_value:
    li t3, 4                                                            # t3 = 4

    lw t4, 0(a4)                                                        # load a4 (head) 
    mul t5, t4, t3                                                      # t5 (deslocamento) = head *4
    add t6, a0, t5                                                      # t6 (endereço) =  buffer + deslocamento
    sw a5, 0(t6)                                                        # guarda o valor no endereço calculado

    addi t4, t4, 1                                                      # incrementa head
    rem t4, t4, a1                                                      # garante que o ponteiro nunca ultrapassa os limites do array
    sw t4, 0(a4)                                                        # guarda head atualizado

    lw   t0, 0(a2)                                                      # load a2 (nelem)
    addi t0, t0, 1                                                      # incrementa nelem
    sw   t0, 0(a2)                                                      # guarda nelem atualizado

    beq t0, a1, ret_full                                                # se nº de elementos == capacidade do array -> retorna buffer full

    li a0, 0                                                            # retorna 0
    ret

ret_full:
    li a0, 1                                                            # retorna 1
    ret