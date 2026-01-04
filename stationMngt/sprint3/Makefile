# Sprint 3 Top-Level Makefile
# Runs all components (UI, Manager, Board) together

.PHONY: all clean run build

# Default target
all: build

# Build all components
build:
	$(MAKE) -C ui
	$(MAKE) -C manager
	$(MAKE) -C board

# Clean all components
clean:
	$(MAKE) -C ui clean
	$(MAKE) -C manager clean
	$(MAKE) -C board clean

# Run all components together (UI -> Manager -> Board)
# Uses the exact command structure that works in the console
run: build
	@echo "Starting all components..."
	@echo "UI -> Manager -> Board"
	@qemu-riscv32 -L $${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot ./ui/ui.elf | \
	qemu-riscv32 -L $${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot ./manager/manager.elf manager/config_example.txt /dev/ttyS0 /dev/ttyS0 /dev/ttyS0 | \
	qemu-riscv32 -L $${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot ./board/board.elf

# Debug mode (same as run but with more verbose output)
debug: build
	@echo "Starting all components in debug mode..."
	@qemu-riscv32 -L $${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot -g 1234 ./ui/ui.elf | \
	qemu-riscv32 -L $${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot -g 1235 ./manager/manager.elf manager/config_example.txt /dev/ttyS0 /dev/ttyS0 /dev/ttyS0 | \
	qemu-riscv32 -L $${HOME}/bin/bootlin/riscv32-buildroot-linux-gnu/sysroot -g 1236 ./board/board.elf

