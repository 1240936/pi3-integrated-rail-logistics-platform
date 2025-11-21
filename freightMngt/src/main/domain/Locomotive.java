package main.domain;

/**
 * Represents a locomotive with its specifications
 */
public class Locomotive {
    private final int id;
    private final int vehicleModelId;
    private final int trainOperatorId;
    private final LocomotiveSpecs specs;

    public Locomotive(int id, int vehicleModelId, int trainOperatorId, LocomotiveSpecs specs) {
        this.id = id;
        this.vehicleModelId = vehicleModelId;
        this.trainOperatorId = trainOperatorId;
        this.specs = specs;
    }

    public int getId() {
        return id;
    }

    public int getVehicleModelId() {
        return vehicleModelId;
    }

    public int getTrainOperatorId() {
        return trainOperatorId;
    }

    public LocomotiveSpecs getSpecs() {
        return specs;
    }

    public double getPower() {
        return specs != null ? specs.getPower() : 0.0;
    }

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

