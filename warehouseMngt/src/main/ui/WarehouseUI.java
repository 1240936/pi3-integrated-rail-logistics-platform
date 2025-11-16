package main.ui;

import main.domain.AVL;
import main.controller.InventoryService;
import main.controller.PickingService;
import main.controller.QuarantineService;
import main.controller.StationService;
import main.repositories.*;
import main.domain.*;


import java.util.*;

/**
 * Interactive User Interface for the Warehouse Management System.
 * Provides menu-driven commands to load CSV data, manage inventory, perform dispatch and relocation,
 * plan allocations, create picking plans, compute pick sequences, and handle returns and quarantine.
 */
public class WarehouseUI {
    private InventoryService inventoryService;
    private PickingService pickingService;
    private QuarantineService quarantineService;
    private StationService stationService;
    private Scanner scanner;
    private boolean dataLoaded = false;
    private boolean stationDataLoaded = false;
    private String currentWarehouseId = "W1";
    private int currentAisle = 1;
    private List<OrderLine> loadedOrderLines = new ArrayList<>();

    /**
     * Constructs a WarehouseUI with default services and scanner.
     */
    public WarehouseUI() {
        this.inventoryService = new InventoryService();
        this.pickingService = new PickingService();
        this.quarantineService = new QuarantineService(inventoryService, "audit_log.txt");
        this.stationService = new StationService();
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
                    showSprint1Menu();
                    break;
                case 2:
                    showSprint2Menu();
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
        System.out.println("\n=== MAIN MENU ===");
        System.out.println("1. Sprint 1 - Warehouse Operations");
        System.out.println("2. Sprint 2 - Station Indexing");
        System.out.println("0. Exit");
        System.out.println("Current warehouse: " + currentWarehouseId + ", Aisle: " + currentAisle);
    }

    private void showSprint1Menu() {
        while (true) {
            System.out.println("\n=== SPRINT 1 - WAREHOUSE OPERATIONS ===");
            System.out.println("1. Load CSV Data");
            System.out.println("2. View All Inventory");
            System.out.println("3. Perform Dispatch");
            System.out.println("4. Relocate Box");
            System.out.println("5. Plan Allocations");
            System.out.println("6. Create Picking Plan");
            System.out.println("7. Compute Pick Path Sequence");
            System.out.println("8. Returns & Quarantine");
            System.out.println("9. Change Warehouse Settings");
            System.out.println("0. Back to Main Menu");
            
            int choice = getIntInput("Enter your choice: ");
            
            switch (choice) {
                case 1:
                    loadCsvData();
                    break;
                case 2:
                    viewInventory();
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
                    if (dataLoaded) {
                        computePickPathSequence();
                    } else {
                        System.out.println("Please load CSV data first.");
                    }
                    break;
                case 8:
                    manageReturnsAndQuarantine();
                    break;
                case 9:
                    changeWarehouseSettings();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void showSprint2Menu() {
        while (true) {
            System.out.println("\n=== SPRINT 2 - STATION INDEXING ===");
            System.out.println("1. USEI06 - Time-Zone Index");
            System.out.println("2. USEI07 - 2D-Tree Spatial Index");
            System.out.println("0. Back to Main Menu");
            
            int choice = getIntInput("Enter your choice: ");
            
            switch (choice) {
                case 1:
                    manageStationsTimeZoneIndex();
                    break;
                case 2:
                    manageStationsSpatialIndex();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void loadCsvData() {
        System.out.println("\n=== LOAD CSV DATA ===");

        try {
            System.out.println("Enter paths to CSV files (all paths are required):");

            String itemsPath = getValidFilePath("Items CSV path: ", "items CSV");
            String baysPath = getValidFilePath("Bays CSV path: ", "bays CSV");
            String wagonsPath = getValidFilePath("Wagons CSV path: ", "wagons CSV");
            String ordersPath = getValidFilePath("Orders CSV path: ", "orders CSV");
            String orderLinesPath = getValidFilePath("Order Lines CSV path: ", "order lines CSV");

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
            stationDataLoaded = false;

            // Reorder entire inventory to ensure proper FEFO ordering after loading new data
            inventoryService.reorderInventoryFefo();

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

        // Validate and get SKU
        String sku;
        while (true) {
            sku = getStringInput("Enter SKU to dispatch: ");
            if (sku.isEmpty()) {
                System.out.println("Error: SKU cannot be empty. Please enter a valid SKU.");
                continue;
            }
            if (!inventoryService.isKnownSku(warehouseId, sku)) {
                System.out.println("Error: SKU '" + sku + "' is not known in warehouse '" + warehouseId + "'. Please enter a valid SKU.");
                continue;
            }
            break;
        }

        // Get aisle (use current as default)
        int aisle;
        while (true) {
            String aisleInput = getStringInput("Enter aisle number (or press Enter for current: " + currentAisle + "): ");
            if (aisleInput.isEmpty()) {
                aisle = currentAisle;
                break;
            }
            try {
                aisle = Integer.parseInt(aisleInput);
                if (aisle <= 0) {
                    System.out.println("Error: Aisle number must be positive. Please enter a valid aisle number.");
                    continue;
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid aisle number format. Please enter a valid number.");
            }
        }

        // Validate and get quantity
        int quantity;
        while (true) {
            quantity = getIntInput("Enter quantity to dispatch: ");
            if (quantity <= 0) {
                System.out.println("Error: Quantity must be positive. Please enter a valid quantity.");
                continue;
            }
            break;
        }

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

    private void computePickPathSequence() {
        System.out.println("\n=== COMPUTE PICK PATH SEQUENCE ===");

        String warehouseId = getStringInput("Enter warehouse ID (or press Enter for current: " + currentWarehouseId + "): ");
        if (warehouseId.isEmpty()) {
            warehouseId = currentWarehouseId;
        }

        int aisle = getIntInput("Enter aisle number (or press Enter for current: " + currentAisle + "): ");
        if (aisle == -1) {
            aisle = currentAisle;
        }

        try {
            if (loadedOrderLines.isEmpty()) {
                System.out.println("Error: No order lines have been loaded. Please load CSV data first.");
                return;
            }

            System.out.println("Using " + loadedOrderLines.size() + " loaded order lines for allocation planning.");

            // First, plan allocations
            AllocationResult allocationResult = inventoryService.planAllocations(warehouseId, loadedOrderLines, AllocationMode.PARTIAL, aisle);

            if (allocationResult.getAllocations().isEmpty()) {
                System.out.println("No allocations found. Cannot compute pick path sequence.");
                return;
            }

            // Create a basic picking plan for sequencing
            PickPlan pickPlan = pickingService.createPickPlan(allocationResult.getAllocations(), PackingHeuristic.FIRST_FIT, 1000.0, true);

            System.out.println("\n=== PICK PATH SEQUENCING RESULTS ===");
            /*
            Takes every trolley in the pick plan.
            Takes all the items from those trolleys.
            Converts each item into a Coordinate made from its aisle and bay.
            Removes duplicates so the same aisle/bay pair only counts once.
            Counts how many remain.
             */
            System.out.println("Total bays to visit: " + pickPlan.getTrolleys().stream()
                    .flatMap(t -> t.getItems().stream())
                    .map(item -> new Coordinate(item.getAisle(), item.getBay()))
                    .distinct()
                    .count());

            // Strategy A: Deterministic Sweep
            System.out.println("\n--- Strategy A: Deterministic Sweep ---");
            PickSequenceResult sweepResult = pickingService.computeDeterministicSweep(pickPlan);
            System.out.println("Strategy: " + sweepResult.getStrategyName());
            System.out.println("Total Distance: " + String.format("%.2f", sweepResult.getTotalDistance()));
            System.out.println("Bays Visited: " + sweepResult.getTotalBays());
            System.out.println("Path Sequence:");
            for (int i = 0; i < sweepResult.getSequence().size(); i++) {
                Coordinate coord = sweepResult.getSequence().get(i);
                System.out.printf("  %d. Bay %s%n", i + 1, coord.toString());
            }

            // Strategy B: Nearest-Neighbour Greedy
            System.out.println("\n--- Strategy B: Nearest-Neighbour Greedy ---");
            PickSequenceResult nearestResult = pickingService.computeNearestNeighbor(pickPlan);
            System.out.println("Strategy: " + nearestResult.getStrategyName());
            System.out.println("Total Distance: " + String.format("%.2f", nearestResult.getTotalDistance()));
            System.out.println("Bays Visited: " + nearestResult.getTotalBays());
            System.out.println("Path Sequence:");
            for (int i = 0; i < nearestResult.getSequence().size(); i++) {
                Coordinate coord = nearestResult.getSequence().get(i);
                System.out.printf("  %d. Bay %s%n", i + 1, coord.toString());
            }

            // Comparison
            System.out.println("\n--- Comparison ---");
            System.out.printf("Deterministic Sweep Distance: %.2f%n", sweepResult.getTotalDistance());
            System.out.printf("Nearest-Neighbour Distance:   %.2f%n", nearestResult.getTotalDistance());
            if (sweepResult.getTotalDistance() < nearestResult.getTotalDistance()) {
                System.out.println("Deterministic Sweep is more efficient by " +
                        String.format("%.2f", nearestResult.getTotalDistance() - sweepResult.getTotalDistance()) + " units");
            } else if (nearestResult.getTotalDistance() < sweepResult.getTotalDistance()) {
                System.out.println("Nearest-Neighbour is more efficient by " +
                        String.format("%.2f", sweepResult.getTotalDistance() - nearestResult.getTotalDistance()) + " units");
            } else {
                System.out.println("Both strategies have the same efficiency");
            }

        } catch (Exception e) {
            System.out.println("Error computing pick path sequence: " + e.getMessage());
            e.printStackTrace();
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


    private void manageReturnsAndQuarantine() {
        System.out.println("\n=== RETURNS & QUARANTINE MANAGEMENT ===");

        while (true) {
            System.out.println("\n--- Quarantine Menu ---");
            System.out.println("1. Load Returns from CSV" + (dataLoaded ? "" : " (requires initial data loaded)"));
            System.out.println("2. View Quarantine Queue");
            System.out.println("3. Process Quarantine (Inspect & Restock/Discard)");
            System.out.println("4. Clear Quarantine Queue");
            System.out.println("0. Back to Main Menu");
            System.out.println("Current quarantine size: " + quarantineService.getQuarantineSize());
            if (!dataLoaded) {
                System.out.println("Note: Load initial CSV data first before processing returns.");
            }

            int choice = getIntInput("Enter your choice: ");

            switch (choice) {
                case 1:
                    if (dataLoaded) {
                        loadReturnsFromCsv();
                    } else {
                        System.out.println("Please load initial CSV data first (items, bays, wagons).");
                    }
                    break;
                case 2:
                    viewQuarantineQueue();
                    break;
                case 3:
                    if (dataLoaded) {
                        processQuarantine();
                    } else {
                        System.out.println("Please load initial CSV data first (items, bays, wagons).");
                    }
                    break;
                case 4:
                    quarantineService.clearQuarantine();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }


    private void viewQuarantineQueue() {
        System.out.println("\n--- Quarantine Queue ---");
        List<Return> queue = quarantineService.getQuarantineQueue();

        if (queue.isEmpty()) {
            System.out.println("Quarantine queue is empty.");
            return;
        }

        System.out.printf("%-12s %-10s %-8s %-15s %-20s %-12s%n",
                "Return ID", "SKU", "Qty", "Reason", "Timestamp", "Expiry Date");
        System.out.println("-".repeat(80));

        for (Return returnItem : queue) {
            String expiryStr = returnItem.getExpiryDate() != null ?
                    returnItem.getExpiryDate().toString() : "Unknown";
            System.out.printf("%-12s %-10s %-8d %-15s %-20s %-12s%n",
                    returnItem.getReturnId(),
                    returnItem.getSku(),
                    returnItem.getQuantity(),
                    returnItem.getReason(),
                    returnItem.getTimestamp().toString(),
                    expiryStr);
        }
    }

    private void processQuarantine() {
        System.out.println("\n--- Process Quarantine ---");

        if (quarantineService.getQuarantineSize() == 0) {
            System.out.println("Quarantine queue is empty. Nothing to process.");
            return;
        }

        System.out.println("Processing " + quarantineService.getQuarantineSize() + " items in quarantine...");
        System.out.println("Items will be processed in reverse order of arrival (latest first).");

        List<InspectionResult> results = quarantineService.processQuarantine(currentWarehouseId, currentAisle);

        System.out.println("\n--- Processing Results ---");
        for (InspectionResult result : results) {
            System.out.println("Return ID: " + result.getReturnId());
            System.out.println("SKU: " + result.getSku());
            System.out.println("Action: " + result.getAction());
            System.out.println("Total Quantity: " + result.getTotalQuantity());

            if (result.isPartialRestock()) {
                System.out.println("Quantity Restocked: " + result.getQuantityRestocked());
                System.out.println("Quantity Discarded: " + result.getQuantityDiscarded());
            }
            System.out.println("---");
        }

        System.out.println("All items have been processed and logged to audit file.");

        // Reorder entire inventory to ensure proper FEFO ordering after processing returns
        inventoryService.reorderInventoryFefo();
        System.out.println("Inventory reordered to maintain FEFO sequence.");
    }

    private void loadReturnsFromCsv() {
        System.out.println("\n--- Load Returns from CSV ---");

        String csvPath = getStringInput("Enter path to returns CSV file: ");
        if (csvPath.isEmpty()) {
            System.out.println("No file path provided.");
            return;
        }

        try {
            CsvValidatorResult<Return> result = ReturnsCsvLoader.load(csvPath);

            if (result.hasErrors()) {
                System.out.println("CSV loading completed with errors:");
                for (String error : result.getErrors()) {
                    System.out.println(" - " + error);
                }
            }

            // Add all valid returns to quarantine
            int addedCount = 0;
            for (Return returnItem : result.getRecords()) {
                quarantineService.addToQuarantine(returnItem);
                addedCount++;
            }

            System.out.println("Successfully loaded " + addedCount + " returns from CSV.");
            System.out.println("Total quarantine size: " + quarantineService.getQuarantineSize());

        } catch (Exception e) {
            System.out.println("Error loading returns CSV: " + e.getMessage());
        }
    }

    private void manageStationsTimeZoneIndex() {
        while (true) {
            System.out.println("\n=== USEI06 - TIME-ZONE INDEX ===");
            System.out.println("1. Load Stations CSV");
            System.out.println("2. Show Tree Statistics");
            System.out.println("3. Query by Time Zone");
            System.out.println("4. Query by Time Zone Window");
            System.out.println("5. Run Sample Queries");
            System.out.println("6. Complexity Analysis");
            System.out.println("0. Back to Sprint 2 Menu");
            
            int choice = getIntInput("Enter your choice: ");
            
            switch (choice) {
                case 1:
                    loadStationsAndCreateAVLTrees();
                    break;
                case 2:
                    showAVLTreeStatistics();
                    break;
                case 3:
                    queryByTimeZoneGroup();
                    break;
                case 4:
                    queryByTimeZoneGroupWindow();
                    break;
                case 5:
                    runSampleQueries();
                    break;
                case 6:
                    showTemporalComplexityAnalysis();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void loadStationsAndCreateAVLTrees() {
        System.out.println("\n--- Load Stations CSV and Create AVL Trees ---");
        String csvPath = getValidFilePath("Stations CSV path: ", "stations CSV");
        
        try {
            CsvValidatorResult<Station> result = stationService.loadStationsAndCreateAVLTrees(csvPath);
            
            if (result.hasErrors()) {
                System.out.println("Stations import completed with errors:");
                for (String error : result.getErrors()) {
                    System.out.println(" - " + error);
                }
            }
            
            int loadedCount = result.getRecords().size();
            if (loadedCount > 0) {
                System.out.println("Successfully loaded " + loadedCount + " stations.");
                System.out.println("AVL trees created and ready for queries.");
            } else {
                System.out.println("No stations were loaded.");
            }
        } catch (Exception e) {
            System.out.println("Error loading stations CSV: " + e.getMessage());
        }
    }

    private void showAVLTreeStatistics() {
        AVL<StationComparable> latTree = stationService.getLatitudeTree();
        AVL<StationComparable> lonTree = stationService.getLongitudeTree();
        AVL<StationComparable> tzTree = stationService.getTimeZoneGroupTree();
        
        if (latTree == null || lonTree == null || tzTree == null) {
            System.out.println("AVL trees not created. Please load stations CSV first.");
            return;
        }
        
        System.out.println("\n--- AVL Tree Statistics ---");
        System.out.println("Latitude Tree:");
        System.out.println("  - Size: " + latTree.size() + " stations");
        System.out.println("  - Height: " + latTree.height());
        System.out.println("Longitude Tree:");
        System.out.println("  - Size: " + lonTree.size() + " stations");
        System.out.println("  - Height: " + lonTree.height());
        System.out.println("Time Zone Group Tree:");
        System.out.println("  - Size: " + tzTree.size() + " stations");
        System.out.println("  - Height: " + tzTree.height());
    }

    private void queryByTimeZoneGroup() {
        AVL<StationComparable> tzTree = stationService.getTimeZoneGroupTree();
        if (tzTree == null) {
            System.out.println("Time zone group tree not created. Please load stations CSV first.");
            return;
        }
        
        System.out.println("\n--- Query by Time Zone Group ---");
        String timeZoneGroup = getStringInput("Enter time zone group (e.g., CET, WET/GMT): ");
        
        if (timeZoneGroup.isEmpty()) {
            System.out.println("Time zone group cannot be empty.");
            return;
        }
        
        long startTime = System.nanoTime();
        List<Station> results = stationService.queryByTimeZoneGroup(timeZoneGroup);
        long elapsedTime = System.nanoTime() - startTime;
        
        System.out.println("Found " + results.size() + " stations in time zone group '" + timeZoneGroup + "'");
        System.out.println("Query time: " + (elapsedTime / 1_000_000.0) + " ms");
        
        int limit = 20;
        int index = 0;
        while (index < results.size() && index < limit) {
            Station station = results.get(index);
            System.out.println("  - " + station.getName() + " (" + station.getCountry() + ") - " + station.getTimeZoneGroup());
            index++;
        }
        
        if (results.size() > limit) {
            System.out.println("... (" + (results.size() - limit) + " more stations not shown)");
        }
    }

    private void queryByTimeZoneGroupWindow() {
        AVL<StationComparable> tzTree = stationService.getTimeZoneGroupTree();
        if (tzTree == null) {
            System.out.println("Time zone group tree not created. Please load stations CSV first.");
            return;
        }
        
        System.out.println("\n--- Query by Time Zone Group Window ---");
        System.out.println("Enter time zone groups (comma-separated, e.g., CET,WET/GMT): ");
        String input = getStringInput("");
        
        if (input.isEmpty()) {
            System.out.println("Time zone groups cannot be empty.");
            return;
        }
        
        String[] timeZoneGroups = input.split(",");
        for (int i = 0; i < timeZoneGroups.length; i++) {
            timeZoneGroups[i] = timeZoneGroups[i].trim();
        }
        
        long startTime = System.nanoTime();
        List<Station> results = stationService.queryByTimeZoneGroupWindow(timeZoneGroups);
        long elapsedTime = System.nanoTime() - startTime;
        
        System.out.println("Found " + results.size() + " stations in time zone groups: " + String.join(", ", timeZoneGroups));
        System.out.println("Query time: " + (elapsedTime / 1_000_000.0) + " ms");
        
        int limit = 20;
        int index = 0;
        while (index < results.size() && index < limit) {
            Station station = results.get(index);
            System.out.println("  - " + station.getName() + " (" + station.getCountry() + ") - " + station.getTimeZoneGroup());
            index++;
        }
        
        if (results.size() > limit) {
            System.out.println("... (" + (results.size() - limit) + " more stations not shown)");
        }
    }

    private void runSampleQueries() {
        AVL<StationComparable> tzTree = stationService.getTimeZoneGroupTree();
        if (tzTree == null) {
            System.out.println("Time zone group tree not created. Please load stations CSV first.");
            return;
        }
        
        System.out.println("\n=== Sample Queries (USEI06) ===");
        
        // Query 1: Single time zone group
        System.out.println("\nQuery 1: All stations in CET time zone");
        long start1 = System.nanoTime();
        List<Station> results1 = stationService.queryByTimeZoneGroup("CET");
        long elapsed1 = System.nanoTime() - start1;
        System.out.println("  Results: " + results1.size() + " stations");
        System.out.println("  Time: " + (elapsed1 / 1_000_000.0) + " ms");
        if (!results1.isEmpty()) {
            System.out.println("  Sample: " + results1.get(0).getName() + " (" + results1.get(0).getCountry() + ")");
        }
        
        // Query 2: Another time zone group
        System.out.println("\nQuery 2: All stations in WET/GMT time zone");
        long start2 = System.nanoTime();
        List<Station> results2 = stationService.queryByTimeZoneGroup("WET/GMT");
        long elapsed2 = System.nanoTime() - start2;
        System.out.println("  Results: " + results2.size() + " stations");
        System.out.println("  Time: " + (elapsed2 / 1_000_000.0) + " ms");
        if (!results2.isEmpty()) {
            System.out.println("  Sample: " + results2.get(0).getName() + " (" + results2.get(0).getCountry() + ")");
        }
        
        // Query 3: Window query
        System.out.println("\nQuery 3: Stations in time zone window [CET, WET/GMT]");
        long start3 = System.nanoTime();
        List<Station> results3 = stationService.queryByTimeZoneGroupWindow(new String[]{"CET", "WET/GMT"});
        long elapsed3 = System.nanoTime() - start3;
        System.out.println("  Results: " + results3.size() + " stations");
        System.out.println("  Time: " + (elapsed3 / 1_000_000.0) + " ms");
        if (!results3.isEmpty()) {
            System.out.println("  Sample: " + results3.get(0).getName() + " (" + results3.get(0).getCountry() + ") - " + results3.get(0).getTimeZoneGroup());
        }
        
        // Query 4: Another time zone group
        System.out.println("\nQuery 4: All stations in EET time zone");
        long start4 = System.nanoTime();
        List<Station> results4 = stationService.queryByTimeZoneGroup("EET");
        long elapsed4 = System.nanoTime() - start4;
        System.out.println("  Results: " + results4.size() + " stations");
        System.out.println("  Time: " + (elapsed4 / 1_000_000.0) + " ms");
        if (!results4.isEmpty()) {
            System.out.println("  Sample: " + results4.get(0).getName() + " (" + results4.get(0).getCountry() + ")");
        }
        
        // Query 5: Window query with 3 time zones
        System.out.println("\nQuery 5: Stations in time zone window [CET, EET, WET/GMT]");
        long start5 = System.nanoTime();
        List<Station> results5 = stationService.queryByTimeZoneGroupWindow(new String[]{"CET", "EET", "WET/GMT"});
        long elapsed5 = System.nanoTime() - start5;
        System.out.println("  Results: " + results5.size() + " stations");
        System.out.println("  Time: " + (elapsed5 / 1_000_000.0) + " ms");
        if (!results5.isEmpty()) {
            System.out.println("  Sample: " + results5.get(0).getName() + " (" + results5.get(0).getCountry() + ") - " + results5.get(0).getTimeZoneGroup());
        }
    }

    private void showTemporalComplexityAnalysis() {
        System.out.println("\n=== Temporal Complexity Analysis (USEI06) ===");
        System.out.println("\nAVL Tree Operations:");
        System.out.println("  - Insertion: O(log n) - balanced tree guarantees logarithmic height");
        System.out.println("  - Search: O(log n) - binary search in balanced tree");
        System.out.println("  - In-order traversal: O(n) - must visit all n nodes");
        System.out.println("\nQuery Operations:");
        System.out.println("  - Query by time zone group: O(n) - linear scan after in-order traversal");
        System.out.println("    * Could be optimized to O(log n + k) with range queries, but current");
        System.out.println("      implementation uses in-order + filter for simplicity");
        System.out.println("  - Query by time zone group window: O(n) - same as single group query");
        System.out.println("\nTree Construction:");
        System.out.println("  - Building AVL trees: O(n log n) - n insertions, each O(log n)");
        System.out.println("  - Space complexity: O(n) - stores all n stations");
    }

    private void manageStationsSpatialIndex() {
        boolean exit = false;
        while (!exit) {
            System.out.println("\n=== USEI07 - 2D-TREE SPATIAL INDEX ===");
            System.out.println("1. Build 2D-Tree");
            System.out.println("2. Show Tree Statistics");
            System.out.println("3. Range Query");
            System.out.println("0. Back to Sprint 2 Menu");

            int choice = getIntInput("Select an option: ");
            if (choice == -1) {
                choice = 0;
            }

            switch (choice) {
                case 1:
                    build2DTreeOnly();
                    break;
                case 2:
                    showStationsTreeStatistics();
                    break;
                case 3:
                    runStationsRangeQuery();
                    break;
                case 0:
                    exit = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void build2DTreeOnly() {
        AVL<StationComparable> latTree = stationService.getLatitudeTree();
        AVL<StationComparable> lonTree = stationService.getLongitudeTree();
        
        if (latTree == null || lonTree == null) {
            System.out.println("AVL trees not found. Please create them first using USEI06 (option 10).");
            return;
        }
        
        try {
            System.out.println("\n--- Build 2D-tree using existing AVL trees ---");
            stationService.buildBalanced2DTree();
            stationDataLoaded = true;
            System.out.println("2D-tree built successfully using AVL trees from USEI06.");
        } catch (Exception e) {
            System.out.println("Error building 2D-tree: " + e.getMessage());
        }
    }

    private void showStationsTreeStatistics() {
        if (!stationDataLoaded) {
            System.out.println("No station data loaded. Please load a stations CSV first.");
            return;
        }

        System.out.println("\n--- 2D-tree Statistics ---");
        System.out.println("Total stations indexed: " + stationService.getTreeSize());
        System.out.println("Tree height: " + stationService.getTreeHeight());

        Set<Integer> bucketSizes = stationService.getDistinctBucketSizes();
        if (bucketSizes.isEmpty()) {
            System.out.println("No bucket sizes available.");
        } else {
            System.out.print("Distinct bucket sizes (stations sharing coordinates): ");
            Iterator<Integer> iterator = bucketSizes.iterator();
            while (iterator.hasNext()) {
                Integer value = iterator.next();
                System.out.print(value);
                if (iterator.hasNext()) {
                    System.out.print(", ");
                }
            }
            System.out.println();
        }
    }

    private void runStationsRangeQuery() {
        if (!stationDataLoaded) {
            System.out.println("No station data loaded. Please load a stations CSV first.");
            return;
        }

        System.out.println("\n--- Range Query ---");
        double minLat = getRequiredDoubleInput("Minimum latitude: ");
        double maxLat = getRequiredDoubleInput("Maximum latitude: ");
        double minLon = getRequiredDoubleInput("Minimum longitude: ");
        double maxLon = getRequiredDoubleInput("Maximum longitude: ");

        if (minLat > maxLat) {
            double temp = minLat;
            minLat = maxLat;
            maxLat = temp;
        }
        if (minLon > maxLon) {
            double temp = minLon;
            minLon = maxLon;
            maxLon = temp;
        }

        List<Station> results = stationService.rangeQuery(minLat, maxLat, minLon, maxLon);
        System.out.println("Stations found: " + results.size());

        int limit = 20;
        int index = 0;
        while (index < results.size() && index < limit) {
            Station station = results.get(index);
            System.out.println(" - " + station.getName() + " (" + station.getLatitude() + ", " + station.getLongitude() + ") - " + station.getCountry());
            index++;
        }

        if (results.size() > limit) {
            System.out.println("... (" + (results.size() - limit) + " more stations not shown)");
        }
    }

    private String getValidFilePath(String prompt, String fileType) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                System.out.println("Error: This field is required. Please enter a valid path.");
                continue;
            }

            // Check if file exists
            java.io.File file = new java.io.File(input);
            if (!file.exists()) {
                System.out.println("Error: File '" + input + "' does not exist. Please enter a valid " + fileType + " file path.");
                continue;
            }

            if (!file.isFile()) {
                System.out.println("Error: '" + input + "' is not a file. Please enter a valid " + fileType + " file path.");
                continue;
            }

            if (!file.canRead()) {
                System.out.println("Error: Cannot read file '" + input + "'. Please check file permissions.");
                continue;
            }

            return input;
        }
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

    private double getRequiredDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.println("Value required. Please enter a number.");
                    continue;
                }
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }
}