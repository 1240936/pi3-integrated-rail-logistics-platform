# Makefile for USAC16 - Track Management

CC = riscv32-linux-gcc
# Minimal compilation flags (same as board component that worked)
CFLAGS = -Wall -Wextra -std=c99 -I../../../USAC12 -I../../../USAC11/sprint3/manager
LDFLAGS = -static

# Source files
SOURCES = main.c track_manager.c board_comm.c lightsigns_comm.c serial_comm.c \
          ../../../USAC12/logging.c ../../../USAC11/sprint3/manager/initialize.c \
          ../../../USAC11/sprint3/manager/helper_functions.c
HEADERS = track_manager.h board_comm.h lightsigns_comm.h serial_comm.h \
          ../../../USAC12/logging.h ../../../USAC12/data_structures.h \
          ../../../USAC11/sprint3/manager/data_structures.h \
          ../../../USAC11/sprint3/manager/helper_functions.h \
          ../../../USAC11/sprint3/manager/asm.h

# Output executable
TARGET = manager_usac16.elf

# Default target
all: $(TARGET)

# Build the executable
$(TARGET): $(SOURCES) $(HEADERS)
	$(CC) $(CFLAGS) $(SOURCES) -o $(TARGET) $(LDFLAGS)

# Clean build artifacts
clean:
	rm -f $(TARGET)

# Run the program with QEMU (RISC-V emulator)
run: $(TARGET)
	qemu-riscv32 ./$(TARGET)

# Run with custom arguments
run-args: $(TARGET)
	qemu-riscv32 ./$(TARGET) $(ARGS)

# Run with custom ports (commented out for RISC-V compatibility)
# run-custom: $(TARGET)
#	./$(TARGET) /dev/ttyUSB0 /dev/ttyUSB1

.PHONY: all clean run run-custom
