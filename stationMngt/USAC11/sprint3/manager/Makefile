# Makefile for USAC11 - Manager Component Initialization
# Compiles for RV32IM architecture (Linux)

CC = riscv32-linux-gcc
CFLAGS = -Wall -Wextra -g
ARCHFLAGS = -march=rv32im -mabi=ilp32
LDFLAGS = -static
LIBRV32 = ${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot

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
TARGET = manager_init.elf

.PHONY: all clean

all: $(TARGET)

$(TARGET): $(OBJS) $(ASM_OBJS)
	$(CC) $(CFLAGS) $(ARCHFLAGS) $(LDFLAGS) -o $(TARGET) $(OBJS) $(ASM_OBJS)

%.o: %.c
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

# Compile assembly functions
$(ASM_DIR)/USAC01/sprint2/usac01/func1.o: $(ASM_DIR)/USAC01/sprint2/usac01/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

$(ASM_DIR)/USAC02/sprint2/usac02/func1.o: $(ASM_DIR)/USAC02/sprint2/usac02/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

$(ASM_DIR)/USAC03/sprint2/usac03/func1.o: $(ASM_DIR)/USAC03/sprint2/usac03/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

$(ASM_DIR)/USAC04/sprint2/usac04/func1.o: $(ASM_DIR)/USAC04/sprint2/usac04/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

$(ASM_DIR)/USAC05/sprint2/usac05/func1.o: $(ASM_DIR)/USAC05/sprint2/usac05/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

$(ASM_DIR)/USAC06/sprint2/usac06/func1.o: $(ASM_DIR)/USAC06/sprint2/usac06/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

$(ASM_DIR)/USAC07/sprint2/usac07/func1.o: $(ASM_DIR)/USAC07/sprint2/usac07/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

$(ASM_DIR)/USAC08/sprint2/usac08/func1.o: $(ASM_DIR)/USAC08/sprint2/usac08/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

$(ASM_DIR)/USAC09/sprint2/usac09/func1.o: $(ASM_DIR)/USAC09/sprint2/usac09/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

clean:
	rm -f $(OBJS) $(TARGET)

test: $(TARGET)
	qemu-riscv32 -L $(LIBRV32) ./$(TARGET) config_example.txt

run: $(TARGET)
	qemu-riscv32 -L $(LIBRV32) ./$(TARGET) config_example.txt

debug: $(TARGET)
	@echo "On other terminal window do:"
	@echo "riscv32-linux-gdb -tui ./$(TARGET)"
	qemu-riscv32 -L $(LIBRV32) -g 1234 $(TARGET) -S

