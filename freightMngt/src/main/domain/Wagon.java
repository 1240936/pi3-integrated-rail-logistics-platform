package main.domain;

/**
 * Represents a wagon with its specifications and load status.
 * A wagon can be loaded with freight or empty. The total weight of a wagon
 * is the sum of its tare weight (empty weight) and payload (if loaded).
 * Wagon weight affects the train's total weight and speed performance.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class Wagon {
    /** The unique identifier for this wagon */
    private final int id;
    
    /** The ID of the vehicle model for this wagon */
    private final int vehicleModelId;
    
    /** The ID of the train operator that owns this wagon */
    private final int trainOperatorId;
    
    /** The specifications for this wagon (payload capacity, etc.) */
    private final WagonSpecs specs;
    
    /** The empty weight (tare) of this wagon in tons */
    private final double tare;
    
    /** Whether this wagon is currently carrying freight */
    private boolean isLoaded;

    /**
     * Constructs a Wagon object.
     * 
     * @param id the unique identifier for this wagon
     * @param vehicleModelId the ID of the vehicle model
     * @param trainOperatorId the ID of the train operator that owns this wagon
     * @param specs the specifications for this wagon
     * @param tare the empty weight (tare) in tons
     */
    public Wagon(int id, int vehicleModelId, int trainOperatorId, WagonSpecs specs, double tare) {
        this.id = id;
        this.vehicleModelId = vehicleModelId;
        this.trainOperatorId = trainOperatorId;
        this.specs = specs;
        this.tare = tare;
        this.isLoaded = false;
    }

    /**
     * Gets the unique identifier for this wagon.
     * 
     * @return the wagon ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the ID of the vehicle model for this wagon.
     * 
     * @return the vehicle model ID
     */
    public int getVehicleModelId() {
        return vehicleModelId;
    }

    /**
     * Gets the ID of the train operator that owns this wagon.
     * 
     * @return the train operator ID
     */
    public int getTrainOperatorId() {
        return trainOperatorId;
    }

    /**
     * Gets the specifications for this wagon.
     * 
     * @return the wagon specifications
     */
    public WagonSpecs getSpecs() {
        return specs;
    }

    /**
     * Gets the empty weight (tare) of this wagon.
     * 
     * @return the tare weight in tons
     */
    public double getTare() {
        return tare;
    }

    /**
     * Checks if this wagon is currently carrying freight.
     * 
     * @return true if the wagon is loaded, false otherwise
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Sets the load status of this wagon.
     * 
     * @param loaded true if the wagon is loaded with freight, false if empty
     */
    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    /**
     * Calculates the total weight of the wagon.
     * Total weight = tare weight + payload (if loaded).
     * 
     * @return the total weight in tons
     */
    public double getTotalWeight() {
        if (isLoaded && specs != null) {
            return tare + specs.getPayload();
        }
        return tare;
    }

    @Override
    public String toString() {
        return "Wagon{" +
                "id=" + id +
                ", vehicleModelId=" + vehicleModelId +
                ", weight=" + getTotalWeight() +
                ", loaded=" + isLoaded +
                '}';
    }
}

