package wms.service;

import wms.model.*;

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

    /**
     * Inserts a box into its bay using FEFO ordering and updates SKU index.
     * Enforces uniqueness of boxId within a warehouse.
     */
    public void insertBox(Box box) {
        // enforce unique boxId per warehouse
        String boxKey = box.getWarehouseId() + "#" + box.getBoxId();
        if (boxIdsInWarehouse.contains(boxKey)) {
            throw new IllegalArgumentException("Duplicate boxId in warehouse: " + box.getBoxId());
        }
        Bay bay = getOrCreateBay(box.getWarehouseId(), box.getAisle(), box.getBay());
        if (!bay.hasSpace()) {
            throw new IllegalStateException("Bay is at capacity: " + box.getWarehouseId() + "/" + box.getAisle() + "/" + box.getBay());
        }
        bay.insertBoxFefo(box);
        indexSkuBox(box.getSku(), box.getWarehouseId(), box.getAisle(), bay.getBayNumber());
        boxIdsInWarehouse.add(boxKey);
    }

    /**
     * Adds a bay number to the SKU index for quick lookup by SKU/warehouse/aisle.
     */
    private void indexSkuBox(String sku, String warehouseId, int aisle, int bayNumber) {
        Map<String, Map<Integer, SortedSet<Integer>>> byWarehouse = skuIndex.get(sku);
        if (byWarehouse == null) {
            byWarehouse = new HashMap<>();
            skuIndex.put(sku, byWarehouse);
        }
        Map<Integer, SortedSet<Integer>> byAisle = byWarehouse.get(warehouseId);
        if (byAisle == null) {
            byAisle = new HashMap<>();
            byWarehouse.put(warehouseId, byAisle);
        }
        SortedSet<Integer> bays = byAisle.get(aisle);
        if (bays == null) {
            bays = new TreeSet<>();
            byAisle.put(aisle, bays);
        }
        bays.add(bayNumber);
    }

    /**
     * Returns true if the SKU exists in the loaded item master data for the warehouse.
     */
    public boolean isKnownSku(String warehouseId, String sku) {
        Map<String, Item> bySku = itemsByWarehouse.get(warehouseId);
        return bySku != null && bySku.containsKey(sku);
    }

    /**
     * Removes a bay number from the SKU index only if the bay no longer holds any box of that SKU.
     * Keeps empty bays in the index if other boxes with the SKU still exist there (policy choice).
     */
    private void cleanupSkuIndex(String sku, String warehouseId, int aisle, int bayNumber) {
        Map<String, Map<Integer, SortedSet<Integer>>> byWarehouse = skuIndex.getOrDefault(sku, Collections.emptyMap());
        Map<Integer, SortedSet<Integer>> byAisle = byWarehouse.getOrDefault(warehouseId, Collections.emptyMap());
        SortedSet<Integer> bays = byAisle.getOrDefault(aisle, null);
        if (bays == null) return;
        // keep bay even if empty per requirements; only remove from index if truly no boxes of SKU exist there
        Bay bay = getOrCreateBay(warehouseId, aisle, bayNumber);
        boolean hasSku = bay.getBoxes().stream().anyMatch(b -> b.getSku().equals(sku));
        if (!hasSku) {
            bays.remove(bayNumber);
        }
    }

    /**
     * Dispatches up to quantityUnits of a SKU from the specified warehouse/aisle.
     * Consumes from bays in increasing bay number order and from boxes in FEFO order.
     * @return number of units actually dispatched.
     */
    public int dispatch(String warehouseId, String sku, int aisle, int quantityUnits) {
        Map<Integer, SortedSet<Integer>> byAisle = skuIndex
                .getOrDefault(sku, Collections.emptyMap()) // procura o sku, se nao tiver retorna um mapa vazio de modo a evitar nullPointer
                .getOrDefault(warehouseId, Collections.emptyMap());
        SortedSet<Integer> bayNumbers = byAisle.getOrDefault(aisle, new TreeSet<>());
        int remaining = quantityUnits;
        for (Integer bayNumber : new ArrayList<>(bayNumbers)) {
            if (remaining <= 0) break;
            Bay bay = getOrCreateBay(warehouseId, aisle, bayNumber);
            List<Box> boxes = bay.getBoxes();
            int i = 0;
            while (i < boxes.size() && remaining > 0) {
                Box box = boxes.get(i);
                if (!box.getSku().equals(sku)) {
                    i++; continue; }
                int use = Math.min(remaining, box.getQuantity());
                box.setQuantity(box.getQuantity() - use);
                remaining -= use;
                if (box.getQuantity() == 0) {
                    boxes.remove(i);
                } else {
                    i++;
                }
            }
            cleanupSkuIndex(sku, warehouseId, aisle, bayNumber);
        }
        return quantityUnits - remaining;
    }
}
