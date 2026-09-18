package main.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents cargo operations (load/unload) at a specific station in a route.
 * This class tracks which freight items need to be loaded and which need to be
 * unloaded at a particular station along the route.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class StationOperation {
    /** The station (facility) where operations occur */
    private final Facility station;
    
    /** The sequence number of this station in the route (1-based) */
    private final int sequenceNumber;
    
    /** List of freight items to be loaded at this station */
    private final List<Freight> cargoToLoad;
    
    /** List of freight items to be unloaded at this station */
    private final List<Freight> cargoToUnload;

    /**
     * Constructs a StationOperation object.
     * 
     * @param station the facility where operations occur
     * @param sequenceNumber the sequence number of this station in the route (1-based)
     */
    public StationOperation(Facility station, int sequenceNumber) {
        this.station = station;
        this.sequenceNumber = sequenceNumber;
        this.cargoToLoad = new ArrayList<>();
        this.cargoToUnload = new ArrayList<>();
    }

    /**
     * Gets the station (facility) where operations occur.
     * 
     * @return the facility
     */
    public Facility getStation() {
        return station;
    }

    /**
     * Gets the sequence number of this station in the route.
     * 
     * @return the sequence number (1-based)
     */
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Gets the list of freight items to be loaded at this station.
     * 
     * @return list of freight to be loaded
     */
    public List<Freight> getCargoToLoad() {
        return new ArrayList<>(cargoToLoad);
    }

    /**
     * Gets the list of freight items to be unloaded at this station.
     * 
     * @return list of freight to be unloaded
     */
    public List<Freight> getCargoToUnload() {
        return new ArrayList<>(cargoToUnload);
    }

    /**
     * Adds a freight item to be loaded at this station.
     * 
     * @param freight the freight to be loaded
     */
    public void addCargoToLoad(Freight freight) {
        if (freight != null && !cargoToLoad.contains(freight)) {
            cargoToLoad.add(freight);
        }
    }

    /**
     * Adds a freight item to be unloaded at this station.
     * 
     * @param freight the freight to be unloaded
     */
    public void addCargoToUnload(Freight freight) {
        if (freight != null && !cargoToUnload.contains(freight)) {
            cargoToUnload.add(freight);
        }
    }

    /**
     * Checks if there are any operations at this station.
     * 
     * @return true if there are cargo items to load or unload, false otherwise
     */
    public boolean hasOperations() {
        return !cargoToLoad.isEmpty() || !cargoToUnload.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Station: ").append(station.getName())
          .append(" (Sequence: ").append(sequenceNumber).append(")");
        
        if (!cargoToLoad.isEmpty()) {
            sb.append("\n  Load: ");
            for (Freight freight : cargoToLoad) {
                sb.append("Freight #").append(freight.getId()).append(" ");
            }
        }
        
        if (!cargoToUnload.isEmpty()) {
            sb.append("\n  Unload: ");
            for (Freight freight : cargoToUnload) {
                sb.append("Freight #").append(freight.getId()).append(" ");
            }
        }
        
        return sb.toString();
    }
}

