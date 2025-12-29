CC = riscv32-linux-gcc
CFLAGS = -Wall -Wextra -fanalyzer -g
ARCHFLAGS = -march=rv32imd -mabi=ilp32d
LIBRV32 = ${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot

# Paths to other USAC directories
USAC13_DIR = ../../../USAC13/sprint3/manager
ASM_DIR = ../../..

# Assembly function object file from USAC04 (format_command)
ASM_OBJ = $(ASM_DIR)/USAC04/sprint2/usac04/func1.o

# Serial communication object files from USAC13
SERIAL_OBJS = \
	$(USAC13_DIR)/serial_comm.o

# Target executable
manager.elf : main.o lightsigns_comm.o $(ASM_OBJ) $(SERIAL_OBJS)
	$(CC) $(CFLAGS) $(ARCHFLAGS) main.o lightsigns_comm.o $(ASM_OBJ) $(SERIAL_OBJS) -o manager.elf

# Object files
main.o : main.c lightsigns_comm.h
	$(CC) $(CFLAGS) $(ARCHFLAGS) -I$(USAC13_DIR) -c main.c

lightsigns_comm.o : lightsigns_comm.c lightsigns_comm.h asm.h
	$(CC) $(CFLAGS) $(ARCHFLAGS) -I$(USAC13_DIR) -c lightsigns_comm.c

# Compile assembly function from USAC04
$(ASM_DIR)/USAC04/sprint2/usac04/func1.o: $(ASM_DIR)/USAC04/sprint2/usac04/func1.s
	riscv32-linux-gcc $(ARCHFLAGS) -c $(ASM_DIR)/USAC04/sprint2/usac04/func1.s -o $@

# Compile serial_comm from USAC13
$(USAC13_DIR)/serial_comm.o: $(USAC13_DIR)/serial_comm.c $(USAC13_DIR)/serial_comm.h
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c $(USAC13_DIR)/serial_comm.c -o $@

clean :
	rm -f *.o *.elf $(ASM_OBJ) $(SERIAL_OBJS)

run : manager.elf
	qemu-riscv32 -L $(LIBRV32) ./manager.elf $(ARGS)

# Run with serial port argument: make run-serial SERIAL=/dev/ttyS0
run-serial : manager.elf
	@if [ -z "$(SERIAL)" ]; then \
		echo "Usage: make run-serial SERIAL=/dev/ttyS0"; \
		exit 1; \
	fi
	qemu-riscv32 -L $(LIBRV32) ./manager.elf $(SERIAL)

debug : manager.elf
	@echo "On other terminal window do:"
	@echo "riscv32-linux-gdb -tui ./manager.elf"
	qemu-riscv32 -L $(LIBRV32) -g 1234 manager.elf -S

.PHONY: clean run run-serial debug

