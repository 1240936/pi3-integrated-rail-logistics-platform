package domain;

/**
 * Represents a coordinate position in the warehouse.
 * Used for bay locations with aisle and bay number.
 */
public class Coordinate {
    private final int aisle;
    private final int bay;

    public Coordinate(int aisle, int bay) {
        this.aisle = aisle;
        this.bay = bay;
    }

    public int getAisle() {
        return aisle;
    }

    public int getBay() {
        return bay;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Coordinate that = (Coordinate) obj;
        return aisle == that.aisle && bay == that.bay;
    }

    @Override
    public int hashCode() {
        return 31 * aisle + bay;
    }

    @Override
    public String toString() {
        return "(" + aisle + ", " + bay + ")";
    }
}
