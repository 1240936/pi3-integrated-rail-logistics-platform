package main.domain;

/**
 * Represents a rail line connecting two facilities
 */
public class RailLine {
    private final int id;
    private final int ownerId;
    private final Facility startFacility;
    private final Facility endFacility;
    private final int gaugeId;
    private final boolean isElectrified;

    public RailLine(int id, int ownerId, Facility startFacility, Facility endFacility, 
                   int gaugeId, boolean isElectrified) {
        this.id = id;
        this.ownerId = ownerId;
        this.startFacility = startFacility;
        this.endFacility = endFacility;
        this.gaugeId = gaugeId;
        this.isElectrified = isElectrified;
    }

    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public Facility getStartFacility() {
        return startFacility;
    }

    public Facility getEndFacility() {
        return endFacility;
    }

    public int getGaugeId() {
        return gaugeId;
    }

    public boolean isElectrified() {
        return isElectrified;
    }

    @Override
    public String toString() {
        return "RailLine{" +
                "id=" + id +
                ", start=" + startFacility.getName() +
                ", end=" + endFacility.getName() +
                ", electrified=" + isElectrified +
                '}';
    }
}

