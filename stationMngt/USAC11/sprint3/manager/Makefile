# Makefile for USAC11 - Manager Component Initialization
# Compiles for RV32IM architecture (Linux)

CC = riscv64-unknown-linux-gnu-gcc
CFLAGS = -march=rv32im -mabi=ilp32 -Wall -Wextra -O2
LDFLAGS = -static

# Assembly function object files from USAC01-09
ASM_DIR = ../../..
ASM_OBJS = \
	$(ASM_DIR)/USAC01/sprint2/usac01/func1.o \
	$(ASM_DIR)/USAC02/sprint2/usac02/func1.o \
	$(ASM_DIR)/USAC03/sprint2/usac03/func1.o \
	$(ASM_DIR)/USAC04/sprint2/usac04/func1.o \
	$(ASM_DIR)/USAC05/sprint2/usac05/func1.o \
	$(ASM_DIR)/USAC06/sprint2/usac06/func1.o \
	$(ASM_DIR)/USAC07/sprint2/usac07/func1.o \
	$(ASM_DIR)/USAC08/sprint2/usac08/func1.o \
	$(ASM_DIR)/USAC09/sprint2/usac09/func1.o

# Source files
SRCS = main.c initialize.c
OBJS = $(SRCS:.c=.o)

# Target executable
TARGET = manager_init

.PHONY: all clean

all: $(TARGET)

$(TARGET): $(OBJS) $(ASM_OBJS)
	$(CC) $(CFLAGS) $(LDFLAGS) -o $(TARGET) $(OBJS) $(ASM_OBJS)

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

# Compile assembly functions
$(ASM_DIR)/USAC01/sprint2/usac01/func1.o: $(ASM_DIR)/USAC01/sprint2/usac01/func1.s
	riscv64-unknown-linux-gnu-as -march=rv32im -mabi=ilp32 -o $@ $<

$(ASM_DIR)/USAC02/sprint2/usac02/func1.o: $(ASM_DIR)/USAC02/sprint2/usac02/func1.s
	riscv64-unknown-linux-gnu-as -march=rv32im -mabi=ilp32 -o $@ $<

$(ASM_DIR)/USAC03/sprint2/usac03/func1.o: $(ASM_DIR)/USAC03/sprint2/usac03/func1.s
	riscv64-unknown-linux-gnu-as -march=rv32im -mabi=ilp32 -o $@ $<

$(ASM_DIR)/USAC04/sprint2/usac04/func1.o: $(ASM_DIR)/USAC04/sprint2/usac04/func1.s
	riscv64-unknown-linux-gnu-as -march=rv32im -mabi=ilp32 -o $@ $<

$(ASM_DIR)/USAC05/sprint2/usac05/func1.o: $(ASM_DIR)/USAC05/sprint2/usac05/func1.s
	riscv64-unknown-linux-gnu-as -march=rv32im -mabi=ilp32 -o $@ $<

$(ASM_DIR)/USAC06/sprint2/usac06/func1.o: $(ASM_DIR)/USAC06/sprint2/usac06/func1.s
	riscv64-unknown-linux-gnu-as -march=rv32im -mabi=ilp32 -o $@ $<

$(ASM_DIR)/USAC07/sprint2/usac07/func1.o: $(ASM_DIR)/USAC07/sprint2/usac07/func1.s
	riscv64-unknown-linux-gnu-as -march=rv32im -mabi=ilp32 -o $@ $<

$(ASM_DIR)/USAC08/sprint2/usac08/func1.o: $(ASM_DIR)/USAC08/sprint2/usac08/func1.s
	riscv64-unknown-linux-gnu-as -march=rv32im -mabi=ilp32 -o $@ $<

$(ASM_DIR)/USAC09/sprint2/usac09/func1.o: $(ASM_DIR)/USAC09/sprint2/usac09/func1.s
	riscv64-unknown-linux-gnu-as -march=rv32im -mabi=ilp32 -o $@ $<

clean:
	rm -f $(OBJS) $(TARGET)

test: $(TARGET)
	./$(TARGET) config_example.txt

