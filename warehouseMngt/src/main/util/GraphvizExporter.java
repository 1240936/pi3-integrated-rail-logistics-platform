package main.util;

import java.io.File;
import java.io.IOException;

/**
 * Exports DOT files to SVG format using Graphviz neato application.
 * USEI12: Generates SVG visualizations from DOT files.
 */
public class GraphvizExporter {

    /**
     * Finds the neato executable path.
     * Tries common installation locations if not in PATH.
     *
     * @return path to neato executable, or "neato" if found in PATH
     */
    private static String findNeatoPath() {
        // First, try to find neato in PATH
        if (isNeatoAvailable()) {
            return "neato";
        }
        
        // Common Graphviz installation paths
        String os = System.getProperty("os.name").toLowerCase();
        String[] possiblePaths;
        
        if (os.contains("win")) {
            // Windows paths
            String programFiles = System.getenv("ProgramFiles");
            String programFilesX86 = System.getenv("ProgramFiles(x86)");
            String userHome = System.getProperty("user.home");
            String desktop = userHome + "\\Desktop";
            
            // Try to find any Graphviz installation on Desktop
            java.util.List<String> desktopPaths = new java.util.ArrayList<>();
            File desktopDir = new File(desktop);
            if (desktopDir.exists() && desktopDir.isDirectory()) {
                File[] desktopFiles = desktopDir.listFiles();
                if (desktopFiles != null) {
                    for (File file : desktopFiles) {
                        if (file.isDirectory() && file.getName().toLowerCase().startsWith("graphviz")) {
                            File binDir = new File(file, "bin");
                            File neatoExe = new File(binDir, "neato.exe");
                            if (neatoExe.exists()) {
                                desktopPaths.add(neatoExe.getAbsolutePath());
                            }
                        }
                    }
                }
            }
            
            // Build list of possible paths
            java.util.List<String> pathsList = new java.util.ArrayList<>();
            
            // Add Desktop Graphviz installations first (most likely custom installs)
            pathsList.addAll(desktopPaths);
            
            // Add specific Desktop path
            pathsList.add(desktop + "\\Graphviz-14.1.0-win64\\bin\\neato.exe");
            
            // Add standard installation locations
            if (programFiles != null) {
                pathsList.add(programFiles + "\\Graphviz\\bin\\neato.exe");
            }
            if (programFilesX86 != null) {
                pathsList.add(programFilesX86 + "\\Graphviz\\bin\\neato.exe");
            }
            pathsList.add("C:\\Program Files\\Graphviz\\bin\\neato.exe");
            pathsList.add("C:\\Program Files (x86)\\Graphviz\\bin\\neato.exe");
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                pathsList.add(localAppData + "\\Programs\\Graphviz\\bin\\neato.exe");
            }
            
            possiblePaths = pathsList.toArray(new String[0]);
        } else if (os.contains("mac")) {
            // macOS paths
            possiblePaths = new String[]{
                "/usr/local/bin/neato",
                "/opt/homebrew/bin/neato",
                "/usr/bin/neato"
            };
        } else {
            // Linux paths
            possiblePaths = new String[]{
                "/usr/bin/neato",
                "/usr/local/bin/neato"
            };
        }
        
        // Check each possible path
        for (String path : possiblePaths) {
            if (path != null) {
                File neatoFile = new File(path);
                if (neatoFile.exists() && neatoFile.canExecute()) {
                    return path;
                }
            }
        }
        
        // If not found, return "neato" and let it fail with a better error message
        return "neato";
    }

    /**
     * Generates an SVG file from a DOT file by executing Graphviz neato.
     * 
     * Note: This method does NOT generate SVG in Java code. It calls the external
     * Graphviz 'neato' application, which reads the DOT file and generates the SVG.
     * The SVG generation is done entirely by Graphviz, not by this Java code.
     *
     * @param dotFilePath path to the input DOT file
     * @param svgOutputPath path where the SVG file will be created
     * @throws IOException if there is an error executing Graphviz or creating the file
     * @throws InterruptedException if the process is interrupted
     */
    public static void generateSVG(String dotFilePath, String svgOutputPath) 
            throws IOException, InterruptedException {
        
        // Check if DOT file exists
        File dotFile = new File(dotFilePath);
        if (!dotFile.exists()) {
            throw new IOException("DOT file not found: " + dotFilePath);
        }
        
        // Find neato executable
        String neatoPath = findNeatoPath();
        
        // Execute Graphviz neato to generate SVG from DOT file
        // Command: neato -Tsvg input.dot -o output.svg
        // The SVG is generated by Graphviz, not by Java code
        ProcessBuilder pb = new ProcessBuilder(
            neatoPath,
            "-Tsvg",
            dotFilePath,
            "-o",
            svgOutputPath
        );
        
        // Redirect error stream to standard output for debugging
        pb.redirectErrorStream(true);
        
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new IOException(
                    "Graphviz neato failed with exit code: " + exitCode + 
                    ". Make sure Graphviz is installed and 'neato' is in your PATH."
                );
            }
            
            // Verify that SVG file was created
            File svgFile = new File(svgOutputPath);
            if (!svgFile.exists()) {
                throw new IOException("SVG file was not created: " + svgOutputPath);
            }
            
        } catch (IOException e) {
            // Check if it's because neato is not found
            if (e.getMessage() != null && (e.getMessage().contains("Cannot run program") || 
                e.getMessage().contains("neato"))) {
                String os = System.getProperty("os.name").toLowerCase();
                String instructions;
                
                if (os.contains("win")) {
                    instructions = 
                        "Graphviz 'neato' command not found.\n" +
                        "Solutions:\n" +
                        "1. Add Graphviz to PATH: Add 'C:\\Program Files\\Graphviz\\bin' to your system PATH\n" +
                        "2. Restart your IDE/terminal after adding to PATH\n" +
                        "3. Or reinstall Graphviz and select 'Add to PATH' during installation\n" +
                        "Visit https://graphviz.org/download/ for installation instructions.";
                } else {
                    instructions = 
                        "Graphviz 'neato' command not found.\n" +
                        "Please install Graphviz and ensure 'neato' is in your PATH.\n" +
                        "Visit https://graphviz.org/download/ for installation instructions.";
                }
                
                throw new IOException(instructions, e);
            }
            throw e;
        }
    }

    /**
     * Checks if Graphviz neato is available in the system.
     *
     * @return true if neato is available, false otherwise
     */
    public static boolean isNeatoAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("neato", "-V");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

