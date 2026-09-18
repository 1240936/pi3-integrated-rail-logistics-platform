package main.graph;

import main.graph.map.MapGraph;
import main.graph.matrix.MatrixGraph;

import java.util.*;

import java.util.function.BinaryOperator;

/**
 *
 * @author DEI-ISEP
 *
 */
public class Algorithms {

    /** Performs breadth-first search of a Graph starting in a vertex
     *
     * @param g Graph instance
     * @param vert vertex that will be the source of the search
     * @return a LinkedList with the vertices of breadth-first search
     */
    public static <V, E> LinkedList<V> BreadthFirstSearch(Graph<V, E> g, V vert) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** Performs depth-first search starting in a vertex
     *
     * @param g Graph instance
     * @param vOrig vertex of graph g that will be the source of the search
     * @param visited set of previously visited vertices
     * @param qdfs return LinkedList with vertices of depth-first search
     */
    private static <V, E> void DepthFirstSearch(Graph<V, E> g, V vOrig, boolean[] visited, LinkedList<V> qdfs) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** Performs depth-first search starting in a vertex
     *
     * @param g Graph instance
     * @param vert vertex of graph g that will be the source of the search
     * @return a LinkedList with the vertices of depth-first search
     */
    public static <V, E> LinkedList<V> DepthFirstSearch(Graph<V, E> g, V vert) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** Returns all paths from vOrig to vDest
     *
     * @param g       Graph instance
     * @param vOrig   Vertex that will be the source of the path
     * @param vDest   Vertex that will be the end of the path
     * @param visited set of discovered vertices
     * @param path    stack with vertices of the current path (the path is in reverse order)
     * @param paths   ArrayList with all the paths (in correct order)
     */
    private static <V, E> void allPaths(Graph<V, E> g, V vOrig, V vDest, boolean[] visited,
                                        LinkedList<V> path, ArrayList<LinkedList<V>> paths) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** Returns all paths from vOrig to vDest
     *
     * @param g     Graph instance
     * @param vOrig information of the Vertex origin
     * @param vDest information of the Vertex destination
     * @return paths ArrayList with all paths from vOrig to vDest
     */
    public static <V, E> ArrayList<LinkedList<V>> allPaths(Graph<V, E> g, V vOrig, V vDest) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Computes shortest-path distance from a source vertex to all reachable
     * vertices of a graph g with non-negative edge weights
     * This implementation uses Dijkstra's algorithm
     *
     * @param g        Graph instance
     * @param vOrig    Vertex that will be the source of the path
     * @param visited  set of previously visited vertices
     * @param pathKeys minimum path vertices keys
     * @param dist     minimum distances
     */
    private static <V, E> void shortestPathDijkstra(Graph<V, E> g, V vOrig,
                                                    Comparator<E> ce, BinaryOperator<E> sum, E zero,
                                                    boolean[] visited, V [] pathKeys, E [] dist) {
        
        throw new UnsupportedOperationException("Not supported yet.");
    }
   
    /** Shortest-path between two vertices
     *
     * @param g graph
     * @param vOrig origin vertex
     * @param vDest destination vertex
     * @param ce comparator between elements of type E
     * @param sum sum two elements of type E
     * @param zero neutral element of the sum in elements of type E
     * @param shortPath returns the vertices which make the shortest path
     * @return if vertices exist in the graph and are connected, true, false otherwise
     */
    public static <V, E> E shortestPath(Graph<V, E> g, V vOrig, V vDest,
                                        Comparator<E> ce, BinaryOperator<E> sum, E zero,
                                        LinkedList<V> shortPath) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** Shortest-path between a vertex and all other vertices
     *
     * @param g graph
     * @param vOrig start vertex
     * @param ce comparator between elements of type E
     * @param sum sum two elements of type E
     * @param zero neutral element of the sum in elements of type E
     * @param paths returns all the minimum paths
     * @param dists returns the corresponding minimum distances
     * @return if vOrig exists in the graph true, false otherwise
     */
    public static <V, E> boolean shortestPaths(Graph<V, E> g, V vOrig,
                                               Comparator<E> ce, BinaryOperator<E> sum, E zero,
                                               ArrayList<LinkedList<V>> paths, ArrayList<E> dists) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Extracts from pathKeys the minimum path between voInf and vdInf
     * The path is constructed from the end to the beginning
     *
     * @param g        Graph instance
     * @param vOrig    information of the Vertex origin
     * @param vDest    information of the Vertex destination
     * @param pathKeys minimum path vertices keys
     * @param path     stack with the minimum path (correct order)
     */
    private static <V, E> void getPath(Graph<V, E> g, V vOrig, V vDest,
                                       V [] pathKeys, LinkedList<V> path) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** Calculates the minimum distance graph using Floyd-Warshall
     * 
     * @param g initial graph
     * @param ce comparator between elements of type E
     * @param sum sum two elements of type E
     * @return the minimum distance graph
     */
    public static <V,E> MatrixGraph <V,E> minDistGraph(Graph <V,E> g, Comparator<E> ce, BinaryOperator<E> sum) {
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Performs topological sort of a directed graph using Kahn's algorithm.
     * Returns null if the graph contains cycles.
     * 
     * @param g Graph instance (must be directed)
     * @return LinkedList with vertices in topological order, or null if cycles detected
     */
    public static <V, E> LinkedList<V> topologicalSort(Graph<V, E> g) {
        if (!g.isDirected()) {
            throw new IllegalArgumentException("Topological sort requires a directed graph");
        }
        
        // 1. Calcular in-degree de cada vértice
        Map<V, Integer> inDegree = new HashMap<>();
        for (V vert : g.vertices()) {
            inDegree.put(vert, 0);
        }
        
        // Calcular in-degree contando arestas que chegam
        for (V vert : g.vertices()) {
            Collection<Edge<V, E>> incoming = g.incomingEdges(vert);
            if (incoming != null) {
                inDegree.put(vert, incoming.size());
            }
        }
        
        // 2. Fila com vértices de in-degree = 0
        Queue<V> queue = new LinkedList<>();
        for (V vert : g.vertices()) {
            if (inDegree.get(vert) == 0) {
                queue.offer(vert);
            }
        }
        
        // 3. Processar vértices
        LinkedList<V> topologicalOrder = new LinkedList<>();
        int processedCount = 0;
        
        while (!queue.isEmpty()) {
            V current = queue.poll();
            topologicalOrder.add(current);
            processedCount++;
            
            // Reduzir in-degree dos vizinhos
            Collection<V> neighbors = g.adjVertices(current);
            if (neighbors != null) {
                for (V neighbor : neighbors) {
                    int newDegree = inDegree.get(neighbor) - 1;
                    inDegree.put(neighbor, newDegree);
                    
                    if (newDegree == 0) {
                        queue.offer(neighbor);
                    }
                }
            }
        }
        
        // 4. Verificar se há ciclos
        if (processedCount != g.numVertices()) {
            return null;  // Há ciclos
        }
        
        return topologicalOrder;
    }

    /**
     * Finds vertices involved in cycles in a directed graph.
     * Uses DFS with color marking (WHITE/GRAY/BLACK).
     * 
     * @param g Graph instance (must be directed)
     * @return Set of vertices involved in cycles
     */
    public static <V, E> Set<V> findCycleVertices(Graph<V, E> g) {
        if (!g.isDirected()) {
            throw new IllegalArgumentException("Cycle detection requires a directed graph");
        }
        
        // Usar inteiros para representar cores: 0=WHITE, 1=GRAY, 2=BLACK
        Map<V, Integer> color = new HashMap<>();
        Set<V> cycleVertices = new HashSet<>();
        
        // Inicializar todos como WHITE (0)
        for (V vert : g.vertices()) {
            color.put(vert, 0);
        }
        
        // DFS para detectar ciclos
        for (V vert : g.vertices()) {
            if (color.get(vert) == 0) {  // WHITE
                dfsCycleDetection(g, vert, color, cycleVertices);
            }
        }
        
        return cycleVertices;
    }

    private static <V, E> void dfsCycleDetection(Graph<V, E> g, V vert, 
                                                 Map<V, Integer> color, Set<V> cycleVertices) {
        color.put(vert, 1);  // GRAY
        
        Collection<V> neighbors = g.adjVertices(vert);
        if (neighbors != null) {
            for (V neighbor : neighbors) {
                Integer neighborColor = color.get(neighbor);
                
                if (neighborColor != null && neighborColor == 1) {  // GRAY
                    // Ciclo detectado!
                    cycleVertices.add(vert);
                    cycleVertices.add(neighbor);
                } else if (neighborColor != null && neighborColor == 0) {  // WHITE
                    dfsCycleDetection(g, neighbor, color, cycleVertices);
                    // Se o vizinho está em ciclo, este também pode estar
                    if (cycleVertices.contains(neighbor)) {
                        cycleVertices.add(vert);
                    }
                }
            }
        }
        
        color.put(vert, 2);  // BLACK
    }

    /**
     * Computes the Minimum Spanning Tree (MST) of an undirected graph using Prim's algorithm.
     * Uses a PriorityQueue (Heap) for efficient edge selection.
     *
     * @param graph Graph instance (must be undirected)
     * @param startVertex Starting vertex for MST construction
     * @param weightComparator Comparator to compare edge weights
     * @param sum Binary operator to sum edge weights (for future use)
     * @param zero Zero value for edge weight type (for future use)
     * @return Graph representing the MST
     * @throws IllegalArgumentException if graph is directed or startVertex is invalid
     */
    public static <V, E extends Comparable<E>> Graph<V, E> primMST(
            Graph<V, E> graph,
            V startVertex,
            Comparator<E> weightComparator,
            BinaryOperator<E> sum,
            E zero) {

        if (graph.isDirected()) {
            throw new IllegalArgumentException("Prim's algorithm requires an undirected graph");
        }

        if (!graph.validVertex(startVertex)) {
            throw new IllegalArgumentException("Start vertex not in graph");
        }

        // Create empty MST graph (undirected)
        MapGraph<V, E> mst = new MapGraph<>(false);

        // Data structures
        Set<V> inMST = new HashSet<>();  // Vertices already in MST
        PriorityQueue<Edge<V, E>> pq = new PriorityQueue<>(
                (e1, e2) -> weightComparator.compare(e1.getWeight(), e2.getWeight())
        );

        // Initialize with start vertex
        inMST.add(startVertex);
        mst.addVertex(startVertex);

        // Add edges from start vertex to priority queue
        Collection<Edge<V, E>> startEdges = graph.outgoingEdges(startVertex);
        if (startEdges != null) {
            for (Edge<V, E> edge : startEdges) {
                pq.offer(edge);
            }
        }

        // Process until MST has V-1 edges
        while (!pq.isEmpty() && mst.numEdges() < graph.numVertices() - 1) {
            Edge<V, E> minEdge = pq.poll();
            V u = minEdge.getVOrig();
            V v = minEdge.getVDest();

            // Determine which vertex is in MST and which is not
            V inTree = inMST.contains(u) ? u : v;
            V notInTree = inMST.contains(u) ? v : u;

            // Skip if both vertices are already in MST (would form cycle)
            if (inMST.contains(notInTree)) {
                continue;
            }

            // Add vertex and edge to MST
            inMST.add(notInTree);
            mst.addVertex(notInTree);
            mst.addEdge(inTree, notInTree, minEdge.getWeight());

            // Add edges from new vertex to priority queue
            Collection<Edge<V, E>> newEdges = graph.outgoingEdges(notInTree);
            if (newEdges != null) {
                for (Edge<V, E> edge : newEdges) {
                    // Determine neighbor (graph is undirected, so edge can be u->v or v->u)
                    V neighbor = edge.getVDest().equals(notInTree) ?
                            edge.getVOrig() : edge.getVDest();
                    if (!inMST.contains(neighbor)) {
                        pq.offer(edge);
                    }
                }
            }
        }

        return mst;
    }
}

