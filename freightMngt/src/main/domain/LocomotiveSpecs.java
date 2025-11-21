package main.domain;

/**
 * Specifications of a locomotive model
 */
public class LocomotiveSpecs {
    private final int vehicleModelId;
    private final String make;
    private final double power; // in kW
    private final Double acceleration; // in m/s² (nullable)
    private final double maxSpeed; // in km/h
    private final int numberOfWheels;

    public LocomotiveSpecs(int vehicleModelId, String make, double power, Double acceleration, 
                          double maxSpeed, int numberOfWheels) {
        this.vehicleModelId = vehicleModelId;
        this.make = make;
        this.power = power;
        this.acceleration = acceleration;
        this.maxSpeed = maxSpeed;
        this.numberOfWheels = numberOfWheels;
    }

    public int getVehicleModelId() {
        return vehicleModelId;
    }

    public String getMake() {
        return make;
    }

    public double getPower() {
        return power;
    }

    public Double getAcceleration() {
        return acceleration;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public int getNumberOfWheels() {
        return numberOfWheels;
    }
}

