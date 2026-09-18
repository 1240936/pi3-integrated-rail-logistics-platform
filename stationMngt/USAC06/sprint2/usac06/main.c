#include <stdio.h>
#include "asm.h"

int main(void){
    int buffer[3] = {0, 0, 0};
    int length = 3;
    int nelem = 0;
    int tail = 0;
    int head = 0;
    int value;

    int res = dequeue_value(buffer, length, &nelem, &tail, &head, &value);
    printf("This function return 1 in case of successs, 0 otherwise: %d\n", res);

    if (res == 1) {
    printf("value = %d\n", value);
    } else {
    printf("No value removed.\n");
    }


    printf("Buffer state: ");
    for (int i = 0; i < length; i++) {
        printf("%d\n ", buffer[i]);
    }
    printf("nelem = %d, tail = %d, head = %d\n", nelem, tail, head);

}