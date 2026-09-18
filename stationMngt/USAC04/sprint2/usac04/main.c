#include <stdio.h>
#include "asm.h"   

int main() {
    int value;
    char str[20];       // buffer para a string
    char cmd[20];
    int res;

    // Caso 1
    str[0] = ' ';
    str[1] = 'r';
    str[2] = 'B';
    str[3] = ' ';
    str[4] = '\0';
    value = 5;
    res = format_command(str, value, cmd);
    printf("%d: %s\n", res, cmd); // esperado: 1: RB,05

    // Caso 2
    str[0] = ' ';
    str[1] = 'Y';
    str[2] = 'e';
    str[3] = ' ';
    str[4] = '\0';
    value = 25;
    res = format_command(str, value, cmd);
    printf("%d: %s\n", res, cmd); // esperado: 1: YE,25

    // Caso 3
    str[0] = ' ';
    str[1] = 'Y';
    str[2] = 'e';
    str[3] = ' ';
    str[4] = '\0';
    value = 125;
    res = format_command(str, value, cmd);
    printf("%d: %s\n", res, cmd); // esperado: 0:

    // Caso 4
    str[0] = ' ';
    str[1] = 'a';
    str[2] = 'a';
    str[3] = 'a';
    str[4] = ' ';
    str[5] = '\0';
    value = 25;
    res = format_command(str, value, cmd);
    printf("%d: %s\n", res, cmd); // esperado: 0:

    // Caso 5
    str[0] = ' ';
    str[1] = 'g';
    str[2] = 'T';
    str[3] = 'h';
    str[4] = ' ';
    str[5] = '\0';
    // value mantém 25 do caso anterior
    res = format_command(str, value, cmd);
    printf("%d: %s\n", res, cmd); // esperado: 1:GTH

    return 0;
}