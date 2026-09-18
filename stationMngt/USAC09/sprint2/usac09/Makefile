CC = riscv32-linux-gcc
CFLAGS = -Wall -Wextra -fanalyzer -g
ARCHFLAGS = -march=rv32imd -mabi=ilp32d
LIBRV32 = ${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot

main.elf : main.o func1.o
	$(CC) $(CFLAGS) $(ARCHFLAGS) main.o func1.o -o main.elf

main.o : main.c
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c main.c

func1.o : func1.s asm.h
	$(CC) $(CFLAGS) $(ARCHFLAGS) -c func1.s

clean :
	rm -f *.o *.elf

run : main.elf
	qemu-riscv32 -L $(LIBRV32) ./main.elf

debug : main.elf
	@echo "On other terminal window do:"
	@echo "riscv32-linux-gdb -tui ./main.elf"
	qemu-riscv32 -L $(LIBRV32) -g 1234 main.elf -S
