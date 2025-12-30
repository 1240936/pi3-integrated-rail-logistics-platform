# Compiler and flags
CC = gcc
CFLAGS = -Wall -Wextra -std=c11

# Output executable
TARGET = manager_usac12.elf

# Source files
SRCS = main.c logging.c
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
