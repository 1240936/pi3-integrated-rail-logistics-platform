package main.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents cargo operations (load/unload) that occur at a specific station/facility
 * during a route. A station can have multiple freight items being loaded and/or
 * unloaded at the same stop.
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class StationCargoOperation {
    /** The facility/station where this operation occurs */
    private final Facility facility;
    
    /** The sequence number indicating the order in the route */
    private final int sequenceNumber;
    
    /** List of freight items to be loaded at this station */
    private final List<Freight> freightToLoad;
    
    /** List of freight items to be unloaded at this station */
    private final List<Freight> freightToUnload;

    /**
     * Constructs a StationCargoOperation object.
     * 
     * @param facility the facility/station where this operation occurs
     * @param sequenceNumber the sequence number indicating the order in the route
     */
    public StationCargoOperation(Facility facility, int sequenceNumber) {
        this.facility = facility;
        this.sequenceNumber = sequenceNumber;
        this.freightToLoad = new ArrayList<>();
        this.freightToUnload = new ArrayList<>();
    }

    /**
     * Gets the facility/station where this operation occurs.
     * 
     * @return the facility
     */
    public Facility getFacility() {
        return facility;
    }

    /**
     * Gets the sequence number indicating the order in the route.
     * 
     * @return the sequence number
     */
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Gets the list of freight items to be loaded at this station.
     * 
     * @return an unmodifiable list of freight to load
     */
    public List<Freight> getFreightToLoad() {
        return Collections.unmodifiableList(freightToLoad);
    }

    /**
     * Gets the list of freight items to be unloaded at this station.
     * 
     * @return an unmodifiable list of freight to unload
     */
    public List<Freight> getFreightToUnload() {
        return Collections.unmodifiableList(freightToUnload);
    }

    /**
     * Adds a freight item to be loaded at this station.
     * 
     * @param freight the freight item to load
     */
    public void addFreightToLoad(Freight freight) {
        if (freight != null) {
            freightToLoad.add(freight);
        }
    }

    /**
     * Adds a freight item to be unloaded at this station.
     * 
     * @param freight the freight item to unload
     */
    public void addFreightToUnload(Freight freight) {
        if (freight != null) {
            freightToUnload.add(freight);
        }
    }

    /**
     * Checks if there are any operations at this station.
     * 
     * @return true if there are freight items to load or unload, false otherwise
     */
    public boolean hasOperations() {
        return !freightToLoad.isEmpty() || !freightToUnload.isEmpty();
    }

    @Override
    public String toString() {
        return "StationCargoOperation{" +
                "facility=" + facility.getName() +
                ", sequence=" + sequenceNumber +
                ", toLoad=" + freightToLoad.size() +
                ", toUnload=" + freightToUnload.size() +
                '}';
    }
}

