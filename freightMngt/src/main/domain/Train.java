package main.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a train composed of locomotives and wagons
 */
public class Train {
    private final int id;
    private final int trainOperatorId;
    private final List<Locomotive> locomotives;
    private final List<Wagon> wagons;

    public Train(int id, int trainOperatorId) {
        this.id = id;
        this.trainOperatorId = trainOperatorId;
        this.locomotives = new ArrayList<>();
        this.wagons = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public int getTrainOperatorId() {
        return trainOperatorId;
    }

    public List<Locomotive> getLocomotives() {
        return locomotives;
    }

    public List<Wagon> getWagons() {
        return wagons;
    }

    public void addLocomotive(Locomotive locomotive) {
        this.locomotives.add(locomotive);
    }

    public void addWagon(Wagon wagon) {
        this.wagons.add(wagon);
    }

    /**
     * Calculate total power of all locomotives
     */
    public double getTotalPower() {
        return locomotives.stream()
                .mapToDouble(Locomotive::getPower)
                .sum();
    }

    /**
     * Calculate total weight of the train (locomotives + wagons)
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
     * Get maximum speed based on locomotive specs
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

