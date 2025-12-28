CC = riscv32-linux-gcc
CFLAGS = -Wall -Wextra -fanalyzer -g
ARCHFLAGS = -march=rv32imd -mabi=ilp32d
LIBRV32 = ${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot

# Paths to USAC11 files
USAC11_DIR = ../../../USAC11/sprint3/manager
ASM_DIR = ../../..

# Assembly function object files from USAC01-09 (needed by initialize.c)
ASM_OBJS = \
	$(ASM_DIR)/USAC01/sprint2/usac01/func1.o \
	$(ASM_DIR)/USAC02/sprint2/usac02/func1.o \
	$(ASM_DIR)/USAC04/sprint2/usac04/func1.o \
	$(ASM_DIR)/USAC05/sprint2/usac05/func1.o \
	$(ASM_DIR)/USAC06/sprint2/usac06/func1.o \
	$(ASM_DIR)/USAC07/sprint2/usac07/func1.o \
	$(ASM_DIR)/USAC08/sprint2/usac08/func1.o \
	$(ASM_DIR)/USAC09/sprint2/usac09/func1.o

# Target executable
manager.elf : main.o sensor_manager.o sensor_data.o serial_comm.o func1.o initialize.o helper_functions.o $(ASM_OBJS)
	$(CC) $(CFLAGS) $(ARCHFLAGS) main.o sensor_manager.o sensor_data.o serial_comm.o func1.o initialize.o helper_functions.o $(ASM_OBJS) -o manager.elf

# Object files
main.o : main.c sensor_manager.h sensor_data.h serial_comm.h
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c main.c

sensor_manager.o : sensor_manager.c sensor_manager.h sensor_data.h serial_comm.h asm.h
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c sensor_manager.c

sensor_data.o : sensor_data.c sensor_data.h
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c sensor_data.c

serial_comm.o : serial_comm.c serial_comm.h
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c serial_comm.c

func1.o : func1.s asm.h
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c func1.s

# USAC11 initialize.c (needs data_structures.h and asm.h from USAC11, plus helper_functions.h)
# Include helper_functions.h so initialize.c can use get_token, int_to_string, parse_long_int
initialize.o : $(USAC11_DIR)/initialize.c $(USAC11_DIR)/data_structures.h $(USAC11_DIR)/asm.h helper_functions.h
	$(CC) $(CFLAGS) $(ARCHFLAGS) -I$(USAC11_DIR) -I. -include helper_functions.h -c $(USAC11_DIR)/initialize.c -o initialize.o

# Helper functions for initialize.c
helper_functions.o : helper_functions.c
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c helper_functions.c -o helper_functions.o

# Compile assembly functions from USAC01-09
$(ASM_DIR)/USAC01/sprint2/usac01/func1.o: $(ASM_DIR)/USAC01/sprint2/usac01/func1.s
	riscv32-linux-gcc $(ARCHFLAGS) -c $(ASM_DIR)/USAC01/sprint2/usac01/func1.s -o $@

$(ASM_DIR)/USAC02/sprint2/usac02/func1.o: $(ASM_DIR)/USAC02/sprint2/usac02/func1.s
	riscv32-linux-gcc $(ARCHFLAGS) -c $(ASM_DIR)/USAC02/sprint2/usac02/func1.s -o $@


$(ASM_DIR)/USAC04/sprint2/usac04/func1.o: $(ASM_DIR)/USAC04/sprint2/usac04/func1.s
	riscv32-linux-gcc $(ARCHFLAGS) -c $(ASM_DIR)/USAC04/sprint2/usac04/func1.s -o $@

$(ASM_DIR)/USAC05/sprint2/usac05/func1.o: $(ASM_DIR)/USAC05/sprint2/usac05/func1.s
	riscv32-linux-gcc $(ARCHFLAGS) -c $(ASM_DIR)/USAC05/sprint2/usac05/func1.s -o $@

$(ASM_DIR)/USAC06/sprint2/usac06/func1.o: $(ASM_DIR)/USAC06/sprint2/usac06/func1.s
	riscv32-linux-gcc $(ARCHFLAGS) -c $(ASM_DIR)/USAC06/sprint2/usac06/func1.s -o $@

$(ASM_DIR)/USAC07/sprint2/usac07/func1.o: $(ASM_DIR)/USAC07/sprint2/usac07/func1.s
	riscv32-linux-gcc $(ARCHFLAGS) -c $(ASM_DIR)/USAC07/sprint2/usac07/func1.s -o $@

$(ASM_DIR)/USAC08/sprint2/usac08/func1.o: $(ASM_DIR)/USAC08/sprint2/usac08/func1.s
	riscv32-linux-gcc $(ARCHFLAGS) -c $(ASM_DIR)/USAC08/sprint2/usac08/func1.s -o $@

$(ASM_DIR)/USAC09/sprint2/usac09/func1.o: $(ASM_DIR)/USAC09/sprint2/usac09/func1.s
	riscv32-linux-gcc $(ARCHFLAGS) -c $(ASM_DIR)/USAC09/sprint2/usac09/func1.s -o $@

test-parse : test_parse.o sensor_manager.o sensor_data.o func1.o
	$(CC) $(CFLAGS) $(ARCHFLAGS) test_parse.o sensor_manager.o sensor_data.o func1.o -o test_parse.elf

test_parse.o : test_parse.c sensor_manager.h sensor_data.h
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c test_parse.c

run-test : test-parse
	qemu-riscv32 -L $(LIBRV32) ./test_parse.elf

clean :
	rm -f *.o *.elf test_parse.elf $(ASM_OBJS)

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

.PHONY: clean run debug

