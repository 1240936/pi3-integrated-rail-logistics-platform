package main.ui;

import main.controller.TrainAssemblyController;
import main.domain.Locomotive;
import main.domain.LocomotiveForAssembly;
import main.domain.Route;
import main.domain.Train;
import main.domain.Wagon;
import main.domain.WagonForAssembly;
import main.repositories.DatabaseConnection;
import main.repositories.FacilityRepository;
import main.repositories.LocomotiveRepository;
import main.repositories.RouteRepository;
import main.repositories.TrainRepository;
import main.repositories.WagonRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * User Interface for Traffic Manager to assemble and assign trains to routes
 */
public class TrafficManagerUI {
    private final Scanner scanner;
    private TrainAssemblyController controller;
    private Connection connection;
    private TrainRepository trainRepository;
    private LocomotiveRepository locomotiveRepository;
    private WagonRepository wagonRepository;

    public TrafficManagerUI() {
        this.scanner = new Scanner(System.in);
        this.controller = null;
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
                asSysdba = "sys".equalsIgnoreCase(username);
            } else {
                asSysdba = "y".equals(sysdbaInput) || "yes".equals(sysdbaInput);
            }
            
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
            return false;
        }
    }

    /**
     * Start the UI
     */
    public void start() {
        System.out.println("\n=== TRAFFIC MANAGEMENT SYSTEM - TRAIN ASSEMBLY ===");

        if (!initializeDatabase()) {
            System.out.println("\nCannot proceed without database connection.");
            return;
        }

        // Initialize repositories and controller
        FacilityRepository facilityRepository = new FacilityRepository(connection);
        RouteRepository routeRepository = new RouteRepository(connection, facilityRepository);
        TrainRepository trainRepository = new TrainRepository(connection, 
            new LocomotiveRepository(connection), 
            new WagonRepository(connection));
        LocomotiveRepository locomotiveRepository = new LocomotiveRepository(connection);
        WagonRepository wagonRepository = new WagonRepository(connection);
        
        this.trainRepository = trainRepository;
        this.locomotiveRepository = locomotiveRepository;
        this.wagonRepository = wagonRepository;
        
        this.controller = new TrainAssemblyController(connection, routeRepository, trainRepository,
            locomotiveRepository, wagonRepository);

        boolean running = true;
        while (running) {
            showMainMenu();
            System.out.print("\nEnter your choice: ");
            String choice = scanner.nextLine().trim();

            try {
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
                        running = false;
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("\n✗ Error: " + e.getMessage());
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    // Silent rollback on error
                }
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    System.out.println("\n✗ Error: " + errorMessage);
                } else {
                    System.out.println("\n✗ An unexpected error occurred. Please try again.");
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
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }

    private void showMainMenu() {
        System.out.println("\n=== MAIN MENU ===");
        System.out.println("1. Assemble and assign train to route");
        System.out.println("2. View assembled train for route");
        System.out.println("3. View available locomotives for route");
        System.out.println("4. View available wagons for route");
        System.out.println("5. Delete planned train");
        System.out.println("0. Exit");
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
        Route route = controller.getAllRoutes().stream()
            .filter(r -> r.getId() == routeId)
            .findFirst()
            .orElse(null);
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
        while (controller.checkDateConflict(routeId, requestedStartDate)) {
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
            int trainId = controller.ensurePlannedTrain(routeId, requestedStartDate);
            System.out.println("✓ Planned train (ID: " + trainId + ") ready for route " + routeId + 
                " at " + dateTimeStr);
        } catch (IllegalArgumentException e) {
            System.out.println("\n✗ Error: " + e.getMessage());
            return;
        }

        // Get available locomotives filtered by requested date/time
        List<LocomotiveForAssembly> locomotives = controller.getAvailableLocomotives(routeId, requestedStartDate);

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
                        controller.assignLocomotiveToRoute(locoId, routeId, requestedStartDate);
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
        List<WagonForAssembly> wagons = controller.getAvailableWagons(routeId, requestedStartDate);

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
                        controller.assignWagonToRoute(wagonId, routeId, requestedStartDate);
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

    private void viewAssembledTrain() throws SQLException {
        System.out.println("\n=== VIEW ASSEMBLED TRAIN ===");
        
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

        // Get all planned trains for this route
        List<TrainAssemblyController.PlannedTrainInfo> plannedTrains = controller.getAllPlannedTrains();
        List<TrainAssemblyController.PlannedTrainInfo> trainsForRoute = plannedTrains.stream()
            .filter(pt -> pt.getRouteId() == routeId)
            .collect(Collectors.toList());
        
        if (trainsForRoute.isEmpty()) {
            System.out.println("No planned trains found for route " + routeId + ".");
            return;
        }

        // Display all planned trains for this route
        System.out.println("\n--- Planned Trains for Route " + routeId + " ---");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (int i = 0; i < trainsForRoute.size(); i++) {
            TrainAssemblyController.PlannedTrainInfo pt = trainsForRoute.get(i);
            System.out.println((i + 1) + ". Train ID: " + pt.getTrainId() + 
                ", Start Date: " + pt.getStartDate().format(formatter) +
                ", Start: " + pt.getStartFacilityName() + " -> End: " + pt.getEndFacilityName());
        }

        // Ask user to select which planned train to view
        System.out.print("\nEnter the number of the planned train to view (or press Enter to cancel): ");
        String selectionInput = scanner.nextLine().trim();
        if (selectionInput.isEmpty()) {
            return;
        }

        int selectionNumber;
        try {
            selectionNumber = Integer.parseInt(selectionInput);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return;
        }

        if (selectionNumber < 1 || selectionNumber > trainsForRoute.size()) {
            System.out.println("Invalid selection. Please enter a number between 1 and " + trainsForRoute.size() + ".");
            return;
        }

        TrainAssemblyController.PlannedTrainInfo selectedTrain = trainsForRoute.get(selectionNumber - 1);
        
        // Get train details for the selected planned train
        Train train = trainRepository.getById(selectedTrain.getTrainId());
        if (train == null) {
            System.out.println("Train with ID " + selectedTrain.getTrainId() + " not found.");
            return;
        }

        // Load locomotives for this specific planned train (using trainId and startDate)
        java.sql.Timestamp startDate = java.sql.Timestamp.valueOf(selectedTrain.getStartDate());
        List<Locomotive> locomotives = locomotiveRepository.getByTrainId(selectedTrain.getTrainId(), startDate);
        for (Locomotive loco : locomotives) {
            train.addLocomotive(loco);
        }

        // Load wagons for this specific planned train (using trainId and startDate)
        List<Wagon> wagons = wagonRepository.getByTrainId(selectedTrain.getTrainId(), startDate);
        for (Wagon wagon : wagons) {
            train.addWagon(wagon);
        }

        System.out.println("\n--- Train Information ---");
        System.out.println("Train ID: " + train.getId());
        System.out.println("Train Operator ID: " + train.getTrainOperatorId());
        System.out.println("Start Date: " + selectedTrain.getStartDate().format(formatter));
        System.out.println("Route: " + selectedTrain.getStartFacilityName() + " -> " + selectedTrain.getEndFacilityName());
        System.out.println("Total Power: " + train.getTotalPower() + " kW");
        System.out.println("Total Weight: " + train.getTotalWeight() + " tons");
        System.out.println("Max Speed: " + train.getMaxSpeed() + " km/h");

        System.out.println("\n--- Locomotives (" + train.getLocomotives().size() + ") ---");
        if (train.getLocomotives().isEmpty()) {
            System.out.println("No locomotives assigned.");
        } else {
            for (int i = 0; i < train.getLocomotives().size(); i++) {
                var loco = train.getLocomotives().get(i);
                System.out.println((i + 1) + ". Locomotive ID: " + loco.getId() + 
                    ", Power: " + loco.getPower() + " kW, Max Speed: " + loco.getMaxSpeed() + " km/h");
            }
        }

        System.out.println("\n--- Wagons (" + train.getWagons().size() + ") ---");
        if (train.getWagons().isEmpty()) {
            System.out.println("No wagons assigned.");
        } else {
            for (int i = 0; i < train.getWagons().size(); i++) {
                var wagon = train.getWagons().get(i);
                System.out.println((i + 1) + ". Wagon ID: " + wagon.getId() + 
                    ", Weight: " + wagon.getTotalWeight() + " tons, Loaded: " + (wagon.isLoaded() ? "Yes" : "No"));
            }
        }
    }

    private void viewAvailableLocomotives() throws SQLException {
        System.out.println("\n=== VIEW AVAILABLE LOCOMOTIVES ===");
        
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

        // Ask for start date/time
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

        List<LocomotiveForAssembly> locomotives = controller.getAvailableLocomotives(routeId, requestedStartDate);
        displayLocomotives(locomotives);
    }

    private void viewAvailableWagons() throws SQLException {
        System.out.println("\n=== VIEW AVAILABLE WAGONS ===");
        
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

        // Ask for start date/time
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

        List<WagonForAssembly> wagons = controller.getAvailableWagons(routeId, requestedStartDate);
        displayWagons(wagons);
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

    /**
     * Display all available routes to help user select a route ID.
     */
    private void displayAvailableRoutes() throws SQLException {
        List<Route> routes = controller.getAllRoutes();
        
        if (routes.isEmpty()) {
            System.out.println("\nNo routes available in the system.");
            return;
        }

        System.out.println("\n--- Available Routes ---");
        // Format with proper column widths and alignment
        System.out.println(String.format("%-10s | %-25s | %-25s | %s",
            "Route ID", "Start Facility", "End Facility", "Path"));
        System.out.println(String.format("%-10s-+-%-25s-+-%-25s-+-%s",
            "----------", "-------------------------", "-------------------------", "------------------------------------------------------------"));

        for (Route route : routes) {
            String startFacility = route.getStartFacility() != null ? route.getStartFacility().getName() : "N/A";
            String endFacility = route.getEndFacility() != null ? route.getEndFacility().getName() : "N/A";
            String path = buildRoutePath(route);
            
            System.out.println(String.format("%-10d | %-25s | %-25s | %s",
                route.getId(),
                truncateString(startFacility, 25),
                truncateString(endFacility, 25),
                path));
        }
    }

    /**
     * Build a path string for a route showing the travel path.
     * Format: Start -> Facility1 -> Facility2 -> ... -> End
     */
    private String buildRoutePath(Route route) {
        StringBuilder pathBuilder = new StringBuilder();
        
        // Add start facility
        if (route.getStartFacility() != null) {
            pathBuilder.append(route.getStartFacility().getName());
        } else {
            pathBuilder.append("N/A");
        }
        
        // Add intermediate facilities from path
        List<Route.RoutePathPoint> pathPoints = route.getPath();
        if (pathPoints != null && !pathPoints.isEmpty()) {
            // Sort by sequence number to ensure correct order
            List<Route.RoutePathPoint> sortedPath = new ArrayList<>(pathPoints);
            sortedPath.sort((a, b) -> Integer.compare(a.getSequenceNumber(), b.getSequenceNumber()));
            
            for (Route.RoutePathPoint point : sortedPath) {
                pathBuilder.append(" -> ");
                if (point.getFacility() != null) {
                    pathBuilder.append(point.getFacility().getName());
                } else {
                    pathBuilder.append("N/A");
                }
            }
        }
        
        // Add end facility
        pathBuilder.append(" -> ");
        if (route.getEndFacility() != null) {
            pathBuilder.append(route.getEndFacility().getName());
        } else {
            pathBuilder.append("N/A");
        }
        
        return pathBuilder.toString();
    }
    
    /**
     * Truncate a string to a maximum length, adding ellipsis if needed.
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) {
            return "N/A";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    private void deletePlannedTrain() throws SQLException {
        System.out.println("\n=== DELETE PLANNED TRAIN ===");
        
        // Get all planned trains
        List<TrainAssemblyController.PlannedTrainInfo> plannedTrains = controller.getAllPlannedTrains();
        
        if (plannedTrains.isEmpty()) {
            System.out.println("\nNo planned trains found.");
            return;
        }
        
        // Display all planned trains with numbers for selection
        System.out.println("\n--- All Planned Trains ---");
        System.out.println(String.format("%-5s | %-12s | %-20s | %-30s | %-30s | %-19s",
            "#", "Route ID", "Train ID", "Start Facility", "End Facility", "Start Date"));
        System.out.println(String.format("%-5s-+-%-12s-+-%-20s-+-%-30s-+-%-30s-+-%-19s",
            "-----", "------------", "--------------------", "------------------------------", "------------------------------", "-------------------"));
        
        int index = 1;
        for (TrainAssemblyController.PlannedTrainInfo pt : plannedTrains) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            System.out.println(String.format("%-5d | %-12d | %-20d | %-30s | %-30s | %-19s",
                index++,
                pt.getRouteId(),
                pt.getTrainId(),
                truncateString(pt.getStartFacilityName(), 28),
                truncateString(pt.getEndFacilityName(), 28),
                pt.getStartDate().format(formatter)));
        }
        
        // Ask user to select a planned train by number
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
        
        // Find the planned train with the specified number
        if (selectionNumber < 1 || selectionNumber > plannedTrains.size()) {
            System.out.println("Invalid selection. Please enter a number between 1 and " + plannedTrains.size() + ".");
            return;
        }
        
        TrainAssemblyController.PlannedTrainInfo selectedTrain = plannedTrains.get(selectionNumber - 1);
        
        // Confirm deletion
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.print("\nAre you sure you want to delete the planned train for route " + selectedTrain.getRouteId() + 
            " (Train " + selectedTrain.getTrainId() + ") at " + selectedTrain.getStartDate().format(formatter) + "? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!"y".equals(confirm) && !"yes".equals(confirm)) {
            System.out.println("Deletion cancelled.");
            return;
        }

        // Delete the planned train
        try {
            boolean success = controller.deletePlannedTrain(selectedTrain.getRouteId(), selectedTrain.getStartDate());
            if (success) {
                connection.commit();
                System.out.println("\n✓ Planned train deleted successfully!");
                System.out.println("   All assigned locomotives and wagons have been moved back to parked status.");
            } else {
                System.out.println("\n✗ Error: Planned train not found.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\n✗ Error: " + e.getMessage());
        }
    }
}

