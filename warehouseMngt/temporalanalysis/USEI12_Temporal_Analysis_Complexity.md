# USEI12 - Análise de Complexidade Temporal

## Metodologia

O documento segue a metodologia sugerida nos slides fornecidos nas aulas de ESINF, analisando individualmente os métodos implementados na USEI12.

**Notação:**
- **V** = número de vértices (estações)
- **E** = número de arestas (conexões entre estações)
- **n** = número de estações
- **m** = número de conexões
- **k** = número de vizinhos de um vértice específico

**Sobre T(n) e S(n):**
- **T(n)** = Complexidade Temporal (tempo de execução) - mede a quantidade de operações realizada pelo algoritmo
- **S(n)** = Complexidade Espacial (memória) - mede o espaço na memória utilizado pelo algoritmo

**Sobre Caso Médio:**
O caso médio representa o comportamento esperado do algoritmo quando executado com entradas aleatórias. Para calcular:
1. Identificar as possíveis entradas e as suas probabilidades
2. Calcular a complexidade para cada entrada
3. Calcular a média ponderada: Σ(probabilidade × complexidade)

Quando o caso médio é difícil de modelar, assume-se que tende para o pior caso. (Informação dos slides de ESINF)

---

## 1. MinimalBackboneService.loadEdgesWithDistance(String csvPath, Map<String, Station> stationMap)

### 1.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo simples (iteração sobre linhas CSV)
- **Recursivo:** Não

### 1.2 Análise de Operações Primitivas

```java
private CsvValidatorResult<Edge<Station, Double>> loadEdgesWithDistance(
        String csvPath,
        Map<String, Station> stationMap) throws IOException {

    CsvValidatorResult<Edge<Station, Double>> result = new CsvValidatorResult<>();  // 1

    CsvReader.readCsv(csvPath, (lineNo, fields) -> {
        if (fields.length < 5) {                                    // 1 comparação
            result.addError(...);                                    // 1 chamada função
            return;
        }

        try {
            String fromKey = fields[0].trim();                     // 1 atribuição + 1 chamada função
            String toKey = fields[1].trim();                       // 1 atribuição + 1 chamada função
            double dist = Double.parseDouble(fields[2].trim());    // 1 atribuição + 2 chamadas função

            if (dist < 0) {                                         // 1 comparação
                result.addError(...);                                // 1 chamada função
                return;
            }

            Station fromStation = stationMap.get(fromKey);         // 1 atribuição + 1 chamada função O(1)
            Station toStation = stationMap.get(toKey);             // 1 atribuição + 1 chamada função O(1)

            if (fromStation == null) {                              // 1 comparação
                result.addError(...);                                // 1 chamada função
                return;
            }
            if (toStation == null) {                                // 1 comparação
                result.addError(...);                                // 1 chamada função
                return;
            }

            Edge<Station, Double> edge = new Edge<>(...);          // 1 atribuição + 1 criação objeto
            result.addRecord(edge);                                 // 1 chamada função O(1) amortizado
        } catch (NumberFormatException e) {
            result.addError(...);                                    // 1 chamada função
        }
    });

    return result;  // 1 retorno
}
```

**Operações primitivas por linha (caminho normal):**
- Processamento de campos: 1 + 1 + 1 = **3**
- Validações e lookups: 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 = **8**
- Criação de edge e adição: 1 + 1 = **2**
- **Total por linha: 13 operações**

**Expressão completa:**
- Inicialização: **1**
- Loop: m × 13 = **13m** (assumindo todas as linhas válidas)
- Retorno: **1**
- **Total: 13m + 2**

### 1.3 Análise de Casos

**Melhor caso:** todas as linhas inválidas (retorno precoce) → T(n) = 2m + 2

**Pior caso:** todas as linhas válidas → T(n) = 13m + 2

**Caso médio:** Em CSVs reais, a maioria das linhas são válidas, então o caso médio tende para o pior caso: T(n) ≈ 13m + 2

### 1.4 Simplificação Big-Oh

**T(n) = O(m)** onde m = número de conexões (linhas no CSV)

**S(n) = O(m)** - espaço para armazenar as arestas no resultado

**Complexidade Final: O(m)**

**Explicação:** O método processa cada linha do CSV uma vez, realizando operações constantes por linha. O número de operações é linear no número de conexões.

---

## 2. MinimalBackboneService.convertToUndirected(Graph<Station, Double> directedGraph)

### 2.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclos aninhados e sequenciais
- **Recursivo:** Não

### 2.2 Análise de Operações Primitivas

```java
private Graph<Station, Double> convertToUndirected(Graph<Station, Double> directedGraph) {
    MapGraph<Station, Double> undirected = new MapGraph<>(false);  // 1

    // Add all vertices
    for (Station station : directedGraph.vertices()) {              // n iterações
        undirected.addVertex(station);                              // 1 chamada função O(1)
    }

    // Process edges: for each directed edge, create undirected edge
    Map<String, Double> edgeMap = new HashMap<>();                  // 1

    for (Edge<Station, Double> edge : directedGraph.edges()) {     // m iterações
        Station from = edge.getVOrig();                             // 1 atribuição
        Station to = edge.getVDest();                               // 1 atribuição
        Double weight = edge.getWeight();                           // 1 atribuição

        String key1 = from.getName() + "|" + to.getName();         // 1 atribuição + concatenação
        String key2 = to.getName() + "|" + from.getName();         // 1 atribuição + concatenação

        if (edgeMap.containsKey(key2)) {                            // 1 comparação + 1 chamada função O(1)
            Double existingWeight = edgeMap.get(key2);              // 1 atribuição + 1 chamada função O(1)
            if (weight < existingWeight) {                          // 1 comparação
                edgeMap.remove(key2);                               // 1 chamada função O(1)
                edgeMap.put(key1, weight);                          // 1 chamada função O(1)
            }
        } else {
            edgeMap.put(key1, weight);                              // 1 chamada função O(1)
        }
    }

    // Add edges to undirected graph
    for (Edge<Station, Double> edge : directedGraph.edges()) {     // m iterações
        Station from = edge.getVOrig();                             // 1 atribuição
        Station to = edge.getVDest();                               // 1 atribuição
        String key = from.getName() + "|" + to.getName();          // 1 atribuição + concatenação

        if (edgeMap.containsKey(key)) {                             // 1 comparação + 1 chamada função O(1)
            undirected.addEdge(from, to, edgeMap.get(key));         // 1 chamada função O(1) + 1 lookup O(1)
        }
    }

    return undirected;  // 1 retorno
}
```

**Análise passo a passo:**

#### Passo 1: Adicionar vértices
- Loop: n × 1 = **n**

#### Passo 2: Processar arestas (primeiro loop)
- Por iteração: 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 = **8 operações** (no pior caso, quando key2 existe)
- Loop executa m vezes
- **Total: 8m**

#### Passo 3: Adicionar arestas ao grafo não direcionado (segundo loop)
- Por iteração: 1 + 1 + 1 + 1 + 1 + 1 = **6 operações**
- Loop executa m vezes
- **Total: 6m**

**Expressão completa:**
- Inicialização: **2**
- Passo 1: **n**
- Passo 2: **8m**
- Passo 3: **6m**
- Retorno: **1**
- **Total: n + 14m + 3**

### 2.3 Análise de Casos

**Melhor caso:** grafo sem arestas bidirecionais (m = 0) → T(n) = n + 3

**Pior caso:** todas as arestas têm correspondente bidirecional → T(n) = n + 14m + 3

**Caso médio:** Em grafos reais de estações ferroviárias, tipicamente existe uma mistura de arestas bidirecionais e unidirecionais. O caso médio tende para o pior caso: T(n) ≈ n + 14m + 3

### 2.4 Simplificação Big-Oh

**T(n) = O(n + m)** onde n = número de vértices, m = número de arestas

**S(n) = O(m)** - espaço para o HashMap edgeMap e o grafo não direcionado resultante

**Complexidade Final: O(n + m)**

**Explicação:** O método percorre todos os vértices uma vez e todas as arestas duas vezes (uma para construir o mapa e outra para adicionar ao grafo não direcionado). Em grafos esparsos típicos (m ≈ n), a complexidade é dominada por O(m).

---

## 3. Algorithms.primMST(Graph<V, E> graph, V startVertex, ...)

### 3.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo while com PriorityQueue
- **Recursivo:** Não
- **Algoritmo:** Prim's Algorithm (usando PriorityQueue/Heap)

### 3.2 Análise de Operações Primitivas

```java
public static <V, E extends Comparable<E>> Graph<V, E> primMST(
        Graph<V, E> graph,
        V startVertex,
        Comparator<E> weightComparator,
        BinaryOperator<E> sum,
        E zero) {

    if (graph.isDirected()) {                                       // 1 comparação + 1 chamada função
        throw new IllegalArgumentException(...);
    }

    if (!graph.validVertex(startVertex)) {                          // 1 comparação + 1 chamada função
        throw new IllegalArgumentException(...);
    }

    MapGraph<V, E> mst = new MapGraph<>(false);                     // 1

    Set<V> inMST = new HashSet<>();                                 // 1
    PriorityQueue<Edge<V, E>> pq = new PriorityQueue<>(...);       // 1

    inMST.add(startVertex);                                         // 1 chamada função O(1) amortizado
    mst.addVertex(startVertex);                                     // 1 chamada função O(1)

    Collection<Edge<V, E>> startEdges = graph.outgoingEdges(startVertex);  // 1 atribuição + 1 chamada função O(k)
    if (startEdges != null) {                                       // 1 comparação
        for (Edge<V, E> edge : startEdges) {                        // k iterações (k = grau do vértice inicial)
            pq.offer(edge);                                         // 1 chamada função O(log m) amortizado
        }
    }

    while (!pq.isEmpty() && mst.numEdges() < graph.numVertices() - 1) {  // até n-1 iterações
        Edge<V, E> minEdge = pq.poll();                             // 1 atribuição + 1 chamada função O(log m)
        V u = minEdge.getVOrig();                                   // 1 atribuição
        V v = minEdge.getVDest();                                   // 1 atribuição

        V inTree = inMST.contains(u) ? u : v;                       // 1 atribuição + 1 comparação + 1 chamada função O(1)
        V notInTree = inMST.contains(u) ? v : u;                    // 1 atribuição + 1 comparação + 1 chamada função O(1)

        if (inMST.contains(notInTree)) {                            // 1 comparação + 1 chamada função O(1)
            continue;
        }

        inMST.add(notInTree);                                       // 1 chamada função O(1) amortizado
        mst.addVertex(notInTree);                                   // 1 chamada função O(1)
        mst.addEdge(inTree, notInTree, minEdge.getWeight());        // 1 chamada função O(1)

        Collection<Edge<V, E>> newEdges = graph.outgoingEdges(notInTree);  // 1 atribuição + 1 chamada função O(k)
        if (newEdges != null) {                                     // 1 comparação
            for (Edge<V, E> edge : newEdges) {                      // k iterações
                V neighbor = edge.getVDest().equals(notInTree) ?    // 1 atribuição + 1 comparação
                        edge.getVOrig() : edge.getVDest();
                if (!inMST.contains(neighbor)) {                    // 1 comparação + 1 chamada função O(1)
                    pq.offer(edge);                                 // 1 chamada função O(log m) amortizado
                }
            }
        }
    }

    return mst;  // 1 retorno
}
```

**Análise passo a passo:**

#### Inicialização:
- Validações: **2**
- Criação de estruturas: **3**
- Adicionar vértice inicial: **2**
- Adicionar arestas iniciais à fila: k × O(log m) = **O(k log m)**
- **Total: 7 + O(k log m)**

#### Loop principal (executa no máximo n-1 vezes):
- **poll() da PriorityQueue:** O(log m) - onde m é o tamanho atual da fila
- **Processamento do edge:** O(1) - operações de comparação e adição a HashSet
- **Adição de arestas do novo vértice:** k × (O(1) + O(log m)) = **O(k log m)** - onde k é o grau do vértice

**Complexidade do loop:**
- Cada vértice é adicionado ao MST exatamente uma vez: **n-1 iterações**
- Cada aresta é examinada no máximo uma vez (quando o vértice é adicionado): **m arestas processadas**
- Poll da fila: (n-1) × O(log m) = **O(n log m)**
- Offer na fila: m × O(log m) = **O(m log m)** (cada aresta é inserida no máximo uma vez)

**Expressão completa:**
- Inicialização: **7 + O(k log m)**
- Loop: **O(n log m) + O(m log m) = O((n + m) log m)**
- **Total: O((n + m) log m)**

**Nota:** Em grafos densos (m ≈ n²), temos O((n + n²) log n²) = **O(n² log n)**.
Em grafos esparsos (m ≈ n), temos O((n + n) log n) = **O(n log n)**.

### 3.3 Análise de Casos

**Melhor caso:** grafo esparso (m ≈ n) → T(n) = O(n log n)

**Pior caso:** grafo denso (m ≈ n²) → T(n) = O(n² log n)

**Caso médio:** Em grafos reais de estações ferroviárias, o grafo é tipicamente esparso (m ≈ n). O caso médio tende para o melhor caso: T(n) ≈ O(n log n)

### 3.4 Simplificação Big-Oh

**T(n) = O((n + m) log m)** onde n = número de vértices, m = número de arestas

**S(n) = O(n + m)** - para PriorityQueue (até m arestas), HashSet inMST (n vértices) e grafo MST

**Complexidade Final: O((n + m) log m)**

**Explicação:** O algoritmo de Prim com PriorityQueue processa cada vértice uma vez e cada aresta no máximo uma vez. As operações de inserção e remoção da PriorityQueue têm complexidade O(log m), onde m é o número de arestas na fila. Em grafos esparsos, a complexidade é O(n log n), que é eficiente. Em grafos densos, torna-se O(n² log n).

---

## 4. MinimalBackboneService.computeMinimalBackbone(String stationsWithIdCsvPath, String stationToStationCsvPath, String outputDir)

### 4.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Sequencial (múltiplas etapas)
- **Recursivo:** Não
- **Algoritmo:** Orquestrador que combina carregamento de dados, conversão de grafo, Prim's MST e geração de visualizações

### 4.2 Análise de Operações Primitivas

```java
public MinimalBackboneResult computeMinimalBackbone(...) throws IOException, InterruptedException {

    // 1. Load stations with IDs
    Map<String, Station> stationMap = StationsWithIdCsvLoader.loadWithIdMap(stationsWithIdCsvPath);  // O(n)

    // 2. Load connections (edges)
    CsvValidatorResult<Edge<Station, Double>> edgesResult =
            loadEdgesWithDistance(stationToStationCsvPath, stationMap);  // O(m)

    // 3. Build directed graph from edges
    MapGraph<Station, Double> directedGraph = new MapGraph<>(true);  // O(1)
    for (Station station : stationMap.values()) {                     // n iterações
        directedGraph.addVertex(station);                             // O(1)
    }
    for (Edge<Station, Double> edge : edgesResult.getRecords()) {     // m iterações
        directedGraph.addEdge(edge.getVOrig(), edge.getVDest(), edge.getWeight());  // O(1)
    }

    // 4. Convert directed graph to undirected
    Graph<Station, Double> undirectedGraph = convertToUndirected(directedGraph);  // O(n + m)

    // 5. Check if graph is connected (optional but recommended)
    if (undirectedGraph.numVertices() == 0) {  // O(1)
        throw new IllegalStateException(...);
    }

    // 6. Compute MST using Prim's algorithm
    Station startVertex = undirectedGraph.vertices().get(0);  // O(1)
    Graph<Station, Double> mst = Algorithms.primMST(...);     // O((n + m) log m)

    // 7. Calculate total distance
    double totalDistance = 0.0;                                 // O(1)
    List<Edge<Station, Double>> mstEdges = new ArrayList<>();   // O(1)
    for (Edge<Station, Double> edge : mst.edges()) {            // n-1 iterações (MST tem n-1 arestas)
        totalDistance += edge.getWeight();                      // O(1)
        mstEdges.add(edge);                                     // O(1) amortizado
    }

    // 8. Prepare output directory
    File outputDirFile = new File(outputDir);                    // O(1)
    if (!outputDirFile.exists()) {                               // O(1)
        outputDirFile.mkdirs();                                  // O(1) (assumindo diretório simples)
    }

    // 9. Convert coordinates to XY
    List<Station> stations = new ArrayList<>(mst.vertices());    // O(n)
    CoordinateConverter converter = new CoordinateConverter(stations, 1000.0, 1000.0);  // O(n)
    Map<Station, double[]> coordinates = new HashMap<>();        // O(1)
    for (Station station : stations) {                           // n iterações
        coordinates.put(station, converter.getXY(station));      // O(1) amortizado + O(1)
    }

    // 10. Generate DOT file
    DotFileGenerator.generateDotFile(mst, coordinates, dotFilePath);  // O(n) (escreve n vértices e n-1 arestas)

    // 11. Generate SVG file (opcional, pode falhar se Graphviz não disponível)
    try {
        GraphvizExporter.generateSVG(dotFilePath, svgFilePath);  // O(1) (processo externo, não contabilizado)
    } catch (IOException e) {
        svgFilePath = null;
    }

    return new MinimalBackboneResult(...);  // O(1)
}
```

**Análise passo a passo:**

#### Passo 1: Carregar estações
- **O(n)** - `StationsWithIdCsvLoader.loadWithIdMap()`

#### Passo 2: Carregar conexões
- **O(m)** - `loadEdgesWithDistance()`

#### Passo 3: Construir grafo direcionado
- Criação: **O(1)**
- Adicionar vértices: n × O(1) = **O(n)**
- Adicionar arestas: m × O(1) = **O(m)**
- **Total: O(n + m)**

#### Passo 4: Converter para não direcionado
- **O(n + m)** - `convertToUndirected()`

#### Passo 5: Verificação
- **O(1)**

#### Passo 6: Calcular MST
- **O((n + m) log m)** - `Algorithms.primMST()`

#### Passo 7: Calcular distância total
- Loop: (n-1) × O(1) = **O(n)**

#### Passo 8: Preparar diretório
- **O(1)**

#### Passo 9: Converter coordenadas
- Criar lista: **O(n)**
- Criar converter: **O(n)**
- Loop: n × O(1) = **O(n)**
- **Total: O(n)**

#### Passo 10: Gerar arquivo DOT
- **O(n)** - escreve n vértices e n-1 arestas

#### Passo 11: Gerar SVG
- **O(1)** - processo externo (não contabilizado na complexidade do algoritmo)

**Expressão completa:**
- O(n) + O(m) + O(n + m) + O(n + m) + O(1) + O((n + m) log m) + O(n) + O(1) + O(n) + O(n) + O(1)
- Simplificando: **O(n + m) + O((n + m) log m) + O(n)**
- Termo dominante: **O((n + m) log m)** (algoritmo de Prim)

### 4.3 Análise de Casos

**Melhor caso (grafo esparso, m ≈ n):** T(n) = O(n log n) - Prim's domina com O(n log n)

**Pior caso (grafo denso, m ≈ n²):** T(n) = O(n² log n) - Prim's domina com O(n² log n)

**Caso médio:** Em grafos reais de estações ferroviárias, o grafo é tipicamente esparso (m ≈ n). O caso médio tende para o melhor caso: T(n) ≈ O(n log n)

### 4.4 Simplificação Big-Oh

**T(n) = O((n + m) log m)** onde n = número de estações, m = número de conexões

**S(n) = O(n + m)** - espaço dominante para grafos (direcionado, não direcionado, MST), mapas e estruturas auxiliares

**Complexidade Final: O((n + m) log m)**

**Explicação:** O método principal combina várias etapas, mas o cálculo do MST usando o algoritmo de Prim domina a complexidade com O((n + m) log m). Para grafos esparsos (m ≈ n), temos O(n log n), que é eficiente. Para grafos densos (m ≈ n²), temos O(n² log n).

---

## Resumo Final

| Método | T(n) | S(n) | Observações |
|--------|------|------|-------------|
| `loadEdgesWithDistance()` | O(m) | O(m) | m = número de conexões (linhas no CSV) |
| `convertToUndirected()` | O(n + m) | O(m) | Converte grafo direcionado para não direcionado |
| `Algorithms.primMST()` | O((n + m) log m) | O(n + m) | Algoritmo de Prim com PriorityQueue |
| `computeMinimalBackbone()` | O((n + m) log m) | O(n + m) | Método principal, dominado por Prim's |

- **n** = número de estações
- **m** = número de conexões

---

## Conclusão

O método principal `MinimalBackboneService.computeMinimalBackbone()` tem complexidade **T(n) = O((n + m) log m)**, onde o termo dominante O((n + m) log m) vem do algoritmo de Prim usando PriorityQueue.

Para grafos esparsos típicos de redes ferroviárias (m ≈ n), a complexidade é **O(n log n)**, que é eficiente. Para grafos densos (m ≈ n²), a complexidade torna-se **O(n² log n)**.

A complexidade espacial é **S(n) = O(n + m)** para armazenar os grafos (direcionado, não direcionado e MST), mapas e estruturas auxiliares.

O algoritmo de Prim com PriorityQueue é uma escolha adequada para calcular MSTs em grafos esparsos, sendo eficiente e amplamente utilizado em problemas de redes.

