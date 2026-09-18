#include <stdio.h>
#include "asm.h"

int main(void){

    //Scenario 1: Buffer Full, Head = Tail 
    int buffer[3] = {5, 10, 16};
    int length = 3;
    int nelem = 3;
    int tail = 0;
    int head = 0;
    int value = 1;
    

    //Scenario 2: Buffer Not Full, Head Ahead of Tail
    /*
    int buffer[3] = {0, 0, 0};
    int length = 3;
    int nelem = 0;
    int tail = 0;
    int head = 0;
    int value = 42;
    */

    //Scenario 3: Buffer Almost Full, Insert at End
    /*
    int buffer[3] = {7, 8, 0};
    int length = 3;
    int nelem = 2;
    int tail = 0;
    int head = 2;
    int value = 99;
    */


    //Scenario 4: Overwrite Oldest When Full, Tail Not Zero
    /*
    int buffer[3] = {11, 22, 33};
    int length = 3;
    int nelem = 3;
    int tail = 1;
    int head = 1;
    int value = 77;
    */

    int res = enqueue_value(buffer, length, &nelem, &tail, &head, value); 
    printf("This function returns 1 if after insertion the buffer is full, 0 otherwise: %d\n", res);

    printf("Buffer after insertion:\n");
    for (int i = 0; i < length; i++) {
        printf("%d ", buffer[i]);
    }
    
    printf("\n");
    printf("nelem = %d, tail = %d, head = %d\n", nelem, tail, head);

    return 0;
}
