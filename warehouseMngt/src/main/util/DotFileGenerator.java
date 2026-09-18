package main.util;

import main.domain.Station;
import main.graph.Graph;
import main.graph.Edge;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Generates DOT file representation of a graph for Graphviz visualization.
 * USEI12: Creates DOT files with vertices positioned at XY coordinates.
 */
public class DotFileGenerator {

    /**
     * Generates a DOT file from a graph with stations positioned at XY coordinates.
     *
     * @param graph the graph to represent
     * @param coordinates map from station to [x, y] coordinates
     * @param outputPath path where the DOT file will be written
     * @throws IOException if there is an error writing the file
     */
    public static void generateDotFile(Graph<Station, Double> graph,
                                       Map<Station, double[]> coordinates,
                                       String outputPath) throws IOException {
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            // Graph declaration (undirected graph)
            writer.println("graph MinimalBackboneNetwork {");
            
            // Graph attributes
            writer.println("    overlap=false;");
            writer.println("    splines=true;");
            
            // Default node attributes
            writer.println("    node [shape=circle, style=filled, fillcolor=lightblue, fontsize=8];");
            
            // Default edge attributes
            writer.println("    edge [color=gray, fontsize=8];");
            writer.println();
            
            // Write vertices with fixed positions (for neato layout)
            // The '!' in pos forces fixed positions
            for (Station station : graph.vertices()) {
                double[] xy = coordinates.get(station);
                if (xy == null || xy.length < 2) {
                    continue;  // Skip if coordinates not available
                }
                
                String name = escapeDotString(station.getName());
                // pos="x,y!" - the '!' forces neato to use these exact positions
                writer.printf("    \"%s\" [pos=\"%.2f,%.2f!\", label=\"%s\"];%n",
                             name, xy[0], xy[1], name);
            }
            
            writer.println();
            
            // Write edges (undirected graph uses --)
            for (Edge<Station, Double> edge : graph.edges()) {
                String from = escapeDotString(edge.getVOrig().getName());
                String to = escapeDotString(edge.getVDest().getName());
                double distance = edge.getWeight();
                writer.printf("    \"%s\" -- \"%s\" [label=\"%.1f km\"];%n",
                             from, to, distance);
            }
            
            writer.println("}");
        }
    }

    /**
     * Escapes special characters in a string for DOT format.
     *
     * @param str the string to escape
     * @return escaped string
     */
    private static String escapeDotString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
}

