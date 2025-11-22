.section .text
.global median

# sorts the array
# save the median value in the me parameter

# Return 1: if it succeeds
# Return 0: otherwise

# a0 = vec
# a1 = length
# a2 = me

median:
    addi    sp, sp, -16                                     # reserva espaço na stack
    sw      ra, 12(sp)                                      # guarda ra
    sw      s0, 8(sp)                                       # guarda s0 (resultado)
    sw      s1, 4(sp)                                       # guarda s1 (contador externo)
    sw      s2, 0(sp)                                       # guarda s2 (ponteiro base)

    mv      s2, a0                                          # s2 = vec

                                                                         
    bge     zero, a1, fail                                  # length <= 0 -> fail

    addi    s1, a1, -1                                      # s1 = length - 1 (passes do bubble)

outer_loop:
    bge     zero, s1, calculate_median                      # se s1 <= 0 -> calcular mediana

    mv      t2, s1                                          # t2 = contador interno
    mv      t3, a0                                          # t3 = ponteiro array (início)

inner_loop:
    # verifica se já foram percorridos os elementos no loop interno. Se sim, avança para o próximo loop externo.
    bge     zero, t2, next_outer                            # se t2 <= 0 -> loop externo

    lw      t4, 0(t3)                                       # t4 = arr[j]
    lw      t5, 4(t3)                                       # t5 = arr[j+1]


    bge     t5, t4, no_swap                                 # se t4 <= t5 -> não troca

    # troca arr[j] <-> arr[j+1]
    sw      t5, 0(t3)
    sw      t4, 4(t3)

no_swap:
    addi    t3, t3, 4                                       # avança ponteiro
    addi    t2, t2, -1                                      # decrementa interno
    j       inner_loop

next_outer:
    addi    s1, s1, -1                                      # decrementa passes
    j       outer_loop

calculate_median:
    li      t4, 2
    rem     t0, a1, t4                                      # t0 = length % 2
    beq     t0, zero, even_case                             # se par -> even_case

odd_case:
    # mediana = vec[length/2]
    div     t1, a1, t4                                      # t1 = length/2
    slli    t1, t1, 2                                       # deslocamento = (length/2)*4
    add     t2, a0, t1                                      # t2 = &vec[length/2]
    lw      t3, 0(t2)                                       # t3 = vec[length/2]
    sw      t3, 0(a2)                                       # *me = t3

    li      s0, 1
    mv      a0, s0
    j       end

even_case:
    # mediana = (vec[len/2 - 1] + vec[len/2]) / 2, arredondado para baixo
    div     t1, a1, t4                                      # t1 = length/2
    slli    t1, t1, 2                                       # deslocamento = (length/2)*4

    add     t2, a0, t1                                      # t2 = &vec[length/2]
    lw      t3, 0(t2)                                       # t3 = vec[length/2]

    addi    t2, t2, -4                                      # t2 = &vec[length/2 - 1]
    lw      t4, 0(t2)                                       # t4 = vec[length/2 - 1]

    add     t5, t3, t4                                      # t5 = soma
    li      t6, 2
    div     t0, t5, t6                                      # t0 = quociente sem decimal
    rem     t1, t5, t6                                      # t1 = resto

    beq     t1, zero, no_adjust                             # resto != 0 -> ajusta para baixo
    bge     t5, zero, no_adjust                             # se soma < 0 -> ajusta para baixo
    addi    t0, t0, -1

no_adjust:
    sw      t0, 0(a2)                                       # *me = mediana

    li      s0, 1
    mv      a0, s0
    j       end


fail:
    li      s0, 0
    mv      a0, s0
    j       end

end:
    lw      s2, 0(sp)                                       # restaura registradores
    lw      s1, 4(sp)
    lw      s0, 8(sp)
    lw      ra, 12(sp)
    addi    sp, sp, 16                                      # liberta espaço da stack
    ret