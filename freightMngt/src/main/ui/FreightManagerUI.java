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
                        showTrainDispatchMenu();
                        break;
                    case "5":
                        showViewMenu();
                        break;
                    case "6":
                        showManagementMenu();
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
        System.out.println("1. Route Planning (USLP08)");
        System.out.println("2. Train Assembly (USLP09)");
        System.out.println("3. Train Scheduling (USLP10)");
        System.out.println("4. Train Dispatch (with Freight)");
        System.out.println("5. View Information");
        System.out.println("6. Management Operations");
        System.out.println("0. Exit");
    }

    // ============================================================================
    // ROUTE PLANNING MENU (USLP08)
    // ============================================================================

    private void showRoutePlanningMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== ROUTE PLANNING (USLP08) ===");
            System.out.println("1. View pending freights");
            System.out.println("2. Create route plan");
            System.out.println("3. View route plan with cargo operations");
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
            List<Train> trains = dispatchController.getAllTrains();
            List<Route> allRoutes = new ArrayList<>();
            for (Train train : trains) {
                allRoutes.addAll(dispatchController.getRoutesByTrainId(train.getId()));
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
            
            Integer routeId = getRouteIdFromUser();
            if (routeId == null) {
                return;
            }
            
            if (!validateRouteId(routeId)) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
            String routePlanText = routePlannerService.presentRoutePlan(routeId);
            System.out.println("\n" + routePlanText);
        } catch (Exception e) {
            throw new RuntimeException("Error viewing route plan: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // TRAIN ASSEMBLY MENU (USLP09)
    // ============================================================================

    private void showTrainAssemblyMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== TRAIN ASSEMBLY (USLP09) ===");
            System.out.println("1. Assemble and assign train to route");
            System.out.println("2. View assembled train for route");
            System.out.println("3. View available locomotives for route");
            System.out.println("4. View available wagons for route");
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
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void assembleTrain() {
        System.out.println("\n=== ASSEMBLE TRAIN TO ROUTE ===");
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
            if (route == null) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
            if (route.getStartDate() == null) {
                System.out.println("✗ Route does not have a scheduled start date.");
                return;
            }
            
            LocalDateTime startDate = route.getStartDate();
            
            // Show available locomotives
            System.out.println("\n--- Available Locomotives ---");
            List<LocomotiveForAssembly> locomotives = assemblyController.getAvailableLocomotives(routeId, startDate);
            if (locomotives.isEmpty()) {
                System.out.println("No locomotives available for this route.");
            } else {
                displayLocomotives(locomotives);
            }
            
            // Show available wagons
            System.out.println("\n--- Available Wagons ---");
            List<WagonForAssembly> wagons = assemblyController.getAvailableWagons(routeId, startDate);
            if (wagons.isEmpty()) {
                System.out.println("No wagons available for this route.");
            } else {
                displayWagons(wagons);
            }
            
            // Assign locomotives
            System.out.println("\n--- Assign Locomotives ---");
            System.out.print("Enter locomotive IDs (comma-separated, or 'skip' to skip): ");
            String locoInput = scanner.nextLine().trim();
            if (!locoInput.equalsIgnoreCase("skip") && !locoInput.isEmpty()) {
                String[] locoIds = locoInput.split(",");
                for (String locoIdStr : locoIds) {
                    try {
                        int locoId = Integer.parseInt(locoIdStr.trim());
                        assemblyController.assignLocomotiveToRoute(locoId, routeId, startDate);
                        System.out.println("✓ Locomotive " + locoId + " assigned successfully.");
                    } catch (Exception e) {
                        System.out.println("✗ Error assigning locomotive " + locoIdStr + ": " + e.getMessage());
                    }
                }
            }
            
            // Assign wagons
            System.out.println("\n--- Assign Wagons ---");
            System.out.print("Enter wagon IDs (comma-separated, or 'skip' to skip): ");
            String wagonInput = scanner.nextLine().trim();
            if (!wagonInput.equalsIgnoreCase("skip") && !wagonInput.isEmpty()) {
                String[] wagonIds = wagonInput.split(",");
                for (String wagonIdStr : wagonIds) {
                    try {
                        int wagonId = Integer.parseInt(wagonIdStr.trim());
                        assemblyController.assignWagonToRoute(wagonId, routeId, startDate);
                        System.out.println("✓ Wagon " + wagonId + " assigned successfully.");
                    } catch (Exception e) {
                        System.out.println("✗ Error assigning wagon " + wagonIdStr + ": " + e.getMessage());
                    }
                }
            }
            
            connection.commit();
            System.out.println("\n✓ Train assembly completed successfully!");
        } catch (Exception e) {
            throw new RuntimeException("Error assembling train: " + e.getMessage(), e);
        }
    }

    private void viewAssembledTrain() {
        System.out.println("\n=== VIEW ASSEMBLED TRAIN ===");
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
            
            Train train = assemblyController.getTrainForRoute(routeId);
            if (train == null) {
                System.out.println("No train assigned to route " + routeId + ".");
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

    // ============================================================================
    // TRAIN SCHEDULING MENU (USLP10)
    // ============================================================================

    private void showTrainSchedulingMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== TRAIN SCHEDULING (USLP10) ===");
            System.out.println("1. Dispatch train (scheduler - manual/automatic path)");
            System.out.println("2. View scheduled routes");
            System.out.println("3. View passage times for route");
            System.out.println("4. View crossings for route");
            System.out.println("5. View trains available for dispatch");
            System.out.println("0. Back to main menu");
            System.out.print("\nEnter your choice: ");
            
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    dispatchTrainScheduler();
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

    private void dispatchTrainScheduler() {
        System.out.print("\nDo you want to use (A)utomatic or (M)anual path calculation? [A/M]: ");
        String pathType = scanner.nextLine().trim().toUpperCase();
        
        if (!"A".equals(pathType) && !"AUTOMATIC".equals(pathType) && 
            !"M".equals(pathType) && !"MANUAL".equals(pathType)) {
            System.out.println("✗ Invalid choice. Please enter 'A' for Automatic or 'M' for Manual.");
            return;
        }
        
        boolean isAutomatic = "A".equals(pathType) || "AUTOMATIC".equals(pathType);
        
        try {
            // Get trains with their routes
            List<Train> availableTrains = schedulerController.getTrainsAvailableForDispatch();
            if (availableTrains.isEmpty()) {
                System.out.println("No trains available for dispatch.");
                return;
            }
            
            // Show trains with their routes
            System.out.println("\n--- Available Trains and Routes ---");
            Map<Integer, List<Route>> trainRoutesMap = new java.util.HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            for (Train train : availableTrains) {
                List<Route> routes = dispatchController.getRoutesByTrainId(train.getId());
                trainRoutesMap.put(train.getId(), routes);
                
                System.out.println("\nTrain ID: " + train.getId() + " (Operator ID: " + train.getTrainOperatorId() + ")");
                if (routes.isEmpty()) {
                    System.out.println("  No routes defined for this train.");
                } else {
                    System.out.println("  Routes:");
                    for (Route route : routes) {
                        String startDate = route.getStartDate() != null 
                            ? route.getStartDate().format(formatter) 
                            : "Not scheduled";
                        System.out.printf("    Route ID: %d - %s -> %s (Departure: %s)\n",
                            route.getId(),
                            route.getStartFacility().getName(),
                            route.getEndFacility().getName(),
                            startDate);
                    }
                }
            }
            
            System.out.print("\nEnter Train ID: ");
            int trainId;
            try {
                trainId = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("✗ Invalid Train ID. Please enter a number.");
                return;
            }
            
            // Validate train exists
            Train selectedTrain = availableTrains.stream()
                .filter(t -> t.getId() == trainId)
                .findFirst()
                .orElse(null);
            if (selectedTrain == null) {
                System.out.println("✗ Train ID " + trainId + " not found in available trains.");
                return;
            }
            
            // Get routes for selected train
            List<Route> trainRoutes = trainRoutesMap.get(trainId);
            if (trainRoutes == null || trainRoutes.isEmpty()) {
                System.out.println("✗ No routes defined for Train " + trainId + ".");
                System.out.println("Please create a route first using Route Planning or Train Dispatch menu.");
                return;
            }
            
            // If multiple routes, let user select one
            Route selectedRoute;
            if (trainRoutes.size() == 1) {
                selectedRoute = trainRoutes.get(0);
                System.out.println("\nUsing Route ID: " + selectedRoute.getId());
                System.out.println("  Start: " + selectedRoute.getStartFacility().getName());
                System.out.println("  End: " + selectedRoute.getEndFacility().getName());
                if (selectedRoute.getStartDate() != null) {
                    System.out.println("  Departure: " + selectedRoute.getStartDate().format(formatter));
                }
            } else {
                System.out.println("\n--- Routes for Train " + trainId + " ---");
                for (int i = 0; i < trainRoutes.size(); i++) {
                    Route route = trainRoutes.get(i);
                    String startDate = route.getStartDate() != null 
                        ? route.getStartDate().format(formatter) 
                        : "Not scheduled";
                    System.out.printf("%d. Route ID: %d - %s -> %s (Departure: %s)\n",
                        i + 1,
                        route.getId(),
                        route.getStartFacility().getName(),
                        route.getEndFacility().getName(),
                        startDate);
                }
                System.out.print("\nSelect route (1-" + trainRoutes.size() + "): ");
                int routeChoice;
                try {
                    routeChoice = Integer.parseInt(scanner.nextLine().trim());
                    if (routeChoice < 1 || routeChoice > trainRoutes.size()) {
                        System.out.println("✗ Invalid route selection.");
                        return;
                    }
                    selectedRoute = trainRoutes.get(routeChoice - 1);
                } catch (NumberFormatException e) {
                    System.out.println("✗ Invalid input. Please enter a number.");
                    return;
                }
            }
            
            // Validate route has required information
            if (selectedRoute.getStartDate() == null) {
                System.out.println("✗ Route " + selectedRoute.getId() + " does not have a scheduled departure time.");
                return;
            }
            
            // Use the existing route
            int existingRouteId = selectedRoute.getId();
            int startFacilityId = selectedRoute.getStartFacility().getId();
            int endFacilityId = selectedRoute.getEndFacility().getId();
            
            List<Integer> pathFacilityIds = new ArrayList<>();
            
            if (isAutomatic) {
                // Automatic path calculation
                System.out.println("\nCalculating automatic path...");
                try {
                    List<Integer> calculatedPath = automaticPathService.calculateShortestPath(startFacilityId, endFacilityId);
                    if (calculatedPath == null || calculatedPath.isEmpty()) {
                        System.out.println("✗ No path found from facility " + startFacilityId + " to facility " + endFacilityId);
                        return;
                    }
                    // Remove start and end facilities from path (they're route attributes)
                    calculatedPath.removeIf(id -> id == startFacilityId || id == endFacilityId);
                    pathFacilityIds = calculatedPath;
                    System.out.println("✓ Path calculated automatically with " + pathFacilityIds.size() + " intermediate facility(ies).");
                } catch (Exception e) {
                    System.out.println("✗ Error calculating automatic path: " + e.getMessage());
                    return;
                }
            } else {
                // Manual path definition
                System.out.println("\n=== DEFINE ROUTE PATH (Manual) ===");
                System.out.println("Enter intermediate facility IDs in order (press Enter with empty line to finish):");
                int currentFacilityId = startFacilityId;
                
                // Get all facilities for validation
                List<Facility> facilities = facilityRepository.getAll();
                
                while (true) {
                    try {
                        List<Facility> connectedFacilities = routePlannerService.getConnectedFacilities(currentFacilityId);
                        if (!connectedFacilities.isEmpty()) {
                            System.out.println("\nFacilities connected to current facility (ID: " + currentFacilityId + "):");
                            for (Facility connected : connectedFacilities) {
                                if (connected.getId() == endFacilityId) {
                                    System.out.printf("  ID: %d - %s (destination)\n", connected.getId(), connected.getName());
                                } else if (!pathFacilityIds.contains(connected.getId())) {
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
                    
                    try {
                        int facilityId = Integer.parseInt(input);
                        Facility facility = facilities.stream()
                            .filter(f -> f.getId() == facilityId)
                            .findFirst()
                            .orElse(null);
                        
                        if (facility == null) {
                            System.out.println("✗ Invalid facility ID.");
                            continue;
                        }
                        
                        if (!pathFacilityIds.isEmpty() && 
                            pathFacilityIds.get(pathFacilityIds.size() - 1).equals(facilityId)) {
                            System.out.println("✗ Cannot add the same facility twice in a row.");
                            continue;
                        }
                        
                        pathFacilityIds.add(facilityId);
                        System.out.println("✓ Added: " + facility.getName());
                        currentFacilityId = facilityId;
                    } catch (NumberFormatException e) {
                        System.out.println("✗ Invalid facility ID. Please enter a number.");
                    }
                }
            }
            
            // Clear existing path points and add new ones
            System.out.println("\nUpdating route path...");
            try {
                // Delete existing path points for this route
                java.sql.PreparedStatement deleteStmt = connection.prepareStatement(
                    "DELETE FROM Path WHERE RouteID = ?");
                deleteStmt.setInt(1, existingRouteId);
                deleteStmt.executeUpdate();
                deleteStmt.close();
                
                // Add new path points
                if (!pathFacilityIds.isEmpty()) {
                    int seqNumber = 2; // Start from 2 (1 is start facility)
                    for (Integer facilityId : pathFacilityIds) {
                        routeRepository.addPathPoint(existingRouteId, facilityId, seqNumber++);
                    }
                }
                
                // Reload route with new path points
                Route updatedRoute = routeRepository.getById(existingRouteId);
                if (updatedRoute == null) {
                    System.out.println("✗ Failed to reload route.");
                    return;
                }
                
                // Schedule the route (calculate times and detect crossings)
                TrainSchedulerService schedulerService = new TrainSchedulerService(connection);
                SchedulingResult result = schedulerService.scheduleRoute(updatedRoute);
                
                connection.commit();
                
                System.out.println("✓ Route scheduled successfully!");
                System.out.println("\n--- Route Information ---");
                System.out.println("Route ID: " + updatedRoute.getId());
                System.out.println("Start Facility: " + updatedRoute.getStartFacility().getName());
                System.out.println("End Facility: " + updatedRoute.getEndFacility().getName());
                System.out.println("Departure Time: " + updatedRoute.getStartDate().format(formatter));
                
                System.out.println("\n--- Estimated Passage Times ---");
                displayPassageTimesTable(result.getEvents());
                
                if (!result.getCrossings().isEmpty()) {
                    System.out.println("\n--- Crossing Operations Required ---");
                    displayCrossingsTable(result.getCrossings());
                } else {
                    System.out.println("\n--- No crossing operations required ---");
                }
            } catch (SQLException e) {
                System.out.println("✗ Error updating route path: " + e.getMessage());
                connection.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error dispatching train: " + e.getMessage(), e);
        }
    }

    private void viewScheduledRoutesScheduler() {
        System.out.println("\n=== SCHEDULED ROUTES ===");
        try {
            List<Route> routes = schedulerController.getAllScheduledRoutes();
            if (routes.isEmpty()) {
                System.out.println("No routes scheduled.");
                return;
            }
            
            System.out.println(String.format("%-10s | %-25s | %-25s | %-12s | %-19s",
                "Route ID", "Start Facility", "End Facility", "Train ID", "Departure Time"));
            System.out.println(String.format("%-10s-+-%-25s-+-%-25s-+-%-12s-+-%-19s",
                "----------", "-------------------------", "-------------------------", "------------", "-------------------"));
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Route route : routes) {
                String startFacility = route.getStartFacility() != null ? route.getStartFacility().getName() : "N/A";
                String endFacility = route.getEndFacility() != null ? route.getEndFacility().getName() : "N/A";
                String trainId = route.getTrainId() > 0 ? String.valueOf(route.getTrainId()) : "Not assigned";
                String startDate = route.getStartDate() != null 
                    ? route.getStartDate().format(formatter) 
                    : "Not scheduled";
                
                System.out.println(String.format("%-10d | %-25s | %-25s | %-12s | %-19s",
                    route.getId(),
                    truncateString(startFacility, 25),
                    truncateString(endFacility, 25),
                    trainId,
                    startDate));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error viewing scheduled routes: " + e.getMessage(), e);
        }
    }

    private void viewPassageTimesScheduler() {
        System.out.println("\n=== VIEW PASSAGE TIMES ===");
        try {
            // Show available routes first
            List<Route> routes = schedulerController.getAllScheduledRoutes();
            if (routes.isEmpty()) {
                System.out.println("No routes scheduled.");
                return;
            }
            
            System.out.println("\nAvailable routes:");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Route route : routes) {
                String startDate = route.getStartDate() != null 
                    ? route.getStartDate().format(formatter) 
                    : "Not scheduled";
                System.out.printf("  Route ID: %d - Train %d: %s -> %s (Departure: %s)\n",
                    route.getId(),
                    route.getTrainId(),
                    route.getStartFacility().getName(),
                    route.getEndFacility().getName(),
                    startDate);
            }
            
            Integer routeId = getRouteIdFromUser();
            if (routeId == null) {
                return;
            }
            
            if (!validateRouteId(routeId)) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
            List<TrainEvent> events = schedulerController.getPassageTimes(routeId);
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
            // Show available routes first
            List<Route> routes = schedulerController.getAllScheduledRoutes();
            if (routes.isEmpty()) {
                System.out.println("No routes scheduled.");
                return;
            }
            
            System.out.println("\nAvailable routes:");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Route route : routes) {
                String startDate = route.getStartDate() != null 
                    ? route.getStartDate().format(formatter) 
                    : "Not scheduled";
                System.out.printf("  Route ID: %d - Train %d: %s -> %s (Departure: %s)\n",
                    route.getId(),
                    route.getTrainId(),
                    route.getStartFacility().getName(),
                    route.getEndFacility().getName(),
                    startDate);
            }
            
            Integer routeId = getRouteIdFromUser();
            if (routeId == null) {
                return;
            }
            
            if (!validateRouteId(routeId)) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
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

    // ============================================================================
    // TRAIN DISPATCH MENU (with Freight)
    // ============================================================================

    private void showTrainDispatchMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== TRAIN DISPATCH (with Freight) ===");
            System.out.println("1. Dispatch a train (with freight assignment)");
            System.out.println("2. View train schedule with passage times");
            System.out.println("3. View crossing operations");
            System.out.println("0. Back to main menu");
            System.out.print("\nEnter your choice: ");
            
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    dispatchTrain();
                    break;
                case "2":
                    viewTrainSchedule();
                    break;
                case "3":
                    viewCrossingOperations();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void dispatchTrain() {
        System.out.println("\n=== DISPATCH TRAIN ===");
        try {
            List<Train> trains = dispatchController.getAllTrains();
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
            
            Train selectedTrain = trains.stream()
                .filter(t -> t.getId() == trainId)
                .findFirst()
                .orElse(null);
            if (selectedTrain == null) {
                System.out.println("✗ Invalid train ID.");
                return;
            }
            
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
            
            // Ask for path type (manual or automatic)
            System.out.print("\nPath type - (A)utomatic or (M)anual? [A/M]: ");
            String pathType = scanner.nextLine().trim().toUpperCase();
            
            List<Integer> pathFacilityIds = new ArrayList<>();
            if ("A".equals(pathType) || "AUTOMATIC".equals(pathType)) {
                // Automatic path calculation
                System.out.println("\nCalculating automatic path...");
                try {
                    List<Integer> calculatedPath = automaticPathService.calculateShortestPath(startFacilityId, endFacilityId);
                    if (calculatedPath == null || calculatedPath.isEmpty()) {
                        System.out.println("✗ No path found from facility " + startFacilityId + " to facility " + endFacilityId);
                        return;
                    }
                    // Remove start and end facilities from path (they're route attributes)
                    calculatedPath.removeIf(id -> id == startFacilityId || id == endFacilityId);
                    pathFacilityIds = calculatedPath;
                    System.out.println("✓ Path calculated automatically with " + pathFacilityIds.size() + " intermediate facility(ies).");
                    if (!pathFacilityIds.isEmpty()) {
                        System.out.println("Intermediate facilities:");
                        for (Integer facilityId : pathFacilityIds) {
                            Facility facility = facilities.stream()
                                .filter(f -> f.getId() == facilityId)
                                .findFirst()
                                .orElse(null);
                            if (facility != null) {
                                System.out.println("  - " + facility.getName() + " (ID: " + facilityId + ")");
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("✗ Error calculating automatic path: " + e.getMessage());
                    return;
                }
            } else {
                // Manual path definition
                System.out.println("\n=== DEFINE ROUTE PATH (Manual) ===");
                System.out.println("Enter facility IDs in order (press Enter with empty line to finish):");
                while (true) {
                    System.out.print("Facility ID (or press Enter to finish): ");
                    String input = scanner.nextLine().trim();
                    if (input.isEmpty()) {
                        break;
                    }
                    try {
                        int facilityId = Integer.parseInt(input);
                        pathFacilityIds.add(facilityId);
                        Facility facility = facilities.stream()
                            .filter(f -> f.getId() == facilityId)
                            .findFirst()
                            .orElse(null);
                        if (facility != null) {
                            System.out.println("  ✓ Added: " + facility.getName());
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("✗ Invalid facility ID.");
                    }
                }
            }
            
            // Select freight
            System.out.println("\n=== SELECT FREIGHT ===");
            List<Freight> unassignedFreight = dispatchController.getUnassignedFreight();
            List<Integer> selectedFreightIds = new ArrayList<>();
            
            if (!unassignedFreight.isEmpty()) {
                System.out.println("Available unassigned freight:");
                for (Freight freight : unassignedFreight) {
                    System.out.printf("  Freight ID: %d - %s -> %s\n",
                        freight.getId(),
                        freight.getOriginFacility().getName(),
                        freight.getDestinationFacility().getName());
                }
                System.out.println("\nEnter freight IDs to assign (press Enter with empty line to finish):");
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
                            System.out.println("  ✓ Added: Freight " + freightId);
                        } else {
                            System.out.println("✗ Invalid freight ID.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("✗ Invalid freight ID.");
                    }
                }
            } else {
                System.out.println("No unassigned freight available. Route will be created without freight.");
            }
            
            // Dispatch the train
            System.out.println("\nDispatching train...");
            SchedulingResult result = dispatchController.dispatchTrain(
                trainId, startFacilityId, endFacilityId, startDate, pathFacilityIds, selectedFreightIds);
            
            System.out.println("\n✓ Train dispatched successfully!");
            printSchedule(result);
        } catch (Exception e) {
            throw new RuntimeException("Error dispatching train: " + e.getMessage(), e);
        }
    }

    private void viewTrainSchedule() {
        System.out.println("\n=== TRAIN SCHEDULE ===");
        try {
            // Show available routes first
            List<Train> trains = dispatchController.getAllTrains();
            List<Route> allRoutes = new ArrayList<>();
            for (Train train : trains) {
                allRoutes.addAll(dispatchController.getRoutesByTrainId(train.getId()));
            }
            
            if (allRoutes.isEmpty()) {
                System.out.println("No routes scheduled.");
                return;
            }
            
            System.out.println("\nAvailable routes:");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Route route : allRoutes) {
                System.out.printf("  Route ID: %d - Train %d: %s -> %s (Departure: %s)\n",
                    route.getId(),
                    route.getTrainId(),
                    route.getStartFacility().getName(),
                    route.getEndFacility().getName(),
                    route.getStartDate() != null ? route.getStartDate().format(formatter) : "Not scheduled");
            }
            
            Integer routeId = getRouteIdFromUser();
            if (routeId == null) {
                return;
            }
            
            if (!validateRouteId(routeId)) {
                System.out.println("✗ Route ID " + routeId + " does not exist.");
                return;
            }
            
            SchedulingResult result = dispatchController.getScheduleForRoute(routeId);
            printSchedule(result);
        } catch (Exception e) {
            throw new RuntimeException("Error viewing train schedule: " + e.getMessage(), e);
        }
    }

    private void viewCrossingOperations() {
        System.out.println("\n=== ALL CROSSING OPERATIONS ===");
        try {
            List<Train> trains = dispatchController.getAllTrains();
            List<Route> allRoutes = new ArrayList<>();
            for (Train train : trains) {
                allRoutes.addAll(dispatchController.getRoutesByTrainId(train.getId()));
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
        } catch (Exception e) {
            throw new RuntimeException("Error viewing crossing operations: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // VIEW MENU
    // ============================================================================

    private void showViewMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== VIEW INFORMATION ===");
            System.out.println("1. View all trains");
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
                    viewAllTrains();
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

    private void viewAllTrains() {
        System.out.println("\n=== ALL TRAINS ===");
        try {
            List<Train> trains = dispatchController.getAllTrains();
            for (Train train : trains) {
                System.out.printf("\nTrain ID: %d\n", train.getId());
                System.out.printf("  Locomotives: %d (Total Power: %.2f kW)\n",
                    train.getLocomotives().size(), train.getTotalPower());
                System.out.printf("  Wagons: %d (Total Weight: %.2f tons)\n",
                    train.getWagons().size(), train.getTotalWeight());
                System.out.printf("  Max Speed: %.2f km/h\n", train.getMaxSpeed());
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
            List<Train> trains = dispatchController.getAllTrains();
            List<Route> allRoutes = new ArrayList<>();
            for (Train train : trains) {
                allRoutes.addAll(dispatchController.getRoutesByTrainId(train.getId()));
            }
            
            if (allRoutes.isEmpty()) {
                System.out.println("No routes scheduled.");
                return;
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Route route : allRoutes) {
                System.out.printf("\nRoute ID: %d - Train %d: %s -> %s (Departure: %s)\n",
                    route.getId(),
                    route.getTrainId(),
                    route.getStartFacility().getName(),
                    route.getEndFacility().getName(),
                    route.getStartDate() != null ? route.getStartDate().format(formatter) : "Not scheduled");
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
                System.out.printf("  Wagon ID: %d - Payload: %.2f t, Volume: %.2f m³, Loaded: %s\n",
                    wagon.getId(), wagon.getSpecs().getPayload(), wagon.getSpecs().getVolumeCapacity(),
                    wagon.isLoaded() ? "Yes" : "No");
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

    // ============================================================================
    // MANAGEMENT MENU
    // ============================================================================

    private void showManagementMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== MANAGEMENT OPERATIONS ===");
            System.out.println("1. Delete a route");
            System.out.println("0. Back to main menu");
            System.out.print("\nEnter your choice: ");
            
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
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

    private void deleteRoute() {
        System.out.println("\n=== DELETE ROUTE ===");
        try {
            List<Train> trains = dispatchController.getAllTrains();
            List<Route> allRoutes = new ArrayList<>();
            for (Train train : trains) {
                allRoutes.addAll(dispatchController.getRoutesByTrainId(train.getId()));
            }
            
            if (allRoutes.isEmpty()) {
                System.out.println("No routes scheduled.");
                return;
            }
            
            System.out.println("\nScheduled routes:");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Route route : allRoutes) {
                System.out.printf("  Route ID: %d - Train %d: %s -> %s (Departure: %s)\n",
                    route.getId(),
                    route.getTrainId(),
                    route.getStartFacility().getName(),
                    route.getEndFacility().getName(),
                    route.getStartDate() != null ? route.getStartDate().format(formatter) : "Not scheduled");
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
        System.out.println("Path: " + result.getRoute().getStartFacility().getName() + " -> " +
            result.getRoute().getEndFacility().getName());
        
        FreightRepository freightRepo = new FreightRepository(connection, facilityRepository);
        Map<Integer, List<Freight>> pickupsByFacility = freightRepo.getPickupsByFacility(result.getRoute().getId());
        Map<Integer, List<Freight>> deliveriesByFacility = freightRepo.getDeliveriesByFacility(result.getRoute().getId());
        
        System.out.println("\nPassage times:");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (TrainEvent event : result.getEvents()) {
            System.out.printf("  %s: %s\n",
                event.getFacility().getName(),
                event.getEventTime().format(formatter));
            
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
        
        List<CrossingOperation> crossings = result.getCrossings();
        if (!crossings.isEmpty()) {
            System.out.println("\n=== CROSSING OPERATIONS ===");
            int currentRouteId = result.getRoute().getId();
            for (CrossingOperation crossing : crossings) {
                Train currentTrain, otherTrain;
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
                    currentTrain.getId(), currentRouteId, otherTrain.getId(), otherRouteId,
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
        System.out.println(String.format("%-10s | %-25s | %-25s | %-12s | %-19s",
            "Route ID", "Start Facility", "End Facility", "Train ID", "Start Date"));
        System.out.println(String.format("%-10s-+-%-25s-+-%-25s-+-%-12s-+-%-19s",
            "----------", "-------------------------", "-------------------------", "------------", "-------------------"));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Route route : routes) {
            String startFacility = route.getStartFacility() != null ? route.getStartFacility().getName() : "N/A";
            String endFacility = route.getEndFacility() != null ? route.getEndFacility().getName() : "N/A";
            String trainId = route.getTrainId() > 0 ? String.valueOf(route.getTrainId()) : "Not assigned";
            String startDate = route.getStartDate() != null 
                ? route.getStartDate().format(formatter) 
                : "Not scheduled";
            
            System.out.println(String.format("%-10d | %-25s | %-25s | %-12s | %-19s",
                route.getId(),
                truncateString(startFacility, 25),
                truncateString(endFacility, 25),
                trainId,
                startDate));
        }
    }

    private void displayLocomotives(List<LocomotiveForAssembly> locomotives) {
        if (locomotives.isEmpty()) {
            System.out.println("No locomotives available.");
            return;
        }
        
        System.out.println("\nLocomotives are ordered by: In-transit first, then parked by distance (descending)");
        System.out.println(String.format("%-8s | %-12s | %-12s | %-12s | %-25s | %-40s",
            "ID", "Status", "Power (kW)", "Max Speed", "Make", "Location"));
        System.out.println(String.format("%-8s-+-%-12s-+-%-12s-+-%-12s-+-%-25s-+-%-40s",
            "--------", "------------", "------------", "------------", "-------------------------", "----------------------------------------"));
        
        for (LocomotiveForAssembly loco : locomotives) {
            String status = loco.isInTransit() ? "IN-TRANSIT" : "PARKED";
            String location = loco.getLocationDescription();
            String make = truncateString(loco.getLocomotive().getSpecs().getMake(), 25);
            System.out.println(String.format("%-8d | %-12s | %-12.1f | %-12.1f | %-25s | %-40s",
                loco.getLocomotive().getId(),
                status,
                loco.getLocomotive().getPower(),
                loco.getLocomotive().getMaxSpeed(),
                make,
                truncateString(location, 40)));
        }
    }

    private void displayWagons(List<WagonForAssembly> wagons) {
        if (wagons.isEmpty()) {
            System.out.println("No wagons available.");
            return;
        }
        
        System.out.println("\nWagons are ordered by: In-transit first, then parked by distance (descending)");
        System.out.println(String.format("%-10s | %-12s | %-12s | %-12s | %-12s | %-40s",
            "ID", "Status", "Payload (t)", "Volume (m³)", "Weight (t)", "Location"));
        System.out.println(String.format("%-10s-+-%-12s-+-%-12s-+-%-12s-+-%-12s-+-%-40s",
            "----------", "------------", "------------", "------------", "------------", "----------------------------------------"));
        
        for (WagonForAssembly wagon : wagons) {
            String status = wagon.isInTransit() ? "IN-TRANSIT" : "PARKED";
            String location = wagon.getLocationDescription();
            System.out.println(String.format("%-10d | %-12s | %-12.1f | %-12.1f | %-12.1f | %-40s",
                wagon.getWagon().getId(),
                status,
                wagon.getWagon().getSpecs().getPayload(),
                wagon.getWagon().getSpecs().getVolumeCapacity(),
                wagon.getWagon().getTotalWeight(),
                truncateString(location, 40)));
        }
    }

    private void displayTrainsTable(List<Train> trains) {
        System.out.println(String.format("%-10s | %-15s",
            "Train ID", "Operator ID"));
        System.out.println(String.format("%-10s-+-%-15s",
            "----------", "---------------"));
        
        for (Train train : trains) {
            System.out.println(String.format("%-10d | %-15d",
                train.getId(),
                train.getTrainOperatorId()));
        }
    }

    private void displayFacilitiesTable() throws SQLException {
        List<Facility> facilities = facilityRepository.getAll();
        if (facilities.isEmpty()) {
            System.out.println("No facilities available.");
            return;
        }
        
        System.out.println("\n--- Available Facilities ---");
        System.out.println(String.format("%-10s | %-40s",
            "Facility ID", "Name"));
        System.out.println(String.format("%-10s-+-%-40s",
            "----------", "----------------------------------------"));
        
        for (Facility facility : facilities) {
            System.out.println(String.format("%-10d | %-40s",
                facility.getId(),
                truncateString(facility.getName(), 40)));
        }
    }

    private void displayPassageTimesTable(List<TrainEvent> events) {
        System.out.println(String.format("%-10s | %-25s | %-19s",
            "Sequence", "Facility", "Estimated Time"));
        System.out.println(String.format("%-10s-+-%-25s-+-%-19s",
            "----------", "-------------------------", "-------------------"));
        
        int sequence = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (TrainEvent event : events) {
            System.out.println(String.format("%-10d | %-25s | %-19s",
                sequence++,
                truncateString(event.getFacility().getName(), 25),
                event.getEventTime().format(formatter)));
        }
    }

    private void displayCrossingsTable(List<CrossingOperation> crossings) {
        System.out.println(String.format("%-10s | %-10s | %-25s | %-10s | %-19s",
            "Train 1", "Train 2", "Location", "Siding", "Crossing Time"));
        System.out.println(String.format("%-10s-+-%-10s-+-%-25s-+-%-10s-+-%-19s",
            "----------", "----------", "-------------------------", "----------", "-------------------"));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (CrossingOperation crossing : crossings) {
            System.out.println(String.format("%-10d | %-10d | %-25s | %-10s | %-19s",
                crossing.getTrain1().getId(),
                crossing.getTrain2().getId(),
                truncateString(crossing.getCrossingLocation().getName(), 25),
                crossing.usesSiding() ? "Yes" : "No",
                crossing.getCrossingTime().format(formatter)));
        }
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
}
