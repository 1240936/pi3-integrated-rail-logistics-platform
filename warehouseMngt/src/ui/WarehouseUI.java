package ui;

import repositories.*;
import domain.*;
import controller.InventoryService;

import java.util.*;

/**
 * Interactive UI for warehouse management system.
 * Provides menu-driven interface for loading CSV data and managing inventory.
 */
public class WarehouseUI {
    private InventoryService inventoryService;
    private Scanner scanner;
    private boolean dataLoaded = false;
    private String currentWarehouseId = "W1";
    private int currentAisle = 1;

    public WarehouseUI() {
        this.inventoryService = new InventoryService();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("=== Warehouse Management System ===");
        System.out.println("Welcome to the warehouse management interface!");
        
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
                        System.out.println("Please load CSV data first!");
                    }
                    break;
                case 3:
                    if (dataLoaded) {
                        performDispatch();
                    } else {
                        System.out.println("Please load CSV data first!");
                    }
                    break;
                case 4:
                    if (dataLoaded) {
                        performRelocation();
                    } else {
                        System.out.println("Please load CSV data first!");
                    }
                    break;
                case 5:
                    if (dataLoaded) {
                        planAllocations();
                    } else {
                        System.out.println("Please load CSV data first!");
                    }
                    break;
                case 6:
                    changeWarehouseSettings();
                    break;
                case 0:
                    System.out.println("Thank you for using the Warehouse Management System!");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
            
            // No need to wait for Enter - just continue to next menu iteration
        }
    }

    private void showMainMenu() {
        System.out.println("\n=== MAIN MENU ===");
        System.out.println("1. Load CSV Data");
        System.out.println("2. View All Inventory");
        System.out.println("3. Perform Dispatch");
        System.out.println("4. Relocate Box");
        System.out.println("5. Plan Allocations");
        System.out.println("6. Change Warehouse Settings");
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

            System.out.println("Loading data...");

            // Load items
            CsvValidatorResult<Item> items = ItemsCsvLoader.load(itemsPath);
            inventoryService.loadItemsForWarehouse(currentWarehouseId, items.getRecords());
            System.out.println("✓ Loaded items: " + items.getRecords().size());
            if (items.hasErrors()) {
                System.out.println("Items import errors:");
                for (String err : items.getErrors()) System.out.println(" - " + err);
            }

            // Load bays
            CsvValidatorResult<String> bays = BayCsvLoader.load(baysPath, inventoryService);
            System.out.println("✓ Loaded bays: " + bays.getRecords().size());
            if (bays.hasErrors()) {
                System.out.println("Bays import errors:");
                for (String err : bays.getErrors()) System.out.println(" - " + err);
            }

            // Load wagons (boxes)
            var wagons = WagonCsvLoader.load(wagonsPath, currentWarehouseId, currentAisle, inventoryService);
            System.out.println("✓ Loaded boxes: " + wagons.getRecords().size());
            if (wagons.hasErrors()) {
                System.out.println("Wagons import errors:");
                for (String err : wagons.getErrors()) System.out.println(" - " + err);
            }

            // Load orders
            var orders = OrdersCsvLoader.load(ordersPath);
            System.out.println("✓ Loaded orders: " + orders.getRecords().size());
            if (orders.hasErrors()) {
                System.out.println("Orders import errors:");
                for (String err : orders.getErrors()) System.out.println(" - " + err);
            }

            // Load order lines
            java.util.Map<String, OrderHeader> headers = new java.util.HashMap<>();
            for (OrderHeader oh : orders.getRecords()) headers.put(oh.getOrderId(), oh);

            var orderLines = OrderLinesCsvLoader.load(orderLinesPath, headers);
            System.out.println("✓ Loaded order lines: " + orderLines.getRecords().size());
            if (orderLines.hasErrors()) {
                System.out.println("Order lines import errors:");
                for (String err : orderLines.getErrors()) System.out.println(" - " + err);
            }

            dataLoaded = true;
            System.out.println("\n✓ All CSV data loaded successfully!");

        } catch (Exception e) {
            System.out.println("Error loading CSV data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void viewInventory() {
        System.out.println("\n=== VIEW INVENTORY ===");
        System.out.println("Showing inventory for all warehouses...");
        System.out.println("=" + "=".repeat(60));

        try {
            var allWarehouses = inventoryService.getAllWarehouses();

            if (allWarehouses.isEmpty()) {
                System.out.println("No warehouses found in the system.");
                return;
            }

            for (var warehouseEntry : allWarehouses.entrySet()) {
                String warehouseId = warehouseEntry.getKey();
                Warehouse warehouse = warehouseEntry.getValue();
                var aisles = warehouse.getAisles();

                System.out.println("\n🏢 WAREHOUSE: " + warehouseId);
                System.out.println("=" + "=".repeat(50));

                if (aisles.isEmpty()) {
                    System.out.println("  (No inventory in this warehouse)");
                    continue;
                }

                for (var aisleEntry : aisles.entrySet()) {
                    int aisleNum = aisleEntry.getKey();
                    var bays = aisleEntry.getValue();
                    
                    System.out.println("\n  📦 Aisle " + aisleNum + ":");
                    System.out.println("  " + "-".repeat(40));

                    for (var bayEntry : bays.entrySet()) {
                        int bayNum = bayEntry.getKey();
                        Bay bay = bayEntry.getValue();
                        var boxes = bay.getBoxes();

                        System.out.printf("    Bay %d (Capacity: %d, Used: %d):%n", 
                            bayNum, bay.getCapacityBoxes(), boxes.size());
                        
                        if (boxes.isEmpty()) {
                            System.out.println("      (empty)");
                        } else {
                            for (Box box : boxes) {
                                System.out.printf("      📦 Box %s: SKU=%s, Qty=%d, Expiry=%s, Received=%s%n",
                                    box.getBoxId(), box.getSku(), box.getQuantity(), 
                                    box.getExpiryDate() != null ? box.getExpiryDate() : "N/A",
                                    box.getReceivedAt());
                            }
                        }
                    }
                }
            }

            System.out.println("\n✅ Inventory view complete for all warehouses.");

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
            System.out.println("✓ Successfully dispatched " + dispatched + " units of SKU " + sku + 
                             " from warehouse " + warehouseId + ", aisle " + aisle);
            
            if (dispatched < quantity) {
                System.out.println("⚠ Warning: Only " + dispatched + " out of " + quantity + " requested units were available.");
            }

        } catch (Exception e) {
            System.out.println("Error performing dispatch: " + e.getMessage());
        }
    }

    private void performRelocation() {
        System.out.println("\n=== RELOCATE BOX ===");
        
        String boxId = getStringInput("Enter box ID to relocate: ");
        String newWarehouseId = getStringInput("Enter new warehouse ID: ");
        int newAisle = getIntInput("Enter new aisle number: ");
        int newBay = getIntInput("Enter new bay number: ");

        try {
            inventoryService.relocate(boxId, newWarehouseId, newAisle, newBay);
            System.out.println("✓ Successfully relocated box " + boxId + 
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
        AllocationMode mode = (modeChoice == 1) ? AllocationMode.STRICT : AllocationMode.PARTIAL;

        try {
            // For demo purposes, we'll create some sample order lines
            // In a real system, these would come from the loaded order data
            System.out.println("Note: This demo uses sample order lines. In production, use loaded order data.");
            
            // Create sample order lines for demonstration
            List<OrderLine> sampleOrderLines = createSampleOrderLines();
            
            AllocationResult result = inventoryService.planAllocations(warehouseId, sampleOrderLines, mode, aisle);
            
            System.out.println("\n=== ALLOCATION RESULTS ===");
            System.out.println("Eligibility results:");
            for (LineEligibility e : result.getEligibilities()) {
                System.out.printf("  %s#%d %s: requested=%d, allocated=%d, status=%s%n",
                    e.getOrderId(), e.getLineNo(), e.getSku(), 
                    e.getRequestedQty(), e.getAllocatedQty(), e.getStatus());
            }
            
            System.out.println("\nAllocations:");
            for (AllocationRow r : result.getAllocations()) {
                System.out.printf("  %s#%d %s qty=%d from box=%s at %d/%d%n",
                    r.getOrderId(), r.getLineNo(), r.getSku(), r.getQty(),
                    r.getBoxId(), r.getAisle(), r.getBay());
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

        System.out.println("✓ Settings updated. Current warehouse: " + currentWarehouseId + ", Aisle: " + currentAisle);
    }

    private List<OrderLine> createSampleOrderLines() {
        List<OrderLine> orderLines = new ArrayList<>();
        
        // Create sample order lines for demonstration
        orderLines.add(new OrderLine("ORD001", 1, "SKU001", 10, 1, java.time.LocalDate.now().plusDays(7)));
        orderLines.add(new OrderLine("ORD001", 2, "SKU002", 5, 2, java.time.LocalDate.now().plusDays(7)));
        orderLines.add(new OrderLine("ORD002", 1, "SKU001", 15, 1, java.time.LocalDate.now().plusDays(14)));
        
        return orderLines;
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
                    return -1; // Use -1 for empty input instead of 0
                }
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }
}
