# Makefile for UI Component - Sprint 3
# Compiles for RV32IM architecture (Linux)

CC = riscv32-linux-gcc
CFLAGS = -Wall -Wextra -g
ARCHFLAGS = -march=rv32im -mabi=ilp32
LDFLAGS = -static

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
	./ui.elf

