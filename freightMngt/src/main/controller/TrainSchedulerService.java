package main.controller;

import main.domain.*;
import main.repositories.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for train scheduling including speed calculation, crossing detection, and time estimation.
 * This service handles all scheduling logic including:
 * <ul>
 *   <li>Speed calculation based on train power, weight, and track constraints</li>
 *   <li>Travel time estimation for routes</li>
 *   <li>Route passage time calculation with freight pickup/delivery</li>
 *   <li>Crossing detection on single-track segments</li>
 * </ul>
 * 
 * @author Freight Management System
 * @version 1.0
 */
public class TrainSchedulerService {
    private final FacilityRepository facilityRepository;
    private final TrainRepository trainRepository;
    private final RouteRepository routeRepository;
    private final LineSegmentRepository lineSegmentRepository;
    private final RailLineRepository railLineRepository;
    private final TrainEventRepository trainEventRepository;
    private final FreightRepository freightRepository;

    /**
     * Constructs a TrainSchedulerService with the given database connection.
     * Initializes all required repositories for train scheduling operations.
     * 
     * @param connection the database connection to use for repository operations
     */
    public TrainSchedulerService(Connection connection) {
        this.facilityRepository = new FacilityRepository(connection);
        LocomotiveRepository locoRepo = new LocomotiveRepository(connection);
        WagonRepository wagonRepo = new WagonRepository(connection);
        this.trainRepository = new TrainRepository(connection, locoRepo, wagonRepo);
        FacilityRepository facilityRepo = new FacilityRepository(connection);
        this.routeRepository = new RouteRepository(connection, facilityRepo);
        this.lineSegmentRepository = new LineSegmentRepository(connection);
        this.railLineRepository = new RailLineRepository(connection, facilityRepo);
        this.trainEventRepository = new TrainEventRepository(connection, facilityRepo);
        this.freightRepository = new FreightRepository(connection, facilityRepo);
    }

    /**
     * Calculate the maximum speed for a train on a line segment.
     * The speed is determined by taking the minimum of:
     * <ul>
     *   <li>Track speed limit (if specified)</li>
     *   <li>Locomotive maximum speed</li>
     *   <li>Calculated speed based on power-to-weight ratio</li>
     * </ul>
     * 
     * The power-to-weight calculation uses the formula: v = k * sqrt(P/W)
     * where k is approximately 18 km/h for trains.
     * 
     * @param train the train for which to calculate speed
     * @param segment the line segment on which the train will travel
     * @return the maximum speed in km/h, or 0 if train weight is zero
     */
    public double calculateSpeed(Train train, LineSegment segment) {
        // Start with locomotive's operational speed (not max speed)
        double operationalLocoSpeed = train.getOperationalSpeed();
        
        // Apply track speed limit if present
        double speedLimit = segment.getSpeedLimit() != null ? segment.getSpeedLimit() : Double.MAX_VALUE;
        
        // Calculate speed based on power and weight
        // Simplified formula: speed = sqrt(power / weight) * factor
        // Power-to-weight ratio approach
        double totalPower = train.getTotalPower(); // in kW
        double totalWeight = train.getTotalWeight(); // in tons
        
        if (totalWeight == 0) {
            return 0;
        }
        
        // Power-to-weight ratio (kW per ton)
        double powerToWeightRatio = totalPower / totalWeight;
        
        // Empirical formula: v = k * sqrt(P/W) where k is a conversion factor
        // For trains, typical values: k ≈ 15-20 for km/h
        double calculatedSpeed = Math.sqrt(powerToWeightRatio) * 18.0; // km/h
        
        // Take the minimum of all constraints: operational speed, track speed limit, and calculated speed
        return Math.min(Math.min(operationalLocoSpeed, speedLimit), calculatedSpeed);
    }

    /**
     * Calculate travel time for a train on a line segment.
     * The travel time is calculated based on the segment length and the train's
     * calculated speed (which considers power, weight, and speed limits).
     * 
     * @param train the train for which to calculate travel time
     * @param segment the line segment on which the train will travel
     * @return the travel time as a Duration, or Duration.ZERO if speed is zero
     */
    public Duration calculateTravelTime(Train train, LineSegment segment) {
        double speed = calculateSpeed(train, segment); // km/h
        if (speed == 0) {
            return Duration.ZERO;
        }
        
        // Convert segment length from meters to kilometers
        double lengthKm = segment.getLength() / 1000.0;
        
        // Calculate time in hours
        double timeHours = lengthKm / speed;
        
        // Convert to Duration
        long seconds = Math.round(timeHours * 3600);
        return Duration.ofSeconds(seconds);
    }

    /**
     * Calculate estimated passage times for a train along its route.
     * This method processes the entire route, handling:
     * <ul>
     *   <li>Travel between consecutive facilities in the path</li>
     *   <li>Freight pickup and delivery operations (which affect train weight)</li>
     *   <li>Speed recalculation as train weight changes</li>
     *   <li>Direction handling for bidirectional rail lines</li>
     * </ul>
     * 
     * The method returns a list of TrainEvent objects representing passage times
     * at each facility along the route, including the start and end facilities.
     * 
     * @param route the route for which to calculate passage times
     * @param train the train that will travel the route
     * @return a list of TrainEvent objects representing passage times at each facility
     * @throws SQLException if there is a database error during calculation
     * @throws IllegalArgumentException if facilities are not properly connected by rail lines,
     *         or if travel time cannot be calculated (zero-length segments, zero speed, etc.)
     */
    public List<TrainEvent> calculateRouteTimes(Route route, Train train) throws SQLException {
        List<TrainEvent> events = new ArrayList<>();
        List<Route.RoutePathPoint> path = route.getPath();

        // Get freight data for this route
        // IMPORTANT: Use startDate to ensure we get freights for the correct journey
        Map<Integer, List<Freight>> pickupsByFacility;
        Map<Integer, List<Freight>> deliveriesByFacility;
        if (route.getStartDate() != null) {
            pickupsByFacility = freightRepository.getPickupsByFacility(route.getId(), route.getStartDate());
            deliveriesByFacility = freightRepository.getDeliveriesByFacility(route.getId(), route.getStartDate());
        } else {
            // Fallback if startDate is not available
            pickupsByFacility = freightRepository.getPickupsByFacility(route.getId());
            deliveriesByFacility = freightRepository.getDeliveriesByFacility(route.getId());
        }

        // Track which wagons are loaded (map of wagon ID to freight ID)
        Map<Integer, Integer> wagonToFreightMap = new HashMap<>();
        
        // Track which freight has already been picked up (to avoid picking up multiple times)
        java.util.Set<Integer> pickedUpFreightIds = new java.util.HashSet<>();
        
        // Track which freight has already been delivered (to avoid delivering multiple times)
        java.util.Set<Integer> deliveredFreightIds = new java.util.HashSet<>();

        // Don't filter path - allow start/end facilities to be in the path
        // (they may be added as intermediate points in manual path selection)

        LocalDateTime currentTime = route.getStartDate();
        int startFacilityId = route.getStartFacility().getId();
        int endFacilityId = route.getEndFacility().getId();

        // Check if start facility is the first path point
        boolean startIsInPath = !path.isEmpty() && path.get(0).getFacility().getId() == startFacilityId;
        
        // Start with the start facility (unless it's already the first path point)
        if (!startIsInPath) {
            TrainEvent startEvent = new TrainEvent(
                0, route.getId(), train.getId(), route.getStartFacility(), currentTime
            );
            events.add(startEvent);
            
            // Process freight at start facility
            processFreightAtFacility(train, route.getStartFacility().getId(), pickupsByFacility,
                                    deliveriesByFacility, wagonToFreightMap, freightRepository,
                                    pickedUpFreightIds, deliveredFreightIds);
        }

        // Process each segment of the path (may include start/end facilities if manually added)
        Facility previousFacility = route.getStartFacility();
        
        // Handle empty path case
        if (path.isEmpty()) {
            // If path is empty, we still need to travel from start to end
            // This will be handled in the end facility section below
        }

        for (Route.RoutePathPoint pathPoint : path) {
            Facility nextFacility = pathPoint.getFacility();

            // Skip if next facility is the same as previous (duplicate path point)
            if (nextFacility.getId() == previousFacility.getId()) {
                continue;
            }

            // Note: Start and end facilities are already filtered out at the beginning,
            // so we don't need to check for them here
            // Find RailLine(s) connecting previousFacility to nextFacility
            List<RailLine> connectingRailLines = railLineRepository.findConnectingLines(
                previousFacility.getId(), nextFacility.getId());

            // Get ALL LineSegments from all connecting RailLines
            List<LineSegment> segments = new ArrayList<>();
            for (RailLine railLine : connectingRailLines) {
                List<LineSegment> lineSegments = lineSegmentRepository.getByRailLineId(railLine.getId());

                // Determine direction of travel on this RailLine
                boolean isForward = railLine.getStartFacility().getId() == previousFacility.getId() &&
                                  railLine.getEndFacility().getId() == nextFacility.getId();

                if (!isForward) {
                    // Reverse direction - sort segments in reverse order
                    lineSegments.sort((a, b) -> Integer.compare(b.getOrderNum(), a.getOrderNum()));
                } else {
                    // Forward direction - sort segments in normal order
                    lineSegments.sort((a, b) -> Integer.compare(a.getOrderNum(), b.getOrderNum()));
                }

                segments.addAll(lineSegments);
            }

            // Check if segments were found - if not, no direct connection exists
            if (segments.isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("No rail connection found between %s (ID: %d) and %s (ID: %d). " +
                                "These facilities must be directly connected by a RailLine. " +
                                "Please check your route path (sequence number %d) and ensure all consecutive facilities have a direct rail connection.",
                        previousFacility.getName(), previousFacility.getId(),
                        nextFacility.getName(), nextFacility.getId(),
                        pathPoint.getSequenceNumber()));
            }

            // Calculate travel time through all segments using current train weight
            Duration totalTravelTime = Duration.ZERO;
            for (LineSegment segment : segments) {
                Duration segmentTime = calculateTravelTime(train, segment);
                totalTravelTime = totalTravelTime.plus(segmentTime);
            }

            // If total travel time is still zero after processing all segments, something is wrong
            if (totalTravelTime.isZero()) {
                throw new IllegalArgumentException(
                    String.format("Cannot calculate travel time from %s (ID: %d) to %s (ID: %d). " +
                                "Segments found but travel time is zero. This may indicate: " +
                                "(1) Segments have zero length, (2) Train speed calculation returned zero, " +
                                "or (3) Facilities are not properly connected.",
                        previousFacility.getName(), previousFacility.getId(),
                        nextFacility.getName(), nextFacility.getId()));
            }

            // Add arrival time at next facility
            currentTime = currentTime.plus(totalTravelTime);
            TrainEvent arrivalEvent = new TrainEvent(
                0, route.getId(), train.getId(), nextFacility, currentTime
            );
            events.add(arrivalEvent);

            // Process freight pickup/drop-off at this facility
            processFreightAtFacility(train, nextFacility.getId(), pickupsByFacility,
                                    deliveriesByFacility, wagonToFreightMap, freightRepository,
                                    pickedUpFreightIds, deliveredFreightIds);

            previousFacility = nextFacility;
        }

        // Check if end facility is already the last path point
        boolean endIsInPath = !path.isEmpty() && 
                              path.get(path.size() - 1).getFacility().getId() == endFacilityId;
        
        // Always add final destination facility (end facility) as the last event
        // BUT: Only if it's not already the last path point
        Facility endFacility = route.getEndFacility();

        // Check if end facility is already the last event (same as last path point)
        boolean endFacilityIsLastEvent = !events.isEmpty() &&
                                        events.get(events.size() - 1).getFacility().getId() == endFacility.getId();

        // Only add end facility if:
        // 1. It's not already the last event
        // 2. It's different from the last path point (previousFacility)
        // 3. It's not already in the path as the last point
        if (!endFacilityIsLastEvent && endFacility.getId() != previousFacility.getId() && !endIsInPath) {
            // Find RailLine connecting last path point to end facility
            List<RailLine> connectingRailLines = railLineRepository.findConnectingLines(
                previousFacility.getId(), endFacility.getId());

            List<LineSegment> segments = new ArrayList<>();
            for (RailLine railLine : connectingRailLines) {
                List<LineSegment> lineSegments = lineSegmentRepository.getByRailLineId(railLine.getId());

                // Determine if we're going in forward or reverse direction on this RailLine
                boolean isForward = railLine.getStartFacility().getId() == previousFacility.getId() &&
                                  railLine.getEndFacility().getId() == endFacility.getId();

                if (!isForward) {
                    // Reverse direction - sort by orderNum descending (3, 2, 1)
                    lineSegments.sort((a, b) -> Integer.compare(b.getOrderNum(), a.getOrderNum()));
                } else {
                    // Forward direction - sort by orderNum ascending (1, 2, 3)
                    lineSegments.sort((a, b) -> Integer.compare(a.getOrderNum(), b.getOrderNum()));
                }

                segments.addAll(lineSegments);
            }

            if (segments.isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("No rail connection found between %s (ID: %d) and %s (ID: %d). " +
                                "The end facility (%s) must be directly connected by a RailLine to the last path point (%s). " +
                                "Please check your route path and ensure the end facility is reachable from the last path point.",
                        previousFacility.getName(), previousFacility.getId(),
                        endFacility.getName(), endFacility.getId(),
                        endFacility.getName(),
                        previousFacility.getName()));
            }

            Duration totalTravelTime = Duration.ZERO;
            for (LineSegment segment : segments) {
                Duration segmentTime = calculateTravelTime(train, segment);
                totalTravelTime = totalTravelTime.plus(segmentTime);
            }

            if (totalTravelTime.isZero()) {
                throw new IllegalArgumentException(
                    String.format("Cannot calculate travel time from %s (ID: %d) to %s (ID: %d). " +
                                "Segments found but travel time is zero. This may indicate: " +
                                "(1) Segments have zero length, (2) Train speed calculation returned zero, " +
                                "or (3) Facilities are not properly connected.",
                        previousFacility.getName(), previousFacility.getId(),
                        endFacility.getName(), endFacility.getId()));
            }

            currentTime = currentTime.plus(totalTravelTime);
            TrainEvent endEvent = new TrainEvent(
                0, route.getId(), train.getId(), endFacility, currentTime
            );
            events.add(endEvent);

            // Process freight pickup/drop-off at end facility
            processFreightAtFacility(train, endFacility.getId(), pickupsByFacility,
                                    deliveriesByFacility, wagonToFreightMap, freightRepository,
                                    pickedUpFreightIds, deliveredFreightIds);
        } else if (endFacility.getId() == previousFacility.getId()) {
            // End facility is same as last path point, but ensure it's shown as the final destination
            // The event was already added in the loop, so just ensure freight is processed
            processFreightAtFacility(train, endFacility.getId(), pickupsByFacility,
                                    deliveriesByFacility, wagonToFreightMap, freightRepository,
                                    pickedUpFreightIds, deliveredFreightIds);
        }

        return events;
    }

    /**
     * Process freight pickup and drop-off at a facility.
     * This method updates wagon load status based on freight operations:
     * <ul>
     *   <li>Unloads wagons for freight deliveries at the facility</li>
     *   <li>Loads wagons for freight pickups at the facility</li>
     * </ul>
     * The wagon load status affects train weight, which in turn affects speed calculations.
     * 
     * @param train the train at the facility
     * @param facilityId the ID of the facility where freight operations occur
     * @param pickupsByFacility map of facility ID to list of freight to be picked up
     * @param deliveriesByFacility map of facility ID to list of freight to be delivered
     * @param wagonToFreightMap map tracking which wagons are loaded with which freight
     * @param freightRepository repository for freight operations
     * @throws SQLException if there is a database error during freight processing
     */
    private void processFreightAtFacility(Train train, int facilityId,
                                         Map<Integer, List<Freight>> pickupsByFacility,
                                         Map<Integer, List<Freight>> deliveriesByFacility,
                                         Map<Integer, Integer> wagonToFreightMap,
                                         FreightRepository freightRepository,
                                         java.util.Set<Integer> pickedUpFreightIds,
                                         java.util.Set<Integer> deliveredFreightIds) throws SQLException {
        // Drop off freight (unload wagons) - only if not already delivered
        List<Freight> deliveries = deliveriesByFacility.get(facilityId);
        if (deliveries != null) {
            for (Freight freight : deliveries) {
                // Only deliver if not already delivered
                if (!deliveredFreightIds.contains(freight.getId())) {
                    List<Integer> wagonIds = freightRepository.getWagonIdsByFreightId(freight.getId());
                    for (Integer wagonId : wagonIds) {
                        Wagon wagon = train.getWagons().stream()
                                .filter(w -> w.getId() == wagonId)
                                .findFirst()
                                .orElse(null);
                        if (wagon != null) {
                            wagon.setLoaded(false);
                            wagonToFreightMap.remove(wagonId);
                        }
                    }
                    deliveredFreightIds.add(freight.getId());
                }
            }
        }

        // Pick up freight (load wagons) - only if not already picked up
        List<Freight> pickups = pickupsByFacility.get(facilityId);
        if (pickups != null) {
            for (Freight freight : pickups) {
                // Only pick up if not already picked up
                if (!pickedUpFreightIds.contains(freight.getId())) {
                    List<Integer> wagonIds = freightRepository.getWagonIdsByFreightId(freight.getId());
                    for (Integer wagonId : wagonIds) {
                        Wagon wagon = train.getWagons().stream()
                                .filter(w -> w.getId() == wagonId)
                                .findFirst()
                                .orElse(null);
                        if (wagon != null) {
                            wagon.setLoaded(true);
                            wagonToFreightMap.put(wagonId, freight.getId());
                        }
                    }
                    pickedUpFreightIds.add(freight.getId());
                }
            }
        }
    }

    /**
     * Detect potential crossings between trains on single-track segments.
     * This method analyzes all provided routes and identifies conflicts where two trains
     * need to use the same single-track segment at overlapping times. For each conflict,
     * a CrossingOperation is created specifying:
     * <ul>
     *   <li>The two trains involved</li>
     *   <li>The location where the crossing should occur</li>
     *   <li>Any available sidings for the crossing</li>
     *   <li>The scheduled crossing time</li>
     * </ul>
     * 
     * Only single-track segments are checked; multi-track segments allow parallel travel.
     * 
     * @param routes the list of routes to check for crossings
     * @return a list of CrossingOperation objects representing detected conflicts
     * @throws SQLException if there is a database error during crossing detection
     */
    public List<CrossingOperation> detectCrossings(List<Route> routes) throws SQLException {
        List<CrossingOperation> crossings = new ArrayList<>();

        // Load all line segments and sidings
        Map<Integer, List<Siding>> sidingsBySegment = lineSegmentRepository.getSidingsBySegmentMap();
        Map<Integer, Facility> facilities = facilityRepository.getAllAsMap();

        // Build a map of route ID to train for quick lookup
        Map<Integer, Train> routeToTrainMap = new HashMap<>();
        for (Route route : routes) {
            Train train = trainRepository.getTrainForRoute(route.getId());
            if (train != null) {
                routeToTrainMap.put(route.getId(), train);
            }
        }

        // Group routes by their passage times on each segment
        Map<Integer, List<SegmentUsage>> segmentUsages = new HashMap<>();

        for (Route route : routes) {
            Train train = trainRepository.getTrainForRoute(route.getId());
            if (train == null) continue;

            List<TrainEvent> events = calculateRouteTimes(route, train);
            List<Route.RoutePathPoint> path = route.getPath();

            // Filter out start and end facilities from path (they are route attributes)
            int startFacilityId = route.getStartFacility().getId();
            int endFacilityId = route.getEndFacility().getId();
            List<Route.RoutePathPoint> filteredPath = new ArrayList<>();
            for (Route.RoutePathPoint pathPoint : path) {
                int facilityId = pathPoint.getFacility().getId();
                if (facilityId != startFacilityId && facilityId != endFacilityId) {
                    filteredPath.add(pathPoint);
                }
            }
            path = filteredPath;

            Facility previousFacility = route.getStartFacility();
            LocalDateTime previousTime = route.getStartDate();

            // Process segments between consecutive facilities in the path
            for (Route.RoutePathPoint pathPoint : path) {
                Facility nextFacility = pathPoint.getFacility();

                // Skip if next facility is the same as previous (duplicate path point)
                if (nextFacility.getId() == previousFacility.getId()) {
                    continue;
                }

                // Find RailLine(s) that connect previousFacility to nextFacility
                // RailLines are bidirectional - can be used in either direction
                List<RailLine> connectingRailLines = railLineRepository.findConnectingLines(
                    previousFacility.getId(), nextFacility.getId());

                // Get ALL LineSegments from all connecting RailLines
                List<LineSegment> segments = new ArrayList<>();
                for (RailLine railLine : connectingRailLines) {
                    List<LineSegment> lineSegments = lineSegmentRepository.getByRailLineId(railLine.getId());

                    // Determine if we're going in forward or reverse direction on this RailLine
                    // Forward: StartFacility -> EndFacility (use segments in orderNum order)
                    // Reverse: EndFacility -> StartFacility (use segments in reverse orderNum order)
                    boolean isForward = railLine.getStartFacility().getId() == previousFacility.getId() &&
                                      railLine.getEndFacility().getId() == nextFacility.getId();

                    if (!isForward) {
                        // Reverse direction - sort by orderNum descending (3, 2, 1)
                        lineSegments.sort((a, b) -> Integer.compare(b.getOrderNum(), a.getOrderNum()));
                    } else {
                        // Forward direction - sort by orderNum ascending (1, 2, 3)
                        lineSegments.sort((a, b) -> Integer.compare(a.getOrderNum(), b.getOrderNum()));
                    }

                    segments.addAll(lineSegments);
                }

                // Track time spent on each segment
                LocalDateTime segmentStartTime = previousTime;

                for (LineSegment segment : segments) {
                    // Calculate travel time for this segment
                    Duration travelTime = calculateTravelTime(train, segment);
                    LocalDateTime segmentEndTime = segmentStartTime.plus(travelTime);

                    // Track usage on SINGLE-TRACK segments only (crossings needed here)
                    if (segment.isSingleTrack()) {
                        segmentUsages.computeIfAbsent(segment.getId(), k -> new ArrayList<>())
                                .add(new SegmentUsage(route, train, segmentStartTime, segmentEndTime));
                    }

                    // Move to next segment (advance time)
                    segmentStartTime = segmentEndTime;
                }

                // Find the arrival time at next facility from calculated events
                for (TrainEvent event : events) {
                    if (event.getFacility().getId() == nextFacility.getId()) {
                        previousTime = event.getEventTime();
                        break;
                    }
                }
                previousFacility = nextFacility;
            }

            // Process segment from last path point (or start facility if path is empty) to end facility
            Facility endFacility = route.getEndFacility();
            if (previousFacility.getId() != endFacility.getId()) {
                // Find RailLine(s) connecting last facility to end facility
                List<RailLine> connectingRailLines = railLineRepository.findConnectingLines(
                    previousFacility.getId(), endFacility.getId());

                List<LineSegment> segments = new ArrayList<>();
                for (RailLine railLine : connectingRailLines) {
                    List<LineSegment> lineSegments = lineSegmentRepository.getByRailLineId(railLine.getId());

                    // Determine direction of travel
                    boolean isForward = railLine.getStartFacility().getId() == previousFacility.getId() &&
                                      railLine.getEndFacility().getId() == endFacility.getId();

                    if (!isForward) {
                        lineSegments.sort((a, b) -> Integer.compare(b.getOrderNum(), a.getOrderNum()));
                    } else {
                        lineSegments.sort((a, b) -> Integer.compare(a.getOrderNum(), b.getOrderNum()));
                    }

                    segments.addAll(lineSegments);
                }

                // Track time spent on each segment
                LocalDateTime segmentStartTime = previousTime;

                for (LineSegment segment : segments) {
                    Duration travelTime = calculateTravelTime(train, segment);
                    LocalDateTime segmentEndTime = segmentStartTime.plus(travelTime);

                    // Track usage on SINGLE-TRACK segments only
                    if (segment.isSingleTrack()) {
                        segmentUsages.computeIfAbsent(segment.getId(), k -> new ArrayList<>())
                                .add(new SegmentUsage(route, train, segmentStartTime, segmentEndTime));
                    }

                    segmentStartTime = segmentEndTime;
                }
            }
        }

        // Detect conflicts on single-track segments
        for (Map.Entry<Integer, List<SegmentUsage>> entry : segmentUsages.entrySet()) {
            int segmentId = entry.getKey();
            List<SegmentUsage> usages = entry.getValue();

            if (usages.size() < 2) {
                continue;
            }

            LineSegment segment = lineSegmentRepository.getById(segmentId);

            // Check all pairs for conflicts
            for (int i = 0; i < usages.size(); i++) {
                for (int j = i + 1; j < usages.size(); j++) {
                    SegmentUsage usage1 = usages.get(i);
                    SegmentUsage usage2 = usages.get(j);

                    // Skip if same route (same route appears multiple times on same segment)
                    if (usage1.getRoute().getId() == usage2.getRoute().getId()) {
                        continue;
                    }

                    // Check if time intervals overlap
                    // Two intervals overlap if: end1 > start2 AND end2 > start1
                    boolean timeOverlaps = usage1.getEndTime().isAfter(usage2.getStartTime()) &&
                                          usage2.getEndTime().isAfter(usage1.getStartTime());

                    if (timeOverlaps) {
                        // Conflict detected - find crossing location
                        if (segment == null) {
                            segment = lineSegmentRepository.getById(segmentId);
                            if (segment == null) continue;
                        }

                        // Only create crossing if it's a single-track segment
                        if (!segment.isSingleTrack()) {
                            continue; // Multi-track segments don't need crossings
                        }

                        List<Siding> sidings = sidingsBySegment.get(segmentId);
                        Facility crossingFacility = findCrossingLocation(segment, sidings, facilities);

                        Siding siding = null;
                        if (sidings != null && !sidings.isEmpty()) {
                            siding = sidings.get(0); // Use first available siding
                        }

                        // Determine crossing time (when the second train would enter the segment)
                        LocalDateTime crossingTime;
                        if (usage1.getStartTime().isBefore(usage2.getStartTime())) {
                            // Train 1 enters segment first, train 2 should wait
                            crossingTime = usage2.getStartTime();
                        } else {
                            // Train 2 enters segment first, train 1 should wait
                            crossingTime = usage1.getStartTime();
                        }

                        // Make sure we have valid trains and facility
                        if (usage1.getTrain() != null && usage2.getTrain() != null && crossingFacility != null) {
                            crossings.add(new CrossingOperation(
                                usage1.getTrain(),
                                usage2.getTrain(),
                                usage1.getRoute().getId(),
                                usage2.getRoute().getId(),
                                crossingFacility,
                                siding,
                                crossingTime
                            ));
                        }
                    }
                }
            }
        }

        return crossings;
    }

    /**
     * Find the best location for a crossing operation.
     * This method selects an appropriate facility where two trains can cross,
     * preferring facilities with sidings if available. The location is typically
     * the start facility of the rail line containing the segment.
     * 
     * @param segment the line segment where the crossing will occur
     * @param sidings list of available sidings on the segment (may be empty)
     * @param facilities map of facility ID to Facility object for lookup
     * @return the Facility where the crossing should occur
     * @throws SQLException if a crossing location cannot be determined
     */
    private Facility findCrossingLocation(LineSegment segment, List<Siding> sidings,
                                         Map<Integer, Facility> facilities) throws SQLException {
        // If there's a siding, prefer using it
        if (sidings != null && !sidings.isEmpty()) {
            // Return a facility near the siding (find facility on the rail line)
            RailLine railLine = railLineRepository.getById(segment.getRailLineId());
            if (railLine != null) {
                // Prefer the start facility (where train enters the segment)
                return railLine.getStartFacility();
            }
        }

        // Otherwise, use the facility before the segment (station crossing)
        // Find the start facility of the rail line
        RailLine railLine = railLineRepository.getById(segment.getRailLineId());
        if (railLine != null) {
            return railLine.getStartFacility();
        }

        // Fallback: return first available facility
        if (!facilities.isEmpty()) {
            return facilities.values().iterator().next();
        }

        throw new SQLException("Cannot find crossing location");
    }

    /**
     * Schedule a route: calculate passage times and detect crossings with other routes.
     * This is the main scheduling method that:
     * <ul>
     *   <li>Calculates passage times for all facilities on the route</li>
     *   <li>Saves train events to the database</li>
     *   <li>Detects crossings with other scheduled routes</li>
     *   <li>Filters crossings to only include those involving this route</li>
     * </ul>
     * 
     * @param route the route to schedule
     * @return a SchedulingResult containing the route, calculated events, and detected crossings
     * @throws SQLException if there is a database error during scheduling
     * @throws IllegalArgumentException if the train for the route is not found
     */
    public SchedulingResult scheduleRoute(Route route) throws SQLException {
        Train train = trainRepository.getTrainForRoute(route.getId());
        if (train == null) {
            throw new IllegalArgumentException("Train not found for route: " + route.getId());
        }

        // Calculate route times
        List<TrainEvent> events = calculateRouteTimes(route, train);

        // Save events to database
        trainEventRepository.deleteByRouteId(route.getId());
        for (TrainEvent event : events) {
            trainEventRepository.createEvent(
                event.getRouteId(),
                event.getTrainId(),
                event.getFacility().getId(),
                event.getEventTime()
            );
        }

        // Check for crossings with other scheduled routes (including this newly created route)
        List<Route> allRoutes = routeRepository.getAll();
        List<CrossingOperation> allCrossings = detectCrossings(allRoutes);
        
        // Filter crossings to only include those involving this specific route
        // This ensures we show crossings between the new route and existing routes
        List<CrossingOperation> routeCrossings = new ArrayList<>();
        int currentRouteId = route.getId();
        for (CrossingOperation crossing : allCrossings) {
            // Include crossing if it involves the current route (by route ID, not just train ID)
            if (crossing.getRoute1Id() == currentRouteId || 
                crossing.getRoute2Id() == currentRouteId) {
                routeCrossings.add(crossing);
            }
        }

        return new SchedulingResult(route, events, routeCrossings);
    }

}

