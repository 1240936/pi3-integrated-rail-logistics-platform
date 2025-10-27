package main.domain;

import java.util.Map;
import java.util.TreeMap;

/**
 * In-memory representation of a warehouse containing aisles and bays.
 * Structure: aisle number → bay number → {@link Bay}.
 */
public class Warehouse {
    private final String warehouseId;
    private final Map<Integer, Map<Integer, Bay>> aisles;

    /**
     * Constructs an empty warehouse with the given ID.
     * @param warehouseId unique identifier for the warehouse
     */
    public Warehouse(String warehouseId) {
        this.warehouseId = warehouseId;
        this.aisles = new TreeMap<>();
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public Map<Integer, Map<Integer, Bay>> getAisles() {
        return aisles;
    }

    /**
     * Retrieves a bay at the specified aisle and bay number.
     * If the aisle or bay does not exist, returns null.
     *
     * @param aisleNumber the aisle number
     * @param bayNumber the bay number
     * @return the {@link Bay} if present, else null
     */
    public Bay getBay(int aisleNumber, int bayNumber) {
        Map<Integer, Bay> bayMap = aisles.get(aisleNumber);
        if (bayMap == null) return null;
        return bayMap.get(bayNumber);
    }
}
