package main.domain;

/**
 * Represents a product or stock keeping unit (SKU) in the warehouse.
 * Contains descriptive attributes and measurements for handling and storage.
 */
public class Item {
    private final String sku;
    private final String name;
    private final String category;
    private final String unit;
    private final double volume;
    private final double unitWeight;

    /**
     * Constructs an item with all relevant properties.
     *
     * @param sku unique identifier for the item
     * @param name descriptive name of the item
     * @param category category or type of the item
     * @param unit unit of measure (e.g., pcs, box)
     * @param volume physical volume of a single unit
     * @param unitWeight weight of a single unit
     */
    public Item(String sku, String name, String category, String unit, double volume, double unitWeight) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.volume = volume;
        this.unitWeight = unitWeight;
    }

    /**
     * Returns the SKU (stock keeping unit) identifier.
     *
     * @return SKU string
     */
    public String getSku() { return sku; }

    /**
     * Returns the descriptive name of the item.
     *
     * @return item name
     */
    public String getName() { return name; }

    /**
     * Returns the category of the item.
     *
     * @return category name
     */
    public String getCategory() { return category; }

    /**
     * Returns the unit of measure for this item.
     *
     * @return unit string
     */
    public String getUnit() { return unit; }

    /**
     * Returns the volume of a single unit.
     *
     * @return volume
     */
    public double getVolume() { return volume; }

    /**
     * Returns the weight of a single unit.
     *
     * @return unit weight
     */
    public double getUnitWeight() { return unitWeight; }
}
