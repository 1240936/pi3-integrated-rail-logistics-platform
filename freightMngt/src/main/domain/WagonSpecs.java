package main.domain;

/**
 * Specifications of a wagon model
 */
public class WagonSpecs {
    private final int vehicleModelId;
    private final int wagonTypeId;
    private final double volumeCapacity; // in m³
    private final double payload; // maximum payload in tons

    public WagonSpecs(int vehicleModelId, int wagonTypeId, double volumeCapacity, double payload) {
        this.vehicleModelId = vehicleModelId;
        this.wagonTypeId = wagonTypeId;
        this.volumeCapacity = volumeCapacity;
        this.payload = payload;
    }

    public int getVehicleModelId() {
        return vehicleModelId;
    }

    public int getWagonTypeId() {
        return wagonTypeId;
    }

    public double getVolumeCapacity() {
        return volumeCapacity;
    }

    public double getPayload() {
        return payload;
    }
}

