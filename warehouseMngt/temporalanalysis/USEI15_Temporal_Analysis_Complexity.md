# USEI15 - Análise de Complexidade Temporal

## Metodologia

O documento segue a metodologia sugerida nos slides fornecidos nas aulas de ESINF, analisando individualmente os métodos implementados na USEI15.

**Notação:**
- **V** = número de vértices (estações)
- **E** = número de arestas (conexões entre estações)
- **n** = número de estações
- **m** = número de conexões

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

## 1. RiskAwarePathService.computeShortestPath()

### 1.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Sequencial (múltiplas etapas)
- **Recursivo:** Não
- **Algoritmo:** Orquestrador que carrega dados e chama Bellman-Ford

### 1.2 Análise de Operações Primitivas

```java
public ShortestPathResult computeShortestPath(
        String stationsCsvPath,
        String connectionsCsvPath,
        String sourceStationId,
        String targetStationId) throws IOException {
    
    long startTime = System.currentTimeMillis();  // 1 atribuição + 1 chamada função (O(1))
    
    // 1. Load stations
    Map<String, Station> stationMap = StationsWithIdCsvLoader.loadWithIdMap(stationsCsvPath);  // O(n)
    
    if (stationMap.isEmpty()) {                    // 1 comparação + 1 chamada função (O(1))
        throw new IllegalArgumentException(...);  // 1 lançamento exceção (O(1))
    }
    
    // 2. Validate source and target stations
    Station sourceStation = stationMap.get(sourceStationId);  // 1 atribuição + 1 chamada função (O(1))
    Station targetStation = stationMap.get(targetStationId);  // 1 atribuição + 1 chamada função (O(1))
    
    if (sourceStation == null) {                  // 1 comparação
        throw new IllegalArgumentException(...);  // 1 lançamento exceção (O(1))
    }
    if (targetStation == null) {                  // 1 comparação
        throw new IllegalArgumentException(...);  // 1 lançamento exceção (O(1))
    }
    
    // 3. Load connections (edges)
    CsvValidatorResult<Edge<Station, Double>> edgesResult =
            StationToStationCsvLoader.load(connectionsCsvPath, stationMap);  // O(m)
    
    if (edgesResult.hasErrors()) {                // 1 comparação + 1 chamada função (O(1))
        for (String error : edgesResult.getErrors()) {  // e iterações (e = número de erros)
            System.err.println("  " + error);     // 1 chamada função (O(1))
        }
    }
    
    if (edgesResult.getRecords().isEmpty()) {     // 1 comparação + 1 chamada função (O(1))
        throw new IllegalArgumentException(...);  // 1 lançamento exceção (O(1))
    }
    
    // 4. Build directed graph
    MapGraph<Station, Double> graph = new MapGraph<>(true);  // 1 atribuição + 1 criação objeto (O(1))
    
    for (Station station : stationMap.values()) {  // n iterações
        graph.addVertex(station);                  // 1 chamada função (O(1))
    }
    
    for (Edge<Station, Double> edge : edgesResult.getRecords()) {  // m iterações
        graph.addEdge(edge.getVOrig(), edge.getVDest(), edge.getWeight());  // 1 chamada função (O(1))
    }
    
    // 5. Run Bellman-Ford algorithm
    return bellmanFord(graph, sourceStation, targetStation, startTime);  // O(V * E)
}
```

**Análise passo a passo:**

#### Passo 1: Carregar estações
- **O(n)** onde n = número de estações

#### Passo 2: Validar estações origem e destino
- **O(1)** - lookups em HashMap

#### Passo 3: Carregar conexões
- **O(m)** onde m = número de conexões
- Loop de erros: **O(e)** onde e = número de erros (e ≤ m)

#### Passo 4: Construir grafo
- Criação: **1**
- Adicionar vértices: n × 1 = **n**
- Adicionar arestas: m × 1 = **m**
- **Total: n + m + 1**

#### Passo 5: Executar Bellman-Ford
- **O(V * E)** = **O(n * m)**

**Expressão completa:**
- T(n) = n + 1 + 1 + 1 + 1 + m + e + 1 + 1 + (n + m + 1) + (n * m)
- **T(n) = n * m + n + m + e + 7**

### 1.3 Análise de Casos

**Melhor caso:** grafo pequeno, sem erros → T(n) = n * m + n + m + 7

**Pior caso:** grafo grande, com erros → T(n) = n * m + n + m + e + 7

**Caso médio:** Em grafos reais, o número de erros é tipicamente pequeno (e << m), então o caso médio tende para: T(n) ≈ n * m + n + m + 7. Como n * m domina, temos T(n) ≈ n * m.

### 1.4 Simplificação Big-Oh

**T(n) = O(n * m)** onde n = número de estações, m = número de conexões

**S(n) = O(n + m)** - para armazenar grafo, mapas e resultados

**Complexidade Final: O(n * m)**

**Explicação:** O método combina várias etapas, mas o algoritmo Bellman-Ford domina com O(n * m).

---

## 2. RiskAwarePathService.bellmanFord()

### 2.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Múltiplos ciclos aninhados e sequenciais
- **Recursivo:** Não
- **Algoritmo:** Bellman-Ford para caminhos mais curtos com detecção de ciclos negativos

### 2.2 Análise de Operações Primitivas

```java
private ShortestPathResult bellmanFord(Graph<Station, Double> graph,
                                      Station source,
                                      Station target,
                                      long startTime) {
    
    int verticesCount = graph.numVertices();      // 1 atribuição + 1 chamada função (O(1))
    int edgesCount = graph.numEdges();           // 1 atribuição + 1 chamada função (O(1))
    
    // Initialize distances: all vertices start at infinity except source
    Map<Station, Double> distances = new HashMap<>();      // 1 atribuição + 1 criação objeto (O(1))
    Map<Station, Station> predecessors = new HashMap<>();   // 1 atribuição + 1 criação objeto (O(1))
    
    for (Station vertex : graph.vertices()) {     // n iterações
        distances.put(vertex, Double.POSITIVE_INFINITY);  // 1 chamada função (O(1) amortizado)
        predecessors.put(vertex, null);           // 1 chamada função (O(1) amortizado)
    }
    distances.put(source, 0.0);                   // 1 chamada função (O(1) amortizado)
    
    // Relax edges (V-1) times
    for (int i = 0; i < verticesCount - 1; i++) {  // (n-1) iterações
        boolean changed = false;                    // 1 atribuição
        for (Edge<Station, Double> edge : graph.edges()) {  // m iterações
            Station u = edge.getVOrig();           // 1 atribuição + 1 chamada função (O(1))
            Station v = edge.getVDest();           // 1 atribuição + 1 chamada função (O(1))
            Double weight = edge.getWeight();      // 1 atribuição + 1 chamada função (O(1))
            
            if (distances.get(u) != Double.POSITIVE_INFINITY) {  // 1 comparação + 1 chamada função (O(1))
                double newDist = distances.get(u) + weight;      // 1 atribuição + 1 chamada função (O(1)) + 1 soma
                if (newDist < distances.get(v)) {                // 1 comparação + 1 chamada função (O(1))
                    distances.put(v, newDist);                   // 1 chamada função (O(1) amortizado)
                    predecessors.put(v, u);                      // 1 chamada função (O(1) amortizado)
                    changed = true;                               // 1 atribuição
                }
            }
        }
        // Early termination if no changes (optimization)
        if (!changed) {                           // 1 comparação
            break;                                // 1 break (se condição verdadeira)
        }
    }
    
    // Check for negative cycles
    Set<Station> affectedVertices = new HashSet<>();  // 1 atribuição + 1 criação objeto (O(1))
    
    for (Edge<Station, Double> edge : graph.edges()) {  // m iterações
        Station u = edge.getVOrig();                   // 1 atribuição + 1 chamada função (O(1))
        Station v = edge.getVDest();                   // 1 atribuição + 1 chamada função (O(1))
        Double weight = edge.getWeight();              // 1 atribuição + 1 chamada função (O(1))
        
        if (distances.get(u) != Double.POSITIVE_INFINITY) {  // 1 comparação + 1 chamada função (O(1))
            double newDist = distances.get(u) + weight;      // 1 atribuição + 1 chamada função (O(1)) + 1 soma
            if (newDist < distances.get(v)) {                // 1 comparação + 1 chamada função (O(1))
                affectedVertices.add(v);                      // 1 chamada função (O(1) amortizado)
                distances.put(v, Double.NEGATIVE_INFINITY);   // 1 chamada função (O(1) amortizado)
            }
        }
    }
    
    // If negative cycle detected, propagate negative infinity and find cycle
    if (!affectedVertices.isEmpty()) {            // 1 comparação + 1 chamada função (O(1))
        boolean changed = true;                    // 1 atribuição
        while (changed) {                          // Máximo n iterações (propagação)
            changed = false;                       // 1 atribuição
            for (Edge<Station, Double> edge : graph.edges()) {  // m iterações
                Station u = edge.getVOrig();                   // 1 atribuição + 1 chamada função (O(1))
                Station v = edge.getVDest();                   // 1 atribuição + 1 chamada função (O(1))
                if (distances.get(u) == Double.NEGATIVE_INFINITY && 
                    distances.get(v) != Double.NEGATIVE_INFINITY) {  // 2 comparações + 2 chamadas função (O(1))
                    distances.put(v, Double.NEGATIVE_INFINITY);     // 1 chamada função (O(1) amortizado)
                    affectedVertices.add(v);                        // 1 chamada função (O(1) amortizado)
                    changed = true;                                  // 1 atribuição
                }
            }
        }
        
        // Extract a negative cycle
        List<Station> cycleStations = findNegativeCycle(...);  // O(n)
        List<Edge<Station, Double>> cycleEdges = extractCycleEdges(...);  // O(c) onde c = tamanho do ciclo
        
        long executionTime = System.currentTimeMillis() - startTime;  // 1 atribuição + 1 chamada função (O(1)) + 1 subtração
        ShortestPathResult.NegativeCycle negativeCycle = ...;  // 1 atribuição + 1 criação objeto (O(c))
        
        return new ShortestPathResult(...);  // 1 criação objeto (O(1))
    }
    
    // No negative cycle - check if path exists
    long executionTime = System.currentTimeMillis() - startTime;  // 1 atribuição + 1 chamada função (O(1)) + 1 subtração
    
    if (distances.get(target) == Double.POSITIVE_INFINITY) {  // 1 comparação + 1 chamada função (O(1))
        return new ShortestPathResult(...);  // 1 criação objeto (O(1))
    }
    
    // Build path from source to target
    List<ShortestPathResult.PathStep> path = buildPath(...);  // O(p) onde p = tamanho do caminho
    double totalCost = distances.get(target);  // 1 atribuição + 1 chamada função (O(1))
    
    return new ShortestPathResult(...);  // 1 criação objeto (O(p))
}
```

**Análise passo a passo:**

#### Inicialização
- Obter contagens: **2**
- Criar estruturas: **2**
- Inicializar distâncias e predecessores: n × 2 = **2n**
- Definir origem: **1**
- **Total inicialização: 2n + 5**

#### Relaxação de arestas (V-1 iterações)
- Loop externo: (n-1) iterações
- Loop interno: m iterações
- Por iteração do loop interno: 3 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 = **11 operações** (no pior caso)
- **Total relaxação: (n-1) × m × 11 = 11n*m - 11m**

**Nota:** Com early termination, em casos favoráveis pode terminar antes, mas no pior caso executa todas as iterações.

#### Detecção de ciclos negativos
- Loop sobre arestas: m × 11 = **11m**
- Propagação de negativo infinito: no pior caso, n iterações × m arestas × 6 operações = **6n*m**
- Extração de ciclo: **O(n)** + **O(c)** onde c = tamanho do ciclo (c ≤ n)

#### Construção do caminho (se não houver ciclo negativo)
- **O(p)** onde p = tamanho do caminho (p ≤ n)

**Expressão completa (caso sem ciclo negativo):**
- T(n) = (2n + 5) + (11n*m - 11m) + 11m + O(p) + 3
- **T(n) = 11n*m - 11m + 2n + O(p) + 8**
- Como O(p) ≤ O(n), temos **T(n) = 11n*m + O(n)**

**Expressão completa (caso com ciclo negativo):**
- T(n) = (2n + 5) + (11n*m - 11m) + 11m + (6n*m) + O(n) + O(c) + 3
- **T(n) = 17n*m - 11m + 2n + O(n) + O(c) + 8**
- Como O(c) ≤ O(n), temos **T(n) = 17n*m + O(n)**

### 2.3 Análise de Casos

**Melhor caso (sem ciclo negativo, early termination na primeira iteração):**
- T(n) = 2n + 5 + 11m + O(p) + 3 = **O(n + m + p)**

**Pior caso (com ciclo negativo, todas as iterações):**
- T(n) = 17n*m + O(n) = **O(n * m)**

**Caso médio:** Em grafos reais, ciclos negativos são raros. O algoritmo tipicamente executa várias iterações antes de convergir. O caso médio tende para: T(n) ≈ 11n*m + O(n) ≈ **O(n * m)**.

### 2.4 Simplificação Big-Oh

**T(n) = O(n * m)** onde n = número de vértices, m = número de arestas

**S(n) = O(n + m)** - para HashMap de distâncias, predecessores, e estruturas auxiliares

**Complexidade Final: O(n * m)**

**Explicação:** O algoritmo Bellman-Ford relaxa todas as arestas (n-1) vezes, resultando em O(n * m). A detecção de ciclos negativos adiciona no máximo O(n * m) adicional para propagação, mantendo a complexidade geral em O(n * m).

---

## 3. RiskAwarePathService.buildPath()

### 3.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo simples
- **Recursivo:** Não
- **Algoritmo:** Reconstrução de caminho usando predecessores

### 3.2 Análise de Operações Primitivas

```java
private List<ShortestPathResult.PathStep> buildPath(Station source,
                                                    Station target,
                                                    Map<Station, Station> predecessors,
                                                    Map<Station, Double> distances) {
    List<ShortestPathResult.PathStep> path = new ArrayList<>();  // 1 atribuição + 1 criação objeto (O(1))
    
    // Reconstruct path backwards
    List<Station> reversePath = new ArrayList<>();  // 1 atribuição + 1 criação objeto (O(1))
    Station current = target;                        // 1 atribuição
    
    while (current != null) {                        // Máximo n iterações (caminho pode ter no máximo n vértices)
        reversePath.add(current);                    // 1 chamada função (O(1) amortizado)
        current = predecessors.get(current);         // 1 atribuição + 1 chamada função (O(1))
    }
    
    // Reverse to get forward path
    Collections.reverse(reversePath);                // O(p) onde p = tamanho do caminho
    
    // Build path steps with costs
    for (Station station : reversePath) {             // p iterações
        double cost = distances.get(station);        // 1 atribuição + 1 chamada função (O(1))
        path.add(new ShortestPathResult.PathStep(station, cost));  // 1 criação objeto (O(1)) + 1 chamada função (O(1) amortizado)
    }
    
    return path;  // 1 retorno
}
```

**Operações primitivas:**

#### Construção do caminho reverso
- Inicialização: **3**
- Loop while: p iterações × 2 = **2p** onde p = tamanho do caminho (p ≤ n)
- **Total: 2p + 3**

#### Reversão
- **O(p)**

#### Construção dos passos
- Loop: p iterações × 3 = **3p**

**Expressão completa:**
- T(n) = 3 + 2p + O(p) + 3p + 1 = **6p + O(p) + 4**
- Como O(p) domina, temos **T(n) = O(p)**

### 3.3 Análise de Casos

**Melhor caso:** caminho muito curto (p = 1) → T(n) = 6 + O(1) = **O(1)**

**Pior caso:** caminho longo (p = n) → T(n) = 6n + O(n) = **O(n)**

**Caso médio:** Em grafos reais, caminhos típicos têm comprimento muito menor que n. O caso médio tende para: T(n) ≈ **O(p)** onde p << n.

### 3.4 Simplificação Big-Oh

**T(n) = O(p)** onde p = tamanho do caminho (p ≤ n)

**S(n) = O(p)** - para armazenar o caminho reverso e o caminho final

**Complexidade Final: O(p)** ou **O(n)** no pior caso

**Explicação:** O método reconstrói o caminho seguindo predecessores do destino até a origem, depois reverte e constrói os passos. O número de operações é proporcional ao tamanho do caminho.

---

## 4. RiskAwarePathService.findNegativeCycle()

### 4.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo simples com condicionais
- **Recursivo:** Não
- **Algoritmo:** Busca de ciclo seguindo predecessores

### 4.2 Análise de Operações Primitivas

```java
private List<Station> findNegativeCycle(Graph<Station, Double> graph,
                                        Map<Station, Station> predecessors,
                                        Set<Station> affectedVertices) {
    if (affectedVertices.isEmpty()) {               // 1 comparação + 1 chamada função (O(1))
        return new ArrayList<>();                   // 1 criação objeto (O(1))
    }
    
    // Start from an affected vertex and follow predecessors
    Station start = affectedVertices.iterator().next();  // 1 atribuição + 1 chamada função (O(1))
    Map<Station, Integer> visited = new HashMap<>();    // 1 atribuição + 1 criação objeto (O(1))
    List<Station> path = new ArrayList<>();              // 1 atribuição + 1 criação objeto (O(1))
    Station current = start;                             // 1 atribuição
    int position = 0;                                    // 1 atribuição
    
    // Follow predecessors until we find a cycle
    while (current != null) {                            // Máximo n iterações
        if (visited.containsKey(current)) {              // 1 comparação + 1 chamada função (O(1))
            // Found a cycle! Extract it
            int cycleStart = visited.get(current);        // 1 atribuição + 1 chamada função (O(1))
            List<Station> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));  // O(c) onde c = tamanho do ciclo
            cycle.add(current);                          // 1 chamada função (O(1) amortizado)
            return cycle;                                // 1 retorno
        }
        
        visited.put(current, position);                  // 1 chamada função (O(1) amortizado)
        path.add(current);                               // 1 chamada função (O(1) amortizado)
        current = predecessors.get(current);             // 1 atribuição + 1 chamada função (O(1))
        position++;                                      // 1 incremento
        
        // Safety limit to avoid infinite loops
        if (position > graph.numVertices()) {            // 1 comparação + 1 chamada função (O(1))
            break;                                       // 1 break (se condição verdadeira)
        }
    }
    
    // Fallback: return the affected vertices as a cycle indicator
    return new ArrayList<>(affectedVertices);            // O(a) onde a = número de vértices afetados
}
```

**Operações primitivas:**

#### Caminho com ciclo encontrado
- Inicialização: **6**
- Loop while: até encontrar ciclo (máximo n iterações)
    - Por iteração: 1 + 1 + 1 + 1 + 1 + 1 + 1 = **7 operações**
    - Quando encontra ciclo: 1 + 1 + 1 + O(c) + 1 + 1 = **O(c) + 5**
- **Total: 6 + 7k + O(c) + 5** onde k = número de vértices antes do ciclo, c = tamanho do ciclo

#### Caminho sem ciclo (fallback)
- Inicialização: **6**
- Loop while: n iterações × 7 = **7n**
- Fallback: **O(a)** onde a = número de vértices afetados (a ≤ n)
- **Total: 7n + O(a) + 6**

**Expressão completa:**
- **Caso com ciclo:** T(n) = 6 + 7k + O(c) + 5 = **O(k + c)** onde k + c ≤ n
- **Caso sem ciclo:** T(n) = 7n + O(a) + 6 = **O(n)**

### 4.3 Análise de Casos

**Melhor caso:** ciclo encontrado rapidamente (k pequeno, c pequeno) → T(n) = O(k + c) ≈ **O(1)** se k e c são constantes

**Pior caso:** sem ciclo detectável, percorre todos os vértices → T(n) = **O(n)**

**Caso médio:** Em grafos com ciclos negativos, o ciclo é tipicamente encontrado após algumas iterações. O caso médio tende para: T(n) ≈ **O(k + c)** onde k + c << n.

### 4.4 Simplificação Big-Oh

**T(n) = O(n)** no pior caso

**S(n) = O(n)** - para HashMap de visitados, lista de caminho e ciclo

**Complexidade Final: O(n)**

**Explicação:** O método segue predecessores até encontrar um ciclo ou percorrer todos os vértices. No pior caso, percorre todos os n vértices.

---

## 5. RiskAwarePathService.extractCycleEdges()

### 5.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo simples
- **Recursivo:** Não
- **Algoritmo:** Extração de arestas de um ciclo conhecido

### 5.2 Análise de Operações Primitivas

```java
private List<Edge<Station, Double>> extractCycleEdges(Graph<Station, Double> graph,
                                                      List<Station> cycleStations) {
    List<Edge<Station, Double>> cycleEdges = new ArrayList<>();  // 1 atribuição + 1 criação objeto (O(1))
    
    for (int i = 0; i < cycleStations.size() - 1; i++) {  // (c-1) iterações onde c = tamanho do ciclo
        Station from = cycleStations.get(i);               // 1 atribuição + 1 chamada função (O(1))
        Station to = cycleStations.get(i + 1);             // 1 atribuição + 1 chamada função (O(1))
        Edge<Station, Double> edge = graph.edge(from, to); // 1 atribuição + 1 chamada função (O(1))
        if (edge != null) {                                // 1 comparação
            cycleEdges.add(edge);                          // 1 chamada função (O(1) amortizado)
        }
    }
    
    return cycleEdges;  // 1 retorno
}
```

**Operações primitivas por iteração:**
- Por iteração: 1 + 1 + 1 + 1 + 1 + 1 + 1 = **7 operações** (no pior caso)
- Loop executa (c-1) vezes onde c = tamanho do ciclo (c ≤ n)

**Expressão completa:**
- Inicialização: **2**
- Loop: (c-1) × 7 = **7(c-1)**
- Retorno: **1**
- **Total: 7c - 6**

### 5.3 Análise de Casos

**Melhor caso:** ciclo pequeno (c = 2) → T(n) = 7 × 2 - 6 = **8**

**Pior caso:** ciclo grande (c = n) → T(n) = 7n - 6

**Caso médio:** Em grafos reais, ciclos negativos são tipicamente pequenos. O caso médio tende para: T(n) ≈ **O(c)** onde c << n.

### 5.4 Simplificação Big-Oh

**T(n) = O(c)** onde c = tamanho do ciclo (c ≤ n)

**S(n) = O(c)** - para armazenar as arestas do ciclo

**Complexidade Final: O(c)** ou **O(n)** no pior caso

**Explicação:** O método percorre o ciclo uma vez, extraindo as arestas entre vértices consecutivos. O número de operações é proporcional ao tamanho do ciclo.

---

## Resumo Final

| Método | T(n) | S(n) | Observações |
|--------|------|------|-------------|
| `RiskAwarePathService.computeShortestPath()` | O(n * m) | O(n + m) | n = estações, m = conexões |
| `RiskAwarePathService.bellmanFord()` | O(n * m) | O(n + m) | Algoritmo principal |
| `RiskAwarePathService.buildPath()` | O(p) | O(p) | p = tamanho do caminho (p ≤ n) |
| `RiskAwarePathService.findNegativeCycle()` | O(n) | O(n) | Busca de ciclo |
| `RiskAwarePathService.extractCycleEdges()` | O(c) | O(c) | c = tamanho do ciclo (c ≤ n) |

- **n** = número de estações (vértices)
- **m** = número de conexões (arestas)
- **p** = tamanho do caminho encontrado
- **c** = tamanho do ciclo negativo (se detectado)

---

## Conclusão

O método principal `RiskAwarePathService.computeShortestPath()` tem complexidade temporal **T(n) = O(n * m)**, onde o termo dominante O(n * m) vem do algoritmo Bellman-Ford que relaxa todas as arestas (n-1) vezes.

A complexidade espacial é **S(n) = O(n + m)** para armazenar o grafo, mapas de distâncias e predecessores, e estruturas auxiliares.

**Comparação com Dijkstra:**
- **Dijkstra:** O((n + m) log n) com heap binário, mas **não funciona com custos negativos**
- **Bellman-Ford:** O(n * m), mas **funciona com custos negativos e detecta ciclos negativos**

O trade-off é justificado pela necessidade de lidar com custos negativos (penalidades/bônus) e detectar inconsistências (ciclos negativos) na configuração da rede ferroviária.

---

## Análise de Complexidade Temporal Detalhada

### Fase 1: Carregamento e Construção do Grafo
- Carregar estações: **O(n)**
- Carregar conexões: **O(m)**
- Construir grafo: **O(n + m)**
- **Total Fase 1: O(n + m)**

### Fase 2: Algoritmo Bellman-Ford
- Inicialização: **O(n)**
- Relaxação de arestas (V-1 iterações): **O(n * m)**
- Detecção de ciclos negativos: **O(m)**
- Propagação de negativo infinito (se necessário): **O(n * m)** no pior caso
- **Total Fase 2: O(n * m)**

### Fase 3: Construção do Resultado
- Construir caminho (se não houver ciclo): **O(p)** onde p ≤ n
- Extrair ciclo negativo (se detectado): **O(n)** para encontrar + **O(c)** para extrair arestas
- **Total Fase 3: O(n)**

### Complexidade Total
**T(n) = O(n + m) + O(n * m) + O(n) = O(n * m)**

Como n * m domina n + m para grafos não-triviais (m ≥ n), a complexidade final é **O(n * m)**.

---

## Observações sobre Otimizações

1. **Early Termination:** O algoritmo implementa early termination na fase de relaxação, terminando quando nenhuma distância é atualizada. Isso pode melhorar significativamente o desempenho em casos favoráveis, mas não altera a complexidade assintótica no pior caso.

2. **Propagação de Negativo Infinito:** A propagação de negativo infinito para vértices alcançáveis a partir de ciclos negativos pode adicionar até O(n * m) operações adicionais, mas isso só ocorre quando um ciclo negativo é detectado (caso raro em configurações válidas).

3. **Extração de Ciclo:** A extração do ciclo negativo é O(n) no pior caso, mas tipicamente muito mais rápida quando o ciclo é encontrado rapidamente seguindo predecessores.

