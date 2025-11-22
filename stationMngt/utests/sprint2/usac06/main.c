#include <string.h>  
#include "../../unity.h"
#include "asm.h" 
// int dequeue_value(int* array, int length, int * nelem, int* read, int* write, int *value);

int callfunc ( int (*f)(int* array, int len, int * nelem, int* rd, int* wr, int *val),
                           int* array, int len, int * nelem, int* rd, int *wr, int *val);  

void setUp(void) {
    // set stuff up here
}

void tearDown(void) {
    // clean stuff up here
}



void run_test(int * array, int len, int nelem, int rd, int wr, int  exp_val, 
			  int * exp_arr, int exp_nelem, int exp_rd, int exp_wr, int exp_res)
{
    int vec[100];
	// 0 - sentinel  
	// 1 - read 
	// 2 - sentinel  
	// 3 - write 
    // 4 - sentinel 
	// 5 - nelem 
	// 6 - sentinel  
	// 7 - buffer 
	// 7+length - sentinel 	

    int val[3]={-1,-1,-1}; 	
    int res; // result 	
    // setup 
    memset(vec, 0x55, sizeof vec);
	memcpy(vec+7,array,len*sizeof(int));  // buffer   
	vec[1]=rd; 
	vec[3]=wr; 
	vec[5]=nelem; 

     res=callfunc(dequeue_value,vec+7,len,vec+5,vec+1,vec+3, &val[1]);
   
    TEST_ASSERT_EQUAL_INT(exp_res,res);     // result  
    if (exp_res==1) // success 
	    TEST_ASSERT_EQUAL_INT(exp_val,val[1]);  // value     
    
    TEST_ASSERT_EQUAL_INT(-1,val[0]);  // sentinels     
    TEST_ASSERT_EQUAL_INT(-1,val[0]);      

    TEST_ASSERT_EQUAL_INT(0x55555555, vec[0]);    // check sentinel 
    TEST_ASSERT_EQUAL_INT(0x55555555, vec[2]);    // check sentinel  
    TEST_ASSERT_EQUAL_INT(0x55555555, vec[4]);    // check sentinel  
    TEST_ASSERT_EQUAL_INT(0x55555555, vec[6]);    // check sentinel  
    TEST_ASSERT_EQUAL_INT(0x55555555, vec[len+7]);    // check sentinel  
    TEST_ASSERT_EQUAL_INT_ARRAY(exp_arr, vec+7, len);    // check buffer 
    TEST_ASSERT_EQUAL_INT(exp_rd, vec[1]);    // check read   
    TEST_ASSERT_EQUAL_INT(exp_wr, vec[3]);    // check write  
    TEST_ASSERT_EQUAL_INT(exp_nelem, vec[5]);    // check nelem  
    
}

//  void run_test(int * array, int len, int nelem, int rd, int wr,
// 					int exp_val, int * exp_arr, int exp_nelem, int exp_rd, int exp_wr, int exp_res)


void test_One()
{ 
    run_test((int[]){0,0,0}, 3, 0, 0, 0, 
						5, (int[]){0,0,0}, 0, 0, 0, 0); 
}
void test_Zero()
{ 
    run_test((int[]){6,7,8}, 3, 3, 0, 2,
						6, (int[]){6,7,8}, 2, 1, 2, 1); 
}
void test_Three()                                             
{ 
    run_test((int[]){6,7,8,9}, 4, 0, 3, 3,
						5, (int[]){6,7,8,9}, 0, 3, 3, 0); 
}
void test_Five()
{ 
    run_test((int[]){6,7,8,9}, 4, 4, 0, 3,
						 6, (int[]){6,7,8,9}, 3, 1, 3, 1); 
}
void test_Six()
{ 
    run_test((int[]){6,7,8,9}, 4, 3, 3, 2,
						 9, (int[]){6,7,8,9}, 2, 0, 2, 1); 
}




int main()
  { 

    UNITY_BEGIN();
    RUN_TEST(test_One);
    RUN_TEST(test_Zero);
    RUN_TEST(test_Three);
    RUN_TEST(test_Five);
    RUN_TEST(test_Six);
    return UNITY_END();  

  } 






