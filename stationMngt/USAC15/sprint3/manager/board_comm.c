// sprint3/manager/board_comm.c
#include "../shared/communication.h"
#include <unistd.h>
#include <pthread.h>
#include <stdio.h>

// Mutex para garantir Thread-Safe (Requisito da tua US)
static pthread_mutex_t comm_mutex = PTHREAD_MUTEX_INITIALIZER;

// Implementação manual de funções de string (conforme notas do sistema)
char calculate_checksum(const char* str) {
    char cksum = 0;
    while (*str) {
        cksum ^= *str++;
    }
    return cksum;
}

/**
 * USAC15: Envia dados formatados para o componente Board.
 * Esta função é o ponto central de saída de dados do Manager.
 */
int send_to_board(const char* formatted_data) {
    if (formatted_data == NULL) return 0;

    pthread_mutex_lock(&comm_mutex); // Garante exclusão mútua

    // 1. Calcular Checksum do Payload
    char cksum = calculate_checksum(formatted_data);

    // 2. Formatar o pacote estruturado [STX][DATA][CHKSUM][LF]
    // Usamos o formato definido pela tua US para garantir integridade
    char packet_buffer[256];
    int packet_len = 0;

    packet_buffer[packet_len++] = STX;

    // Copia os dados manualmente (sem string.h)
    for (int i = 0; formatted_data[i] != '\0'; i++) {
        packet_buffer[packet_len++] = formatted_data[i];
    }

    packet_buffer[packet_len++] = cksum;
    packet_buffer[packet_len++] = LF;

    // 3. Enviar via sistema de baixo nível (write para o canal da Board)
    // No Linux, isto seria um Pipe ou Socket para o processo 'Board'
    // Aqui simulamos o envio para o descritor de ficheiro da Board (ex: STDOUT ou Pipe)
    ssize_t bytes_written = write(STDOUT_FILENO, packet_buffer, packet_len);

    pthread_mutex_unlock(&comm_mutex);

    return (bytes_written > 0) ? 1 : 0;
}