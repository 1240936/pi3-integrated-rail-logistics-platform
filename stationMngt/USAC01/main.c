#include <stdio.h>
#include "asm.h"

int main(void){
    char in[10] = "WYWXKQ";
    int key = 29;
    char out[10];

    int result = encrypt_data(in, key, out);


    printf("Return 1 in case of success, 0 otherwise: %d\n", result);
    if (result == 1) {
        printf("Encrypted: %s\n", out);
    } else {
        printf("Encryption failed.\n");
    }

    return 0;
}