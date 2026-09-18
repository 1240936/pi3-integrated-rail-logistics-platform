package main.controller;

import main.domain.MinimalBackboneResult;
import main.domain.Station;
import main.graph.Edge;
import main.graph.Graph;
import main.graph.Algorithms;
import main.graph.map.MapGraph;
import main.repositories.CsvValidatorResult;
import main.repositories.StationsWithIdCsvLoader;
import main.util.CoordinateConverter;
import main.util.DotFileGenerator;
import main.util.GraphvizExporter;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Service for computing Minimal Backbone Network (MST) for railway stations.
 * USEI12: Implements Prim's algorithm to compute MST and generates DOT/SVG visualizations.
 */
public class MinimalBackboneService {
    // Note: stationService kept for potential future use
    @SuppressWarnings("unused")
    private final StationService stationService;

    /**
     * Constructs a MinimalBackboneService.
     *
     * @param stationService the station service (kept for potential future use)
     */
    public MinimalBackboneService(StationService stationService) {
        this.stationService = stationService;
    }

    /**
     * Computes the Minimal Backbone Network (MST) for the Belgian railway network.
     *
     * @param stationsWithIdCsvPath path to CSV file with stations (format: Station id,Station,Lat,Lon,CoordX,CoordY)
     * @param stationToStationCsvPath path to CSV file with connections (format: departure_stid,arrival_stid,dist,capacity,cost)
     * @param outputDir directory where DOT and SVG files will be saved
     * @return MinimalBackboneResult with MST graph, files, and statistics
     * @throws IOException if there is an error reading files or generating output
     * @throws InterruptedException if Graphviz process is interrupted
     */
    public MinimalBackboneResult computeMinimalBackbone(
            String stationsWithIdCsvPath,
            String stationToStationCsvPath,
            String outputDir) throws IOException, InterruptedException {

        // 1. Load stations with IDs
        Map<String, Station> stationMap = StationsWithIdCsvLoader.loadWithIdMap(stationsWithIdCsvPath);
        if (stationMap.isEmpty()) {
            throw new IllegalArgumentException("No stations loaded from: " + stationsWithIdCsvPath);
        }

        // 2. Load connections (edges) - Note: loader uses cost, but we need distance
        // We'll need to load and convert to use distance
        CsvValidatorResult<Edge<Station, Double>> edgesResult =
                loadEdgesWithDistance(stationToStationCsvPath, stationMap);

        if (edgesResult.getRecords().isEmpty()) {
            throw new IllegalArgumentException("No valid edges loaded from: " + stationToStationCsvPath);
        }

        // 3. Build directed graph from edges
        MapGraph<Station, Double> directedGraph = new MapGraph<>(true);
        for (Station station : stationMap.values()) {
            directedGraph.addVertex(station);
        }
        for (Edge<Station, Double> edge : edgesResult.getRecords()) {
            directedGraph.addEdge(edge.getVOrig(), edge.getVDest(), edge.getWeight());
        }

        // 4. Convert directed graph to undirected
        Graph<Station, Double> undirectedGraph = convertToUndirected(directedGraph);

        // 5. Check if graph is connected (optional but recommended)
        if (undirectedGraph.numVertices() == 0) {
            throw new IllegalStateException("Graph has no vertices");
        }

        // 6. Compute MST using Prim's algorithm
        Station startVertex = undirectedGraph.vertices().get(0);
        Graph<Station, Double> mst = Algorithms.primMST(
                undirectedGraph,
                startVertex,
                Comparator.comparing(Double::doubleValue),
                Double::sum,
                0.0
        );

        // 7. Calculate total distance
        double totalDistance = 0.0;
        List<Edge<Station, Double>> mstEdges = new ArrayList<>();
        for (Edge<Station, Double> edge : mst.edges()) {
            totalDistance += edge.getWeight();
            mstEdges.add(edge);
        }

        // 8. Prepare output directory
        File outputDirFile = new File(outputDir);
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs();
        }

        // 9. Convert coordinates to XY
        List<Station> stations = new ArrayList<>(mst.vertices());
        CoordinateConverter converter = new CoordinateConverter(stations, 1000.0, 1000.0);
        Map<Station, double[]> coordinates = new HashMap<>();
        for (Station station : stations) {
            coordinates.put(station, converter.getXY(station));
        }

        // 10. Generate DOT file
        String dotFileName = "minimal_backbone.dot";
        String dotFilePath = new File(outputDir, dotFileName).getAbsolutePath();
        DotFileGenerator.generateDotFile(mst, coordinates, dotFilePath);

        // 11. Generate SVG file
        String svgFileName = "minimal_backbone.svg";
        String svgFilePath = new File(outputDir, svgFileName).getAbsolutePath();

        try {
            GraphvizExporter.generateSVG(dotFilePath, svgFilePath);
        } catch (IOException e) {
            // If Graphviz is not available, continue without SVG
            System.err.println("Warning: Could not generate SVG file. " + e.getMessage());
            svgFilePath = null;
        }

        return new MinimalBackboneResult(
                mst,
                mstEdges,
                totalDistance,
                dotFilePath,
                svgFilePath
        );
    }

    /**
     * Loads edges from CSV using distance as weight (instead of cost).
     * This is a helper method that reads the CSV and uses the distance field.
     */
    private CsvValidatorResult<Edge<Station, Double>> loadEdgesWithDistance(
            String csvPath,
            Map<String, Station> stationMap) throws IOException {

        CsvValidatorResult<Edge<Station, Double>> result = new CsvValidatorResult<>();

        // Read CSV manually to extract distance field
        CsvReader.readCsv(csvPath, (lineNo, fields) -> {
            if (fields.length < 5) {
                result.addError("Line " + lineNo + ": expected at least 5 columns, got " + fields.length);
                return;
            }

            try {
                String fromKey = fields[0].trim();
                String toKey = fields[1].trim();
                double dist = Double.parseDouble(fields[2].trim());  // Use distance, not cost

                if (dist < 0) {
                    result.addError("Line " + lineNo + ": distance cannot be negative, got " + dist);
                    return;
                }

                Station fromStation = stationMap.get(fromKey);
                Station toStation = stationMap.get(toKey);

                if (fromStation == null) {
                    result.addError("Line " + lineNo + ": station not found: " + fromKey);
                    return;
                }
                if (toStation == null) {
                    result.addError("Line " + lineNo + ": station not found: " + toKey);
                    return;
                }

                // Create edge with distance as weight
                Edge<Station, Double> edge = new Edge<>(fromStation, toStation, dist);
                result.addRecord(edge);

            } catch (NumberFormatException e) {
                result.addError("Line " + lineNo + ": invalid number format: " + e.getMessage());
            } catch (Exception e) {
                result.addError("Line " + lineNo + ": error parsing connection: " + e.getMessage());
            }
        });

        return result;
    }

    /**
     * Converts a directed graph to an undirected graph.
     * For edges A→B and B→A, uses the minimum distance.
     *
     * @param directedGraph the directed graph to convert
     * @return undirected graph
     */
    private Graph<Station, Double> convertToUndirected(Graph<Station, Double> directedGraph) {
        MapGraph<Station, Double> undirected = new MapGraph<>(false);

        // Add all vertices
        for (Station station : directedGraph.vertices()) {
            undirected.addVertex(station);
        }

        // Process edges: for each directed edge, create undirected edge
        // If both A→B and B→A exist, use minimum distance
        Map<String, Double> edgeMap = new HashMap<>();  // "station1|station2" -> minDistance

        for (Edge<Station, Double> edge : directedGraph.edges()) {
            Station from = edge.getVOrig();
            Station to = edge.getVDest();
            Double weight = edge.getWeight();

            // Create symmetric key (order-independent)
            String key1 = from.getName() + "|" + to.getName();
            String key2 = to.getName() + "|" + from.getName();

            // If reverse edge exists, use minimum
            if (edgeMap.containsKey(key2)) {
                Double existingWeight = edgeMap.get(key2);
                if (weight < existingWeight) {
                    edgeMap.remove(key2);
                    edgeMap.put(key1, weight);
                }
            } else {
                edgeMap.put(key1, weight);
            }
        }

        // Add edges to undirected graph
        for (Edge<Station, Double> edge : directedGraph.edges()) {
            Station from = edge.getVOrig();
            Station to = edge.getVDest();
            String key = from.getName() + "|" + to.getName();

            if (edgeMap.containsKey(key)) {
                undirected.addEdge(from, to, edgeMap.get(key));
            }
        }

        return undirected;
    }
}