package main.domain;

/**
 * Represents a railway facility (station, warehouse, etc.).
 * Facilities are points on the railway network where trains can stop,
 * pick up or deliver freight, or perform crossing operations.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class Facility {
    /** The unique identifier for this facility */
    private final int id;
    
    /** The name of this facility */
    private final String name;

    /**
     * Constructs a Facility object.
     * 
     * @param id the unique identifier for this facility
     * @param name the name of this facility
     */
    public Facility(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Gets the unique identifier for this facility.
     * 
     * @return the facility ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the name of this facility.
     * 
     * @return the facility name
     */
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

