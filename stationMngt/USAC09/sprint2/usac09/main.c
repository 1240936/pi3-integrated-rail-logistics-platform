#include <stdio.h>
#include "asm.h"

int main(void){
    int vec[] = {};
    int length = 0;
    int me = 0;

    int res = median(vec, length, &me);

    if(res == 1){
        printf("The sorted array:\n");
        for(int i=0; i<length; i++){
            printf("%d ", vec[i]);
        }
    }else{
        printf("Function execution failed.");
    }

    printf("\n");
    printf("This function returns 1 in case of success, 0 otherwise: %d\n", res);
    printf("The median value is: %d\n", me);

    return 0;
}