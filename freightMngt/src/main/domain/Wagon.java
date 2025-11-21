package main.domain;

/**
 * Represents a wagon with its specifications and load status
 */
public class Wagon {
    private final int id;
    private final int vehicleModelId;
    private final int trainOperatorId;
    private final WagonSpecs specs;
    private final double tare; // empty weight in tons
    private boolean isLoaded; // whether wagon is carrying freight

    public Wagon(int id, int vehicleModelId, int trainOperatorId, WagonSpecs specs, double tare) {
        this.id = id;
        this.vehicleModelId = vehicleModelId;
        this.trainOperatorId = trainOperatorId;
        this.specs = specs;
        this.tare = tare;
        this.isLoaded = false;
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

    public WagonSpecs getSpecs() {
        return specs;
    }

    public double getTare() {
        return tare;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    /**
     * Calculate the total weight of the wagon (tare + payload if loaded)
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

