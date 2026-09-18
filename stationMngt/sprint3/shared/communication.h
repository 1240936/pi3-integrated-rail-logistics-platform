// sprint3/shared/communication.h
#ifndef COMMUNICATION_H
#define COMMUNICATION_H

#define STX 0x02  // Start of Text
#define LF  '\n'  // Line Feed (End of Packet)

typedef struct {
    char header;
    char payload[128];
    char checksum;
    char end;
} Packet;

// Protótipos da tua US
int send_to_board(const char* data);
char calculate_checksum(const char* str);

#endif