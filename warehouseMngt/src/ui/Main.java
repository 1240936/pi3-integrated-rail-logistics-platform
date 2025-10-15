package ui;

import repositories.WagonCsvLoader;
import repositories.ItemsCsvLoader;
import repositories.BayCsvLoader;
import repositories.CsvValidatorResult;
import domain.Item;
import controller.InventoryService;

import java.util.Scanner;

/**
 
CLI entry point:
loads item master, bay capacities, and incoming boxes from CSV files
demonstrates dispatch of a SKU and relocation of a box
*/
public class Main {
    public static void main(String[] args) {
        InventoryService inv = new InventoryService();
        String warehouseId = "W1";
        int aisle = 1;

        try {
            String itemsPath;
            String baysPath;
            String wagonsPath;
            if (args != null && args.length >= 3) {
                itemsPath = args[0];
                baysPath = args[1];
                wagonsPath = args[2];
            } else {
                try (Scanner sc = new Scanner(System.in)) {
                    // Prompt for CSV paths if not supplied as command-line arguments
                    System.out.println("Enter path to items.csv:");
                    itemsPath = sc.nextLine();
                    System.out.println("Enter path to bays.csv:");
                    baysPath = sc.nextLine();
                    System.out.println("Enter path to wagons.csv:");
                    wagonsPath = sc.nextLine();
                }
            }

            CsvValidatorResult<Item> items = ItemsCsvLoader.load(itemsPath);
            inv.loadItemsForWarehouse(warehouseId, items.getRecords());
            System.out.println("Loaded items: " + items.getRecords().size());
            if (items.hasErrors()) {
                System.out.println("Items import errors:");
                for (String err : items.getErrors()) System.out.println(" - " + err);
            }

            CsvValidatorResult<String> bays = BayCsvLoader.load(baysPath, inv);
            System.out.println("Loaded bays: " + bays.getRecords().size());
            if (bays.hasErrors()) {
                System.out.println("Bays import errors:");
                for (String err : bays.getErrors()) System.out.println(" - " + err);
            }

            var wagons = WagonCsvLoader.load(wagonsPath, warehouseId, aisle, inv);
            System.out.println("Loaded boxes: " + wagons.getRecords().size());
            if (wagons.hasErrors()) {
                System.out.println("Wagons import errors:");
                for (String err : wagons.getErrors()) System.out.println(" - " + err);
            }

            // Demo dispatch: request 100 units for some SKU
            String demoSku = "SKU-001";
            int dispatched = inv.dispatch(warehouseId, demoSku, aisle, 100);
            System.out.println("Dispatched units for " + demoSku + ": " + dispatched);

            // Demo relocation: move a specific boxId to a new bay
            String demoBoxId = "BOX-RELOCATE-1"; // adjust based on CSV contents when testing
            inv.relocate(demoBoxId, warehouseId, aisle, 99);
            System.out.println("Relocated box " + demoBoxId + " to bay 99");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


