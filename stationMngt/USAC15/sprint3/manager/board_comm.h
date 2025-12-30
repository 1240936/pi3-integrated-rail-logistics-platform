#ifndef BOARD_COMM_H
#define BOARD_COMM_H

/**
 * USAC15 - Interface de Comunicação com a Board
 * Este header define as funções base para o envio de dados estruturados.
 */

// 1. Definição do Protocolo (Requisito da tua US)
#define STX 0x02  // Header: Start of Text
#define LF  '\n'  // Fim do pacote

// 2. Protótipo da função de cálculo de integridade
// Essencial para garantir que a Board rejeita dados corrompidos.
char calculate_checksum(const char* str);

// 3. Protótipo da função principal de envio
// Esta função é thread-safe e usa write() de baixo nível.
int send_to_board(const char* formatted_data);

#endif