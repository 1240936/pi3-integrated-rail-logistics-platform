package main.domain;

import main.graph.Graph;
import main.graph.Edge;

import java.util.List;

/**
 * Encapsulates the result of computing a Minimal Backbone Network (MST).
 * USEI12: Contains the MST graph, total distance, and file paths.
 */
public class MinimalBackboneResult {
    private final Graph<Station, Double> mstGraph;
    private final List<Edge<Station, Double>> mstEdges;
    private final double totalDistance;
    private final String dotFilePath;
    private final String svgFilePath;

    /**
     * Constructs a MinimalBackboneResult.
     *
     * @param mstGraph the MST graph
     * @param mstEdges list of edges in the MST
     * @param totalDistance total distance of all edges in MST
     * @param dotFilePath path to the generated DOT file
     * @param svgFilePath path to the generated SVG file
     */
    public MinimalBackboneResult(Graph<Station, Double> mstGraph,
                                 List<Edge<Station, Double>> mstEdges,
                                 double totalDistance,
                                 String dotFilePath,
                                 String svgFilePath) {
        this.mstGraph = mstGraph;
        this.mstEdges = mstEdges;
        this.totalDistance = totalDistance;
        this.dotFilePath = dotFilePath;
        this.svgFilePath = svgFilePath;
    }

    public Graph<Station, Double> getMstGraph() {
        return mstGraph;
    }

    public List<Edge<Station, Double>> getMstEdges() {
        return mstEdges;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public String getDotFilePath() {
        return dotFilePath;
    }

    public String getSvgFilePath() {
        return svgFilePath;
    }

    @Override
    public String toString() {
        return String.format(
                "Minimal Backbone Network:\n" +
                        "  Vertices: %d\n" +
                        "  Edges: %d\n" +
                        "  Total Distance: %.2f km\n" +
                        "  DOT File: %s\n" +
                        "  SVG File: %s",
                mstGraph.numVertices(),
                mstGraph.numEdges(),
                totalDistance,
                dotFilePath,
                svgFilePath
        );
    }
}