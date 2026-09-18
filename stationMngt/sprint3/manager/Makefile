# Makefile for Sprint 3 Manager Component
# Compiles for RV32IMD architecture (Linux)
# Note: Uses ilp32d ABI to match USAC01-09 assembly functions

CC = riscv32-linux-gcc
CFLAGS = -Wall -Wextra -g -std=c99
ARCHFLAGS = -march=rv32imd -mabi=ilp32d
LDFLAGS = -static
LIBRV32 = ${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot

# Paths to assembly functions from Sprint 2 (USAC01-09)
ASM_DIR = ../../USAC01/sprint2
ASM_OBJS = \
	../../USAC01/sprint2/usac01/func1.o \
	../../USAC02/sprint2/usac02/func1.o \
	../../USAC03/sprint2/usac03/func1.o \
	../../USAC04/sprint2/usac04/func1.o \
	../../USAC05/sprint2/usac05/func1.o \
	../../USAC06/sprint2/usac06/func1.o \
	../../USAC07/sprint2/usac07/func1.o \
	../../USAC08/sprint2/usac08/func1.o \
	../../USAC09/sprint2/usac09/func1.o \
	validate_sensor_data.o

# Source files
SRCS = main.c \
       initialize.c \
       helper_functions.c \
       user_auth.c \
       track_manager.c \
       board_comm.c \
       lightsigns_comm.c \
       serial_comm.c \
       sensor_manager.c \
       sensor_data.c \
       logging.c

OBJS = $(SRCS:.c=.o)

# Target executable
TARGET = manager.elf

.PHONY: all clean test run debug

all: $(TARGET)

$(TARGET): $(OBJS) $(ASM_OBJS)
	$(CC) $(CFLAGS) $(ARCHFLAGS) $(LDFLAGS) -o $(TARGET) $(OBJS) $(ASM_OBJS)

%.o: %.c
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

# Compile assembly object files if needed
../../USAC01/sprint2/usac01/func1.o: ../../USAC01/sprint2/usac01/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

../../USAC02/sprint2/usac02/func1.o: ../../USAC02/sprint2/usac02/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

../../USAC03/sprint2/usac03/func1.o: ../../USAC03/sprint2/usac03/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

../../USAC04/sprint2/usac04/func1.o: ../../USAC04/sprint2/usac04/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

../../USAC05/sprint2/usac05/func1.o: ../../USAC05/sprint2/usac05/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

../../USAC06/sprint2/usac06/func1.o: ../../USAC06/sprint2/usac06/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

../../USAC07/sprint2/usac07/func1.o: ../../USAC07/sprint2/usac07/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

../../USAC08/sprint2/usac08/func1.o: ../../USAC08/sprint2/usac08/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

../../USAC09/sprint2/usac09/func1.o: ../../USAC09/sprint2/usac09/func1.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

# Compile validate_sensor_data assembly function (USAC13 - struct parameter requirement)
validate_sensor_data.o: validate_sensor_data.s
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $< -o $@

clean:
	rm -f $(OBJS) $(TARGET)

test: $(TARGET)
	@echo "Manager component compiled successfully"
	@echo "Run with: ./$(TARGET) config_example.txt"

run: $(TARGET)
	qemu-riscv32 -L $(LIBRV32) ./$(TARGET) config_example.txt

debug: $(TARGET)
	@echo "On other terminal window do:"
	@echo "riscv32-linux-gdb -tui ./$(TARGET)"
	qemu-riscv32 -L $(LIBRV32) -g 1234 $(TARGET) -S
