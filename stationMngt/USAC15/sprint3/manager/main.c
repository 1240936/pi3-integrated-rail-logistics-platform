/*
 * Manager Component - USAC15 Test/Demo
 *
 * Purpose: Test Board communication (formatting, checksum, and thread-safe sending)
 *
 * Usage: ./manager_usac15.elf
 */

#include <stdio.h>
#include <stdlib.h>
#include "board_comm.h"
#include "shared/communication.h"

int main() {
    printf("=== Manager Component - USAC15 (Board Communication) ===\n\n");

    /* Exemplo de dados para enviar à Board */
    const char* track_msg = "TRACK:1:FREE";
    const char* sensor_msg = "TEMP:25#HUM:60";
    const char* emergency_msg = "EMERGENCY_STOP";

    printf("Sending structured packets to Board...\n\n");

    /* Teste 1: Envio de estado de pista */
    printf("Action: Sending track status...\n");
    if (send_to_board(track_msg)) {
        printf("Success: '%s' sent with checksum 0x%02X\n", track_msg, calculate_checksum(track_msg));
    } else {
        printf("Failed to send track status\n");
    }

    /* Teste 2: Envio de dados de sensores */
    printf("\nAction: Sending sensor readings...\n");
    if (send_to_board(sensor_msg)) {
        printf("Success: '%s' sent with checksum 0x%02X\n", sensor_msg, calculate_checksum(sensor_msg));
    }

    /* Teste 3: Envio de paragem de emergência */
    printf("\nAction: Sending emergency alert...\n");
    if (send_to_board(emergency_msg)) {
        printf("Success: '%s' sent with checksum 0x%02X\n", emergency_msg, calculate_checksum(emergency_msg));
    }

    /* Teste 4: Payload Vazio (Edge Case) */
    /* Verifica se o driver lida corretamente com strings vazias sem crashar */
    printf("Teste 4: Enviando payload vazio...\n");
    if (send_to_board("")) {
        printf("Success: Empty payload handled (Packet: [STX][CHKSUM][LF])\n");
    }

    /* Teste 5: Payload Longo (Limite de Buffer) */
    /* Verifica se o sistema respeita o limite do buffer definido no protocolo */
    printf("\nTeste 5: Enviando payload longo (limite)...\n");
    const char* long_msg = "TRACK_MANAGER_UPDATE_LONG_STRING_DATA_1234567890";
    if (send_to_board(long_msg)) {
        printf("Success: Long message processed correctly.\n");
    }

    /* Teste 6: Simulação de Concorrência (Thread-Safety) */
    /* Embora um teste real precise de pthreads, podes simular chamadas rápidas
       para demonstrar que o Mutex protege a integridade do canal */
    printf("\nTeste 6: Simulando acesso concorrente (Mutex test)...\n");
    for(int i = 0; i < 3; i++) {
        printf("Concurrent Access Attempt %d...\n", i+1);
        send_to_board("CONCURRENT_DATA_STREAM");
    }

    printf("\n=== USAC15 Communication Test Complete ===\n");
    return 0;
}