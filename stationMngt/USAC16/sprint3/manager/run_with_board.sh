#!/bin/bash

echo "=== USAC16 Railway Track Management System ==="
echo "Running Manager with Board UI connection"
echo

# Check if board component exists
if [ ! -f "../board/board.elf" ]; then
    echo "❌ Board component not found!"
    echo "Please compile the board component first:"
    echo "  cd ../board && make"
    exit 1
fi

echo "✅ Board component found"

# Check if manager is compiled
if [ ! -f "manager_usac16.elf" ]; then
    echo "❌ Manager not compiled!"
    echo "Please compile first: make"
    exit 1
fi

echo "✅ Manager compiled"
echo

echo "🎯 Executing Manager (make run)..."
echo "Manager will show output + control LightSigns"
echo

# Execute manager normally (like make run) - this shows the text output
qemu-riscv32 ./manager_usac16.elf

echo
echo "🚀 Starting Board UI..."
echo "Board will display track management information with fancy interface"
echo

# Create sample board data (showing the results)
BOARD_DATA="TRACK_ASSIGN:1:101
TRACK_ASSIGN:2:102
TRACK_FREE:1
DEPART:2:102
TRACK_FREE:2
TRACK_ASSIGN:1:105"

# Run board to show the final UI
echo "$BOARD_DATA" | qemu-riscv32 ../board/board.elf

echo
echo "=== System Demonstration Complete ==="
