package main.domain;

/**
 * Wrapper classes to make Station comparable for use with PL.BST and PL.AVL
 * which require Comparable<E> instead of Comparator<E>
 */
public class StationComparable implements Comparable<StationComparable> {
    private final Station station;
    private final ComparisonType type;
    
    public enum ComparisonType {
        BY_LATITUDE,      // Sort by latitude, then longitude, then name
        BY_LONGITUDE,     // Sort by longitude, then latitude, then name
        BY_TIMEZONE_GROUP // Sort by time zone group, then country, then name
    }
    
    public StationComparable(Station station, ComparisonType type) {
        this.station = station;
        this.type = type;
    }
    
    public Station getStation() {
        return station;
    }
    
    @Override
    public int compareTo(StationComparable other) {
        if (this.type != other.type) {
            throw new IllegalArgumentException("Cannot compare stations with different comparison types");
        }
        
        switch (type) {
            case BY_LATITUDE:
                int latCmp = Double.compare(this.station.getLatitude(), other.station.getLatitude());
                if (latCmp != 0) return latCmp;
                int lonCmp = Double.compare(this.station.getLongitude(), other.station.getLongitude());
                if (lonCmp != 0) return lonCmp;
                return this.station.getName().compareTo(other.station.getName());
                
            case BY_LONGITUDE:
                int lonCmp2 = Double.compare(this.station.getLongitude(), other.station.getLongitude());
                if (lonCmp2 != 0) return lonCmp2;
                int latCmp2 = Double.compare(this.station.getLatitude(), other.station.getLatitude());
                if (latCmp2 != 0) return latCmp2;
                return this.station.getName().compareTo(other.station.getName());
                
            case BY_TIMEZONE_GROUP:
                int tzCmp = this.station.getTimeZoneGroup().compareTo(other.station.getTimeZoneGroup());
                if (tzCmp != 0) return tzCmp;
                int countryCmp = this.station.getCountry().compareTo(other.station.getCountry());
                if (countryCmp != 0) return countryCmp;
                return this.station.getName().compareTo(other.station.getName());
                
            default:
                throw new IllegalStateException("Unknown comparison type");
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StationComparable that = (StationComparable) obj;
        return station.equals(that.station) && type == that.type;
    }
    
    @Override
    public int hashCode() {
        return station.hashCode() * 31 + type.hashCode();
    }
}

