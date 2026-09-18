package main.ui;

import main.controller.*;
import main.domain.*;
import main.repositories.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * User Interface for Freight Management System
 * Provides access to all freight management operations organized by functional areas
 */
public class FreightManagerUI {
    private final Scanner scanner;
    private Connection connection;
    
    // Repositories
    private FacilityRepository facilityRepository;
    private FreightRepository freightRepository;
    private TrainRepository trainRepository;
    private LocomotiveRepository locomotiveRepository;
    private WagonRepository wagonRepository;
    private RouteRepository routeRepository;
    private RailLineRepository railLineRepository;
    private LineSegmentRepository lineSegmentRepository;
    
    // Controllers
    private TrainDispatchController dispatchController;
    private TrainSchedulerController schedulerController;
    private TrainAssemblyController assemblyController;
    private RoutePlannerService routePlannerService;
    private AutomaticPathService automaticPathService;

    public FreightManagerUI() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Initialize database connection by prompting user for credentials
     */
    public boolean initializeDatabase() {
        try {
            System.out.println("\n=== DATABASE CONNECTION ===");
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
                asSysdba = "sys".equalsIgnoreCase(username);
            } else {
                asSysdba = "y".equals(sysdbaInput) || "yes".equals(sysdbaInput);
            }
            
            String defaultUrl = asSysdba 
                ? "jdbc:oracle:thin:@localhost:1521:XE"
                : "jdbc:oracle:thin:@localhost:1521/XEPDB1";
            
            System.out.print("URL (default: " + defaultUrl + "): ");
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
                initializeRepositories();
                initializeControllers();
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
     * Initialize all repositories
     */
    private void initializeRepositories() {
        this.facilityRepository = new FacilityRepository(connection);
        this.freightRepository = new FreightRepository(connection, facilityRepository);
        this.locomotiveRepository = new LocomotiveRepository(connection);
        this.wagonRepository = new WagonRepository(connection);
        this.trainRepository = new TrainRepository(connection, locomotiveRepository, wagonRepository);
        this.routeRepository = new RouteRepository(connection, facilityRepository);
        this.railLineRepository = new RailLineRepository(connection, facilityRepository);
        this.lineSegmentRepository = new LineSegmentRepository(connection);
    }

    /**
     * Initialize all controllers
     */
    private void initializeControllers() {
        this.dispatchController = new TrainDispatchController(connection);
        this.schedulerController = new TrainSchedulerController(connection);
        this.routePlannerService = new RoutePlannerService(connection);
        this.automaticPathService = new AutomaticPathService(connection);
        this.assemblyController = new TrainAssemblyController(
            connection, routeRepository, trainRepository, locomotiveRepository, wagonRepository);
    }

    /**
     * Start the UI
     */
    public void start() {
        System.out.println("\n=== FREIGHT MANAGEMENT SYSTEM ===");

        if (!initializeDatabase()) {
            System.out.println("\nCannot proceed without database connection.");
            System.out.println("Please ensure:");
            System.out.println("  1. Oracle database is running");
            System.out.println("  2. Database credentials are correct");
            System.out.println("  3. Oracle JDBC driver is available in classpath");
            return;
        }

        boolean running = true;
        while (running) {
            showMainMenu();
            System.out.print("\nEnter your choice: ");
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        showRoutePlanningMenu();
                        break;
                    case "2":
                        showTrainAssemblyMenu();
                        break;
                    case "3":
                        showTrainSchedulingMenu();
                        break;
                    case "4":
                        showViewMenu();
                        break;
                    case "0":
                        running = false;
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("\n✗ Error: " + e.getMessage());
                rollback();
            } catch (DateTimeParseException e) {
                System.out.println("\n✗ Error: Invalid date/time format. Please use format: yyyy-MM-dd HH:mm:ss");
                rollback();
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    System.out.println("\n✗ Error: " + errorMessage);
                } else {
                    System.out.println("\n✗ An unexpected error occurred. Please try again.");
                }
                rollback();
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
        System.out.println("1. Route Planning");
        System.out.println("2. Train Assembly");
        System.out.println("3. Train Scheduling");
        System.out.println("4. View Information");
        System.out.println("0. Exit");
    }

    // ============================================================================
    // ROUTE PLANNING MENU
    // ============================================================================

    private void showRoutePlanningMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== ROUTE PLANNING ===");
            System.out.println("1. View pending freights");
            System.out.println("2. Create route plan");
            System.out.println("3. View route plan with cargo operations");
            System.out.println("4. Delete route");
            System.out.println("0. Back to main menu");
            System.out.print("\nEnter your choice: ");
            
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    viewPendingFreights();
                    break;
                case "2":
                    createRoutePlan();
                    break;
                case "3":
                    viewRoutePlan();
                    break;
                case "4":
                    deleteRoute();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void viewPendingFreights() {
        System.out.println("\n=== PENDING FREIGHTS ===");
        try {
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
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void createRoutePlan() {
        System.out.println("\n=== CREATE ROUTE PLAN ===");
        try {
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
            List<Train> trains = dispatchController.getAllTrains();
            if (trains.isEmpty()) {
                System.out.println("No trains available.");
                return;
            }
            
            System.out.println("\nAvailable trains:");
            for (Train train : trains) {
                System.out.printf("  Train ID: %d\n", train.getId());
            }
            
            System.out.print("\nEnter train ID: ");
            int trainId = Integer.parseInt(scanner.nextLine().trim());
            
            // Validate train ID
            Train selectedTrain = trains.stream()
                .filter(t -> t.getId() == trainId)
                .findFirst()
                .orElse(null);
            if (selectedTrain == null) {
                System.out.println("✗ Invalid train ID.");
                return;
            }
            
            // Get facilities
            List<Facility> facilities = dispatchController.getAllFacilities();
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
            int currentFacilityId = startFacilityId;
            
            while (true) {
                try {
                    List<Facility> connectedFacilities = routePlannerService.getConnectedFacilities(currentFacilityId);
                    if (!connectedFacilities.isEmpty()) {
                        System.out.println("\nFacilities connected to current facility (ID: " + currentFacilityId + "):");
                        for (Facility connected : connectedFacilities) {
                            if (connected.getId() == endFacilityId) {
                                System.out.printf("  ID: %d - %s (destination)\n", connected.getId(), connected.getName());
                            } else if (!intermediateFacilityIds.contains(connected.getId())) {
                                System.out.printf("  ID: %d - %s\n", connected.getId(), connected.getName());
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue without showing connections
                }
                
                System.out.print("\nFacility ID (or press Enter to finish): ");
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    break;
                }
                
                int facilityId = Integer.parseInt(input);
                Facility facility = facilities.stream()
                    .filter(f -> f.getId() == facilityId)
                    .findFirst()
                    .orElse(null);
                
                if (facility == null) {
                    System.out.println("✗ Invalid facility ID.");
                    continue;
                }
                
                if (!intermediateFacilityIds.isEmpty() && 
                    intermediateFacilityIds.get(intermediateFacilityIds.size() - 1).equals(facilityId)) {
                    System.out.println("✗ Cannot add the same facility twice in a row.");
                    continue;
                }
                
                intermediateFacilityIds.add(facilityId);
                System.out.println("✓ Added: " + facility.getName());
                currentFacilityId = facilityId;
            }
            
            // Create route
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
                List<Freight> availableFreights = pendingFreights.stream()
                    .filter(f -> !freightIds.contains(f.getId()))
                    .collect(java.util.stream.Collectors.toList());
                
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
                
                System.out.print("\nFreight ID (or press Enter to finish): ");
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
                        System.out.println("✓ Added: Freight " + freightId);
                    } else {
                        System.out.println("✗ Invalid freight ID.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("✗ Invalid input. Please enter a number.");
                }
            }
            
            if (!freightIds.isEmpty()) {
                routePlannerService.assignFreightsToRoute(freightIds, routeId);
                System.out.println("\n✓ Assigned " + freightIds.size() + " freight(s) to route " + routeId);
                connection.commit();
            } else {
                System.out.println("\nNo freights assigned to this route.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating route plan: " + e.getMessage(), e);
        }
    }

    private void viewRoutePlan() {
        System.out.println("\n=== VIEW ROUTE PLAN WITH CARGO OPERATIONS ===");
        try {
            // Get unique routes (not grouped by train)
            List<Route> allRoutes = routeRepository.getAll();
            
            if (allRoutes.isEmpty()) {
                System.out.println("No routes available.");
                return;
            }
            
            System.out.println("\nAvailable routes:");
            for (Route route : allRoutes) {
                String path = buildRoutePath(route);
                System.out.printf("  Route ID: %d: %s\n",
                    route.getId(),
                    path);
            }
            
            Integer routeId = getRouteIdFromUser();
            if (routeId == null) {
                return;
            }
            
            if (!validateRouteId(routeId)) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
            // Get all planned trains for this route
            List<TrainAssemblyController.PlannedTrainInfo> plannedTrains = assemblyController.getAllPlannedTrains();
            List<TrainAssemblyController.PlannedTrainInfo> trainsForRoute = new ArrayList<>();
            for (TrainAssemblyController.PlannedTrainInfo train : plannedTrains) {
                if (train.getRouteId() == routeId) {
                    trainsForRoute.add(train);
                }
            }
            
            LocalDateTime selectedStartDate = null;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            if (trainsForRoute.isEmpty()) {
                // No planned trains, use route's startDate if available
                Route route = routeRepository.getById(routeId);
                if (route != null && route.getStartDate() != null) {
                    selectedStartDate = route.getStartDate();
                } else {
                    System.out.println("No planned trains found for this route and route has no start date.");
                    return;
                }
            } else if (trainsForRoute.size() == 1) {
                // Only one planned train, use it
                selectedStartDate = trainsForRoute.get(0).getStartDate();
                System.out.println("\nUsing planned train with start date: " + selectedStartDate.format(formatter));
            } else {
                // Multiple planned trains, let user select
                System.out.println("\nMultiple planned trains found for Route ID: " + routeId);
                DateTimeFormatter tableFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                for (int i = 0; i < trainsForRoute.size(); i++) {
                    TrainAssemblyController.PlannedTrainInfo train = trainsForRoute.get(i);
                    System.out.printf("  %d. Train %d - Start: %s\n",
                        (i + 1), train.getTrainId(), train.getStartDate().format(tableFormatter));
                }
                System.out.print("\nSelect train travel (1-" + trainsForRoute.size() + "): ");
                String input = scanner.nextLine().trim();
                try {
                    int selection = Integer.parseInt(input);
                    if (selection < 1 || selection > trainsForRoute.size()) {
                        System.out.println("✗ Invalid selection.");
                        return;
                    }
                    selectedStartDate = trainsForRoute.get(selection - 1).getStartDate();
                } catch (NumberFormatException e) {
                    System.out.println("✗ Invalid input. Please enter a number.");
                    return;
                }
            }
            
            // Generate and display route plan
            String routePlanText = routePlannerService.presentRoutePlan(routeId, selectedStartDate);
            System.out.println("\n" + routePlanText);
        } catch (Exception e) {
            throw new RuntimeException("Error viewing route plan: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // TRAIN ASSEMBLY MENU
    // ============================================================================

    private void showTrainAssemblyMenu() throws SQLException {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== TRAIN ASSEMBLY ===");
            System.out.println("1. Assemble and assign train to route");
            System.out.println("2. View assembled train for route");
            System.out.println("3. View available locomotives for route");
            System.out.println("4. View available wagons for route");
            System.out.println("5. Delete planned train");
            System.out.println("0. Back to main menu");
            System.out.print("\nEnter your choice: ");
            
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    assembleTrain();
                    break;
                case "2":
                    viewAssembledTrain();
                    break;
                case "3":
                    viewAvailableLocomotives();
                    break;
                case "4":
                    viewAvailableWagons();
                    break;
                case "5":
                    deletePlannedTrain();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void assembleTrain() throws SQLException {
        System.out.println("\n=== ASSEMBLE TRAIN TO ROUTE ===");
        
        // Display available routes
        displayAvailableRoutes();
        
        System.out.print("\nEnter Route ID: ");
        String routeIdInput = scanner.nextLine().trim();
        int routeId;
        try {
            routeId = Integer.parseInt(routeIdInput);
        } catch (NumberFormatException e) {
            System.out.println("Invalid Route ID. Please enter a number.");
            return;
        }

        // Validate route exists
        Route route = routeRepository.getById(routeId);
        if (route == null) {
            System.out.println("Route with ID " + routeId + " does not exist.");
            return;
        }

        // Ask for start date/time for the planned train
        System.out.print("\nEnter start date/time for the planned train (yyyy-MM-dd HH:mm:ss): ");
        String dateTimeStr = scanner.nextLine().trim();
        LocalDateTime requestedStartDate;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            requestedStartDate = LocalDateTime.parse(dateTimeStr, formatter);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date/time format. Please use format: yyyy-MM-dd HH:mm:ss (e.g., 2025-10-03 09:45:00)");
            return;
        }

        // Check if date/time conflicts with existing planned train for this route
        // Keep asking until a non-conflicting date/time is provided
        while (assemblyController.checkDateConflict(routeId, requestedStartDate)) {
            System.out.println("\n✗ Error: A planned train already exists for route " + routeId + 
                " at the selected date/time (" + dateTimeStr + ").");
            System.out.print("Please enter a different date/time (yyyy-MM-dd HH:mm:ss): ");
            dateTimeStr = scanner.nextLine().trim();
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                requestedStartDate = LocalDateTime.parse(dateTimeStr, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date/time format. Please use format: yyyy-MM-dd HH:mm:ss (e.g., 2025-10-03 09:45:00)");
                return;
            }
        }

        // Ensure Planned_Train exists for this route and date/time
        try {
            int trainId = assemblyController.ensurePlannedTrain(routeId, requestedStartDate);
            System.out.println("✓ Planned train (ID: " + trainId + ") ready for route " + routeId + 
                " at " + dateTimeStr);
        } catch (IllegalArgumentException e) {
            System.out.println("\n✗ Error: " + e.getMessage());
            return;
        }

        // Get available locomotives filtered by requested date/time
        List<LocomotiveForAssembly> locomotives = assemblyController.getAvailableLocomotives(routeId, requestedStartDate);

        // Assign locomotives one by one
        List<Integer> assignedLocomotiveIds = new ArrayList<>();
        if (!locomotives.isEmpty()) {
            System.out.println("\n--- Assign Locomotives (for " + dateTimeStr + ") ---");
            // Show table once at the beginning
            displayLocomotives(locomotives);
            System.out.println("\nEnter locomotive IDs one by one (press Enter with empty input to finish):");
            
            while (true) {
                System.out.print("Enter locomotive ID (or press Enter to finish): ");
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    break;
                }
                
                try {
                    int locoId = Integer.parseInt(input);
                    
                    // Validate locomotive is available and not in-transit
                    LocomotiveForAssembly selected = locomotives.stream()
                        .filter(l -> l.getLocomotive().getId() == locoId)
                        .findFirst()
                        .orElse(null);
                    
                    if (selected == null) {
                        System.out.println("✗ Locomotive " + locoId + " is not in the available list.");
                        continue;
                    }
                    
                    if (selected.isInTransit()) {
                        System.out.println("✗ Locomotive " + locoId + " is IN-TRANSIT at the requested time and cannot be assigned.");
                        System.out.println("   Location: " + selected.getLocationDescription());
                        continue;
                    }
                    
                    if (assignedLocomotiveIds.contains(locoId)) {
                        System.out.println("✗ Locomotive " + locoId + " has already been assigned.");
                        continue;
                    }
                    
                    // Validate that locomotive is parked at the route's start facility
                    if (selected.getParkedFacilityId() != null && 
                        !selected.getParkedFacilityId().equals(route.getStartFacility().getId())) {
                        System.out.println("✗ Warning: Locomotive " + locoId + " is parked at " + 
                            selected.getParkedFacilityName() + ", but the route starts at " + 
                            route.getStartFacility().getName() + ".");
                        System.out.println("   It is not possible to select this locomotive. Please choose a different one.");
                        continue;
                    }
                    
                    // Assign locomotive
                    try {
                        assemblyController.assignLocomotiveToRoute(locoId, routeId, requestedStartDate);
                        assignedLocomotiveIds.add(locoId);
                        System.out.println("✓ Locomotive " + locoId + " assigned successfully.");
                    } catch (IllegalArgumentException e) {
                        System.out.println("✗ Error: " + e.getMessage());
                    }
                } catch (NumberFormatException e) {
                    System.out.println("✗ Invalid input. Please enter a locomotive ID number or press Enter to finish.");
                }
            }
        }

        // Get available wagons filtered by requested date/time
        List<WagonForAssembly> wagons = assemblyController.getAvailableWagons(routeId, requestedStartDate);

        // Assign wagons one by one
        List<Integer> assignedWagonIds = new ArrayList<>();
        if (!wagons.isEmpty()) {
            System.out.println("\n--- Assign Wagons (for " + dateTimeStr + ") ---");
            // Show table once at the beginning
            displayWagons(wagons);
            System.out.println("\nEnter wagon IDs one by one (press Enter with empty input to finish):");
            
            while (true) {
                System.out.print("Enter wagon ID (or press Enter to finish): ");
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    break;
                }
                
                try {
                    int wagonId = Integer.parseInt(input);
                    
                    // Validate wagon is available and not in-transit
                    WagonForAssembly selected = wagons.stream()
                        .filter(w -> w.getWagon().getId() == wagonId)
                        .findFirst()
                        .orElse(null);
                    
                    if (selected == null) {
                        System.out.println("✗ Wagon " + wagonId + " is not in the available list.");
                        continue;
                    }
                    
                    if (selected.isInTransit()) {
                        System.out.println("✗ Wagon " + wagonId + " is IN-TRANSIT at the requested time and cannot be assigned.");
                        System.out.println("   Location: " + selected.getLocationDescription());
                        continue;
                    }
                    
                    if (assignedWagonIds.contains(wagonId)) {
                        System.out.println("✗ Wagon " + wagonId + " has already been assigned.");
                        continue;
                    }
                    
                    // Validate that wagon is parked at the route's start facility
                    if (selected.getParkedFacilityId() != null && 
                        !selected.getParkedFacilityId().equals(route.getStartFacility().getId())) {
                        System.out.println("✗ Warning: Wagon " + wagonId + " is parked at " + 
                            selected.getParkedFacilityName() + ", but the route starts at " + 
                            route.getStartFacility().getName() + ".");
                        System.out.println("   It is not possible to select this wagon. Please choose a different one.");
                        continue;
                    }
                    
                    // Assign wagon
                    try {
                        assemblyController.assignWagonToRoute(wagonId, routeId, requestedStartDate);
                        assignedWagonIds.add(wagonId);
                        System.out.println("✓ Wagon " + wagonId + " assigned successfully.");
                    } catch (IllegalArgumentException e) {
                        System.out.println("✗ Error: " + e.getMessage());
                    }
                } catch (NumberFormatException e) {
                    System.out.println("✗ Invalid input. Please enter a wagon ID number or press Enter to finish.");
                }
            }
        }

        // Validate that at least one locomotive and one wagon are assigned
        if (assignedLocomotiveIds.isEmpty()) {
            System.out.println("\n✗ Error: A train must have at least one locomotive assigned.");
            connection.rollback();
            return;
        }
        
        if (assignedWagonIds.isEmpty()) {
            System.out.println("\n✗ Error: A train must have at least one wagon assigned.");
            connection.rollback();
            return;
        }
        
        connection.commit();
        System.out.println("\n✓ Train assembly completed successfully!");
        System.out.println("   - Locomotives assigned: " + assignedLocomotiveIds.size());
        System.out.println("   - Wagons assigned: " + assignedWagonIds.size());
    }

    private void viewAssembledTrain() {
        System.out.println("\n=== VIEW ASSEMBLED TRAIN ===");
        try {
            // Get unique routes (not grouped by train)
            List<Route> allRoutes = routeRepository.getAll();
            
            if (allRoutes.isEmpty()) {
                System.out.println("No routes available.");
                return;
            }
            
            displayAvailableRoutes();
            
            Integer routeId = getRouteIdFromUser();
            if (routeId == null) {
                return;
            }
            
            if (!validateRouteId(routeId)) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
            // Get all planned trains for this route
            List<TrainAssemblyController.PlannedTrainInfo> plannedTrains = assemblyController.getAllPlannedTrains();
            List<TrainAssemblyController.PlannedTrainInfo> trainsForRoute = new ArrayList<>();
            for (TrainAssemblyController.PlannedTrainInfo train : plannedTrains) {
                if (train.getRouteId() == routeId) {
                    trainsForRoute.add(train);
                }
            }
            
            LocalDateTime selectedStartDate = null;
            if (trainsForRoute.isEmpty()) {
                System.out.println("No planned trains found for route " + routeId + ".");
                return;
            } else if (trainsForRoute.size() == 1) {
                // Only one planned train, use it
                selectedStartDate = trainsForRoute.get(0).getStartDate();
            } else {
                // Multiple planned trains, let user select
                System.out.println("\nMultiple planned trains found for Route ID: " + routeId);
                DateTimeFormatter tableFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                for (int i = 0; i < trainsForRoute.size(); i++) {
                    TrainAssemblyController.PlannedTrainInfo train = trainsForRoute.get(i);
                    System.out.printf("  %d. Train %d - Start: %s\n",
                        (i + 1), train.getTrainId(), train.getStartDate().format(tableFormatter));
                }
                System.out.print("\nSelect train travel (1-" + trainsForRoute.size() + "): ");
                String input = scanner.nextLine().trim();
                try {
                    int selection = Integer.parseInt(input);
                    if (selection < 1 || selection > trainsForRoute.size()) {
                        System.out.println("✗ Invalid selection.");
                        return;
                    }
                    selectedStartDate = trainsForRoute.get(selection - 1).getStartDate();
                } catch (NumberFormatException e) {
                    System.out.println("✗ Invalid input. Please enter a number.");
                    return;
                }
            }
            
            // Get the train for the selected planned train
            Train train = trainRepository.getTrainForRoute(routeId, selectedStartDate);
            if (train == null) {
                System.out.println("No train assigned to route " + routeId + " for start date " + selectedStartDate + ".");
                return;
            }
            
            System.out.println("\n--- Train Information ---");
            System.out.println("Train ID: " + train.getId());
            System.out.println("Train Operator ID: " + train.getTrainOperatorId());
            System.out.println("Total Power: " + train.getTotalPower() + " kW");
            System.out.println("Total Weight: " + train.getTotalWeight() + " tons");
            System.out.println("Max Speed: " + train.getMaxSpeed() + " km/h");
            
            System.out.println("\n--- Locomotives (" + train.getLocomotives().size() + ") ---");
            if (train.getLocomotives().isEmpty()) {
                System.out.println("No locomotives assigned.");
            } else {
                for (int i = 0; i < train.getLocomotives().size(); i++) {
                    Locomotive loco = train.getLocomotives().get(i);
                    System.out.println((i + 1) + ". Locomotive ID: " + loco.getId() + 
                        ", Power: " + loco.getPower() + " kW, Max Speed: " + loco.getMaxSpeed() + " km/h");
                }
            }
            
            System.out.println("\n--- Wagons (" + train.getWagons().size() + ") ---");
            if (train.getWagons().isEmpty()) {
                System.out.println("No wagons assigned.");
            } else {
                for (int i = 0; i < train.getWagons().size(); i++) {
                    Wagon wagon = train.getWagons().get(i);
                    System.out.println((i + 1) + ". Wagon ID: " + wagon.getId() + 
                        ", Weight: " + wagon.getTotalWeight() + " tons, Loaded: " + (wagon.isLoaded() ? "Yes" : "No"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error viewing assembled train: " + e.getMessage(), e);
        }
    }

    private void viewAvailableLocomotives() {
        System.out.println("\n=== VIEW AVAILABLE LOCOMOTIVES ===");
        try {
            displayAvailableRoutes();
            
            Integer routeId = getRouteIdFromUser();
            if (routeId == null) {
                return;
            }
            
            if (!validateRouteId(routeId)) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
            Route route = routeRepository.getById(routeId);
            if (route == null || route.getStartDate() == null) {
                System.out.println("✗ Route does not have a scheduled start date.");
                return;
            }
            
            List<LocomotiveForAssembly> locomotives = assemblyController.getAvailableLocomotives(routeId, route.getStartDate());
            displayLocomotives(locomotives);
        } catch (Exception e) {
            throw new RuntimeException("Error viewing available locomotives: " + e.getMessage(), e);
        }
    }

    private void viewAvailableWagons() {
        System.out.println("\n=== VIEW AVAILABLE WAGONS ===");
        try {
            displayAvailableRoutes();
            
            Integer routeId = getRouteIdFromUser();
            if (routeId == null) {
                return;
            }
            
            if (!validateRouteId(routeId)) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
            Route route = routeRepository.getById(routeId);
            if (route == null || route.getStartDate() == null) {
                System.out.println("✗ Route does not have a scheduled start date.");
                return;
            }
            
            List<WagonForAssembly> wagons = assemblyController.getAvailableWagons(routeId, route.getStartDate());
            displayWagons(wagons);
        } catch (Exception e) {
            throw new RuntimeException("Error viewing available wagons: " + e.getMessage(), e);
        }
    }

    private void deletePlannedTrain() {
        System.out.println("\n=== DELETE PLANNED TRAIN ===");
        try {
            List<TrainAssemblyController.PlannedTrainInfo> plannedTrains = assemblyController.getAllPlannedTrains();
            
            if (plannedTrains.isEmpty()) {
                System.out.println("No planned trains found.");
                return;
            }
            
            System.out.println("\n--- All Planned Trains ---");
            System.out.printf("%-5s | %-12s | %-18s | %-32s | %-32s | %-19s%n",
                "#", "Route ID", "Train ID", "Start Facility", "End Facility", "Start Date");
            System.out.println("-----+--------------+--------------------+----------------------------------+----------------------------------+-------------------");
            
            DateTimeFormatter tableFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (int i = 0; i < plannedTrains.size(); i++) {
                TrainAssemblyController.PlannedTrainInfo train = plannedTrains.get(i);
                System.out.printf("%-5d | %-12d | %-18d | %-32s | %-32s | %-19s%n",
                    (i + 1), train.getRouteId(), train.getTrainId(),
                    train.getStartFacilityName(), train.getEndFacilityName(),
                    train.getStartDate().format(tableFormatter));
            }
            
            System.out.print("\nEnter the number (#) of the planned train to delete (or press Enter to cancel): ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                System.out.println("Deletion cancelled.");
                return;
            }
            
            int selectionNumber;
            try {
                selectionNumber = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                return;
            }
            
            if (selectionNumber < 1 || selectionNumber > plannedTrains.size()) {
                System.out.println("Invalid selection. Please enter a number between 1 and " + plannedTrains.size() + ".");
                return;
            }
            
            TrainAssemblyController.PlannedTrainInfo selectedTrain = plannedTrains.get(selectionNumber - 1);
            
            DateTimeFormatter confirmFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            System.out.print("\nAre you sure you want to delete the planned train for route " + selectedTrain.getRouteId() + 
                " (Train " + selectedTrain.getTrainId() + ") at " + selectedTrain.getStartDate().format(confirmFormatter) + "? (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            
            if (!"y".equals(confirm) && !"yes".equals(confirm)) {
                System.out.println("Deletion cancelled.");
                return;
            }
            
            boolean success = assemblyController.deletePlannedTrain(selectedTrain.getRouteId(), selectedTrain.getStartDate());
            if (success) {
                connection.commit();
                System.out.println("\n✓ Planned train deleted successfully!");
                System.out.println("   All assigned locomotives and wagons have been moved back to parked status.");
            } else {
                System.out.println("\n✗ Error: Planned train not found.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\n✗ Error: " + e.getMessage());
            rollback();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting planned train: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // TRAIN SCHEDULING MENU
    // ============================================================================

    private void showTrainSchedulingMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== TRAIN SCHEDULING ===");
            System.out.println("1. Dispatch train");
            System.out.println("2. View scheduled trains");
            System.out.println("3. View passage times for route");
            System.out.println("4. View crossings for route");
            System.out.println("5. View trains available for dispatch");
            System.out.println("0. Back to main menu");
            System.out.print("\nEnter your choice: ");
            
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    dispatchTrain();
                    break;
                case "2":
                    viewScheduledRoutesScheduler();
                    break;
                case "3":
                    viewPassageTimesScheduler();
                    break;
                case "4":
                    viewCrossingsScheduler();
                    break;
                case "5":
                    viewAvailableTrainsScheduler();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }


    private void viewScheduledRoutesScheduler() {
        System.out.println("\n=== SCHEDULED TRAINS ===");
        try {
            List<TrainAssemblyController.PlannedTrainInfo> plannedTrains = assemblyController.getAllPlannedTrains();
            
            if (plannedTrains.isEmpty()) {
                System.out.println("No scheduled trains found.");
                return;
            }
            
            System.out.printf("%-10s | %-10s | %-25s | %-25s | %-19s%n",
                "Train ID", "Route ID", "Start Facility", "End Facility", "Start Date");
            System.out.printf("%-10s-+-%-10s-+-%-25s-+-%-25s-+-%-19s%n",
                "----------", "----------", "-------------------------", "-------------------------", "-------------------");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (TrainAssemblyController.PlannedTrainInfo train : plannedTrains) {
                System.out.printf("%-10d | %-10d | %-25s | %-25s | %-19s%n",
                    train.getTrainId(),
                    train.getRouteId(),
                    truncateString(train.getStartFacilityName(), 25),
                    truncateString(train.getEndFacilityName(), 25),
                    train.getStartDate().format(formatter));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error viewing scheduled trains: " + e.getMessage(), e);
        }
    }

    private void viewPassageTimesScheduler() {
        System.out.println("\n=== VIEW PASSAGE TIMES ===");
        try {
            // Get unique routes
            List<Route> routes = routeRepository.getAll();
            if (routes.isEmpty()) {
                System.out.println("No routes scheduled.");
                return;
            }
            
            System.out.println("\nAvailable routes:");
            for (Route route : routes) {
                String path = buildRoutePath(route);
                System.out.printf("  Route ID: %d: %s\n",
                    route.getId(),
                    path);
            }
            
            Integer routeId = getRouteIdFromUser();
            if (routeId == null) {
                return;
            }
            
            if (!validateRouteId(routeId)) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
            // Get all planned trains for this route
            List<TrainAssemblyController.PlannedTrainInfo> plannedTrains = assemblyController.getAllPlannedTrains();
            List<TrainAssemblyController.PlannedTrainInfo> trainsForRoute = new ArrayList<>();
            for (TrainAssemblyController.PlannedTrainInfo train : plannedTrains) {
                if (train.getRouteId() == routeId) {
                    trainsForRoute.add(train);
                }
            }
            
            LocalDateTime selectedStartDate = null;
            if (trainsForRoute.isEmpty()) {
                System.out.println("No planned trains found for route " + routeId + ".");
                return;
            } else if (trainsForRoute.size() == 1) {
                // Only one planned train, use it
                selectedStartDate = trainsForRoute.get(0).getStartDate();
            } else {
                // Multiple planned trains, let user select
                System.out.println("\nMultiple planned trains found for Route ID: " + routeId);
                DateTimeFormatter tableFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                for (int i = 0; i < trainsForRoute.size(); i++) {
                    TrainAssemblyController.PlannedTrainInfo train = trainsForRoute.get(i);
                    System.out.printf("  %d. Train %d - Start: %s\n",
                        (i + 1), train.getTrainId(), train.getStartDate().format(tableFormatter));
                }
                System.out.print("\nSelect planned train (1-" + trainsForRoute.size() + "): ");
                String input = scanner.nextLine().trim();
                try {
                    int selection = Integer.parseInt(input);
                    if (selection < 1 || selection > trainsForRoute.size()) {
                        System.out.println("✗ Invalid selection.");
                        return;
                    }
                    selectedStartDate = trainsForRoute.get(selection - 1).getStartDate();
                } catch (NumberFormatException e) {
                    System.out.println("✗ Invalid input. Please enter a number.");
                    return;
                }
            }
            
            // Get route and update it with selected start date for passage time calculation
            Route route = routeRepository.getById(routeId);
            if (route == null) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
            // Get train for the selected planned train
            Train train = trainRepository.getTrainForRoute(routeId, selectedStartDate);
            if (train == null) {
                System.out.println("No train assigned to route " + routeId + " for start date " + selectedStartDate + ".");
                return;
            }
            
            // Create a route with the selected start date for calculation
            Route routeWithStartDate = new Route(route.getId(), route.getTrainId(), 
                route.getStartFacility(), route.getEndFacility(), selectedStartDate);
            for (Route.RoutePathPoint pathPoint : route.getPath()) {
                routeWithStartDate.addPathPoint(pathPoint.getFacility(), pathPoint.getSequenceNumber());
            }
            
            // Calculate passage times
            TrainSchedulerService schedulerService = new TrainSchedulerService(connection);
            List<TrainEvent> events = schedulerService.calculateRouteTimes(routeWithStartDate, train);
            if (events.isEmpty()) {
                System.out.println("No passage times found for route " + routeId + ".");
                return;
            }
            
            displayPassageTimesTable(events);
        } catch (Exception e) {
            throw new RuntimeException("Error viewing passage times: " + e.getMessage(), e);
        }
    }

    private void viewCrossingsScheduler() {
        System.out.println("\n=== VIEW CROSSINGS ===");
        try {
            // Get unique routes
            List<Route> routes = routeRepository.getAll();
            if (routes.isEmpty()) {
                System.out.println("No routes scheduled.");
                return;
            }
            
            System.out.println("\nAvailable routes:");
            for (Route route : routes) {
                String path = buildRoutePath(route);
                System.out.printf("  Route ID: %d: %s\n",
                    route.getId(),
                    path);
            }
            
            Integer routeId = getRouteIdFromUser();
            if (routeId == null) {
                return;
            }
            
            if (!validateRouteId(routeId)) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
            // Get all planned trains for this route
            List<TrainAssemblyController.PlannedTrainInfo> plannedTrains = assemblyController.getAllPlannedTrains();
            List<TrainAssemblyController.PlannedTrainInfo> trainsForRoute = new ArrayList<>();
            for (TrainAssemblyController.PlannedTrainInfo train : plannedTrains) {
                if (train.getRouteId() == routeId) {
                    trainsForRoute.add(train);
                }
            }
            
            LocalDateTime selectedStartDate = null;
            if (trainsForRoute.isEmpty()) {
                System.out.println("No planned trains found for route " + routeId + ".");
                return;
            } else if (trainsForRoute.size() == 1) {
                // Only one planned train, use it
                selectedStartDate = trainsForRoute.get(0).getStartDate();
            } else {
                // Multiple planned trains, let user select
                System.out.println("\nMultiple planned trains found for Route ID: " + routeId);
                DateTimeFormatter tableFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                for (int i = 0; i < trainsForRoute.size(); i++) {
                    TrainAssemblyController.PlannedTrainInfo train = trainsForRoute.get(i);
                    System.out.printf("  %d. Train %d - Start: %s\n",
                        (i + 1), train.getTrainId(), train.getStartDate().format(tableFormatter));
                }
                System.out.print("\nSelect planned train (1-" + trainsForRoute.size() + "): ");
                String input = scanner.nextLine().trim();
                try {
                    int selection = Integer.parseInt(input);
                    if (selection < 1 || selection > trainsForRoute.size()) {
                        System.out.println("✗ Invalid selection.");
                        return;
                    }
                    selectedStartDate = trainsForRoute.get(selection - 1).getStartDate();
                } catch (NumberFormatException e) {
                    System.out.println("✗ Invalid input. Please enter a number.");
                    return;
                }
            }
            
            // Get crossings for the route (crossings are detected across all routes, so we just need routeId)
            List<CrossingOperation> crossings = schedulerController.getCrossings(routeId);
            if (crossings.isEmpty()) {
                System.out.println("No crossings found for route " + routeId + ".");
                return;
            }
            
            displayCrossingsTable(crossings);
        } catch (Exception e) {
            throw new RuntimeException("Error viewing crossings: " + e.getMessage(), e);
        }
    }

    private void viewAvailableTrainsScheduler() {
        System.out.println("\n=== TRAINS AVAILABLE FOR DISPATCH ===");
        try {
            List<Train> trains = schedulerController.getTrainsAvailableForDispatch();
            if (trains.isEmpty()) {
                System.out.println("No trains found in the database.");
                return;
            }
            
            System.out.println("Total trains available: " + trains.size());
            displayTrainsTable(trains);
        } catch (Exception e) {
            throw new RuntimeException("Error viewing available trains: " + e.getMessage(), e);
        }
    }


    private void dispatchTrain() {
        System.out.println("\n=== DISPATCH TRAIN ===");
        try {
            // Show planned trains with their locomotives and wagons
            List<TrainAssemblyController.PlannedTrainInfo> plannedTrains = assemblyController.getAllPlannedTrains();
            if (plannedTrains.isEmpty()) {
                System.out.println("No planned trains available for dispatch.");
                return;
            }
            
            System.out.println("\nAvailable planned trains:");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (TrainAssemblyController.PlannedTrainInfo plannedTrain : plannedTrains) {
                // Get train with locomotives and wagons for this planned train
                Train train = trainRepository.getTrainForRoute(plannedTrain.getRouteId(), plannedTrain.getStartDate());
                if (train != null) {
                    System.out.printf("  Planned Train: Route %d, Train %d - %s -> %s (Start: %s)\n",
                        plannedTrain.getRouteId(),
                        plannedTrain.getTrainId(),
                        plannedTrain.getStartFacilityName(),
                        plannedTrain.getEndFacilityName(),
                        plannedTrain.getStartDate().format(formatter));
                    System.out.printf("    Locomotives: %d, Wagons: %d, Power: %.2f kW, Weight: %.2f tons\n",
                        train.getLocomotives().size(), train.getWagons().size(),
                        train.getTotalPower(), train.getTotalWeight());
                } else {
                    System.out.printf("  Planned Train: Route %d, Train %d - %s -> %s (Start: %s) - No train assembled\n",
                        plannedTrain.getRouteId(),
                        plannedTrain.getTrainId(),
                        plannedTrain.getStartFacilityName(),
                        plannedTrain.getEndFacilityName(),
                        plannedTrain.getStartDate().format(formatter));
                }
            }
            
            System.out.print("\nEnter Train ID: ");
            String input = scanner.nextLine().trim();
            int trainId;
            try {
                trainId = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("✗ Invalid input. Please enter a valid Train ID.");
                return;
            }
            
            // Find the planned train with this train ID
            TrainAssemblyController.PlannedTrainInfo selectedPlannedTrain = null;
            for (TrainAssemblyController.PlannedTrainInfo train : plannedTrains) {
                if (train.getTrainId() == trainId) {
                    selectedPlannedTrain = train;
                    break;
                }
            }
            
            if (selectedPlannedTrain == null) {
                System.out.println("✗ No planned train found with Train ID " + trainId + ".");
                return;
            }
            
            int routeId = selectedPlannedTrain.getRouteId();
            LocalDateTime selectedStartDate = selectedPlannedTrain.getStartDate();
            int selectedTrainId = selectedPlannedTrain.getTrainId();
            
            // Get the train for the selected planned train
            Train selectedTrain = trainRepository.getTrainForRoute(routeId, selectedStartDate);
            if (selectedTrain == null) {
                System.out.println("✗ No train assembled for route " + routeId + " with start date " + selectedStartDate + ".");
                return;
            }
            
            // Use the route and train from the selected planned train
            Route route = routeRepository.getById(routeId);
            if (route == null) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
            // Get freight for this route to validate path includes origin/destination facilities
            List<Freight> routeFreights = freightRepository.getByRouteId(routeId, selectedStartDate);
            
            // Ask for automatic or manual path definition
            System.out.print("\nPath definition: (a)utomatic (use route's path) or (m)anual (define path manually)? [a/m]: ");
            String pathChoice = scanner.nextLine().trim().toLowerCase();
            
            Route routeForSchedule;
            
            if (pathChoice.equals("m") || pathChoice.equals("manual")) {
                // Manual path selection
                System.out.println("\n=== MANUAL PATH DEFINITION ===");
                List<Integer> intermediateFacilityIds = selectManualPath(
                    route.getStartFacility().getId(), 
                    route.getEndFacility().getId(),
                    routeFreights
                );
                
                if (intermediateFacilityIds == null) {
                    System.out.println("✗ Path selection cancelled or invalid.");
                    return;
                }
                
                // Create route with manually selected path
                routeForSchedule = new Route(route.getId(), route.getTrainId(), 
                    route.getStartFacility(), route.getEndFacility(), selectedStartDate);
                int sequenceNumber = 1;
                for (Integer facilityId : intermediateFacilityIds) {
                    Facility facility = facilityRepository.getById(facilityId);
                    if (facility != null) {
                        routeForSchedule.addPathPoint(facility, sequenceNumber++);
                    }
                }
            } else {
                // Automatic path - use route's existing path
                routeForSchedule = new Route(route.getId(), route.getTrainId(), 
                    route.getStartFacility(), route.getEndFacility(), selectedStartDate);
                for (Route.RoutePathPoint pathPoint : route.getPath()) {
                    routeForSchedule.addPathPoint(pathPoint.getFacility(), pathPoint.getSequenceNumber());
                }
            }
            
            System.out.println("\nDispatching planned train:");
            System.out.println("  Route ID: " + routeId);
            System.out.println("  Train ID: " + selectedTrainId);
            System.out.println("  Start Facility: " + route.getStartFacility().getName());
            System.out.println("  End Facility: " + route.getEndFacility().getName());
            System.out.println("  Path: " + buildRoutePath(routeForSchedule));
            System.out.println("  Departure: " + selectedStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            System.out.println("  Locomotives: " + selectedTrain.getLocomotives().size());
            System.out.println("  Wagons: " + selectedTrain.getWagons().size());
            
            // Schedule the route (this calculates passage times and detects crossings)
            TrainSchedulerService schedulerService = new TrainSchedulerService(connection);
            SchedulingResult result = schedulerService.scheduleRoute(routeForSchedule);
            
            connection.commit();
            
            System.out.println("\n✓ Train dispatched successfully!");
            printSchedule(result);
        } catch (Exception e) {
            throw new RuntimeException("Error dispatching train: " + e.getMessage(), e);
        }
    }


    // ============================================================================
    // VIEW MENU
    // ============================================================================

    private void showViewMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== VIEW INFORMATION ===");
            System.out.println("1. View planned trains");
            System.out.println("2. View all facilities");
            System.out.println("3. View all routes");
            System.out.println("4. View all locomotives");
            System.out.println("5. View all wagons");
            System.out.println("6. View all rail lines");
            System.out.println("0. Back to main menu");
            System.out.print("\nEnter your choice: ");
            
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    viewPlannedTrains();
                    break;
                case "2":
                    viewAllFacilities();
                    break;
                case "3":
                    viewAllRoutes();
                    break;
                case "4":
                    viewAllLocomotives();
                    break;
                case "5":
                    viewAllWagons();
                    break;
                case "6":
                    viewAllRailLines();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void viewPlannedTrains() {
        System.out.println("\n=== PLANNED TRAINS ===");
        try {
            List<TrainAssemblyController.PlannedTrainInfo> plannedTrains = assemblyController.getAllPlannedTrains();
            
            if (plannedTrains.isEmpty()) {
                System.out.println("No planned trains found.");
                return;
            }
            
            // Remove duplicates - group by route ID and start date to show unique planned trains
            java.util.Map<String, TrainAssemblyController.PlannedTrainInfo> uniqueTrains = new java.util.LinkedHashMap<>();
            for (TrainAssemblyController.PlannedTrainInfo train : plannedTrains) {
                String key = train.getRouteId() + "_" + train.getStartDate();
                uniqueTrains.put(key, train);
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (TrainAssemblyController.PlannedTrainInfo train : uniqueTrains.values()) {
                try {
                    Route route = routeRepository.getById(train.getRouteId());
                    if (route != null) {
                        String path = buildRoutePath(route);
                        System.out.printf("\nTrain ID: %d - Route ID: %d\n", train.getTrainId(), train.getRouteId());
                        System.out.printf("  Path: %s\n", path);
                        System.out.printf("  Start Date: %s\n", train.getStartDate().format(formatter));
                    } else {
                        // Fallback if route not found
                        System.out.printf("\nTrain ID: %d - Route ID: %d\n", train.getTrainId(), train.getRouteId());
                        System.out.printf("  Start Facility: %s -> End Facility: %s\n",
                            train.getStartFacilityName(),
                            train.getEndFacilityName());
                        System.out.printf("  Start Date: %s\n", train.getStartDate().format(formatter));
                    }
                } catch (Exception e) {
                    // Fallback on error
                    System.out.printf("\nTrain ID: %d - Route ID: %d\n", train.getTrainId(), train.getRouteId());
                    System.out.printf("  Start Facility: %s -> End Facility: %s\n",
                        train.getStartFacilityName(),
                        train.getEndFacilityName());
                    System.out.printf("  Start Date: %s\n", train.getStartDate().format(formatter));
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void viewAllFacilities() {
        System.out.println("\n=== ALL FACILITIES ===");
        try {
            List<Facility> facilities = dispatchController.getAllFacilities();
            for (Facility facility : facilities) {
                System.out.printf("  ID: %d - %s\n", facility.getId(), facility.getName());
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void viewAllRoutes() {
        System.out.println("\n=== ALL ROUTES ===");
        try {
            List<Route> allRoutes = routeRepository.getAll();
            
            if (allRoutes.isEmpty()) {
                System.out.println("No routes found.");
                return;
            }
            
            for (Route route : allRoutes) {
                String path = buildRoutePath(route);
                System.out.printf("\nRoute ID: %d: %s\n",
                    route.getId(),
                    path);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void viewAllLocomotives() {
        System.out.println("\n=== ALL LOCOMOTIVES ===");
        try {
            List<Locomotive> locomotives = locomotiveRepository.getAll();
            for (Locomotive loco : locomotives) {
                System.out.printf("  Locomotive ID: %d - Make: %s, Power: %.2f kW, Max Speed: %.2f km/h\n",
                    loco.getId(), loco.getSpecs().getMake(), loco.getPower(), loco.getMaxSpeed());
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void viewAllWagons() {
        System.out.println("\n=== ALL WAGONS ===");
        try {
            List<Wagon> wagons = wagonRepository.getAll();
            for (Wagon wagon : wagons) {
                System.out.printf("  Wagon ID: %d - Payload: %.2f t, Volume: %.2f m³\n",
                    wagon.getId(), wagon.getSpecs().getPayload(), wagon.getSpecs().getVolumeCapacity());
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void viewAllRailLines() {
        System.out.println("\n=== ALL RAIL LINES ===");
        try {
            List<RailLine> railLines = railLineRepository.getAll();
            for (RailLine railLine : railLines) {
                System.out.printf("  Rail Line ID: %d - %s -> %s (Electrified: %s)\n",
                    railLine.getId(),
                    railLine.getStartFacility().getName(),
                    railLine.getEndFacility().getName(),
                    railLine.isElectrified() ? "Yes" : "No");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    private void deleteRoute() {
        System.out.println("\n=== DELETE ROUTE ===");
        try {
            // Get unique routes (not grouped by train)
            List<Route> allRoutes = routeRepository.getAll();
            
            if (allRoutes.isEmpty()) {
                System.out.println("No routes scheduled.");
                return;
            }
            
            System.out.println("\nScheduled routes:");
            for (Route route : allRoutes) {
                String path = buildRoutePath(route);
                System.out.printf("  Route ID: %d: %s\n",
                    route.getId(),
                    path);
            }
            
            Integer routeId = getRouteIdFromUser();
            if (routeId == null) {
                return;
            }
            
            if (!validateRouteId(routeId)) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
            System.out.print("Are you sure you want to delete Route " + routeId + "? (yes/no): ");
            String confirmation = scanner.nextLine().trim().toLowerCase();
            
            if ("yes".equals(confirmation) || "y".equals(confirmation)) {
                boolean deleted = dispatchController.deleteRoute(routeId);
                if (deleted) {
                    System.out.println("✓ Route " + routeId + " deleted successfully!");
                } else {
                    System.out.println("✗ Failed to delete route. Route may not exist.");
                }
            } else {
                System.out.println("Deletion cancelled.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting route: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Get route ID from user input with validation
     * @return route ID if valid, null if user input is invalid
     */
    private Integer getRouteIdFromUser() {
        System.out.print("\nEnter Route ID: ");
        String input = scanner.nextLine().trim();
        
        if (input.isEmpty()) {
            System.out.println("✗ Route ID cannot be empty.");
            return null;
        }
        
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("✗ Invalid Route ID. Please enter a valid number.");
            return null;
        }
    }

    /**
     * Validate that a route ID exists in the database
     * @param routeId the route ID to validate
     * @return true if route exists, false otherwise
     */
    private boolean validateRouteId(int routeId) {
        try {
            Route route = routeRepository.getById(routeId);
            return route != null;
        } catch (SQLException e) {
            System.out.println("✗ Error validating route ID: " + e.getMessage());
            return false;
        }
    }

    private void printSchedule(SchedulingResult result) throws Exception {
        System.out.println("\n=== ESTIMATED PASSAGE TIMES ===");
        System.out.printf("Route ID: %d, Train ID: %d\n",
            result.getRoute().getId(), result.getRoute().getTrainId());
        System.out.println("Path: " + buildRoutePath(result.getRoute()));
        
        // Get the train from the repository using routeId and startDate to properly initialize loaded status
        Route route = result.getRoute();
        Train train;
        if (route.getStartDate() != null) {
            train = trainRepository.getTrainForRoute(route.getId(), route.getStartDate());
        } else {
            train = trainRepository.getTrainForRoute(route.getId());
        }
        if (train == null) {
            throw new RuntimeException("Train not found for route: " + route.getId());
        }
        
        TrainSchedulerService schedulerService = new TrainSchedulerService(connection);
        FreightRepository freightRepo = new FreightRepository(connection, facilityRepository);
        Map<Integer, List<Freight>> pickupsByFacility;
        Map<Integer, List<Freight>> deliveriesByFacility;
        if (route.getStartDate() != null) {
            pickupsByFacility = freightRepo.getPickupsByFacility(route.getId(), route.getStartDate());
            deliveriesByFacility = freightRepo.getDeliveriesByFacility(route.getId(), route.getStartDate());
        } else {
            pickupsByFacility = freightRepo.getPickupsByFacility(route.getId());
            deliveriesByFacility = freightRepo.getDeliveriesByFacility(route.getId());
        }
        
        // Create a working copy of the train to track weight changes
        Train currentTrain = new Train(train.getId(), train.getTrainOperatorId());
        for (Locomotive loco : train.getLocomotives()) {
            currentTrain.addLocomotive(loco);
        }
        // Copy wagons and reset them all to unloaded initially
        // We'll mark them as loaded as we process pickups at each facility
        // This ensures speed changes are shown when pickups/deliveries occur
        for (Wagon wagon : train.getWagons()) {
            Wagon wagonCopy = new Wagon(wagon.getId(), wagon.getVehicleModelId(), 
                wagon.getTrainOperatorId(), wagon.getSpecs(), wagon.getTare());
            wagonCopy.setLoaded(false);  // Start all wagons as unloaded
            currentTrain.addWagon(wagonCopy);
        }
        
        // Track which freight has already been picked up/delivered to avoid duplicates
        java.util.Set<Integer> pickedUpFreightIds = new java.util.HashSet<>();
        java.util.Set<Integer> deliveredFreightIds = new java.util.HashSet<>();
        
        System.out.println("\nPassage times:");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Track previous speed for comparison
        Double previousSpeed = null;
        
        // Helper method to calculate speed based on train weight
        // (inline calculation - speed = min(operationalSpeed, sqrt(power/weight) * 18))
        
        for (int i = 0; i < result.getEvents().size(); i++) {
            TrainEvent event = result.getEvents().get(i);
            System.out.printf("  %s: %s\n",
                event.getFacility().getName(),
                event.getEventTime().format(formatter));
            
            int facilityId = event.getFacility().getId();
            List<Freight> pickups = pickupsByFacility.get(facilityId);
            List<Freight> deliveries = deliveriesByFacility.get(facilityId);
            
            // Calculate current speed BEFORE any operations at this facility
            // This represents the speed with the current train state (wagons loaded/unloaded as they are now)
            // For pickup operations, this will be with unloaded wagons
            // For delivery operations, this will be with wagons loaded as they currently are
            double speedBeforeOps;
            double calculatedSpeedBeforeOps = 0;
            if (currentTrain.getTotalWeight() > 0) {
                double powerToWeightRatio = currentTrain.getTotalPower() / currentTrain.getTotalWeight();
                calculatedSpeedBeforeOps = Math.sqrt(powerToWeightRatio) * 18.0;
                speedBeforeOps = Math.min(currentTrain.getOperationalSpeed(), calculatedSpeedBeforeOps);
            } else {
                speedBeforeOps = currentTrain.getOperationalSpeed();
                calculatedSpeedBeforeOps = speedBeforeOps;
            }
            
            // Track if we have pickups/deliveries to show speed change
            boolean hasPickups = (pickups != null && !pickups.isEmpty());
            boolean hasDeliveries = (deliveries != null && !deliveries.isEmpty());
            
            // For the first facility, we'll show speed as part of the pickup operation
            // For facilities without operations, show speed if it changed
            if (previousSpeed != null && !hasPickups && !hasDeliveries) {
                // No operations at this facility, show speed if it changed from previous
                if (Math.abs(speedBeforeOps - previousSpeed) > 0.01) {
                    System.out.printf("     Speed: %.2f km/h -> %.2f km/h\n", previousSpeed, speedBeforeOps);
                    previousSpeed = speedBeforeOps;
                }
            }
            
            // Track speed after operations at this facility
            double currentSpeed = speedBeforeOps;
            
            // Handle pickups - update wagon loaded status and recalculate speed
            if (hasPickups) {
                // Filter pickups to only include freight that hasn't been picked up yet
                List<Freight> actualPickups = new ArrayList<>();
                for (Freight freight : pickups) {
                    if (!pickedUpFreightIds.contains(freight.getId())) {
                        actualPickups.add(freight);
                        pickedUpFreightIds.add(freight.getId());
                    }
                }
                
                if (!actualPickups.isEmpty()) {
                    System.out.printf("     PICKUP: Freight loaded at %s\n", event.getFacility().getName());
                    for (Freight freight : actualPickups) {
                        List<Integer> wagonIds = freightRepo.getWagonIdsByFreightId(freight.getId());
                        System.out.printf("      Freight ID %d: %d wagon(s) -> Destination: %s\n",
                            freight.getId(), wagonIds.size(), freight.getDestinationFacility().getName());
                        
                        // Update wagon loaded status
                        for (Wagon wagon : currentTrain.getWagons()) {
                            if (wagonIds.contains(wagon.getId()) && !wagon.isLoaded()) {
                                wagon.setLoaded(true);
                            }
                        }
                    }
                    // Recalculate speed after pickup (weight increased)
                    // IMPORTANT: speedBeforeOps is calculated BEFORE loading wagons, so it represents
                    // the speed with unloaded wagons. We compare to this to show the speed change.
                    double speedBeforePickup = speedBeforeOps;
                    double calculatedSpeedBefore = calculatedSpeedBeforeOps;
                    
                    // Now calculate speed after pickup with loaded wagons
                    double speedAfterPickup;
                    double calculatedSpeedAfter = 0;
                    if (currentTrain.getTotalWeight() > 0) {
                        double powerToWeightRatio = currentTrain.getTotalPower() / currentTrain.getTotalWeight();
                        calculatedSpeedAfter = Math.sqrt(powerToWeightRatio) * 18.0;
                        speedAfterPickup = Math.min(currentTrain.getOperationalSpeed(), calculatedSpeedAfter);
                        currentSpeed = speedAfterPickup;
                    } else {
                        speedAfterPickup = currentTrain.getOperationalSpeed();
                        calculatedSpeedAfter = speedAfterPickup;
                        currentSpeed = speedAfterPickup;
                    }
                    
                    // Always show speed after pickup operation
                    // Show speed change if it changed, otherwise just show the speed
                    boolean displayedSpeedChanged = Math.abs(speedAfterPickup - speedBeforePickup) > 0.001;
                    
                    if (displayedSpeedChanged) {
                        // Speed changed - show the change
                        System.out.printf("     Speed: %.2f km/h -> %.2f km/h (after pickup)\n", speedBeforePickup, speedAfterPickup);
                    } else {
                        // Speed didn't change (might be capped at operational limit) - still show it
                        System.out.printf("     Speed: %.2f km/h (after pickup)\n", speedAfterPickup);
                    }
                    // Update previousSpeed to the new speed after pickup
                    previousSpeed = speedAfterPickup;
                }
            }
            
            // Handle deliveries - update wagon loaded status and recalculate speed
            if (hasDeliveries) {
                // Filter deliveries to only include freight that hasn't been delivered yet
                List<Freight> actualDeliveries = new ArrayList<>();
                for (Freight freight : deliveries) {
                    if (!deliveredFreightIds.contains(freight.getId())) {
                        actualDeliveries.add(freight);
                        deliveredFreightIds.add(freight.getId());
                    }
                }
                
                if (!actualDeliveries.isEmpty()) {
                    System.out.printf("     DELIVERY: Freight unloaded at %s\n", event.getFacility().getName());
                    for (Freight freight : actualDeliveries) {
                        List<Integer> wagonIds = freightRepo.getWagonIdsByFreightId(freight.getId());
                        System.out.printf("      Freight ID %d: %d wagon(s) from %s\n",
                            freight.getId(), wagonIds.size(), freight.getOriginFacility().getName());
                        
                        // Update wagon loaded status
                        for (Wagon wagon : currentTrain.getWagons()) {
                            if (wagonIds.contains(wagon.getId()) && wagon.isLoaded()) {
                                wagon.setLoaded(false);
                            }
                        }
                    }
                    // Recalculate speed after delivery (weight decreased)
                    double speedBeforeDelivery = (previousSpeed != null) ? previousSpeed : speedBeforeOps;
                    if (currentTrain.getTotalWeight() > 0) {
                        double powerToWeightRatio = currentTrain.getTotalPower() / currentTrain.getTotalWeight();
                        double calculatedSpeed = Math.sqrt(powerToWeightRatio) * 18.0;
                        currentSpeed = Math.min(currentTrain.getOperationalSpeed(), calculatedSpeed);
                    } else {
                        currentSpeed = currentTrain.getOperationalSpeed();
                    }
                    if (Math.abs(currentSpeed - speedBeforeDelivery) > 0.01) {
                        System.out.printf("     Speed: %.2f km/h -> %.2f km/h (after delivery)\n", speedBeforeDelivery, currentSpeed);
                    }
                    // Update previousSpeed to the new speed after delivery
                    previousSpeed = currentSpeed;
                }
            }
            
            // Update previousSpeed for next iteration if no operations occurred
            if ((pickups == null || pickups.isEmpty()) && (deliveries == null || deliveries.isEmpty())) {
                previousSpeed = currentSpeed;
            }
        }
        
        List<CrossingOperation> crossings = result.getCrossings();
        if (!crossings.isEmpty()) {
            System.out.println("\n=== CROSSING OPERATIONS ===");
            int currentRouteId = result.getRoute().getId();
            for (CrossingOperation crossing : crossings) {
                Train crossingCurrentTrain, otherTrain;
                int otherRouteId;
                if (crossing.getRoute1Id() == currentRouteId) {
                    crossingCurrentTrain = crossing.getTrain1();
                    otherTrain = crossing.getTrain2();
                    otherRouteId = crossing.getRoute2Id();
                } else {
                    crossingCurrentTrain = crossing.getTrain2();
                    otherTrain = crossing.getTrain1();
                    otherRouteId = crossing.getRoute1Id();
                }
                
                System.out.printf("Train %d (Route %d) will cross with Train %d (Route %d) at %s\n",
                    crossingCurrentTrain.getId(), currentRouteId, otherTrain.getId(), otherRouteId,
                    crossing.getCrossingLocation().getName());
                if (crossing.usesSiding()) {
                    System.out.printf("    Using siding (ID: %d) on segment %d\n",
                        crossing.getSiding().getId(), crossing.getSiding().getLineSegmentId());
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

    private void displayAvailableRoutes() throws SQLException {
        List<Route> routes = assemblyController.getAllRoutes();
        
        if (routes.isEmpty()) {
            System.out.println("\nNo routes available in the system.");
            return;
        }
        
        System.out.println("\n--- Available Routes ---");
        System.out.printf("%-10s | %s%n", "Route ID", "Path");
        System.out.printf("%-10s-+-%s%n", "----------", "----------------------------------------");
        
        for (Route route : routes) {
            String path = buildRoutePath(route);
            
            System.out.printf("%-10d | %s%n", route.getId(), path);
        }
    }

    private void displayLocomotives(List<LocomotiveForAssembly> locomotives) {
        if (locomotives.isEmpty()) {
            System.out.println("No locomotives available.");
            return;
        }
        
        System.out.println("\nLocomotives are ordered by: In-transit first, then parked by distance (descending)");
        System.out.printf("%-8s | %-12s | %-12s | %-12s | %-25s | %-40s%n",
            "ID", "Status", "Power (kW)", "Max Speed", "Make", "Location");
        System.out.printf("%-8s-+-%-12s-+-%-12s-+-%-12s-+-%-25s-+-%-40s%n",
            "--------", "------------", "------------", "------------", "-------------------------", "----------------------------------------");
        
        for (LocomotiveForAssembly loco : locomotives) {
            String status = loco.isInTransit() ? "IN-TRANSIT" : "PARKED";
            String location = loco.getLocationDescription();
            String make = truncateString(loco.getLocomotive().getSpecs().getMake(), 25);
            System.out.printf("%-8d | %-12s | %-12.1f | %-12.1f | %-25s | %-40s%n",
                loco.getLocomotive().getId(),
                status,
                loco.getLocomotive().getPower(),
                loco.getLocomotive().getMaxSpeed(),
                make,
                truncateString(location, 40));
        }
    }

    private void displayWagons(List<WagonForAssembly> wagons) {
        if (wagons.isEmpty()) {
            System.out.println("No wagons available.");
            return;
        }
        
        System.out.println("\nWagons are ordered by: In-transit first, then parked by distance (descending)");
        System.out.printf("%-10s | %-12s | %-12s | %-12s | %-12s | %-40s%n",
            "ID", "Status", "Payload (t)", "Volume (m³)", "Weight (t)", "Location");
        System.out.printf("%-10s-+-%-12s-+-%-12s-+-%-12s-+-%-12s-+-%-40s%n",
            "----------", "------------", "------------", "------------", "------------", "----------------------------------------");
        
        for (WagonForAssembly wagon : wagons) {
            String status = wagon.isInTransit() ? "IN-TRANSIT" : "PARKED";
            String location = wagon.getLocationDescription();
            System.out.printf("%-10d | %-12s | %-12.1f | %-12.1f | %-12.1f | %-40s%n",
                wagon.getWagon().getId(),
                status,
                wagon.getWagon().getSpecs().getPayload(),
                wagon.getWagon().getSpecs().getVolumeCapacity(),
                wagon.getWagon().getTotalWeight(),
                truncateString(location, 40));
        }
    }

    private void displayTrainsTable(List<Train> trains) {
        System.out.printf("%-10s | %-15s%n", "Train ID", "Operator ID");
        System.out.printf("%-10s-+-%-15s%n", "----------", "---------------");
        
        for (Train train : trains) {
            System.out.printf("%-10d | %-15d%n", train.getId(), train.getTrainOperatorId());
        }
    }

    private void displayFacilitiesTable() throws SQLException {
        List<Facility> facilities = facilityRepository.getAll();
        if (facilities.isEmpty()) {
            System.out.println("No facilities available.");
            return;
        }
        
        System.out.println("\n--- Available Facilities ---");
        System.out.printf("%-10s | %-40s%n", "Facility ID", "Name");
        System.out.printf("%-10s-+-%-40s%n", "----------", "----------------------------------------");
        
        for (Facility facility : facilities) {
            System.out.printf("%-10d | %-40s%n", facility.getId(), truncateString(facility.getName(), 40));
        }
    }

    private void displayPassageTimesTable(List<TrainEvent> events) {
        System.out.printf("%-10s | %-25s | %-19s%n",
            "Sequence", "Facility", "Estimated Time");
        System.out.printf("%-10s-+-%-25s-+-%-19s%n",
            "----------", "-------------------------", "-------------------");
        
        int sequence = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (TrainEvent event : events) {
            System.out.printf("%-10d | %-25s | %-19s%n",
                sequence++,
                truncateString(event.getFacility().getName(), 25),
                event.getEventTime().format(formatter));
        }
    }

    private void displayCrossingsTable(List<CrossingOperation> crossings) {
        System.out.printf("%-10s | %-10s | %-25s | %-10s | %-19s%n",
            "Train 1", "Train 2", "Location", "Siding", "Crossing Time");
        System.out.printf("%-10s-+-%-10s-+-%-25s-+-%-10s-+-%-19s%n",
            "----------", "----------", "-------------------------", "----------", "-------------------");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (CrossingOperation crossing : crossings) {
            System.out.printf("%-10d | %-10d | %-25s | %-10s | %-19s%n",
                crossing.getTrain1().getId(),
                crossing.getTrain2().getId(),
                truncateString(crossing.getCrossingLocation().getName(), 25),
                crossing.usesSiding() ? "Yes" : "No",
                crossing.getCrossingTime().format(formatter));
        }
    }

    private String buildRoutePath(Route route) {
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(route.getStartFacility().getName());
        
        for (Route.RoutePathPoint pathPoint : route.getPath()) {
            pathBuilder.append(" -> ");
            pathBuilder.append(pathPoint.getFacility().getName());
        }
        
        pathBuilder.append(" -> ");
        pathBuilder.append(route.getEndFacility().getName());
        
        return pathBuilder.toString();
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) {
            return "N/A";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    private void rollback() {
        try {
            connection.rollback();
        } catch (SQLException ex) {
            // Silent rollback on error
        }
    }
    
    /**
     * Helper method to select manual path for route dispatch.
     * Reuses logic from createRoutePlan for manual path selection.
     * Validates that all freight origin/destination facilities are included in the path.
     * 
     * @param startFacilityId the start facility ID
     * @param endFacilityId the end facility ID
     * @param routeFreights list of freights assigned to the route (for validation)
     * @return list of intermediate facility IDs in order, or null if cancelled/invalid
     */
    private List<Integer> selectManualPath(int startFacilityId, int endFacilityId, List<Freight> routeFreights) {
        try {
            // Get facilities
            List<Facility> facilities = dispatchController.getAllFacilities();
            
            // Collect required facilities from freight (origin and destination)
            java.util.Set<Integer> requiredFacilityIds = new java.util.HashSet<>();
            for (Freight freight : routeFreights) {
                requiredFacilityIds.add(freight.getOriginFacility().getId());
                requiredFacilityIds.add(freight.getDestinationFacility().getId());
            }
            
            // Show required facilities if any
            if (!requiredFacilityIds.isEmpty()) {
                System.out.println("\nRequired facilities (from freight origin/destination):");
                for (Integer facilityId : requiredFacilityIds) {
                    Facility facility = facilities.stream()
                        .filter(f -> f.getId() == facilityId)
                        .findFirst()
                        .orElse(null);
                    if (facility != null) {
                        System.out.printf("  - %s (ID: %d)\n", facility.getName(), facilityId);
                    }
                }
            }
            
            // Get intermediate facilities (path points)
            System.out.println("\nEnter intermediate facility IDs in order (press Enter with empty line to finish):");
            List<Integer> intermediateFacilityIds = new ArrayList<>();
            int currentFacilityId = startFacilityId;
            
            while (true) {
                try {
                    List<Facility> connectedFacilities = routePlannerService.getConnectedFacilities(currentFacilityId);
                    if (!connectedFacilities.isEmpty()) {
                        System.out.println("\nFacilities connected to current facility (ID: " + currentFacilityId + "):");
                        for (Facility connected : connectedFacilities) {
                            if (connected.getId() == endFacilityId) {
                                System.out.printf("  ID: %d - %s (destination)\n", connected.getId(), connected.getName());
                            } else {
                                System.out.printf("  ID: %d - %s\n", connected.getId(), connected.getName());
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue without showing connections
                }
                
                System.out.print("\nFacility ID (or press Enter to finish): ");
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    break;
                }
                
                int facilityId;
                try {
                    facilityId = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("✗ Invalid facility ID. Please enter a number.");
                    continue;
                }
                
                Facility facility = facilities.stream()
                    .filter(f -> f.getId() == facilityId)
                    .findFirst()
                    .orElse(null);
                
                if (facility == null) {
                    System.out.println("✗ Invalid facility ID.");
                    continue;
                }
                
                if (facilityId == endFacilityId) {
                    // Allow end facility to be added, but warn that path will end here
                    System.out.println("✓ Added: " + facility.getName() + " (destination reached)");
                    intermediateFacilityIds.add(facilityId);
                    break;
                }
                
                if (!intermediateFacilityIds.isEmpty() && 
                    intermediateFacilityIds.get(intermediateFacilityIds.size() - 1).equals(facilityId)) {
                    System.out.println("✗ Cannot add the same facility twice in a row.");
                    continue;
                }
                
                intermediateFacilityIds.add(facilityId);
                System.out.println("✓ Added: " + facility.getName());
                currentFacilityId = facilityId;
            }
            
            // Build the complete path for validation
            List<Integer> completePath = new ArrayList<>();
            completePath.add(startFacilityId);
            completePath.addAll(intermediateFacilityIds);
            completePath.add(endFacilityId);
            
            // Validate that all required facilities (from freight) are in the path
            for (Integer requiredFacilityId : requiredFacilityIds) {
                if (!completePath.contains(requiredFacilityId)) {
                    Facility facility = facilities.stream()
                        .filter(f -> f.getId() == requiredFacilityId)
                        .findFirst()
                        .orElse(null);
                    String facilityName = facility != null ? facility.getName() : "Facility " + requiredFacilityId;
                    System.out.println("\n✗ Error: Required facility " + facilityName + " (ID: " + requiredFacilityId + 
                        ") is not in the selected path.");
                    System.out.println("   The path must include all freight origin and destination facilities.");
                    return null;
                }
            }
            
            return intermediateFacilityIds;
        } catch (Exception e) {
            System.out.println("✗ Error during path selection: " + e.getMessage());
            return null;
        }
    }
}
