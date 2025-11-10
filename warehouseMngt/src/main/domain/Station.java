package main.domain;

/**
 * Represents a European railway station with geographic and time zone information.
 * Used for spatial indexing and time zone queries.
 */
public class Station {
    private final String name;
    private final double latitude;
    private final double longitude;
    private final String country;
    private final String timeZone;
    private final String timeZoneGroup;
    private final boolean isCity;
    private final boolean isMainStation;
    private final boolean isAirport;

    /**
     * Constructs a station with all attributes.
     *
     * @param name station name (non-empty)
     * @param latitude latitude in degrees [-90, 90]
     * @param longitude longitude in degrees [-180, 180]
     * @param country country code or name (non-empty)
     * @param timeZone time zone identifier
     * @param timeZoneGroup time zone group (e.g., 'CET', 'WET/GMT')
     * @param isCity flag indicating if station is in a city
     * @param isMainStation flag indicating if station is a main station
     * @param isAirport flag indicating if station is at an airport
     */
    public Station(String name, double latitude, double longitude, String country,
                   String timeZone, String timeZoneGroup,
                   boolean isCity, boolean isMainStation, boolean isAirport) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.country = country;
        this.timeZone = timeZone;
        this.timeZoneGroup = timeZoneGroup;
        this.isCity = isCity;
        this.isMainStation = isMainStation;
        this.isAirport = isAirport;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getCountry() {
        return country;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String getTimeZoneGroup() {
        return timeZoneGroup;
    }

    public boolean isCity() {
        return isCity;
    }

    public boolean isMainStation() {
        return isMainStation;
    }

    public boolean isAirport() {
        return isAirport;
    }

    @Override
    public String toString() {
        return "Station{" +
                "name='" + name + '\'' +
                ", lat=" + latitude +
                ", lon=" + longitude +
                ", country='" + country + '\'' +
                ", timeZoneGroup='" + timeZoneGroup + '\'' +
                '}';
    }
}

