package wms.service;

import wms.model.BoxFefoComparator;
import wms.model.Warehouse;
import wms.model.Item;

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
}
