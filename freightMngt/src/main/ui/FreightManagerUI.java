package main.ui;

import main.controller.TrainDispatchController;
import main.controller.TrainSchedulerService;
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
    private final TrainDispatchController controller;
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
            
            System.out.print("URL (default: jdbc:oracle:thin:@localhost:1521:XE): ");
            String url = scanner.nextLine().trim();
            if (url.isEmpty()) {
                url = "jdbc:oracle:thin:@localhost:1521:XE";
            }
            
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            if (username.isEmpty()) {
                System.out.println("Username cannot be empty.");
                return false;
            }
            
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();
            
            System.out.print("Connect as SYSDBA? (y/n, default: yes if user is 'sys'): ");
            String sysdbaInput = scanner.nextLine().trim().toLowerCase();
            boolean asSysdba = false;
            if (sysdbaInput.isEmpty()) {
                // Default to SYSDBA if user is 'sys'
                asSysdba = "sys".equalsIgnoreCase(username);
            } else {
                asSysdba = "y".equals(sysdbaInput) || "yes".equals(sysdbaInput);
            }
            
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

        TrainDispatchController controller = new TrainDispatchController(connection);

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
        int trainId = Integer.parseInt(scanner.nextLine().trim());

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

        // Dispatch the train
        System.out.println("\nDispatching train...");
        try {
            TrainSchedulerService.SchedulingResult result = controller.dispatchTrain(
                    trainId, startFacilityId, endFacilityId, startDate, pathFacilityIds);

            System.out.println("\n✓ Train dispatched successfully!");
            System.out.println("\nSchedule:");
            printSchedule(result);
        } catch (IllegalArgumentException e) {
            System.out.println("\n " + e.getMessage());
            throw e; // Re-throw to be caught by outer exception handler
        }
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
            TrainSchedulerService.SchedulingResult result = controller.getScheduleForRoute(routeId);
            printSchedule(result);
        } catch (IllegalArgumentException e) {
            System.out.println("\n " + e.getMessage());
            throw e;
        }
    }

    private void printSchedule(TrainSchedulerService.SchedulingResult result) throws Exception {
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

        // Show crossing operations
        List<CrossingOperation> crossings = result.getCrossings();
        if (!crossings.isEmpty()) {
            System.out.println("\n=== CROSSING OPERATIONS ===");
            for (CrossingOperation crossing : crossings) {
                System.out.printf("  ⚠ Trains %d and %d will cross at %s\n",
                        crossing.getTrain1().getId(),
                        crossing.getTrain2().getId(),
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
                System.out.printf("\nTrains %d and %d:\n",
                        crossing.getTrain1().getId(),
                        crossing.getTrain2().getId());
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
}

