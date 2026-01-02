# Makefile for USAC16 - Board Component
# Compiles for RV32IM architecture (Linux)

CC = riscv32-linux-gcc
CFLAGS = -Wall -Wextra -g
ARCHFLAGS = -march=rv32im -mabi=ilp32
LDFLAGS = -static
LIBRV32 = ${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot

# Source files
SRCS = main.c board.c
OBJS = $(SRCS:.c=.o)

# Target executable
TARGET = board.elf

.PHONY: all clean test run debug

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) $(ARCHFLAGS) $(LDFLAGS) -o $(TARGET) $(OBJS)

%.o: %.c
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

clean:
	rm -f $(OBJS) $(TARGET)

test: $(TARGET)
	@echo "Board component compiled successfully"
	@echo "Run with: echo -e '\x02TRACK_ASSIGN:1:101\x03\n' | ./$(TARGET)"

run: $(TARGET)
	qemu-riscv32 -L $(LIBRV32) ./$(TARGET)

debug: $(TARGET)
	@echo "On other terminal window do:"
	@echo "riscv32-linux-gdb -tui ./$(TARGET)"
	qemu-riscv32 -L $(LIBRV32) -g 1234 $(TARGET) -S

