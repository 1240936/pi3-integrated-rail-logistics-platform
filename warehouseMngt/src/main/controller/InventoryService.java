package main.controller;

import main.domain.*;

import java.util.*;

/**
 * Core inventory management service for warehouses.
 * Manages warehouse structures and box storage, enforcing FEFO ordering for expiration control.
 * Inventory is kept fully in memory for fast lookups and operations.
 *
 * Key responsibilities:
 * • Manage warehouses, aisles, and bays
 * • Insert boxes using FEFO ordering
 * • Maintain a SKU lookup index for fast dispatching and allocation
 * • Dispatch units from bays in FEFO sequence
 * • Relocate boxes while keeping index and FEFO ordering consistent
 */
public class InventoryService {
    private final Map<String, Warehouse> warehouses; // warehouseId -> Warehouse
    private final Map<String, Map<String, Map<Integer, SortedSet<Integer>>>> skuIndex; // sku -> warehouse -> aisle -> bay numbers
    private final Map<String, Map<String, Item>> itemsByWarehouse; // warehouseId -> sku -> Item
    private final Set<String> boxIdsInWarehouse; // unique key "warehouseId#boxId"
    private final BoxFefoComparator fefoComparator = new BoxFefoComparator();

    /**
     * Constructs a new inventory service with empty internal indexes.
     */
    public InventoryService() {
        this.warehouses = new HashMap<>();
        this.skuIndex = new HashMap<>();
        this.itemsByWarehouse = new HashMap<>();
        this.boxIdsInWarehouse = new HashSet<>();
    }

    /**
     * Retrieves an existing warehouse or creates a new one if it does not exist.
     *
     * @param warehouseId warehouse identifier
     * @return the existing or newly created warehouse
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
     * Provides a snapshot of all registered warehouses.
     *
     * @return a new map containing all warehouses
     */
    public Map<String, Warehouse> getAllWarehouses() {
        return new HashMap<>(warehouses);
    }

    /**
     * Retrieves a bay, creating the aisle or bay if they do not exist yet.
     *
     * @param warehouseId warehouse identifier
     * @param aisle aisle number
     * @param bayNumber bay number
     * @return existing or newly created bay
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
     * Sets the maximum number of boxes a bay can hold.
     * A capacity of zero means unlimited.
     *
     * @param warehouseId warehouse identifier
     * @param aisle aisle number
     * @param bayNumber bay number
     * @param capacityBoxes maximum allowed boxes
     */
    public void defineBayCapacity(String warehouseId, int aisle, int bayNumber, int capacityBoxes) {
        Bay bay = getOrCreateBay(warehouseId, aisle, bayNumber);
        bay.setCapacityBoxes(capacityBoxes);
    }

    /**
     * Loads or updates item master data for a warehouse.
     *
     * @param warehouseId warehouse identifier
     * @param items catalog of items to register
     */
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
     * Inserts a box into the appropriate bay enforcing FEFO ordering.
     * Indexes are updated and duplicates are rejected.
     *
     * @param box box to insert
     * @throws IllegalArgumentException if boxId already exists in the warehouse
     * @throws IllegalStateException if destination bay is at capacity
     */
    public void insertBox(Box box) {
        String boxKey = box.getWarehouseId() + "#" + box.getBoxId();
        if (boxIdsInWarehouse.contains(boxKey)) {
            throw new IllegalArgumentException("Duplicate boxId in warehouse: " + box.getBoxId());
        }
        Bay bay = getOrCreateBay(box.getWarehouseId(), box.getAisle(), box.getBay());
        if (!bay.hasSpace()) {
            throw new IllegalStateException("Bay is at capacity: " +
                    box.getWarehouseId() + "/" + box.getAisle() + "/" + box.getBay());
        }
        bay.insertBoxFefo(box);
        indexSkuBox(box.getSku(), box.getWarehouseId(), box.getAisle(), bay.getBayNumber());
        boxIdsInWarehouse.add(boxKey);
    }

    /**
     * Globally reorders all boxes in the system using FEFO ordering and redistributes them
     * back into bays respecting bay capacity.
     */
    public void reorderInventoryFefo() {
        List<Box> allBoxes = new ArrayList<>();
        for (Warehouse warehouse : warehouses.values()) {
            for (Map<Integer, Bay> bays : warehouse.getAisles().values()) {
                for (Bay bay : bays.values()) {
                    allBoxes.addAll(bay.getBoxes());
                }
            }
        }
        allBoxes.sort(fefoComparator);
        redistributeBoxesFefo(allBoxes);
    }

    /**
     * Dispatches units of a SKU from a specific aisle.
     * Traverses bays in increasing bay number and boxes in FEFO order.
     *
     * @param warehouseId warehouse to dispatch from
     * @param sku SKU to dispatch
     * @param aisle aisle number to search
     * @param quantityUnits number of units requested
     * @return actual number of units dispatched
     */
    public int dispatch(String warehouseId, String sku, int aisle, int quantityUnits) {
        Map<Integer, SortedSet<Integer>> byAisle = skuIndex
                .getOrDefault(sku, Collections.emptyMap())
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
                if (removedBox == null) i++;
            }
            cleanupSkuIndex(sku, warehouseId, aisle, bayNumber);
        }
        return quantityUnits - remaining;
    }

    /**
     * Checks if the given SKU is registered in the specified warehouse.
     *
     * @param warehouseId warehouse identifier
     * @param sku SKU identifier
     * @return true if SKU is known
     */
    public boolean isKnownSku(String warehouseId, String sku) {
        Map<String, Item> bySku = itemsByWarehouse.get(warehouseId);
        return bySku != null && bySku.containsKey(sku);
    }

    /**
     * Checks if a box with the given ID exists anywhere in the system.
     *
     * @param boxId box identifier
     * @return true if present
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
     * Checks if a warehouse is registered.
     *
     * @param warehouseId warehouse identifier
     * @return true if exists
     */
    public boolean warehouseExists(String warehouseId) {
        return warehouses.containsKey(warehouseId);
    }

    /**
     * Verifies that a bay has space for an additional box.
     *
     * @param warehouseId warehouse identifier
     * @param aisle aisle number
     * @param bayNumber bay number
     * @return true if space is available
     */
    public boolean bayHasCapacity(String warehouseId, int aisle, int bayNumber) {
        Bay bay = getOrCreateBay(warehouseId, aisle, bayNumber);
        return bay.hasSpace();
    }

    /**
     * Checks if a bay is already defined in the system without creating it.
     *
     * @param warehouseId warehouse identifier
     * @param aisle aisle number
     * @param bayNumber bay number
     * @return true if bay exists
     */
    public boolean bayExists(String warehouseId, int aisle, int bayNumber) {
        Warehouse warehouse = warehouses.get(warehouseId);
        if (warehouse == null) return false;
        Map<Integer, Map<Integer, Bay>> aisles = warehouse.getAisles();
        Map<Integer, Bay> bays = aisles.get(aisle);
        return bays != null && bays.containsKey(bayNumber);
    }

    /**
     * Checks if an aisle is already present in a warehouse.
     *
     * @param warehouseId warehouse identifier
     * @param aisle aisle number
     * @return true if aisle exists
     */
    public boolean aisleExists(String warehouseId, int aisle) {
        Warehouse warehouse = warehouses.get(warehouseId);
        return warehouse != null && warehouse.getAisles().containsKey(aisle);
    }

    /**
     * Relocates a box to a new location updating indexes and FEFO order.
     *
     * @param boxId box identifier
     * @param newWarehouseId destination warehouse
     * @param newAisle destination aisle
     * @param newBayNumber destination bay
     * @throws IllegalArgumentException if the box cannot be found
     * @throws IllegalStateException if the target bay does not exist
     */
    public void relocate(String boxId, String newWarehouseId, int newAisle, int newBayNumber) {
        Box found = null;
        Bay originBay = null;

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
        if (!bayExists(newWarehouseId, newAisle, newBayNumber)) {
            throw new IllegalStateException("Destination bay does not exist: " +
                    newWarehouseId + "/" + newAisle + "/" + newBayNumber);
        }

        originBay.getBoxes().remove(found);
        boxIdsInWarehouse.remove(originBay.getWarehouseId() + "#" + found.getBoxId());
        cleanupSkuIndex(found.getSku(), originBay.getWarehouseId(),
                originBay.getAisle(), originBay.getBayNumber());

        found.setWarehouseId(newWarehouseId);
        found.setAisle(newAisle);
        found.setBay(newBayNumber);

        insertBox(found);
    }

    /**
     * Plans allocations of order lines without modifying inventory.
     * Allocation order:
     * 1. Orders: priority ASC, due date ASC, orderId ASC
     * 2. Order lines: line number ASC
     * Consumes from FEFO-order boxes inside ascending bay numbers.
     *
     * @param warehouseId warehouse to check stocks against
     * @param orderLines order lines to evaluate
     * @param mode strict or partial allocation mode
     * @param aisleFilter aisle to search in
     * @return eligibility per line and allocation rows
     */
    public AllocationResult planAllocations(String warehouseId,
                                            List<OrderLine> orderLines,
                                            AllocationMode mode,
                                            int aisleFilter) {
        List<OrderLine> lines = new ArrayList<>(orderLines);

        lines.sort(Comparator
                .comparingInt(OrderLine::getPriority)
                .thenComparing(OrderLine::getDueDate)
                .thenComparing(OrderLine::getOrderId)
                .thenComparingInt(OrderLine::getLineNo));

        List<LineEligibility> eligibilities = new ArrayList<>();
        List<AllocationRow> allocations = new ArrayList<>();

        for (OrderLine line : lines) {
            int remaining = line.getRequestedQty();
            List<AllocationRow> lineAllocs = new ArrayList<>();

            Map<Integer, SortedSet<Integer>> byAisle = skuIndex
                    .getOrDefault(line.getSku(), Collections.emptyMap())
                    .getOrDefault(warehouseId, Collections.emptyMap());

            SortedSet<Integer> bayNumbers = byAisle.getOrDefault(aisleFilter, new TreeSet<>());

            for (Integer bayNumber : bayNumbers) {
                if (remaining <= 0) break;
                Bay bay = getOrCreateBay(warehouseId, aisleFilter, bayNumber);
                List<Box> boxes = bay.getBoxes();

                for (Box box : boxes) {
                    if (remaining <= 0) break;
                    if (!box.getSku().equals(line.getSku())) continue;
                    int take = Math.min(remaining, box.getQuantity());
                    if (take <= 0) continue;

                    lineAllocs.add(new AllocationRow(
                            line.getOrderId(), line.getLineNo(),
                            line.getSku(), take,
                            box.getBoxId(), aisleFilter, bayNumber));
                    remaining -= take;
                }
            }

            int allocated = line.getRequestedQty() - remaining;

            if (mode == AllocationMode.STRICT) {
                if (remaining == 0) {
                    eligibilities.add(new LineEligibility(
                            line.getOrderId(), line.getLineNo(),
                            line.getSku(), line.getRequestedQty(),
                            allocated, LineStatus.ELIGIBLE));
                    allocations.addAll(lineAllocs);
                } else {
                    eligibilities.add(new LineEligibility(
                            line.getOrderId(), line.getLineNo(),
                            line.getSku(), line.getRequestedQty(),
                            0, LineStatus.UNDISPATCHABLE));
                }
            } else {
                if (allocated == 0) {
                    eligibilities.add(new LineEligibility(
                            line.getOrderId(), line.getLineNo(),
                            line.getSku(), line.getRequestedQty(),
                            0, LineStatus.UNDISPATCHABLE));
                } else if (remaining == 0) {
                    eligibilities.add(new LineEligibility(
                            line.getOrderId(), line.getLineNo(),
                            line.getSku(), line.getRequestedQty(),
                            allocated, LineStatus.ELIGIBLE));
                    allocations.addAll(lineAllocs);
                } else {
                    eligibilities.add(new LineEligibility(
                            line.getOrderId(), line.getLineNo(),
                            line.getSku(), line.getRequestedQty(),
                            allocated, LineStatus.PARTIAL));
                    allocations.addAll(lineAllocs);
                }
            }
        }

        return new AllocationResult(eligibilities, allocations);
    }

    /* ---------------- Internal Helper Methods ---------------- */

    /**
     * Redistributes all boxes in FEFO order back into available bays.
     * Existing bay contents are cleared first.
     *
     * @param sortedBoxes list of boxes sorted in FEFO order
     */
    private void redistributeBoxesFefo(List<Box> sortedBoxes) {
        for (Warehouse warehouse : warehouses.values()) {
            for (Map<Integer, Bay> bays : warehouse.getAisles().values()) {
                for (Bay bay : bays.values()) {
                    bay.getBoxes().clear();
                }
            }
        }
        for (Box box : sortedBoxes) {
            Bay targetBay = findFirstAvailableBay(box.getWarehouseId(), box.getAisle());
            if (targetBay != null) {
                box.setWarehouseId(targetBay.getWarehouseId());
                box.setAisle(targetBay.getAisle());
                box.setBay(targetBay.getBayNumber());
                targetBay.getBoxes().add(box);
            }
        }
    }

    /**
     * Finds the first bay within the aisle that has space available.
     * Maximum bay number scanned is 50.
     *
     * @param warehouseId warehouse identifier
     * @param aisle aisle number
     * @return a bay with remaining space or null if none found
     */
    private Bay findFirstAvailableBay(String warehouseId, int aisle) {
        for (int bayNumber = 1; bayNumber <= 50; bayNumber++) {
            Bay bay = getOrCreateBay(warehouseId, aisle, bayNumber);
            if (bay.hasSpace()) return bay;
        }
        return null;
    }

    /**
     * Adds entry to SKU index linking SKU to warehouse, aisle, and bay.
     *
     * @param sku SKU identifier
     * @param warehouseId warehouse identifier
     * @param aisle aisle number
     * @param bayNumber bay number
     */
    private void indexSkuBox(String sku, String warehouseId, int aisle, int bayNumber) {
        Map<String, Map<Integer, SortedSet<Integer>>> byWarehouse =
                skuIndex.computeIfAbsent(sku, k -> new HashMap<>());
        Map<Integer, SortedSet<Integer>> byAisle =
                byWarehouse.computeIfAbsent(warehouseId, k -> new HashMap<>());
        SortedSet<Integer> bays =
                byAisle.computeIfAbsent(aisle, k -> new TreeSet<>());
        bays.add(bayNumber);
    }

    /**
     * Removes a bay from the SKU index if no remaining boxes of that SKU exist in it.
     *
     * @param sku SKU identifier
     * @param warehouseId warehouse identifier
     * @param aisle aisle number
     * @param bayNumber bay number
     */
    private void cleanupSkuIndex(String sku, String warehouseId, int aisle, int bayNumber) {
        Map<String, Map<Integer, SortedSet<Integer>>> byWarehouse =
                skuIndex.getOrDefault(sku, Collections.emptyMap());
        Map<Integer, SortedSet<Integer>> byAisle =
                byWarehouse.getOrDefault(warehouseId, Collections.emptyMap());
        SortedSet<Integer> bays = byAisle.getOrDefault(aisle, null);
        if (bays == null) return;
        Bay bay = getOrCreateBay(warehouseId, aisle, bayNumber);
        boolean hasSku =
                bay.getBoxes().stream().anyMatch(b -> b.getSku().equals(sku));
        if (!hasSku) {
            bays.remove(bayNumber);
        }
    }
}
