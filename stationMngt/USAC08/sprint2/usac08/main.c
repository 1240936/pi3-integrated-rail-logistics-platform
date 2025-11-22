#include <stdio.h>
#include "asm.h"

int main(void){
    int arr[4] = {5, 3, 8, 1};
    int length = 4;
    char order;   

    printf("Array original: ");
    for (int i = 0; i < length; i++) {
        printf("%d ", arr[i]);
    }
    printf("\n");

    // Ascending test
    order = 1;
    int result = sort_array(arr, length, order);
    printf("This function returns 1 in case of success, 0 otherwise: %d\n", result);
    if (result == 1) {
        printf("Ascending array: ");
        for (int i = 0; i < length; i++) {
            printf("%d ", arr[i]);
        }
        printf("\n");
    } else {
        printf("Function execution failed.\n");
    }
    
    // Descending test
    /*
    int arr2[] = {5, 3, 8, 1};  // reset
    order = 0;
    int res = sort_array(arr2, length, order);
    printf("This function returns 1 in case of success, 0 otherwise: %d\n", res);
    if (res == 1) {
        printf("Descending array: ");
        for (int i = 0; i < length; i++) {
            printf("%d ", arr2[i]);
        }
        printf("\n");
    } else {
        printf("Function execution failed.\n");
    }
    */

    return 0;

}