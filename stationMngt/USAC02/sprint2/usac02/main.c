#include <stdio.h>
#include "asm.h"

int main(void){
    char in[10] = "HJHIVB";
    int key = 7;
    char out[10];

    int result = decrypt_data(in, key, out);
    printf("This function returns 1 in case of success, 0 otherwise: %d\n", result); 

    if(result == 1){
        printf("Decrypt: %s\n", out);
    }else{
        printf("Decrypt failed.\n");
    }
    return 0;
}