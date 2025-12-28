.section .text             
.global find_token           


# It receives as input
# a0 = str (string)
# str -> represents the input data containing tokens and fields. 
# Example: "TEMP&unit:celsius&value:20#HUM&unit:percentage&value:80"

# a1 = token (string)
# token -> represents the identifier we want to search for in str. 
# Example: "TEMP" or "HUM"

# It receives as output
# a2 = unit (string pointer)
# unit -> represents a pointer to a string where the extracted unit will be stored.
# Example: "celsius" or "percentage"

# a3 = value (integer pointer)
# value -> represents a pointer to an integer where the extracted value will be stored.
# Example: 20 or 80

# The function must extract the unit and value fields corresponding to the given token.
# return 1: if it succeeds
# return 0: if it fails -> and it should set unit = "" and *value = 0

find_token:
  mv t6, a0                                   # guarda o início da string (base do input) para verificar fronteira
  mv t0, a0                                   # inicializa ponteiro de varrimento do input

search_loop:
  lb t1, 0(t0)                                # carrega o byte apontado por t0
  beq t1, zero, not_found                     # se for fim da string (NULL), não encontrou o token

  # Verificação de fronteira do campo:
  # Só tentamos comparar se estamos no início da string OU se o caracter anterior é '#'
  beq t0, t6, try_match                       # se t0 é o início do input, tentar match
  lb t4, -1(t0)                               # lê o caracter anterior
  li t5, '#'                                  # carrega literal '#'
  bne t4, t5, next_char                       # se anterior != '#', não é início de campo → avançar

try_match:
  mv t2, t0                                   # guarda posição atual do input em t2
  mv t3, a1                                   # guarda ponteiro do token em t3
cmp_loop:
  lb t4, 0(t3)                                # carrega próximo caracter do token
  beq t4, zero, find_check_amp                # fim do token → verificar se há '&' a seguir
  lb t5, 0(t2)                                # carrega próximo caracter do input
  beq t5, zero, not_found                     # se fim do input, não encontrou
  bne t4, t5, next_char                       # se caracteres diferentes, avança input
  addi t2, t2, 1                              # avança ponteiro do input
  addi t3, t3, 1                              # avança ponteiro do token
  j cmp_loop                                  # repete comparação

find_check_amp:
  lb t1, 0(t2)                                # lê caracter logo após token
  li t4, '&'                                  # carrega literal '&'
  bne t1, t4, next_char                       # se não for '&', não é match
  mv a0, t2                                   # devolve ponteiro para '&' (após token completo)
  ret                                         # retorna

next_char:
  addi t0, t0, 1                              # avança posição inicial do input
  j search_loop                               # volta ao início da procura

not_found:
  li a0, 0                                    # coloca NULL em a0 (não encontrado)
  ret                                         # retorna

.global to_num     

to_num:
  li t0, 0                                    # inicializa acumulador a 0

parse_digits:
  lb t1, 0(a0)                                # lê próximo caractere da string
  beq t1, zero, end_parse                     # se fim da string, termina
  li t2, '#'               
  beq t1, t2, end_parse                       # se encontrou '#', termina
  li t3, '0'               
  blt t1, t3, end_parse                       # se menor que '0', termina
  li t3, '9'                
  bgt t1, t3, end_parse                       # se maior que '9', termina
  addi t1, t1, -48                            # converte ASCII para número (subtrai '0')
  li t2, 10                
  mul t0, t0, t2                              # multiplica acumulador por 10
  add t0, t0, t1                              # adiciona dígito convertido
  addi a0, a0, 1                              # avança ponteiro da string
  j parse_digits                              # repete ciclo

end_parse:
  mv a0, t0                                   # coloca resultado em a0
  ret                                         # retorna


.global extract_data 

extract_data:
  addi sp, sp, -16                            # reserva espaço na stack
  sw ra, 12(sp)                               # guarda endereço de retorno

  # chamar find_token(input, token)
  call find_token                             # chama função find_token
  beq a0, zero, fail                          # se não encontrou, falha

  # verificar literal "&unit:"
  mv t0, a0                                   # ponteiro atual
  li t1, '&'                                  # t1 = &
  lb t2, 0(t0)                                # lê caracter
  bne t1, t2, fail                            # se não for '&', falha
  addi t0, t0, 1                              # avança

  li t1, 'u'                                  # t1 = u        
  lb t2, 0(t0)                                # lê caracter
  bne t1, t2, fail                            # se não for 'u', falha
  addi t0, t0, 1                              # avança

  li t1, 'n'                                  # t1 = n          
  lb t2, 0(t0)                                # lê caracter
  bne t1, t2, fail                            # se não for 'n', falha
  addi t0, t0, 1                              # avança

  li t1, 'i'                                  # t1 = i         
  lb t2, 0(t0)                                # lê caracter
  bne t1, t2, fail                            # se não for 'i', falha
  addi t0, t0, 1                              # avança

  li t1, 't'                                  # t1 = t          
  lb t2, 0(t0)                                # lê caracter
  bne t1, t2, fail                            # se não for 't', falha
  addi t0, t0, 1                              # avança

  li t1, ':'                                  # t1 = :          
  lb t2, 0(t0)                                # lê caracter
  bne t1, t2, fail                            # se não for ':', falha
  addi t0, t0, 1                              # avança

  # copiar unit até '&'
  mv t3, a2                                   # destino unit

copy_unit:
  lb t4, 0(t0)                                # lê caractere
  beq t4, zero, fail                          # se fim de string, falha

  li t5, '&'                                  # t5 = &            
  beq t4, t5, end_unit                        # se encontrou '&', termina cópia
  sb t4, 0(t3)                                # guarda caractere em unit
  addi t3, t3, 1                              # avança destino
  addi t0, t0, 1                              # avança origem
  j copy_unit                                 # repete

end_unit:
  sb zero, 0(t3)                              # termina string unit
  addi t0, t0, 1                              # avança origem

  # verificar literal "value:"
  li t1, 'v'                                  # t1 = v         
  lb t2, 0(t0)                                # lê caractere
  bne t1, t2, fail                            # se não for 'v', falha
  addi t0, t0, 1                              # avança

  li t1, 'a'                                  # t1 = a           
  lb t2, 0(t0)                                # lê caractere
  bne t1, t2, fail                            # se não for 'a', falha
  addi t0, t0, 1                              # avança

  li t1, 'l'                                  # t1 = l        
  lb t2, 0(t0)                                # lê caractere
  bne t1, t2, fail                            # se não for 'l', falha
  addi t0, t0, 1                              # avança

  li t1, 'u'                                  # t1 = u          
  lb t2, 0(t0)                                # lê caractere
  bne t1, t2, fail                            # se não for 'u', falha
  addi t0, t0, 1                              # avança

  li t1, 'e'                                  # t1 = e        
  lb t2, 0(t0)                                # lê caractere
  bne t1, t2, fail                            # se não for 'e', falha
  addi t0, t0, 1                              # avança

  li t1, ':'                                  # t1 = :
  lb t2, 0(t0)                                # lê caractere
  bne t1, t2, fail                            # se não for ':', falha
  addi t0, t0, 1                              # avança

  # converter número
  mv a0, t0                                   # passa ponteiro para to_num
  call to_num                                 # chama função to_num
  sw a0, 0(a3)                                # guarda resultado em *value
  li a0, 1                                    # retorna sucesso
  j end                                       # salta para fim

fail:
  sb zero, 0(a2)                              # unit = "" (string vazia)
  sw zero, 0(a3)                              # *value = 0
  li a0, 0                                    # retorna falha

end:
  lw ra, 12(sp)                               # restaura endereço de retorno
  addi sp, sp, 16                             # liberta espaço da stack
  ret                                         # retorna

# ============================================================================
# validate_sensor_data - Assembly function that takes a struct as parameter
# 
# This function fulfills the Sprint 3 requirement: "implement at least one
# function using assembly, in which at least one of its parameters is a struct"
#
# Input:
#   a0 = SensorData* sensor (pointer to struct)
#
# Struct layout (SensorData):
#   offset 0:  int temperature
#   offset 4:  int humidity
#   offset 8:  char temp_unit[20]
#   offset 28: char hum_unit[20]
#
# Validation rules:
#   - Temperature must be between -50 and 50 (Celsius)
#   - Humidity must be between 0 and 100 (percentage)
#
# Returns:
#   a0 = 1 if valid, 0 if invalid
# ============================================================================

.global validate_sensor_data

validate_sensor_data:
  # Check if pointer is NULL
  beq a0, zero, invalid                      # if sensor == NULL, return 0
  
  # Load temperature (offset 0)
  lw t0, 0(a0)                                # t0 = sensor->temperature
  
  # Check temperature range: -50 <= temp <= 50
  li t1, -50                                  # t1 = -50 (lower bound)
  blt t0, t1, invalid                         # if temp < -50, invalid
  li t1, 50                                   # t1 = 50 (upper bound)
  bgt t0, t1, invalid                         # if temp > 50, invalid
  
  # Load humidity (offset 4)
  lw t0, 4(a0)                                # t0 = sensor->humidity
  
  # Check humidity range: 0 <= hum <= 100
  li t1, 0                                    # t1 = 0 (lower bound)
  blt t0, t1, invalid                         # if hum < 0, invalid
  li t1, 100                                  # t1 = 100 (upper bound)
  bgt t0, t1, invalid                         # if hum > 100, invalid
  
  # All checks passed
  li a0, 1                                    # return 1 (valid)
  ret                                         # return
  
invalid:
  li a0, 0                                    # return 0 (invalid)
  ret                                         # return

