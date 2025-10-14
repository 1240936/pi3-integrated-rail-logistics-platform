package wms.service;

import wms.model.BoxFefoComparator;
import wms.model.Warehouse;
import wms.model.Item;
import wms.model.Bay;

import java.util.*;

/**
 * Core inventory management service for warehouses.
 *
 * Responsibilities:
 * - manage warehouses, aisles, and bays in memory
 * - insert boxes using FEFO ordering
 * - maintain an index from SKU -> warehouse -> aisle -> set of bay numbers
 * - dispatch quantities per SKU from bays following FEFO ordering
 * - relocate boxes across bays/warehouses while keeping indexes consistent
 */
public class InventoryService {
    private final Map<String, Warehouse> warehouses; // warehouseId -> Warehouse
    // sku -> warehouseId -> aisle -> sorted set of bay numbers holding that sku
    private final Map<String, Map<String, Map<Integer, SortedSet<Integer>>>> skuIndex;
    private final Map<String, Map<String, Item>> itemsByWarehouse; // warehouseId -> sku -> Item (for validation/reference)
    private final Set<String> boxIdsInWarehouse; // unique box ids per warehouse (combined key warehouseId#boxId)
    private final BoxFefoComparator fefoComparator = new BoxFefoComparator();

    /**
     * Constructs a new empty inventory service with in-memory indexes.
     */
    public InventoryService() {
        this.warehouses = new HashMap<>();
        this.skuIndex = new HashMap<>();
        this.itemsByWarehouse = new HashMap<>();
        this.boxIdsInWarehouse = new HashSet<>();
    }
    /**
     * Returns an existing warehouse by id or creates a new one if missing.
     */
    public Warehouse getOrCreateWarehouse(String warehouseId) {
        Warehouse warehouse = warehouses.get(warehouseId);
        if (warehouse == null) {
            warehouse = new Warehouse(warehouseId);
            warehouses.put(warehouseId, warehouse);
        }
        return warehouse;
    }

    /**
     * Returns an existing bay or creates it (and its aisle) as needed.
     */
    public Bay getOrCreateBay(String warehouseId, int aisle, int bayNumber) {
        Warehouse warehouse = getOrCreateWarehouse(warehouseId);
        Map<Integer, Map<Integer, Bay>> aisles = warehouse.getAisles();
        Map<Integer, Bay> bays = aisles.get(aisle);
        if (bays == null) {
            bays = new TreeMap<>();
            aisles.put(aisle, bays);
        }
        Bay bay = bays.get(bayNumber);
        if (bay == null) {
            bay = new Bay(warehouseId, aisle, bayNumber, fefoComparator);
            bays.put(bayNumber, bay);
        }
        return bay;
    }

    /**
     * Sets the maximum number of boxes a bay can contain. Capacity 0 means unlimited/unconfigured.
     */
    public void defineBayCapacity(String warehouseId, int aisle, int bayNumber, int capacityBoxes) {
        Bay bay = getOrCreateBay(warehouseId, aisle, bayNumber);
        bay.setCapacityBoxes(capacityBoxes);
    }

    public void loadItemsForWarehouse(String warehouseId, Iterable<Item> items) {
        Map<String, Item> bySku = itemsByWarehouse.get(warehouseId);
        if (bySku == null) {
            bySku = new HashMap<>();
            itemsByWarehouse.put(warehouseId, bySku);
        }
        for (Item item : items) {
            bySku.put(item.getSku(), item);
        }
    }
}
