package main.ui;

import main.controller.TrainDispatchController;
import main.controller.TrainSchedulerService;
import main.controller.RoutePlannerService;
import main.domain.*;
import main.repositories.DatabaseConnection;
import main.repositories.FreightRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * User Interface for Freight Manager to dispatch trains and view schedules
 */
public class FreightManagerUI {
    private final Scanner scanner;
    private TrainDispatchController controller;
    private RoutePlannerService routePlannerService;
    private Connection connection;

    public FreightManagerUI() {
        this.scanner = new Scanner(System.in);
        this.controller = null; // Will be initialized after DB connection
        this.connection = null;
    }

    /**
     * Initialize database connection by prompting user for credentials
     */
    public boolean initializeDatabase() {
        try {
            System.out.println("=== DATABASE CONNECTION ===");
            System.out.println("Enter database connection details:");
            
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            if (username.isEmpty()) {
                System.out.println("Username cannot be empty.");
                return false;
            }
            
            System.out.print("Connect as SYSDBA? (y/n, default: yes if user is 'sys'): ");
            String sysdbaInput = scanner.nextLine().trim().toLowerCase();
            boolean asSysdba = false;
            if (sysdbaInput.isEmpty()) {
                // Default to SYSDBA if user is 'sys'
                asSysdba = "sys".equalsIgnoreCase(username);
            } else {
                asSysdba = "y".equals(sysdbaInput) || "yes".equals(sysdbaInput);
            }
            
            // Show appropriate default URL based on SYSDBA connection
            String defaultUrl;
            String urlPrompt;
            if (asSysdba) {
                defaultUrl = "jdbc:oracle:thin:@localhost:1521:XE";
                urlPrompt = "URL (default for SYSDBA: " + defaultUrl + "): ";
            } else {
                defaultUrl = "jdbc:oracle:thin:@localhost:1521/XEPDB1";
                urlPrompt = "URL (default for regular user: " + defaultUrl + "): ";
            }
            
            System.out.print(urlPrompt);
            String url = scanner.nextLine().trim();
            if (url.isEmpty()) {
                url = defaultUrl;
            }
            
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();
            
            System.out.println("\nConnecting to database...");
            connection = DatabaseConnection.getConnection(url, username, password, asSysdba);
            
            if (connection != null && !connection.isClosed()) {
                System.out.println("✓ Database connection established successfully!");
                return true;
            } else {
                System.out.println("Failed to establish database connection.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error connecting to database: " + e.getMessage());
            System.out.println("\nPlease check:");
            System.out.println("  1. Database is running");
            System.out.println("  2. Connection details are correct");
            System.out.println("  3. If using SYS user, SYSDBA privileges are required");
            return false;
        }
    }

    /**
     * Start the UI
     */
    public void start() {
        System.out.println("\n=== FREIGHT MANAGEMENT SYSTEM - TRAIN SCHEDULER ===");

        if (!initializeDatabase()) {
            System.out.println("\nCannot proceed without database connection.");
            System.out.println("Please ensure:");
            System.out.println("  1. Oracle database is running");
            System.out.println("  2. Database credentials in DatabaseConnection.java are correct");
            System.out.println("  3. Oracle JDBC driver is available in classpath");
            return;
        }

        this.controller = new TrainDispatchController(connection);
        RoutePlannerService routePlannerService = new RoutePlannerService(connection);
        this.routePlannerService = routePlannerService;

        boolean running = true;
        while (running) {
            showMainMenu();
            System.out.print("\nEnter your choice: ");
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        dispatchTrain(controller);
                        break;
                    case "2":
                        viewScheduledRoutes(controller);
                        break;
                    case "3":
                        viewTrainSchedule(controller);
                        break;
                    case "4":
                        viewCrossingOperations(controller);
                        break;
                    case "5":
                        viewTrains(controller);
                        break;
                    case "6":
                        viewFacilities(controller);
                        break;
                    case "7":
                        deleteRoute(controller);
                        break;
                    case "8":
                        viewPendingFreights(routePlannerService);
                        break;
                    case "9":
                        createRoutePlan(routePlannerService);
                        break;
                    case "10":
                        viewRoutePlan(routePlannerService);
                        break;
                    case "0":
                        running = false;
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("\n Error: " + e.getMessage());
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    // Silent rollback on error
                }
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("\n Error: Invalid date/time format. Please use format: yyyy-MM-dd HH:mm:ss (e.g., 2025-10-06 10:00:00)");
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    // Silent rollback on error
                }
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    System.out.println("\n Error: " + errorMessage);
                } else {
                    System.out.println("\n An unexpected error occurred. Please try again.");
                }
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    // Silent rollback on error
                }
            }

            if (running) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("Error closing database connection: " + e.getMessage());
        }
        scanner.close();
    }

    private void showMainMenu() {
        System.out.println("\n=== MAIN MENU ===");
        System.out.println("1. Dispatch a train");
        System.out.println("2. View scheduled routes");
        System.out.println("3. View train schedule with passage times");
        System.out.println("4. View crossing operations");
        System.out.println("5. View all trains");
        System.out.println("6. View all facilities");
        System.out.println("7. Delete a route");
        System.out.println("8. View pending freights");
        System.out.println("9. Create route plan");
        System.out.println("10. View route plan with cargo operations");
        System.out.println("0. Exit");
    }

    private void dispatchTrain(TrainDispatchController controller) throws Exception {
        System.out.println("\n=== DISPATCH TRAIN ===");

        // Show available trains
        List<Train> trains = controller.getAllTrains();
        if (trains.isEmpty()) {
            System.out.println("No trains available.");
            return;
        }

        System.out.println("\nAvailable trains:");
        for (Train train : trains) {
            System.out.printf("  Train ID: %d (Locomotives: %d, Wagons: %d, Power: %.2f kW, Weight: %.2f tons)\n",
                    train.getId(), train.getLocomotives().size(), train.getWagons().size(),
                    train.getTotalPower(), train.getTotalWeight());
        }

        System.out.print("\nEnter train ID: ");
        int trainId;
        try {
            trainId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  Error: Invalid input. Please enter a valid train ID number.");
            return;
        }
        
        // Validate train ID exists
        Train selectedTrain = trains.stream()
                .filter(t -> t.getId() == trainId)
                .findFirst()
                .orElse(null);
        if (selectedTrain == null) {
            System.out.println("  Error: Invalid train ID. Train ID " + trainId + " does not exist.");
            System.out.println("  Please enter a valid train ID from the available trains list.");
            return;
        }

        // Show available facilities
        List<Facility> facilities = controller.getAllFacilities();
        System.out.println("\nAvailable facilities:");
        for (Facility facility : facilities) {
            System.out.printf("  ID: %d - %s\n", facility.getId(), facility.getName());
        }

        System.out.print("\nEnter start facility ID: ");
        String startFacilityInput = scanner.nextLine().trim();
        int startFacilityId;
        try {
            startFacilityId = Integer.parseInt(startFacilityInput);
        } catch (NumberFormatException e) {
            System.out.println("\n Error: Invalid facility ID. Please enter a number.");
            return;
        }

        System.out.print("Enter end facility ID: ");
        String endFacilityInput = scanner.nextLine().trim();
        int endFacilityId;
        try {
            endFacilityId = Integer.parseInt(endFacilityInput);
        } catch (NumberFormatException e) {
            System.out.println("\n Error: Invalid facility ID. Please enter a number.");
            return;
        }

        System.out.print("Enter departure date/time (yyyy-MM-dd HH:mm:ss): ");
        String dateTimeStr = scanner.nextLine().trim();
        LocalDateTime startDate;
        try {
            startDate = TrainDispatchController.parseDateTime(dateTimeStr);
        } catch (java.time.format.DateTimeParseException e) {
            System.out.println("\n Error: Invalid date/time format. Please use format: yyyy-MM-dd HH:mm:ss (e.g., 2025-10-06 10:00:00)");
            return;
        }

        // Manual path definition by Freight Manager
        System.out.println("\n=== DEFINE ROUTE PATH (Manual) ===");
        System.out.println("Enter facility IDs in order (press Enter with empty line to finish):");
        List<Integer> pathFacilityIds = new ArrayList<>();
        while (true) {
            System.out.print("Facility ID (or press Enter to finish): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                break;
            }
            try {
                int facilityId = Integer.parseInt(input);
                pathFacilityIds.add(facilityId);
                Facility facility = controller.getAllFacilities().stream()
                        .filter(f -> f.getId() == facilityId)
                        .findFirst()
                        .orElse(null);
                if (facility != null) {
                    System.out.println("  Added: " + facility.getName());
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid facility ID. Please enter a number.");
            }
        }

        // Show available freight and allow selection
        System.out.println("\n=== SELECT FREIGHT ===");
        List<Freight> unassignedFreight = controller.getUnassignedFreight();
        List<Integer> selectedFreightIds = new ArrayList<>();

        if (!unassignedFreight.isEmpty()) {
            System.out.println("Available unassigned freight:");
            for (Freight freight : unassignedFreight) {
                System.out.printf("  Freight ID: %d - %s -> %s\n",
                        freight.getId(),
                        freight.getOriginFacility().getName(),
                        freight.getDestinationFacility().getName());
            }
            System.out.println("\nEnter freight IDs to assign to this route (press Enter with empty line to finish):");
            while (true) {
                System.out.print("Freight ID (or press Enter to finish): ");
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    break;
                }
                try {
                    int freightId = Integer.parseInt(input);
                    Freight freight = unassignedFreight.stream()
                            .filter(f -> f.getId() == freightId)
                            .findFirst()
                            .orElse(null);
                    if (freight != null) {
                        selectedFreightIds.add(freightId);
                        System.out.println("  Added: Freight " + freightId + " (" +
                                freight.getOriginFacility().getName() + " -> " +
                                freight.getDestinationFacility().getName() + ")");
                    } else {
                        System.out.println("Invalid freight ID or freight is already assigned. Please enter a valid freight ID from the list above.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid freight ID. Please enter a number.");
                }
            }
        } else {
            System.out.println("No unassigned freight available. Route will be created without freight.");
        }

        // Dispatch the train
        System.out.println("\nDispatching train...");
        SchedulingResult result = controller.dispatchTrain(
                trainId, startFacilityId, endFacilityId, startDate, pathFacilityIds, selectedFreightIds);

        System.out.println("\n✓ Train dispatched successfully!");
        System.out.println("\nSchedule:");
        printSchedule(result);
    }

    private void viewScheduledRoutes(TrainDispatchController controller) throws Exception {
        System.out.println("\n=== SCHEDULED ROUTES ===");

        List<Train> trains = controller.getAllTrains();
        for (Train train : trains) {
            List<Route> routes = controller.getRoutesByTrainId(train.getId());
            if (!routes.isEmpty()) {
                System.out.println("\nTrain " + train.getId() + ":");
                for (Route route : routes) {
                    System.out.printf("  Route ID: %d, Start: %s, End: %s, Departure: %s\n",
                            route.getId(),
                            route.getStartFacility().getName(),
                            route.getEndFacility().getName(),
                            route.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
            }
        }
    }

    private void viewTrainSchedule(TrainDispatchController controller) throws Exception {
        System.out.println("\n=== TRAIN SCHEDULE ===");

        System.out.print("Enter route ID: ");
        String routeIdInput = scanner.nextLine().trim();
        int routeId;
        try {
            routeId = Integer.parseInt(routeIdInput);
        } catch (NumberFormatException e) {
            System.out.println(" Error: Invalid route ID. Please enter a number.");
            return;
        }

        try {
            SchedulingResult result = controller.getScheduleForRoute(routeId);
            printSchedule(result);
        } catch (IllegalArgumentException e) {
            System.out.println("\n " + e.getMessage());
            throw e;
        }
    }

    private void printSchedule(SchedulingResult result) throws Exception {
        System.out.println("\n=== ESTIMATED PASSAGE TIMES ===");
        System.out.printf("Route ID: %d, Train ID: %d\n",
                result.getRoute().getId(), result.getRoute().getTrainId());
        System.out.println("Path: " + result.getRoute().getStartFacility().getName() + " -> " +
                result.getRoute().getEndFacility().getName());
        
        // Get freight data for this route
        FreightRepository freightRepo = new FreightRepository(connection, 
            new main.repositories.FacilityRepository(connection));
        Map<Integer, List<Freight>> pickupsByFacility = freightRepo.getPickupsByFacility(result.getRoute().getId());
        Map<Integer, List<Freight>> deliveriesByFacility = freightRepo.getDeliveriesByFacility(result.getRoute().getId());
        
        System.out.println("\nPassage times:");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (TrainEvent event : result.getEvents()) {
            System.out.printf("  %s: %s\n",
                    event.getFacility().getName(),
                    event.getEventTime().format(formatter));
            
            // Show freight operations at this facility
            int facilityId = event.getFacility().getId();
            List<Freight> pickups = pickupsByFacility.get(facilityId);
            List<Freight> deliveries = deliveriesByFacility.get(facilityId);
            
            if (pickups != null && !pickups.isEmpty()) {
                System.out.printf("     PICKUP: Freight loaded at %s\n", event.getFacility().getName());
                for (Freight freight : pickups) {
                    List<Integer> wagonIds = freightRepo.getWagonIdsByFreightId(freight.getId());
                    System.out.printf("      Freight ID %d: %d wagon(s) -> Destination: %s\n",
                        freight.getId(), wagonIds.size(), freight.getDestinationFacility().getName());
                }
            }
            
            if (deliveries != null && !deliveries.isEmpty()) {
                System.out.printf("     DELIVERY: Freight unloaded at %s\n", event.getFacility().getName());
                for (Freight freight : deliveries) {
                    List<Integer> wagonIds = freightRepo.getWagonIdsByFreightId(freight.getId());
                    System.out.printf("      Freight ID %d: %d wagon(s) from %s\n",
                        freight.getId(), wagonIds.size(), freight.getOriginFacility().getName());
                }
            }
        }

        // Show crossing operations involving this route
        List<CrossingOperation> crossings = result.getCrossings();
        if (!crossings.isEmpty()) {
            System.out.println("\n=== CROSSING OPERATIONS ===");
            int currentRouteId = result.getRoute().getId();
            for (CrossingOperation crossing : crossings) {
                // Determine which train/route is from the current route
                Train currentTrain;
                Train otherTrain;
                int otherRouteId;
                if (crossing.getRoute1Id() == currentRouteId) {
                    currentTrain = crossing.getTrain1();
                    otherTrain = crossing.getTrain2();
                    otherRouteId = crossing.getRoute2Id();
                } else {
                    currentTrain = crossing.getTrain2();
                    otherTrain = crossing.getTrain1();
                    otherRouteId = crossing.getRoute1Id();
                }
                
                System.out.printf("Train %d (Route %d) will cross with Train %d (Route %d) at %s\n",
                        currentTrain.getId(),
                        currentRouteId,
                        otherTrain.getId(),
                        otherRouteId,
                        crossing.getCrossingLocation().getName());
                if (crossing.usesSiding()) {
                    System.out.printf("    Using siding (ID: %d) on segment %d\n",
                            crossing.getSiding().getId(),
                            crossing.getSiding().getLineSegmentId());
                } else {
                    System.out.println("    Crossing at station before segment");
                }
                System.out.printf("    Estimated crossing time: %s\n",
                        crossing.getCrossingTime().format(formatter));
            }
        } else {
            System.out.println("\nNo crossing operations required for this route.");
        }
    }

    private void viewCrossingOperations(TrainDispatchController controller) throws Exception {
        System.out.println("\n=== ALL CROSSING OPERATIONS ===");

        // Get all routes and calculate crossings
        List<Train> trains = controller.getAllTrains();
        List<Route> allRoutes = new ArrayList<>();
        for (Train train : trains) {
            allRoutes.addAll(controller.getRoutesByTrainId(train.getId()));
        }

        if (allRoutes.isEmpty()) {
            System.out.println("No routes scheduled.");
            return;
        }

        TrainSchedulerService schedulerService = new TrainSchedulerService(connection);
        List<CrossingOperation> crossings = schedulerService.detectCrossings(allRoutes);
        
        System.out.println("\nDetected " + crossings.size() + " crossing operation(s).");

        if (crossings.isEmpty()) {
            System.out.println("No crossing operations required.");
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (CrossingOperation crossing : crossings) {
                System.out.printf("\nTrain %d (Route %d) and Train %d (Route %d):\n",
                        crossing.getTrain1().getId(),
                        crossing.getRoute1Id(),
                        crossing.getTrain2().getId(),
                        crossing.getRoute2Id());
                System.out.printf("  Location: %s\n", crossing.getCrossingLocation().getName());
                System.out.printf("  Time: %s\n", crossing.getCrossingTime().format(formatter));
                if (crossing.usesSiding()) {
                    System.out.printf("  Using siding ID: %d\n", crossing.getSiding().getId());
                } else {
                    System.out.println("  Crossing at station");
                }
            }
        }
    }

    private void viewTrains(TrainDispatchController controller) throws Exception {
        System.out.println("\n=== ALL TRAINS ===");
        List<Train> trains = controller.getAllTrains();
        for (Train train : trains) {
            System.out.printf("\nTrain ID: %d\n", train.getId());
            System.out.printf("  Locomotives: %d (Total Power: %.2f kW)\n",
                    train.getLocomotives().size(), train.getTotalPower());
            System.out.printf("  Wagons: %d (Total Weight: %.2f tons)\n",
                    train.getWagons().size(), train.getTotalWeight());
            System.out.printf("  Max Speed: %.2f km/h\n", train.getMaxSpeed());
        }
    }

    private void viewFacilities(TrainDispatchController controller) throws Exception {
        System.out.println("\n=== ALL FACILITIES ===");
        List<Facility> facilities = controller.getAllFacilities();
        for (Facility facility : facilities) {
            System.out.printf("  ID: %d - %s\n", facility.getId(), facility.getName());
        }
    }

    private void deleteRoute(TrainDispatchController controller) throws Exception {
        System.out.println("\n=== DELETE ROUTE ===");
        
        // Show all routes
        List<Train> trains = controller.getAllTrains();
        List<Route> allRoutes = new ArrayList<>();
        for (Train train : trains) {
            allRoutes.addAll(controller.getRoutesByTrainId(train.getId()));
        }
        
        if (allRoutes.isEmpty()) {
            System.out.println("No routes scheduled.");
            return;
        }
        
        System.out.println("\nScheduled routes:");
        for (Route route : allRoutes) {
            System.out.printf("  Route ID: %d - Train %d: %s -> %s (Departure: %s)\n",
                    route.getId(),
                    route.getTrainId(),
                    route.getStartFacility().getName(),
                    route.getEndFacility().getName(),
                    route.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        System.out.print("\nEnter route ID to delete: ");
        String routeIdInput = scanner.nextLine().trim();
        int routeId;
        try {
            routeId = Integer.parseInt(routeIdInput);
        } catch (NumberFormatException e) {
            System.out.println("\n Error: Invalid route ID. Please enter a number.");
            return;
        }
        
        System.out.print("Are you sure you want to delete Route " + routeId + "? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        
        if ("yes".equals(confirmation) || "y".equals(confirmation)) {
            try {
                boolean deleted = controller.deleteRoute(routeId);
                if (deleted) {
                    System.out.println("✓ Route " + routeId + " deleted successfully!");
                } else {
                    System.out.println(" Failed to delete route. Route may not exist.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("\n " + e.getMessage());
                throw e;
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private void viewPendingFreights(RoutePlannerService routePlannerService) throws Exception {
        System.out.println("\n=== PENDING FREIGHTS ===");
        
        List<Freight> pendingFreights = routePlannerService.getPendingFreights();
        
        if (pendingFreights.isEmpty()) {
            System.out.println("No pending (unassigned) freights available.");
            return;
        }
        
        System.out.println("Total pending freights: " + pendingFreights.size());
        System.out.println("\nPending Freights:");
        for (Freight freight : pendingFreights) {
            System.out.printf("  Freight ID: %d\n", freight.getId());
            System.out.printf("    Origin: %s (ID: %d)\n", 
                    freight.getOriginFacility().getName(), 
                    freight.getOriginFacility().getId());
            System.out.printf("    Destination: %s (ID: %d)\n",
                    freight.getDestinationFacility().getName(),
                    freight.getDestinationFacility().getId());
        }
    }

    private void createRoutePlan(RoutePlannerService routePlannerService) throws Exception {
        System.out.println("\n=== CREATE ROUTE PLAN ===");
        
        // Show pending freights
        List<Freight> pendingFreights = routePlannerService.getPendingFreights();
        if (pendingFreights.isEmpty()) {
            System.out.println("No pending freights available. Cannot create route plan.");
            return;
        }
        
        System.out.println("\nPending freights:");
        for (Freight freight : pendingFreights) {
            System.out.printf("  Freight ID: %d - %s -> %s\n",
                    freight.getId(),
                    freight.getOriginFacility().getName(),
                    freight.getDestinationFacility().getName());
        }
        
        // Get train information
        List<Train> trains = controller.getAllTrains();
        if (trains.isEmpty()) {
            System.out.println("No trains available.");
            return;
        }
        
        System.out.println("\nAvailable trains:");
        for (Train train : trains) {
            System.out.printf("  Train ID: %d\n", train.getId());
        }
        
        System.out.print("\nEnter train ID: ");
        int trainId;
        try {
            trainId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  Error: Invalid input. Please enter a valid train ID number.");
            return;
        }
        
        // Validate train ID exists
        Train selectedTrain = trains.stream()
                .filter(t -> t.getId() == trainId)
                .findFirst()
                .orElse(null);
        if (selectedTrain == null) {
            System.out.println("  Error: Invalid train ID. Train ID " + trainId + " does not exist.");
            System.out.println("  Please enter a valid train ID from the available trains list.");
            return;
        }
        
        // Get facilities
        List<Facility> facilities = controller.getAllFacilities();
        System.out.println("\nAvailable facilities:");
        for (Facility facility : facilities) {
            System.out.printf("  ID: %d - %s\n", facility.getId(), facility.getName());
        }
        
        System.out.print("\nEnter start facility ID: ");
        int startFacilityId = Integer.parseInt(scanner.nextLine().trim());
        
        System.out.print("Enter end facility ID: ");
        int endFacilityId = Integer.parseInt(scanner.nextLine().trim());
        
        System.out.print("Enter departure date/time (yyyy-MM-dd HH:mm:ss): ");
        String dateTimeStr = scanner.nextLine().trim();
        LocalDateTime startDate = TrainDispatchController.parseDateTime(dateTimeStr);
        
        // Get intermediate facilities (path points)
        System.out.println("\n=== DEFINE ROUTE PATH ===");
        System.out.println("Enter intermediate facility IDs in order (press Enter with empty line to finish):");
        List<Integer> intermediateFacilityIds = new ArrayList<>();
        
        // Determine the current facility to show connections from
        int currentFacilityId = startFacilityId;
        
        while (true) {
            // Show connected facilities to help user choose next path point
            try {
                final int currentFacilityIdForLambda = currentFacilityId;
                List<Facility> connectedFacilities = routePlannerService.getConnectedFacilities(currentFacilityId);
                if (!connectedFacilities.isEmpty()) {
                    System.out.println("\nFacilities connected to " + 
                        facilities.stream().filter(f -> f.getId() == currentFacilityIdForLambda).findFirst()
                            .map(Facility::getName).orElse("current facility") + 
                        " (ID: " + currentFacilityId + "):");
                    
                    // Separate facilities that can reach the end vs those already visited
                    List<Facility> canReachEnd = new ArrayList<>();
                    List<Facility> alreadyVisited = new ArrayList<>();
                    
                    for (Facility connectedFacility : connectedFacilities) {
                        int facilityId = connectedFacility.getId();
                        
                        // Skip start facility only if we're at the start facility itself (no self-connection needed)
                        if (facilityId == startFacilityId && currentFacilityId == startFacilityId) {
                            continue;
                        }
                        
                        // Check if facility has been visited (is in the path)
                        boolean isVisited = intermediateFacilityIds.contains(facilityId);
                        
                        if (facilityId == startFacilityId) {
                            if (isVisited) {
                                alreadyVisited.add(connectedFacility);
                            } else {
                                if (routePlannerService.hasPath(startFacilityId, endFacilityId)) {
                                    canReachEnd.add(connectedFacility);
                                }
                            }
                        } else if (facilityId == endFacilityId) {
                            canReachEnd.add(connectedFacility);
                        } else if (isVisited) {
                            alreadyVisited.add(connectedFacility);
                        } else if (routePlannerService.hasPath(facilityId, endFacilityId)) {
                            canReachEnd.add(connectedFacility);
                        }
                    }
                    
                    // Show facilities that can reach the end
                    for (Facility connectedFacility : canReachEnd) {
                        int facilityId = connectedFacility.getId();
                        if (facilityId == startFacilityId) {
                            System.out.printf("  ID: %d - %s (origin)\n", facilityId, connectedFacility.getName());
                        } else if (facilityId == endFacilityId) {
                            System.out.printf("  ID: %d - %s (destination)\n", facilityId, connectedFacility.getName());
                        } else {
                            System.out.printf("  ID: %d - %s\n", facilityId, connectedFacility.getName());
                        }
                    }
                    
                    // Show already visited facilities
                    if (!alreadyVisited.isEmpty()) {
                        System.out.println("  (Already visited):");
                        for (Facility connectedFacility : alreadyVisited) {
                            int facilityId = connectedFacility.getId();
                            if (facilityId == startFacilityId) {
                                System.out.printf("  ID: %d - %s (origin)\n", facilityId, connectedFacility.getName());
                            } else if (facilityId == endFacilityId) {
                                System.out.printf("  ID: %d - %s (destination)\n", facilityId, connectedFacility.getName());
                            } else {
                                System.out.printf("  ID: %d - %s\n", facilityId, connectedFacility.getName());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // If we can't get connected facilities, continue without showing them
            }
            
            System.out.print("\nFacility ID (or press Enter to finish): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                break;
            }
            try {
                int facilityId = Integer.parseInt(input);
                Facility facility = facilities.stream()
                        .filter(f -> f.getId() == facilityId)
                        .findFirst()
                        .orElse(null);
                
                if (facility == null) {
                    System.out.println("  Error: Invalid facility ID. Facility ID " + facilityId + " does not exist.");
                    System.out.println("  Please enter a valid facility ID from the available facilities list.");
                    continue;
                }
                
                // Prevent adding the same facility twice in a row
                if (!intermediateFacilityIds.isEmpty() && 
                    intermediateFacilityIds.get(intermediateFacilityIds.size() - 1).equals(facilityId)) {
                    System.out.println("  Error: Cannot add the same facility twice in a row. Please choose a different facility.");
                    continue;
                }
                
                intermediateFacilityIds.add(facilityId);
                System.out.println("  Added: " + facility.getName());
                // Update current facility for next iteration
                currentFacilityId = facilityId;
            } catch (NumberFormatException e) {
                System.out.println("  Error: Invalid input. Please enter a valid facility ID number.");
            }
        }
        
        // Create route (simple if no intermediates, complex if intermediates exist)
        int routeId;
        if (intermediateFacilityIds.isEmpty()) {
            routeId = routePlannerService.createSimpleRoute(trainId, startFacilityId, endFacilityId, startDate);
        } else {
            routeId = routePlannerService.createComplexRoute(trainId, startFacilityId, endFacilityId, 
                    intermediateFacilityIds, startDate);
        }
        
        System.out.println("\n✓ Route created with ID: " + routeId);
        
        // Assign freights to route
        System.out.println("\nAssign freights to this route:");
        List<Integer> freightIds = new ArrayList<>();
        while (true) {
            // Filter out already selected freights
            List<Freight> availableFreights = pendingFreights.stream()
                    .filter(f -> !freightIds.contains(f.getId()))
                    .collect(java.util.stream.Collectors.toList());
            
            // Show available freight list before each prompt (excluding already selected)
            if (!availableFreights.isEmpty()) {
                System.out.println("\nAvailable unassigned freight:");
                for (Freight freight : availableFreights) {
                    System.out.printf("  Freight ID: %d - %s -> %s\n",
                            freight.getId(),
                            freight.getOriginFacility().getName(),
                            freight.getDestinationFacility().getName());
                }
            } else {
                System.out.println("\nNo more unassigned freight available.");
            }
            
            System.out.println("\nEnter freight IDs to assign (press Enter with empty line to finish):");
            System.out.print("Freight ID (or press Enter to finish): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                break;
            }
            try {
                int freightId = Integer.parseInt(input);
                Freight freight = availableFreights.stream()
                        .filter(f -> f.getId() == freightId)
                        .findFirst()
                        .orElse(null);
                if (freight != null) {
                    freightIds.add(freightId);
                    System.out.println("  Added: Freight " + freightId);
                } else {
                    System.out.println("  Error: Invalid freight ID. Please enter a valid freight ID from the list above.");
                }
            } catch (NumberFormatException e) {
                System.out.println("  Error: Invalid input. Please enter a valid freight ID number.");
            }
        }
        
        if (!freightIds.isEmpty()) {
            routePlannerService.assignFreightsToRoute(freightIds, routeId);
            System.out.println("\n✓ Assigned " + freightIds.size() + " freight(s) to route " + routeId);
            connection.commit();
        } else {
            System.out.println("\nNo freights assigned to this route.");
        }
    }

    private void viewRoutePlan(RoutePlannerService routePlannerService) throws Exception {
        System.out.println("\n=== VIEW ROUTE PLAN WITH CARGO OPERATIONS ===");
        
        // Show all routes
        List<Train> trains = controller.getAllTrains();
        List<Route> allRoutes = new ArrayList<>();
        for (Train train : trains) {
            allRoutes.addAll(controller.getRoutesByTrainId(train.getId()));
        }
        
        if (allRoutes.isEmpty()) {
            System.out.println("No routes available.");
            return;
        }
        
        System.out.println("\nAvailable routes:");
        for (Route route : allRoutes) {
            System.out.printf("  Route ID: %d - Train %d: %s -> %s (Departure: %s)\n",
                    route.getId(),
                    route.getTrainId(),
                    route.getStartFacility().getName(),
                    route.getEndFacility().getName(),
                    route.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        System.out.print("\nEnter route ID: ");
        String routeIdInput = scanner.nextLine().trim();
        int routeId;
        try {
            routeId = Integer.parseInt(routeIdInput);
        } catch (NumberFormatException e) {
            System.out.println("\n Error: Invalid route ID. Please enter a number.");
            return;
        }
        
        try {
            String routePlanText = routePlannerService.presentRoutePlan(routeId);
            System.out.println("\n" + routePlanText);
        } catch (IllegalArgumentException e) {
            System.out.println("\n Error: " + e.getMessage());
        }
    }
}

