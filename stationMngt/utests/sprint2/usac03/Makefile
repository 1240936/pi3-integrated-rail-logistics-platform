# Utests setup 


CC=riscv32-linux-gcc
CFLAGS=-Wall -Wextra -fanalyzer -g
ARCHFLAGS=-march=rv32imd -mabi=ilp32d
LIBRV32=${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot


ifeq ($(REPO),)
  $(error Environment variable REPO is not set)
endif


space := $(subst ,, ) 		# space macro # not needded 

CURRENT_DIR:=$(strip $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST)))))  
EX_TEST:=$(subst utests,$(REPO),$(CURRENT_DIR)) 

FILES:=$(shell find $(EX_TEST) -type f \( -iname \*.c -o -iname \*.s \) )  ## all the source files 
ifeq ($(strip $(FILES)),)
  export FILES_NOT_FOUND=1 
  $(error Could not find source files in $(EX_TEST) )
endif 
MAIN_FILE:=$(shell grep -l -E 'int[ \t\n]+main' $(FILES))  	##  the file where is "int main" 
REAL_FILES:=$(filter-out $(MAIN_FILE),$(FILES))			## take it out of the list of files to compile 
ifeq ($(strip $(REAL_FILES)),)
  export FILES_NOT_FOUND=1 
  $(error Could not find source files in $(EX_TEST) )
endif 

REAL_OBJS1:=$(notdir $(REAL_FILES))
REAL_OBJS2:=$(subst .c,.o,$(REAL_OBJS1)) 
REAL_OBJS:=$(subst .s,.o,$(REAL_OBJS2)) 


prog.elf: main.o  callfunc.o ../../unity.o $(REAL_OBJS)   
	$(CC) $(CFLAGS)  $(REAL_OBJS) $(ARCHFLAGS) main.o callfunc.o ../../unity.o -o prog.elf  

main.o : main.c
	$(CC) $(CFLAGS) $(ARCHFLAGS) -I ../..   -c $< 

callfunc.o : callfunc.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -I ../..   -c $< 


../../unity.o : ../../unity.c  
	$(CC) $(CFLAGS) $(ARCHFLAGS) -I ../.. -c  $< -o ../../unity.o  


$(REAL_OBJS):  $(REAL_FILES)  
	$(CC) $(CFLAGS) $(ARCHFLAGS)  -c $(REAL_FILES) 

run:  prog.elf 
	qemu-riscv32 -L $(LIBRV32) ./prog.elf 

clean: 
	rm -f prog.elf *.o 	

full-clean:  
	rm -f prog.elf *.o ../../unity.o  

