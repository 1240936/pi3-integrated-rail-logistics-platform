# Compiler and flags
CC = gcc
# -pthread é obrigatório para o Mutex que usas na tua US
CFLAGS = -Wall -Wextra -std=c11 -pthread

# Output executable
TARGET = manager_usac15.elf

# Source files
SRCS = main.c board_comm.c
OBJS = $(SRCS:.c=.o)

# Default rule
all: $(TARGET)

# Link executable
$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) $(OBJS) -o $(TARGET)

# Compile each .c into .o
%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

# Clean build files
clean:
	rm -f $(OBJS) $(TARGET)

# Convenience run target
run: $(TARGET)
	./$(TARGET)

.PHONY: all clean run