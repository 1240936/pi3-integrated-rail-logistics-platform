# Makefile for UI Component - Sprint 3
# Compiles for RV32IMD architecture (Linux)
# Note: Uses ilp32d ABI to match USAC01-09 assembly functions

CC = riscv32-linux-gcc
CFLAGS = -Wall -Wextra -g
ARCHFLAGS = -march=rv32imd -mabi=ilp32d
LDFLAGS = -static
LIBRV32 = ${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot

# Source files
SRCS = main.c ui.c input_handler.c

OBJS = $(SRCS:.c=.o)

# Target executable
TARGET = ui.elf

.PHONY: all clean test run

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) $(ARCHFLAGS) $(LDFLAGS) -o $(TARGET) $(OBJS)

%.o: %.c
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

clean:
	rm -f $(OBJS) $(TARGET)

test: $(TARGET)
	@echo "Run UI component:"
	@echo "./ui.elf"

run: $(TARGET)
	qemu-riscv32 -L $$HOME/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot ./ui.elf

