package main;

import main.ui.FreightManagerUI;
import main.ui.TrafficManagerUI;
import main.ui.TrainSchedulerUI;

import java.util.Scanner;

/**
 * Main entry point for the Freight Management System
 * Allows selection between Freight Manager and Traffic Manager roles
 */
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=== FREIGHT MANAGEMENT SYSTEM ===");
            System.out.println("Select your role:");
            System.out.println("1. Freight Manager");
            System.out.println("2. Traffic Manager (Train Assembly)");
            System.out.println("3. Traffic Manager (Train Scheduler)");
            System.out.println("0. Exit");
            System.out.print("\nEnter your choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.println("\n=== FREIGHT MANAGER MODE ===");
                    FreightManagerUI freightUI = new FreightManagerUI();
                    freightUI.start();
                    break;
                case "2":
                    System.out.println("\n=== TRAFFIC MANAGER MODE - TRAIN ASSEMBLY ===");
                    TrafficManagerUI trafficUI = new TrafficManagerUI();
                    trafficUI.start();
                    break;
                case "3":
                    System.out.println("\n=== TRAFFIC MANAGER MODE - TRAIN SCHEDULER ===");
                    TrainSchedulerUI schedulerUI = new TrainSchedulerUI();
                    schedulerUI.start();
                    break;
                case "0":
                    running = false;
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }

        scanner.close();
    }
}
