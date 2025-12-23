package main.ui;

import main.controller.TrainAssemblyController;
import main.domain.LocomotiveForAssembly;
import main.domain.Route;
import main.domain.Train;
import main.domain.WagonForAssembly;
import main.repositories.DatabaseConnection;
import main.repositories.FacilityRepository;
import main.repositories.LocomotiveRepository;
import main.repositories.RouteRepository;
import main.repositories.TrainRepository;
import main.repositories.WagonRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * User Interface for Traffic Manager to assemble and assign trains to routes
 */
public class TrafficManagerUI {
    private final Scanner scanner;
    private TrainAssemblyController controller;
    private Connection connection;

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

        // Show available locomotives
        System.out.println("\n--- Available Locomotives ---");
        List<LocomotiveForAssembly> locomotives = controller.getAvailableLocomotives(routeId);
        if (locomotives.isEmpty()) {
            System.out.println("No locomotives available for this route.");
        } else {
            displayLocomotives(locomotives);
        }

        // Show available wagons
        System.out.println("\n--- Available Wagons ---");
        List<WagonForAssembly> wagons = controller.getAvailableWagons(routeId);
        if (wagons.isEmpty()) {
            System.out.println("No wagons available for this route.");
        } else {
            displayWagons(wagons);
        }

        // Assign locomotives
        boolean locomotiveErrors = false;
        System.out.println("\n--- Assign Locomotives ---");
        String locoInput;
        do {
            locomotiveErrors = false;
            System.out.print("Enter locomotive IDs to assign (comma-separated, or 'skip' to skip): ");
            locoInput = scanner.nextLine().trim();
            if (!locoInput.equalsIgnoreCase("skip") && !locoInput.isEmpty()) {
                String[] locoIds = locoInput.split(",");
                for (String locoIdStr : locoIds) {
                    try {
                        int locoId = Integer.parseInt(locoIdStr.trim());
                        controller.assignLocomotiveToRoute(locoId, routeId);
                        System.out.println("✓ Locomotive " + locoId + " assigned successfully.");
                    } catch (NumberFormatException e) {
                        System.out.println("✗ Invalid locomotive ID: " + locoIdStr);
                        locomotiveErrors = true;
                    } catch (Exception e) {
                        System.out.println("✗ Error assigning locomotive " + locoIdStr + ": " + e.getMessage());
                        locomotiveErrors = true;
                    }
                }
                
                if (locomotiveErrors) {
                    System.out.print("\nSome locomotives failed to assign. Do you want to retry? (y/n): ");
                    String retry = scanner.nextLine().trim().toLowerCase();
                    if (!"y".equals(retry) && !"yes".equals(retry)) {
                        connection.rollback();
                        System.out.println("Train assembly cancelled.");
                        return;
                    }
                    // Clear the input for retry
                    locoInput = "";
                }
            }
        } while (locomotiveErrors);

        // Assign wagons
        boolean wagonErrors = false;
        System.out.println("\n--- Assign Wagons ---");
        String wagonInput;
        do {
            wagonErrors = false;
            System.out.print("Enter wagon IDs to assign (comma-separated, or 'skip' to skip): ");
            wagonInput = scanner.nextLine().trim();
            if (!wagonInput.equalsIgnoreCase("skip") && !wagonInput.isEmpty()) {
                String[] wagonIds = wagonInput.split(",");
                for (String wagonIdStr : wagonIds) {
                    try {
                        int wagonId = Integer.parseInt(wagonIdStr.trim());
                        controller.assignWagonToRoute(wagonId, routeId);
                        System.out.println("✓ Wagon " + wagonId + " assigned successfully.");
                    } catch (NumberFormatException e) {
                        System.out.println("✗ Invalid wagon ID: " + wagonIdStr);
                        wagonErrors = true;
                    } catch (Exception e) {
                        System.out.println("✗ Error assigning wagon " + wagonIdStr + ": " + e.getMessage());
                        wagonErrors = true;
                    }
                }
                
                if (wagonErrors) {
                    System.out.print("\nSome wagons failed to assign. Do you want to retry? (y/n): ");
                    String retry = scanner.nextLine().trim().toLowerCase();
                    if (!"y".equals(retry) && !"yes".equals(retry)) {
                        System.out.print("Do you want to commit the successful assignments? (y/n): ");
                        String commitAnyway = scanner.nextLine().trim().toLowerCase();
                        if (!"y".equals(commitAnyway) && !"yes".equals(commitAnyway)) {
                            connection.rollback();
                            System.out.println("Train assembly cancelled. All changes rolled back.");
                            return;
                        }
                        break; // Exit retry loop
                    }
                    // Clear the input for retry
                    wagonInput = "";
                }
            }
        } while (wagonErrors);

        connection.commit();
        System.out.println("\n✓ Train assembly completed successfully!");
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

        Train train = controller.getTrainForRoute(routeId);
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

        List<LocomotiveForAssembly> locomotives = controller.getAvailableLocomotives(routeId);
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

        List<WagonForAssembly> wagons = controller.getAvailableWagons(routeId);
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
        System.out.println(String.format("%-10s | %-25s | %-25s | %-12s | %-19s",
            "Route ID", "Start Facility", "End Facility", "Train ID", "Start Date"));
        System.out.println(String.format("%-10s-+-%-25s-+-%-25s-+-%-12s-+-%-19s",
            "----------", "-------------------------", "-------------------------", "------------", "-------------------"));

        for (Route route : routes) {
            String startFacility = route.getStartFacility() != null ? route.getStartFacility().getName() : "N/A";
            String endFacility = route.getEndFacility() != null ? route.getEndFacility().getName() : "N/A";
            String trainId = route.getTrainId() > 0 ? String.valueOf(route.getTrainId()) : "Not assigned";
            
            // Format date more nicely (remove 'T' and format as readable date-time)
            String startDate = "Not scheduled";
            if (route.getStartDate() != null) {
                java.time.format.DateTimeFormatter formatter = 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                startDate = route.getStartDate().format(formatter);
            }
            
            System.out.println(String.format("%-10d | %-25s | %-25s | %-12s | %-19s",
                route.getId(),
                truncateString(startFacility, 25),
                truncateString(endFacility, 25),
                trainId,
                startDate));
        }
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
}

