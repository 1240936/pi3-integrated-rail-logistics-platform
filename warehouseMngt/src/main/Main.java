package main;

import main.ui.WarehouseUI;

/**
 * Usage:
 * - Run the interactive UI
 */
public class Main {
    public static void main(String[] args) {
            runInteractiveMode();
    }

    private static void runInteractiveMode() {
        WarehouseUI ui = new WarehouseUI();
        ui.start();
    }
}


