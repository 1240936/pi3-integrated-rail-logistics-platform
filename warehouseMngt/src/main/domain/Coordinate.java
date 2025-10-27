package main.domain;

/**
 * Represents a coordinate position in the warehouse.
 * Typically used to identify the location of a bay within an aisle.
 */
public class Coordinate {
    private final int aisle;
    private final int bay;

    /**
     * Constructs a coordinate with the given aisle and bay numbers.
     *
     * @param aisle aisle number
     * @param bay bay number
     */
    public Coordinate(int aisle, int bay) {
        this.aisle = aisle;
        this.bay = bay;
    }

    /**
     * Returns the aisle number of this coordinate.
     *
     * @return aisle number
     */
    public int getAisle() {
        return aisle;
    }

    /**
     * Returns the bay number of this coordinate.
     *
     * @return bay number
     */
    public int getBay() {
        return bay;
    }

    /**
     * Checks equality with another object.
     * Two coordinates are equal if their aisle and bay numbers match.
     *
     * @param obj object to compare
     * @return true if both coordinates are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Coordinate that = (Coordinate) obj;
        return aisle == that.aisle && bay == that.bay;
    }

    /**
     * Computes hash code based on aisle and bay.
     * Uses a prime multiplier to distribute objects evenly in hash-based collections.
     *
     * @return hash code of the coordinate
     */
    @Override
    public int hashCode() {
        return 31 * aisle + bay;
    }

    /**
     * Returns a string representation of the coordinate in (aisle, bay) format.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "(" + aisle + ", " + bay + ")";
    }
}
