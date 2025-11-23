package main.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a train composed of locomotives and wagons.
 * A train has properties such as total power (sum of locomotive powers),
 * total weight (locomotives + wagons), and maximum speed (minimum of locomotive max speeds).
 * The train's weight affects its speed and performance on routes.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class Train {
    /** The unique identifier for this train */
    private final int id;
    
    /** The ID of the train operator that owns this train */
    private final int trainOperatorId;
    
    /** The list of locomotives in this train */
    private final List<Locomotive> locomotives;
    
    /** The list of wagons in this train */
    private final List<Wagon> wagons;

    /**
     * Constructs a Train object.
     * 
     * @param id the unique identifier for this train
     * @param trainOperatorId the ID of the train operator that owns this train
     */
    public Train(int id, int trainOperatorId) {
        this.id = id;
        this.trainOperatorId = trainOperatorId;
        this.locomotives = new ArrayList<>();
        this.wagons = new ArrayList<>();
    }

    /**
     * Gets the unique identifier for this train.
     * 
     * @return the train ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the ID of the train operator that owns this train.
     * 
     * @return the train operator ID
     */
    public int getTrainOperatorId() {
        return trainOperatorId;
    }

    /**
     * Gets the list of locomotives in this train.
     * 
     * @return the list of locomotives
     */
    public List<Locomotive> getLocomotives() {
        return locomotives;
    }

    /**
     * Gets the list of wagons in this train.
     * 
     * @return the list of wagons
     */
    public List<Wagon> getWagons() {
        return wagons;
    }

    /**
     * Adds a locomotive to this train.
     * 
     * @param locomotive the locomotive to add
     */
    public void addLocomotive(Locomotive locomotive) {
        this.locomotives.add(locomotive);
    }

    /**
     * Adds a wagon to this train.
     * 
     * @param wagon the wagon to add
     */
    public void addWagon(Wagon wagon) {
        this.wagons.add(wagon);
    }

    /**
     * Calculates the total power of all locomotives in this train.
     * Power is measured in kilowatts (kW).
     * 
     * @return the total power in kW
     */
    public double getTotalPower() {
        return locomotives.stream()
                .mapToDouble(Locomotive::getPower)
                .sum();
    }

    /**
     * Calculates the total weight of the train (locomotives + wagons).
     * Locomotive weight is assumed to be 87.0 tons per locomotive.
     * Wagon weight includes both tare weight and payload (if loaded).
     * Weight is measured in tons.
     * 
     * @return the total weight in tons
     */
    public double getTotalWeight() {
        double locoWeight = locomotives.stream()
                .mapToDouble(l -> l.getSpecs() != null ? 87.0 : 0.0) // Assuming average locomotive weight
                .sum();
        
        double wagonWeight = wagons.stream()
                .mapToDouble(Wagon::getTotalWeight)
                .sum();
        
        return locoWeight + wagonWeight;
    }

    /**
     * Gets the maximum speed based on locomotive specifications.
     * The maximum speed is the minimum of all locomotive maximum speeds,
     * as the train is limited by its slowest locomotive.
     * Speed is measured in kilometers per hour (km/h).
     * 
     * @return the maximum speed in km/h, or 0.0 if there are no locomotives
     */
    public double getMaxSpeed() {
        return locomotives.stream()
                .mapToDouble(Locomotive::getMaxSpeed)
                .min()
                .orElse(0.0);
    }

    @Override
    public String toString() {
        return "Train{" +
                "id=" + id +
                ", operatorId=" + trainOperatorId +
                ", locomotives=" + locomotives.size() +
                ", wagons=" + wagons.size() +
                ", totalPower=" + getTotalPower() +
                ", totalWeight=" + getTotalWeight() +
                '}';
    }
}

