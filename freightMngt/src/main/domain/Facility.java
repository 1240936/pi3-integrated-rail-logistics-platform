package main.domain;

/**
 * Represents a railway facility (station, warehouse, etc.)
 */
public class Facility {
    private final int id;
    private final String name;

    public Facility(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Facility{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}

