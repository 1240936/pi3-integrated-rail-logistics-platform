package domain;

import java.util.Map;
import java.util.TreeMap;

/**
 * In-memory representation of a warehouse containing aisles and bays.
 * Uses a nested map structure: aisle -> bayNumber -> {@link Bay}.
 */
public class Warehouse {
    private final String warehouseId;
    // aisle -> bayNumber -> Bay
    private final Map<Integer, Map<Integer, Bay>> aisles;

    /**
     * Creates a warehouse with empty aisle/bay structure.
     */
    public Warehouse(String warehouseId) {
        this.warehouseId = warehouseId;
        this.aisles = new TreeMap<>();
    }

    public String getWarehouseId() { return warehouseId; }

    public Map<Integer, Map<Integer, Bay>> getAisles() { return aisles; }
}


