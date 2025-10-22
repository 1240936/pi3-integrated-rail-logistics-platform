package main.controller;

import main.domain.*;

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
     * Returns all warehouses in the system.
     */
    public Map<String, Warehouse> getAllWarehouses() {
        return new HashMap<>(warehouses);
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
     * Reorders all boxes in the entire inventory to ensure proper FEFO ordering.
     * This should be called after loading new data to maintain correct FEFO sequence.
     */
    public void reorderInventoryFefo() {
        // Collect all boxes from all bays
        List<Box> allBoxes = new ArrayList<>();
        for (Warehouse warehouse : warehouses.values()) {
            for (Map<Integer, Bay> bays : warehouse.getAisles().values()) {
                for (Bay bay : bays.values()) {
                    allBoxes.addAll(bay.getBoxes());
                }
            }
        }
        
        // Sort all boxes globally using FEFO comparator
        allBoxes.sort(fefoComparator);
        
        // Redistribute boxes back to bays maintaining FEFO order
        redistributeBoxesFefo(allBoxes);
    }

    /**
     * Redistributes boxes back to bays maintaining FEFO order.
     * Boxes are distributed to bays in FEFO order, respecting bay capacities.
     */
    private void redistributeBoxesFefo(List<Box> sortedBoxes) {
        // Clear all bays first
        for (Warehouse warehouse : warehouses.values()) {
            for (Map<Integer, Bay> bays : warehouse.getAisles().values()) {
                for (Bay bay : bays.values()) {
                    bay.getBoxes().clear();
                }
            }
        }
        
        // Redistribute boxes maintaining FEFO order
        for (Box box : sortedBoxes) {
            // Find the first available bay in the same warehouse and aisle
            Bay targetBay = findFirstAvailableBay(box.getWarehouseId(), box.getAisle());
            
            if (targetBay != null) {
                // Update box location to the target bay
                box.setWarehouseId(targetBay.getWarehouseId());
                box.setAisle(targetBay.getAisle());
                box.setBay(targetBay.getBayNumber());
                
                // Add box to the target bay
                targetBay.getBoxes().add(box);
            }
        }
    }
    
    /**
     * Finds the first available bay in the same warehouse and aisle.
     * This ensures boxes are distributed in FEFO order across bays.
     */
    private Bay findFirstAvailableBay(String warehouseId, int aisle) {
        for (int bayNumber = 1; bayNumber <= 50; bayNumber++) {
            Bay bay = getOrCreateBay(warehouseId, aisle, bayNumber);
            if (bay.hasSpace()) {
                return bay;
            }
        }
        return null; // No available bay found
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
                Box removedBox = bay.removeFrontBoxIfEmpty(box);
                if (removedBox == null) {
                    i++;
                }
                // If box was removed, don't increment i since the next box shifts to current position
            }
            cleanupSkuIndex(sku, warehouseId, aisle, bayNumber);
        }
        return quantityUnits - remaining;
    }

    /**
     * Checks if a box with the given ID exists in the system.
     * @param boxId the box ID to search for
     * @return true if the box exists, false otherwise
     */
    public boolean boxExists(String boxId) {
        for (Warehouse w : warehouses.values()) {
            for (Map<Integer, Bay> bays : w.getAisles().values()) {
                for (Bay bay : bays.values()) {
                    for (Box b : bay.getBoxes()) {
                        if (b.getBoxId().equals(boxId)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if a warehouse exists in the system.
     * @param warehouseId the warehouse ID to check
     * @return true if the warehouse exists, false otherwise
     */
    public boolean warehouseExists(String warehouseId) {
        return warehouses.containsKey(warehouseId);
    }

    /**
     * Checks if a bay has capacity for an additional box.
     * @param warehouseId the warehouse ID
     * @param aisle the aisle number
     * @param bayNumber the bay number
     * @return true if the bay has capacity, false otherwise
     */
    public boolean bayHasCapacity(String warehouseId, int aisle, int bayNumber) {
        Bay bay = getOrCreateBay(warehouseId, aisle, bayNumber);
        return bay.hasSpace();
    }

    /**
     * Checks if a specific bay exists in the system (without creating it).
     * @param warehouseId the warehouse ID
     * @param aisle the aisle number
     * @param bayNumber the bay number
     * @return true if the bay exists, false otherwise
     */
    public boolean bayExists(String warehouseId, int aisle, int bayNumber) {
        Warehouse warehouse = warehouses.get(warehouseId);
        if (warehouse == null) {
            return false;
        }
        
        Map<Integer, Map<Integer, Bay>> aisles = warehouse.getAisles();
        Map<Integer, Bay> bays = aisles.get(aisle);
        if (bays == null) {
            return false;
        }
        
        return bays.containsKey(bayNumber);
    }

    /**
     * Checks if a specific aisle exists in a warehouse.
     * @param warehouseId the warehouse ID
     * @param aisle the aisle number
     * @return true if the aisle exists, false otherwise
     */
    public boolean aisleExists(String warehouseId, int aisle) {
        Warehouse warehouse = warehouses.get(warehouseId);
        if (warehouse == null) {
            return false;
        }
        
        Map<Integer, Map<Integer, Bay>> aisles = warehouse.getAisles();
        return aisles.containsKey(aisle);
    }

    /**
     * Moves a box identified by boxId to a new location, updating indexes and FEFO order.
     * If the box is not found, the method returns silently.
     */
    public void relocate(String boxId, String newWarehouseId, int newAisle, int newBayNumber) {
        Box found = null;
        Bay originBay = null;

        // Search for the box
        for (Warehouse w : warehouses.values()) {
            for (Map<Integer, Bay> bays : w.getAisles().values()) {
                for (Bay bay : bays.values()) {
                    for (Box b : bay.getBoxes()) {
                        if (b.getBoxId().equals(boxId)) {
                            found = b;
                            originBay = bay;
                            break;
                        }
                    }
                    if (found != null) break;
                }
                if (found != null) break;
            }
            if (found != null) break;
        }

        if (found == null) {
            throw new IllegalArgumentException("Box with ID '" + boxId + "' not found.");
        }

        // Check if destination bay exists
        if (!bayExists(newWarehouseId, newAisle, newBayNumber)) {
            throw new IllegalStateException("Destination bay does not exist: " + newWarehouseId + "/" + newAisle + "/" + newBayNumber);
        }

        // Remove from origin
        originBay.getBoxes().remove(found);
        boxIdsInWarehouse.remove(originBay.getWarehouseId() + "#" + found.getBoxId());
        cleanupSkuIndex(found.getSku(), originBay.getWarehouseId(), originBay.getAisle(), originBay.getBayNumber());

        // Update box location
        found.setWarehouseId(newWarehouseId);
        found.setAisle(newAisle);
        found.setBay(newBayNumber);

        // Insert into destination (FEFO)
        insertBox(found);
    }



    /**
     * Plans allocations for a list of order lines without mutating inventory.
     * Orders are sorted by priority ASC, dueDate ASC, orderId ASC; lines by lineNo ASC.
     * Allocation traverses boxes by FEFO within each bay and bays by ascending bay number within the same aisle.
     */

    public AllocationResult planAllocations(String warehouseId,
                                            List<OrderLine> orderLines,
                                            AllocationMode mode,
                                            int aisleFilter) {

        List<OrderLine> lines = new ArrayList<>(orderLines);

        // Ordena as linhas pela prioridade, data limite e ID
        lines.sort(Comparator
                .comparingInt(OrderLine::getPriority)
                .thenComparing(OrderLine::getDueDate)
                .thenComparing(OrderLine::getOrderId)
                .thenComparingInt(OrderLine::getLineNo));

        List<LineEligibility> eligibilities = new ArrayList<>();
        List<AllocationRow> allocations = new ArrayList<>();

        // Percorre cada linha da encomenda
        for (OrderLine line : lines) {

            int remaining = line.getRequestedQty(); // quantidade que ainda falta alocar
            List<AllocationRow> lineAllocs = new ArrayList<>();

            // Procura os bays do SKU
            Map<Integer, SortedSet<Integer>> byAisle = skuIndex
                    .getOrDefault(line.getSku(), Collections.emptyMap())
                    .getOrDefault(warehouseId, Collections.emptyMap());

            SortedSet<Integer> bayNumbers = byAisle.getOrDefault(aisleFilter, new TreeSet<>());

            // Percorre todos os bays desse corredor
            for (Integer bayNumber : bayNumbers) {
                if (remaining <= 0) break;

                Bay bay = getOrCreateBay(warehouseId, aisleFilter, bayNumber);
                List<Box> boxes = bay.getBoxes(); // caixas dentro do bay

                for (Box box : boxes) {
                    if (remaining <= 0) break;
                    if (!box.getSku().equals(line.getSku())) continue; // ignora se for outro SKU

                    // Define quantas unidades tirar desta caixa
                    int take = Math.min(remaining, box.getQuantity());
                    if (take <= 0) continue;

                    lineAllocs.add(new AllocationRow(
                            line.getOrderId(),
                            line.getLineNo(),
                            line.getSku(),
                            take,
                            box.getBoxId(),
                            aisleFilter,
                            bayNumber));

                    remaining -= take; // atualiza o que falta alocar
                }
            }

            int allocated = line.getRequestedQty() - remaining;

            if (mode == AllocationMode.STRICT) {
                // Modo strict: só é válido se conseguir tudo
                if (remaining == 0) {
                    eligibilities.add(new LineEligibility(
                            line.getOrderId(),
                            line.getLineNo(),
                            line.getSku(),
                            line.getRequestedQty(),
                            allocated,
                            LineStatus.ELIGIBLE));
                    allocations.addAll(lineAllocs);
                } else {
                    eligibilities.add(new LineEligibility(
                            line.getOrderId(),
                            line.getLineNo(),
                            line.getSku(),
                            line.getRequestedQty(),
                            0,
                            LineStatus.UNDISPATCHABLE));
                }

            } else { // Modo PARCIAL
                if (allocated == 0) {
                    // Nenhuma unidade encontrada
                    eligibilities.add(new LineEligibility(
                            line.getOrderId(),
                            line.getLineNo(),
                            line.getSku(),
                            line.getRequestedQty(),
                            0,
                            LineStatus.UNDISPATCHABLE));
                } else if (remaining == 0) {
                    // Tudo alocado
                    eligibilities.add(new LineEligibility(
                            line.getOrderId(),
                            line.getLineNo(),
                            line.getSku(),
                            line.getRequestedQty(),
                            allocated,
                            LineStatus.ELIGIBLE));
                    allocations.addAll(lineAllocs);
                } else {
                    eligibilities.add(new LineEligibility(
                            line.getOrderId(),
                            line.getLineNo(),
                            line.getSku(),
                            line.getRequestedQty(),
                            allocated,
                            LineStatus.PARTIAL));
                    allocations.addAll(lineAllocs);
                }
            }
        }
        return new AllocationResult(eligibilities, allocations);
    }

}


