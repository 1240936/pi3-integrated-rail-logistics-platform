.section .text
.globl format_command

# Entradas:
# a0 = op (string)
# op -> representa o comando de entrada que deve ser processado.
# Pode conter espaços no início/fim e letras minúsculas, como " rB ", " Ye ", " gTh ".

# a1 = n (inteiro)
# n -> representa o valor associado ao comando.
# Para comandos de 2 letras (RE, RB, YE, GE), deve estar no intervalo [0, 99].
# Para o comando de 3 letras (GTH), o valor é ignorado.

# Saída:
# a2 = cmd (string)
# cmd -> representa o buffer onde será escrita a versão formatada do comando,
# por exemplo "RB,05", "YE,25", "GTH".

# Retorno:
# a0 = inteiro
# 1 -> sucesso (cmd contém um comando formatado válido)
# 0 -> falha (cmd fica vazio, a entrada é inválida ou o valor está fora dos limites)



format_command:
    # guarda os pointers das entradas e os seus valores em temporários
    mv t0, a0                                                       # guarda ponteiro para string de entrada (op)
    mv t1, a1                                                       # guarda valor inteiro n
    mv t2, a2                                                       # guarda ponteiro para buffer de saída (cmd)

    # ----- ignorar o espaço inicial -----
skip_lead:
    lb t3, 0(t0)                                                    # carrega caracter atual
    beq t3, zero, fail_zero_len                                     # se for NULL, falha
    li t4, 32                                                       # espaço em ASCII
    beq t3, t4, lead_inc                                            # se for espaço incrementa, senão continua

    li t4, 9                                                        # tab em ASCII
    beq t3, t4, lead_inc                                            # se for tab incrementa, senão continua
    li t4, 10                                                       # \n em ASCII
    beq t3, t4, lead_inc                                            # se for \n incrementa, senão continua
    li t4, 13                                                       # \r em ASCII
    beq t3, t4, lead_inc                                            # se for \r incrementa, senão continua
    j found_start

lead_inc:
    addi t0, t0, 1                                                  # avança ponteiro
    j skip_lead

# ----- encontrar o primeiro caracter -----
found_start:
    mv t4, t0                                                       # ponteiro de procura

find_end:
    lb t3, 0(t4)                                                    # lê caracter
    beq t3, zero, found_end                                         # se for NULL, encontra o fim
    addi t4, t4, 1                                                  # avança
    j find_end

found_end:
    addi t4, t4, -1                                                 # aponta para último caracter válido

# ----- limpar o espaço final -----
trim_trail:
    blt t4, t0, fail_zero_len                                       # se NULL, falha

    lb t3, 0(t4)                                                    # carrega caracter atual  

    li t5, 32                                                       # espaço em ASCII
    beq t3, t5, dec_trail                                           # se for espaço dec_trail, senão continua

    li t5, 9                                                        # tab em ASCII
    beq t3, t5, dec_trail                                           # se for tab dec_trail, senão continua

    li t5, 10                                                       # \n em ASCII
    beq t3, t5, dec_trail                                           # se for \n dec_trail, senão continua

    li t5, 13                                                       # \r em ASCII
    beq t3, t5, dec_trail                                           # se for \r dec_trail, senão continua
    j compute_length

dec_trail:
    addi t4, t4, -1                                                 # ponteiro vai para o último caracter
    j trim_trail

# ----- calcular o tamanho da string -----
compute_length:
    mv a3, t4                                                       # a3 = t4 (ponteiro do último caracter)
    sub a3, a3, t0                                                  # número de caracteres
    addi a3, a3, 1                                                  # a3 = tamanho da string

    li t5, 3                                                        # t5 = 3
    blt t5, a3, fail_len_too_long                                   # falha se o tamanho da string >3

# ----- ler até 3 caracteres -----
    li t3, 0
    li t4, 0
    li t5, 0
    lb t3, 0(t0)                                                    # carrega o 1º caracter da string em t3 (este existe sempre)
    li t6, 2                                                        # t6 = 2 (limite para verificar se há >= 2 caracteres)
    blt a3, t6, skip_load1                                          # se tamanho < 2, não existe 2º caractere → salta
    lb t4, 1(t0)                                                    # carrega o 2º caracter em t4
skip_load1:
    li t6, 3                                                        # t6 = 3 (limite para verificar se há >= 3 caracteres)
    blt a3, t6, skip_load2                                          # se tamanho < 3, não existe 3º caractere → salta
    lb t5, 2(t0)                                                    # carrega o 3º caracter em t5
skip_load2:

# ----- converter em minúsculas -----
# char0
        li t6, 'a'                                                  # limite inferior das letras minúsculas
    blt t3, t6, not_up0                                             # se t3 < 'a', não é minúscula → não altera
    li t6, 'z'                                                      # limite superior das letras minúsculas
    bgt t3, t6, not_up0                                             # se t3 > 'z', não é minúscula → não altera
    addi t3, t3, -32                                                # converte minúscula para maiúscula subtraindo 32
not_up0:
# char1
    beq t4, zero, skip_up1                                          # se t4=0, não existe 2º char → ignora
    li t6, 'a'
    blt t4, t6, not_up1                                             # se não está entre 'a' e 'z', não converte
    li t6, 'z'
    bgt t4, t6, not_up1
    addi t4, t4, -32                                                # converte minúscula para maiúscula
not_up1:
skip_up1:
# char2
    beq t5, zero, skip_up2                                          # se t5=0, não existe 3º char → ignora
    li t6, 'a'
    blt t5, t6, not_up2                                             # se não está entre 'a' e 'z', não converte
    li t6, 'z'
    bgt t5, t6, not_up2
    addi t5, t5, -32                                                # converte minúscula para maiúscula
not_up2:
skip_up2:

# ----- verificar comando com base no comprimento -----
li t6, 2
beq a3, t6, check_len2                                              # se o comprimento do comando == 2, vai para check_len2
li t6, 3
beq a3, t6, check_len3                                              # se o comprimento do comando == 3, vai para check_len3
j fail_bad_cmd                                                      # caso contrário, comprimento inválido → falha

# ----- tratar comandos de 2 caracteres (RE, RB, YE, GE) -----
check_len2:
li t6, 'R'
beq t3, t6, chk_R                                                   # se o primeiro caracter for 'R', verifica RE ou RB
li t6, 'Y'
beq t3, t6, chk_Y                                                   # se o primeiro caracter for 'Y', verifica YE
li t6, 'G'
beq t3, t6, chk_G                                                   # se o primeiro caracter for 'G', verifica GE
j fail_bad_cmd                                                      # caso contrário, comando de 2 chars inválido

# ----- verificar comandos que começam com 'R' -----
chk_R:
li t6, 'E'
beq t4, t6, matched_len2                                            # se o segundo caractere for 'E', é RE → válido
li t6, 'B'
beq t4, t6, matched_len2                                            # se o segundo caractere for 'B', é RB → válido
j fail_bad_cmd                                                      # senão, comando inválido

# ----- verificar comandos que começam com 'Y' -----
chk_Y:
li t6, 'E'
beq t4, t6, matched_len2                                            # se o segundo caractere for 'E', é YE → válido
j fail_bad_cmd                                                      # senão, comando inválido

# ----- verificar comandos que começam com 'G' -----
chk_G:
li t6, 'E'
beq t4, t6, matched_len2                                            # se o segundo caractere for 'E', é GE → válido
j fail_bad_cmd                                                      # senão, comando inválido

# ----- comando de 2 caracteres válido encontrado -----
matched_len2:
    # garantir que 0 <= n <= 99
    li t6, 0
    blt t1, t6, fail_bad_n                                          # falha se n < 0
    li t6, 100
    bge t1, t6, fail_bad_n                                          # falha se n >= 100

# ----- converter n em dezenas e unidades (sem usar div/rem) -----
    li t6, 0                                                        # contador de dezenas
    mv t0, t1                                                       # copia n para t0 (resto)
div_loop:
    li t1, 10
    blt t0, t1, div_done                                            # se o resto < 10, terminou
    addi t0, t0, -10                                                # subtrai 10 ao resto
    addi t6, t6, 1                                                  # incrementa dezenas
    j div_loop
div_done:
    mv t1, t0                                                       # o resto agora é a unidade

    addi t6, t6, '0'                                                # converte dezenas para ASCII
    addi t1, t1, '0'                                                # converte unidades para ASCII

# ----- escrever a string de saída em cmd -----
    sb t3, 0(t2)                                                    # primeira letra do comando
    sb t4, 1(t2)                                                    # segunda letra do comando
    li t0, ','                                                      # vírgula separadora
    sb t0, 2(t2)
    sb t6, 3(t2)                                                    # dígito das dezenas
    sb t1, 4(t2)                                                    # dígito das unidades
    sb zero, 5(t2)                                                  # terminador NULL
    li a0, 1                                                        # devolver 1 → sucesso
    ret

# ----- tratar comando de 3 caracteres GTH -----
check_len3:
li t6, 'G'
bne t3, t6, fail_bad_cmd                                            # primeiro caractere tem de ser 'G'
li t6, 'T'
bne t4, t6, fail_bad_cmd                                            # segundo caractere tem de ser 'T'
li t6, 'H'
bne t5, t6, fail_bad_cmd                                            # terceiro caractere tem de ser 'H'

# guardar "GTH" no output
sb t3, 0(t2)
sb t4, 1(t2)
sb t5, 2(t2)
sb zero, 3(t2)                                                      # terminar com NULL
li a0, 1                                                            # devolver 1 → sucesso
ret

# ----- fail labels -----
fail_bad_cmd:                                                       # devolver 0 → falha
li a0, 0
ret
fail_len_too_long:
li a0, 0
ret
fail_bad_n:
li a0, 0
ret
fail_zero_len:
li a0, 0
ret