package ui;

import controller.InventoryService;
import controller.PickingService;
import repositories.*;
import domain.*;


import java.util.*;

/**
 * Interactive UI for warehouse management system.
 * Provides menu-driven interface for loading CSV data and managing inventory.
 */
public class WarehouseUI {
    private InventoryService inventoryService;
    private PickingService pickingService;
    private Scanner scanner;
    private boolean dataLoaded = false;
    private String currentWarehouseId = "W1";
    private int currentAisle = 1;
    private List<OrderLine> loadedOrderLines = new ArrayList<>();

    public WarehouseUI() {
        this.inventoryService = new InventoryService();
        this.pickingService = new PickingService();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("=== Warehouse Management System ===");
        System.out.println("Welcome to the warehouse management interface.");

        while (true) {
            showMainMenu();
            int choice = getIntInput("Enter your choice: ");

            switch (choice) {
                case 1:
                    loadCsvData();
                    break;
                case 2:
                    if (dataLoaded) {
                        viewInventory();
                    } else {
                        System.out.println("Please load CSV data first.");
                    }
                    break;
                case 3:
                    if (dataLoaded) {
                        performDispatch();
                    } else {
                        System.out.println("Please load CSV data first.");
                    }
                    break;
                case 4:
                    if (dataLoaded) {
                        performRelocation();
                    } else {
                        System.out.println("Please load CSV data first.");
                    }
                    break;
                case 5:
                    if (dataLoaded) {
                        planAllocations();
                    } else {
                        System.out.println("Please load CSV data first.");
                    }
                    break;
                case 6:
                    if (dataLoaded) {
                        createPickingPlan();
                    } else {
                        System.out.println("Please load CSV data first.");
                    }
                    break;
                case 7:
                    changeWarehouseSettings();
                    break;
                case 0:
                    System.out.println("Exiting.");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void showMainMenu() {
        System.out.println("\n=== MENU ===");
        System.out.println("1. Load CSV Data");
        System.out.println("2. View All Inventory");
        System.out.println("3. Perform Dispatch");
        System.out.println("4. Relocate Box");
        System.out.println("5. Plan Allocations");
        System.out.println("6. Create Picking Plan");
        System.out.println("7. Change Warehouse Settings");
        System.out.println("0. Exit");
        System.out.println("Current warehouse: " + currentWarehouseId + ", Aisle: " + currentAisle);
    }

    private void loadCsvData() {
        System.out.println("\n=== LOAD CSV DATA ===");

        try {
            System.out.println("Enter paths to CSV files (or press Enter for default paths):");

            String itemsPath = getStringInput("Items CSV path: ");
            String baysPath = getStringInput("Bays CSV path: ");
            String wagonsPath = getStringInput("Wagons CSV path: ");
            String ordersPath = getStringInput("Orders CSV path: ");
            String orderLinesPath = getStringInput("Order Lines CSV path: ");

            // Load items
            CsvValidatorResult<Item> items = ItemsCsvLoader.load(itemsPath);
            inventoryService.loadItemsForWarehouse(currentWarehouseId, items.getRecords());
            System.out.println("Loaded items: " + items.getRecords().size());
            if (items.hasErrors()) {
                System.out.println("Items import errors:");
                for (String err : items.getErrors()) System.out.println(" - " + err);
            }

            // Load bays
            CsvValidatorResult<String> bays = BayCsvLoader.load(baysPath, inventoryService);
            System.out.println("Loaded bays: " + bays.getRecords().size());
            if (bays.hasErrors()) {
                System.out.println("Bays import errors:");
                for (String err : bays.getErrors()) System.out.println(" - " + err);
            }

            // Load wagons (boxes)
            CsvValidatorResult<Box> wagons = WagonCsvLoader.load(wagonsPath, currentWarehouseId, currentAisle, inventoryService);
            System.out.println("Loaded boxes: " + wagons.getRecords().size());
            if (wagons.hasErrors()) {
                System.out.println("Wagons import errors:");
                for (String err : wagons.getErrors()) System.out.println(" - " + err);
            }

            // Load orders
            CsvValidatorResult<OrderHeader> orders = OrdersCsvLoader.load(ordersPath);
            System.out.println("Loaded orders: " + orders.getRecords().size());
            if (orders.hasErrors()) {
                System.out.println("Orders import errors:");
                for (String err : orders.getErrors()) System.out.println(" - " + err);
            }
            

            // Load order lines
            java.util.Map<String, OrderHeader> headers = new java.util.HashMap<String, OrderHeader>();
            for (OrderHeader oh : orders.getRecords()) {
                headers.put(oh.getOrderId(), oh);
            }

            CsvValidatorResult<OrderLine> orderLines = OrderLinesCsvLoader.load(orderLinesPath, headers);
            System.out.println("Loaded order lines: " + orderLines.getRecords().size());
            if (orderLines.hasErrors()) {
                System.out.println("Order lines import errors:");
                for (String err : orderLines.getErrors()) System.out.println(" - " + err);
            }
            
            // Store loaded order lines
            loadedOrderLines = new ArrayList<>(orderLines.getRecords());

            dataLoaded = true;
            System.out.println("\n All CSV data loaded successfully.");

        } catch (Exception e) {
            System.out.println("Error loading CSV data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void viewInventory() {
        System.out.println("\n=== VIEW INVENTORY ===");
        System.out.println("=" + "=".repeat(60));

        try {
            Map<String, Warehouse> allWarehouses = inventoryService.getAllWarehouses();

            if (allWarehouses.isEmpty()) {
                System.out.println("No warehouses found in the system.");
                return;
            }

            for (Map.Entry<String, Warehouse> warehouseEntry : allWarehouses.entrySet()) {
                String warehouseId = warehouseEntry.getKey();
                Warehouse warehouse = warehouseEntry.getValue();
                Map<Integer, Map<Integer, Bay>> aisles = warehouse.getAisles();

                System.out.println("\nWAREHOUSE: " + warehouseId);
                System.out.println("=" + "=".repeat(50));

                if (aisles.isEmpty()) {
                    System.out.println("  (No inventory in this warehouse)");
                    continue;
                }

                for (Map.Entry<Integer, Map<Integer, Bay>> aisleEntry : aisles.entrySet()) {
                    int aisleNum = aisleEntry.getKey();
                    Map<Integer, Bay> bays = aisleEntry.getValue();

                    System.out.println("\n  Aisle " + aisleNum + ":");
                    System.out.println("  " + "-".repeat(40));

                    for (Map.Entry<Integer, Bay> bayEntry : bays.entrySet()) {
                        int bayNum = bayEntry.getKey();
                        Bay bay = bayEntry.getValue();
                        List<Box> boxes = bay.getBoxes();

                        System.out.printf("    Bay %d (Capacity: %d, Used: %d):%n",
                                bayNum, bay.getCapacityBoxes(), boxes.size());

                        if (boxes.isEmpty()) {
                            System.out.println("      (empty)");
                        } else {
                            for (Box box : boxes) {
                                String expiryDisplay;
                                if (box.getExpiryDate() != null) {
                                    expiryDisplay = box.getExpiryDate().toString();
                                } else {
                                    expiryDisplay = "N/A";
                                }

                                System.out.printf("       Box %s: SKU=%s, Qty=%d, Expiry=%s, Received=%s%n",
                                        box.getBoxId(), box.getSku(), box.getQuantity(),
                                        expiryDisplay, box.getReceivedAt());
                            }
                        }
                    }
                }
            }

            System.out.println("\nInventory view complete for all warehouses.");

        } catch (Exception e) {
            System.out.println("Error viewing inventory: " + e.getMessage());
        }
    }

    private void performDispatch() {
        System.out.println("\n=== PERFORM DISPATCH ===");

        String warehouseId = getStringInput("Enter warehouse ID (or press Enter for current: " + currentWarehouseId + "): ");
        if (warehouseId.isEmpty()) {
            warehouseId = currentWarehouseId;
        }

        String sku = getStringInput("Enter SKU to dispatch: ");
        int aisle = getIntInput("Enter aisle number: ");
        int quantity = getIntInput("Enter quantity to dispatch: ");

        try {
            int dispatched = inventoryService.dispatch(warehouseId, sku, aisle, quantity);
            System.out.println("Successfully dispatched " + dispatched + " units of SKU " + sku +
                    " from warehouse " + warehouseId + ", aisle " + aisle);

            if (dispatched < quantity) {
                System.out.println("Warning: Only " + dispatched + " out of " + quantity + " requested units were available.");
            }

        } catch (Exception e) {
            System.out.println("Error performing dispatch: " + e.getMessage());
        }
    }

    private void performRelocation() {
        System.out.println("\n=== RELOCATE BOX ===");

        String boxId = getStringInput("Enter box ID to relocate: ");
        
        // Validate that the box exists
        if (!inventoryService.boxExists(boxId)) {
            System.out.println("Error: Box with ID '" + boxId + "' does not exist in the system.");
            return;
        }
        
        String newWarehouseId = getStringInput("Enter new warehouse ID: ");
        int newAisle = getIntInput("Enter new aisle number: ");
        int newBay = getIntInput("Enter new bay number: ");

        // Validate that the destination warehouse exists
        if (!inventoryService.warehouseExists(newWarehouseId)) {
            System.out.println("Error: Warehouse '" + newWarehouseId + "' does not exist in the system.");
            return;
        }

        // Validate that the destination aisle exists
        if (!inventoryService.aisleExists(newWarehouseId, newAisle)) {
            System.out.println("Error: Aisle " + newAisle + " does not exist in warehouse '" + newWarehouseId + "'.");
            return;
        }

        // Validate that the destination bay exists
        if (!inventoryService.bayExists(newWarehouseId, newAisle, newBay)) {
            System.out.println("Error: Bay " + newBay + " does not exist in warehouse '" + newWarehouseId + "', aisle " + newAisle + ".");
            return;
        }

        // Validate destination bay has capacity
        if (!inventoryService.bayHasCapacity(newWarehouseId, newAisle, newBay)) {
            System.out.println("Error: Destination bay " + newWarehouseId + "/" + newAisle + "/" + newBay + 
                             " is at capacity and cannot accept additional boxes.");
            return;
        }

        try {
            inventoryService.relocate(boxId, newWarehouseId, newAisle, newBay);
            System.out.println("Successfully relocated box " + boxId +
                    " to warehouse " + newWarehouseId + ", aisle " + newAisle + ", bay " + newBay);

        } catch (Exception e) {
            System.out.println("Error performing relocation: " + e.getMessage());
        }
    }

    private void planAllocations() {
        System.out.println("\n=== PLAN ALLOCATIONS ===");

        String warehouseId = getStringInput("Enter warehouse ID (or press Enter for current: " + currentWarehouseId + "): ");
        if (warehouseId.isEmpty()) {
            warehouseId = currentWarehouseId;
        }

        int aisle = getIntInput("Enter aisle number (or press Enter for current: " + currentAisle + "): ");
        if (aisle == -1) {
            aisle = currentAisle;
        }

        System.out.println("Allocation modes:");
        System.out.println("1. STRICT - Only allocate if all requested quantity can be fulfilled");
        System.out.println("2. PARTIAL - Allow partial allocations");

        int modeChoice = getIntInput("Choose allocation mode (1-2): ");
        AllocationMode mode;
        if (modeChoice == 1) {
            mode = AllocationMode.STRICT;
        } else {
            mode = AllocationMode.PARTIAL;
        }

        try {
            if (loadedOrderLines.isEmpty()) {
                System.out.println("Error: No order lines have been loaded. Please load CSV data first.");
                return;
            }

            System.out.println("Using " + loadedOrderLines.size() + " loaded order lines for allocation planning.");

            AllocationResult result = inventoryService.planAllocations(warehouseId, loadedOrderLines, mode, aisle);

            System.out.println("\n=== ALLOCATION RESULTS ===");
            System.out.println("Eligibility results:");
            for (LineEligibility e : result.getEligibilities()) {
                System.out.println("  " + e.getOrderId() + "#" + e.getLineNo() + " " + e.getSku() +
                        ": requested=" + e.getRequestedQty() + ", allocated=" + e.getAllocatedQty() +
                        ", status=" + e.getStatus());
            }

            System.out.println("\nAllocations:");
            for (AllocationRow r : result.getAllocations()) {
                System.out.println("  " + r.getOrderId() + "#" + r.getLineNo() + " " + r.getSku() +
                        " qty=" + r.getQty() + " from box=" + r.getBoxId() +
                        " at " + r.getAisle() + "/" + r.getBay());
            }

        } catch (Exception e) {
            System.out.println("Error planning allocations: " + e.getMessage());
        }
    }

    private void changeWarehouseSettings() {
        System.out.println("\n=== CHANGE WAREHOUSE SETTINGS ===");

        String newWarehouseId = getStringInput("Enter new warehouse ID (or press Enter to keep current: " + currentWarehouseId + "): ");
        if (!newWarehouseId.isEmpty()) {
            currentWarehouseId = newWarehouseId;
        }

        int newAisle = getIntInput("Enter new aisle number (or press Enter to keep current: " + currentAisle + "): ");
        if (newAisle != -1) {
            currentAisle = newAisle;
        }

        System.out.println("Settings updated. Current warehouse: " + currentWarehouseId + ", Aisle: " + currentAisle);
    }

    private void createPickingPlan() {
        System.out.println("\n=== CREATE PICKING PLAN ===");

        String warehouseId = getStringInput("Enter warehouse ID (or press Enter for current: " + currentWarehouseId + "): ");
        if (warehouseId.isEmpty()) {
            warehouseId = currentWarehouseId;
        }

        int aisle = getIntInput("Enter aisle number (or press Enter for current: " + currentAisle + "): ");
        if (aisle == -1) {
            aisle = currentAisle;
        }

        System.out.println("Packing heuristics:");
        System.out.println("1. First Fit (FF) - Place items in first available trolley");
        System.out.println("2. First Fit Decreasing (FFD) - Sort by weight, then first fit");
        System.out.println("3. Best Fit Decreasing (BFD) - Sort by weight, then tightest fit");

        int heuristicChoice = getIntInput("Choose packing heuristic (1-3): ");
        PackingHeuristic heuristic;
        switch (heuristicChoice) {
            case 1:
                heuristic = PackingHeuristic.FIRST_FIT;
                break;
            case 2:
                heuristic = PackingHeuristic.FIRST_FIT_DECREASING;
                break;
            case 3:
                heuristic = PackingHeuristic.BEST_FIT_DECREASING;
                break;
            default:
                System.out.println("Invalid choice. Using First Fit.");
                heuristic = PackingHeuristic.FIRST_FIT;
        }

        double trolleyCapacity = getDoubleInput("Enter trolley weight capacity (kg): ");
        boolean allowSplitting = getStringInput("Allow splitting items across trolleys? (y/n): ").toLowerCase().startsWith("y");

        try {
            if (loadedOrderLines.isEmpty()) {
                System.out.println("Error: No order lines have been loaded. Please load CSV data first.");
                return;
            }

            System.out.println("Using " + loadedOrderLines.size() + " loaded order lines for allocation planning.");

            // First, plan allocations
            AllocationResult allocationResult = inventoryService.planAllocations(warehouseId, loadedOrderLines, AllocationMode.PARTIAL, aisle);

            if (allocationResult.getAllocations().isEmpty()) {
                System.out.println("No allocations found. Cannot create picking plan.");
                return;
            }

            // Create picking plan from allocations
            PickPlan pickPlan = pickingService.createPickPlan(allocationResult.getAllocations(), heuristic, trolleyCapacity, allowSplitting);

            // Display results
            System.out.println("\n=== PICKING PLAN RESULTS ===");
            System.out.println("Heuristic: " + pickPlan.getPackingHeuristic());
            System.out.println("Trolleys: " + pickPlan.getTrolleyCount());
            System.out.println("Total Weight: " + String.format("%.2f", pickPlan.getTotalWeight()) + " kg");
            System.out.println("Total Items: " + pickPlan.getTotalItems());
            System.out.println("Weight Utilization: " + String.format("%.1f", pickPlan.getWeightUtilization() * 100) + "%");
            System.out.println("Average Trolley Weight: " + String.format("%.2f", pickPlan.getAverageTrolleyWeight()) + " kg");

            System.out.println("\n=== TROLLEY ASSIGNMENTS ===");
            for (Trolley trolley : pickPlan.getTrolleys()) {
                System.out.println("\n" + trolley.toString());
                System.out.println("  Items:");
                for (PickItem item : trolley.getItems()) {
                    System.out.println("    " + item.toString());
                }
                if (!trolley.getLogs().isEmpty()) {
                    System.out.println("  Logs:");
                    for (String log : trolley.getLogs()) {
                        System.out.println("    " + log);
                    }
                }
            }

            if (!pickPlan.getSkippedItems().isEmpty()) {
                System.out.println("\n=== SKIPPED ITEMS ===");
                for (String skipped : pickPlan.getSkippedItems()) {
                    System.out.println("  " + skipped);
                }
            }

        } catch (Exception e) {
            System.out.println("Error creating picking plan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private double getDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    return -1.0;
                }
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    private String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    return -1;
                }
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }
}
