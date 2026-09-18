#include <string.h>  
#include "../../unity.h"
#include "asm.h" 


int callfunc ( int (*f)(char* in, int key, char* out  ),char* in, int key, char* out);  

void setUp(void) {
    // set stuff up here
}

void tearDown(void) {
    // clean stuff up here
}



void run_test(char * str, int key, char * str_expected, int expected)
{
    char vec[100];
    int  res; 


    // setup 
        memset(vec, 0xaa, sizeof vec);
     
	res = callfunc(encrypt_data,str,key,vec+1);
    
    TEST_ASSERT_EQUAL_CHAR(0xaa, vec[strlen(str)+2]);    // check sentinel 
    TEST_ASSERT_EQUAL_CHAR(0xaa, vec[0]);    // check sentinel 
    TEST_ASSERT_EQUAL_STRING(str_expected,vec+1);  
    TEST_ASSERT_EQUAL_INT(expected,res); 
    
}


void test_None()
{ 
    run_test("",0,"",0); 
}
void test_None1()
{ 
    run_test("",4,"",1); 
}
void test_Error()
{ 
    run_test("heu",2,"",0); 
}
void test_Error2()
{ 
    run_test("HEU",52,"",0); 
}
void test_S1() 
{ 
    run_test("ABCDEFGH",4,"EFGHIJKL",1); 
}
void test_S2()
{ 
    run_test("UVWXYZAAAAA",8,"CDEFGHIIIII",1); 
}

void test_S3()
{ 
    run_test("CDEFGHIIIII",20,"WXYZABCCCCC",1); 
	
}


int main()
  { 

    UNITY_BEGIN();
    RUN_TEST(test_None);
    RUN_TEST(test_None1);
    RUN_TEST(test_Error);
    RUN_TEST(test_Error2);
    RUN_TEST(test_S1);
    RUN_TEST(test_S2);
    RUN_TEST(test_S3);
    return UNITY_END();  

  } 






