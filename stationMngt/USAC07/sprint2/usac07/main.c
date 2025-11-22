#include <stdio.h>
#include "asm.h"

int main(void){
    int buffer[10] ={1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    int length = 10;
    int nelem = 2;
    int tail = 0;
    int head = 2;
    int n = 3;
    int array[5];

    int res = move_n_to_array(buffer, length, &nelem, &tail, &head, n, array);

    printf("This function returns 1 in case of success, 0 otherwise: %d\n", res);

    if(res == 1){
        printf("Array:\n");
        for (int i = 0; i < n; i++) {
            printf("%d ", array[i]);
        }
        printf("\n");
    }else{
        printf("Function execution failed.");     
        printf("\n");
    }

    return 0;
}