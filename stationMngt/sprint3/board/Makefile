# Makefile for USAC16 - Board Component
# Compiles for RISC-V architecture with minimal flags (Linux)
#
# Uses minimal compilation flags to avoid sysroot issues with the toolchain

CC = riscv32-linux-gcc
# Minimal flags that work with the current toolchain (same as manager)
CFLAGS = -Wall -Wextra -g
LDFLAGS = -static

# Source files
SRCS = main.c board.c
OBJS = $(SRCS:.c=.o)

# Target executable
TARGET = board.elf

.PHONY: all clean test run debug

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) $(LDFLAGS) -o $(TARGET) $(OBJS)

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

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

