package main.domain;

/**
 * Represents a locomotive with its specifications.
 * A locomotive provides power to the train and determines the maximum speed.
 * Multiple locomotives can be combined on a single train.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class Locomotive {
    /** The unique identifier for this locomotive */
    private final int id;
    
    /** The ID of the vehicle model for this locomotive */
    private final int vehicleModelId;
    
    /** The ID of the train operator that owns this locomotive */
    private final int trainOperatorId;
    
    /** The specifications for this locomotive (power, max speed, etc.) */
    private final LocomotiveSpecs specs;

    /**
     * Constructs a Locomotive object.
     * 
     * @param id the unique identifier for this locomotive
     * @param vehicleModelId the ID of the vehicle model
     * @param trainOperatorId the ID of the train operator that owns this locomotive
     * @param specs the specifications for this locomotive
     */
    public Locomotive(int id, int vehicleModelId, int trainOperatorId, LocomotiveSpecs specs) {
        this.id = id;
        this.vehicleModelId = vehicleModelId;
        this.trainOperatorId = trainOperatorId;
        this.specs = specs;
    }

    /**
     * Gets the unique identifier for this locomotive.
     * 
     * @return the locomotive ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the ID of the vehicle model for this locomotive.
     * 
     * @return the vehicle model ID
     */
    public int getVehicleModelId() {
        return vehicleModelId;
    }

    /**
     * Gets the ID of the train operator that owns this locomotive.
     * 
     * @return the train operator ID
     */
    public int getTrainOperatorId() {
        return trainOperatorId;
    }

    /**
     * Gets the specifications for this locomotive.
     * 
     * @return the locomotive specifications
     */
    public LocomotiveSpecs getSpecs() {
        return specs;
    }

    /**
     * Gets the power of this locomotive.
     * 
     * @return the power in kilowatts (kW), or 0.0 if specs are not available
     */
    public double getPower() {
        return specs != null ? specs.getPower() : 0.0;
    }

    /**
     * Gets the maximum speed of this locomotive.
     * 
     * @return the maximum speed in km/h, or 0.0 if specs are not available
     */
    public double getMaxSpeed() {
        return specs != null ? specs.getMaxSpeed() : 0.0;
    }

    @Override
    public String toString() {
        return "Locomotive{" +
                "id=" + id +
                ", vehicleModelId=" + vehicleModelId +
                ", power=" + getPower() +
                ", maxSpeed=" + getMaxSpeed() +
                '}';
    }
}

