.section .text
.global sort_array

# This function sorts the array 
# taking into account the order parameter
# 1: for ascending 
# 0: for descending

# Return 1: if it succeeds
# Return 0: otherwise (if the length is less or equal to zero)

# a0 = vec
# a1 = length
# a2 = order

sort_array:
    addi sp, sp, -16                                    # reserva espaço na stack (16 bytes)
    sw ra, 12(sp)                                       # guarda endereço de retorno
    sw s0, 8(sp)                                        # s0 = resultado
    sw s1, 4(sp)                                        # s1 = índice i
    sw s2, 0(sp)                                        # s2 = ponteiro

    blez a1, fail                                       # length <= 0  -> fail

    li t0, 1                                            # t0 = 1
    beq a2, t0, ascending                               # order == 1 -> ascending                                         
    beq a2, zero, descending                            # order == 0 -> descending
    j fail                                              # qualquer outro valor -> falha

ascending:
    addi s1, a1, -1                                     # inicializa o contador externo = length-1

outer_loop_asc:
    blez s1, done                                       # se s1(contador externo) <= 0, termina ordenação
    mv t2, s1                                           # inicializa contador interno com s1 (=t2)
    mv s2, a0                                           # s2 aponta para o início do vec 

inner_loop_asc:
    blez t2, next_outer_asc                             # se contador interno <= 0 -> next_outer_asc
    lw t4, 0(s2)                                        # load arr[j]
    lw t5, 4(s2)                                        # load arr[j+1] (vec + deslocamento (t4 *4))

    ble t4, t5, no_swap_asc                             # se arr[j] <= arr[j+1] -> no_swap_asc

    # troca arr[j] com arr[j+1]
    sw t5, 0(s2)                                        
    sw t4, 4(s2)

no_swap_asc:
    addi s2, s2, 4                                      # avança ponteiro para próximo elemento
    addi t2, t2, -1                                     # decrementa contador interno
    j inner_loop_asc                                    # repete até fim do loop interno

next_outer_asc:
    addi s1, s1, -1                                     # decrementa contador externo
    j outer_loop_asc                                    # repete até fim do loop externo



descending:
    addi s1, a1, -1                                     # inicializa o contador externo = length-1

outer_loop_desc:
    blez s1, done                                       # se s1(contador externo) <= 0, termina ordenação
    mv t2, s1                                           # inicializa contador interno com s1 (=t2)
    mv s2, a0                                           # s2 aponta para o início do vec 

inner_loop_desc:
    blez t2, next_outer_desc                            # se contador interno <= 0 -> next_outer_desc
    lw t4, 0(s2)                                        # load arr[j]
    lw t5, 4(s2)                                        # load arr[j+1] (vec + deslocamento (t4 *4))

    bge t4, t5, no_swap_desc                            # se arr[j] >= arr[j+1] -> no_swap_desc

    # troca arr[j] com arr[j+1]
    sw t5, 0(s2)                                        
    sw t4, 4(s2)

no_swap_desc:
    addi s2, s2, 4                                      # avança ponteiro para próximo elemento
    addi t2, t2, -1                                     # decrementa contador interno
    j inner_loop_desc                                   # repete até fim do loop interno

next_outer_desc:
    addi s1, s1, -1                                     # decrementa contador externo
    j outer_loop_desc                                   # repete até fim do loop externo

done:
    li s0, 1                                            # s0 = 1
    mv a0, s0                                           # a0 = s0 -> retorna 1
    j end

fail:
    li s0, 0                                            # s0 = 1
    mv a0, s0                                           # a0 = s0 -> retorna 0
    j end

end:
    lw s2, 0(sp)                                       # restaura os registradores
    lw s1, 4(sp)          
    lw s0, 8(sp)                                                      
    lw ra, 12(sp)                                       
    addi sp, sp, 16                                    # liberta espaço na stack (16 bytes) 
    ret