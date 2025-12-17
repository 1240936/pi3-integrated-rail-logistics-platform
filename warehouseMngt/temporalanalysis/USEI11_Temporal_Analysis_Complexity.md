# USEI11 - Análise de Complexidade Temporal

## Metodologia

O documento segue a metodologia sugerida nos slides fornecidos nas aulas de ESINF, analisando individualmento os métodos implementados na USEI11,

**Notação:**
- **V** = número de vértices (estações)
- **E** = número de arestas (conexões entre estações)
- **k** = número de vizinhos de um vértice específico
- **n** = número de estações
- **m** = número de conexões

**Sobre T(n) e S(n):**
- **T(n)** = Complexidade Temporal (tempo de execução) - mede a quantidade de operações realizada pelo algoritmo
- **S(n)** = Complexidade Espacial (memória) - mede o espaço na memória utilizado pelo algoritmo

**Sobre Caso Médio:**
O caso médio representa o comportamento esperado do algoritmo quando executado com entradas aleatórias. Para calcular:
1. Identificar as possíveis e as suas probabilidades
2. Calcular a complexidade para cada entrada
3. Calcular a média ponderada: Σ(probabilidade × complexidade)

Quando o caso médio é difícil de modelar, assume-se que tende para o pior caso. (Informação dos slides de ESINF)

---

## 1. MapGraph.adjVertices(V vert)

### 1.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Sequencial (sem ciclos)
- **Recursivo:** Nao

### 1.2 Análise de Operações Primitivas

```java
public Collection<V> adjVertices(V vert) {
    if (!validVertex(vert))        // 1 comparação + 1 chamada função
        return null;                // 1 retorno (se condição verdadeira)
    MapVertex<V, E> mv = mapVertices.get(vert);  // 1 atribuição + 1 chamada função
    return mv.getAllAdjVerts();     // 1 retorno + 1 chamada função
}
```

**Operações primitivas:**
- `validVertex(vert)`: O(1) - lookup em HashMap
- `mapVertices.get(vert)`: O(1) - lookup em HashMap
- `getAllAdjVerts()`: O(k) - cria ArrayList com k elementos

**Expressão completa:**
- Caminho sem retorno: 1 + 1 + 1 + k = **k + 3**
- Caminho com retorno: 1 + 1 = **2**

### 1.3 Análise de Casos

**Melhor caso:** vértice inválido → T(n) = 2

**Pior caso:** vértice válido → T(n) = k + 3, onde k pode ser até n-1

**Caso médio:** T(n) ≈ k_avg + 3, onde k_avg = m/n (grau médio). Como k_avg é tipicamente pequeno em grafos esparsos, tende para O(k).

### 1.4 Simplificação Big-Oh

**T(n) = O(k)** onde k = número de vizinhos do vértice

**S(n) = O(k)** - espaço para criar a lista de retorno

**Complexidade Final: O(k)**

**Explicação:** O método retorna diretamente os vizinhos de um vértice. A operação dominante é criar a lista com k elementos. No pior caso, um vértice pode ter até n-1 vizinhos, mas em grafos reais o número médio de vizinhos é tipicamente muito menor.

---

## 2. MapGraph.incomingEdges(V vert)

### 2.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo simples
- **Recursivo:** Não

### 2.2 Análise de Operações Primitivas

```java
public Collection<Edge<V, E>> incomingEdges(V vert) {
    if (!validVertex(vert))         // 1 comparação + 1 chamada função
        return null;                // 1 retorno (se condição verdadeira)
    
    ArrayList<Edge<V, E>> incoming = new ArrayList<>();  // 1 atribuição + 1 criação objeto
    
    for (MapVertex<V, E> mv : mapVertices.values()) {   // n iterações
        Edge<V, E> edge = mv.getEdge(vert);              // 1 atribuição + 1 chamada função (O(1))
        if (edge != null) {                              // 1 comparação
            incoming.add(edge);                          // 1 chamada função (O(1) amortizado)
        }
    }
    
    return incoming;  // 1 retorno
}
```

**Operações primitas no loop:**
- Por iteração: 1 atribuição + 1 chamada O(1) + 1 comparação + 1 chamada O(1) = **4 operações**
- Loop executa n vezes (onde n = número de vértices)

**Expressão completa:**
- Caminho sem retorno: 1 + 1 + 1 + (n × 4) + 1 = **4n + 4**
- Caminho com retorno: 1 + 1 = **2**

### 2.3 Análise de Casos

**Melhor caso:** vértice inválido → T(n) = 2

**Pior caso:** vértice válido → T(n) = 4n + 4

**Caso médio:** T(n) = 4n + 4 (igual ao pior caso, dado que percorre sempre todos os vértices)

### 2.4 Simplificação Big-Oh

**T(n) = O(n)**

**S(n) = O(inDegree(vert))** - espaço para armazenar arestas que chegam

**Complexidade Final: O(n)**

**Explicação:** O método percorre todos os n vértices do grafo para encontrar arestas que chegam ao vértice dado. 

---

## 3. MatrixGraph.edges()

### 3.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclos aninhados
- **Recursivo:** Não

### 3.2 Análise de Operações Primitivas

```java
public Collection<Edge<V, E>> edges() {
    ArrayList<Edge<V, E>> le = new ArrayList<>();  // 1 atribuição + 1 criação objeto
    
    for (int i = 0; i < numVerts; i++) {          // n iterações
        for (int j = 0; j < numVerts; j++) {      // n iterações (por cada i)
            if (edgeMatrix[i][j] != null) {       // 1 comparação + 1 acesso array
                le.add(edgeMatrix[i][j]);         // 1 chamada função (O(1) amortizado) + 1 acesso array
            }
        }
    }
    
    return le;  // 1 retorno
}
```

**Operações primitivas no loop interno:**
- Por iteração: 1 comparação + 1 acesso array + 1 chamada O(1) + 1 acesso array = **4 operações** (no pior caso)
- Loop interno executa n vezes
- Loop externo executa n vezes

**Expressão completa:**
- Inicialização: 1 + 1 = **2**
- Loops aninhados: n × (n × 4) = **4n²**
- Retorno: 1
- **Total: 4n² + 3**

### 3.3 Análise de Casos

**Melhor caso:** matriz vazia (sem arestas) → T(n) = 4n² + 3 (ainda percorre toda a matriz)

**Pior caso:** matriz completa (todas as posições têm arestas) → T(n) = 4n² + 3

**Caso médio:** T(n) = 4n² + 3 (igual ao pior caso, pois sempre percorre toda a matriz)

**Nota:** Este algoritmo sempre percorre toda a matriz, independentemente do número de arestas. Esta é uma limitação inerente da representação por matriz de adjacência.

### 3.4 Simplificação Big-Oh

**T(n) = O(n²)**

**S(n) = O(m)** - espaço para armazenar todas as arestas (onde m = número de arestas)

**Complexidade Final: O(n²)**

**Explicação:** Dois ciclos aninhados, cada um executado n vezes, resultam em n² iterações.

---

## 4. MatrixGraph.outgoingEdges(V vert)

### 4.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo simples
- **Recursivo:** Não

### 4.2 Análise de Operações Primitivas

```java
public Collection<Edge<V, E>> outgoingEdges(V vert) {
    int vertKey = key(vert);        // 1 atribuição + 1 chamada função (O(n))
    if (vertKey == -1)              // 1 comparação
        return null;                // 1 retorno (se condição verdadeira)
    
    ArrayList<Edge<V, E>> outgoing = new ArrayList<>();  // 1 atribuição + 1 criação objeto
    
    for (int i = 0; i < numVerts; i++) {                  // n iterações
        if (edgeMatrix[vertKey][i] != null) {             // 1 comparação + 1 acesso array
            outgoing.add(edgeMatrix[vertKey][i]);         // 1 chamada função (O(1) amortizado) + 1 acesso array
        }
    }
    
    return outgoing;  // 1 retorno
}
```

**Operações primitivas:**
- `key(vert)`: O(n) - busca linear na lista de vértices
- Loop: n iterações, cada uma com 4 operações (no pior caso)

**Expressão completa:**
- Caminho sem retorno: n + 1 + 1 + 1 + (n × 4) + 1 = **5n + 4**
- Caminho com retorno: n + 1 + 1 = **n + 2**

### 4.3 Análise de Casos

**Melhor caso:** vértice inválido → T(n) = n + 2

**Pior caso:** vértice válido → T(n) = 5n + 4

**Caso médio:** T(n) = 5n + 4 (tende para o pior caso)

### 4.4 Simplificação Big-Oh

**T(n) = O(n)**

**S(n) = O(outDegree(vert))** - espaço para armazenar arestas que saem

**Complexidade Final: O(n)**

**Explicação:** O método busca o índice do vértice na lista (O(n)) e depois percorre uma linha completa da matriz (O(n)). 

---

## 5. Algorithms.topologicalSort(Graph<V, E> g)

### 5.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Múltiplos ciclos simples sequenciais
- **Recursivo:** Não
- **Algoritmo:** Kahn's Algorithm

### 5.2 Análise de Operações Primitivas

```java
public static <V, E> LinkedList<V> topologicalSort(Graph<V, E> g) {
    // 1. Calcular in-degree de cada vértice
    Map<V, Integer> inDegree = new HashMap<>();  // 1 atribuição + 1 criação objeto
    
    for (V vert : g.vertices()) {                 // n iterações
        inDegree.put(vert, 0);                    // 1 chamada função (O(1) amortizado)
    }
    
    for (V vert : g.vertices()) {                 // n iterações
        Collection<Edge<V, E>> incoming = g.incomingEdges(vert);  // 1 atribuição + 1 chamada função (O(n))
        if (incoming != null) {                    // 1 comparação
            inDegree.put(vert, incoming.size());  // 1 chamada função (O(1) amortizado) + 1 chamada função (O(1))
        }
    }
    
    // 2. Fila com vértices de in-degree = 0
    Queue<V> queue = new LinkedList<>();          // 1 atribuição + 1 criação objeto
    
    for (V vert : g.vertices()) {                 // n iterações
        if (inDegree.get(vert) == 0) {            // 1 comparação + 1 chamada função (O(1))
            queue.offer(vert);                    // 1 chamada função (O(1))
        }
    }
    
    // 3. Processar vértices
    LinkedList<V> topologicalOrder = new LinkedList<>();  // 1 atribuição + 1 criação objeto
    int processedCount = 0;                        // 1 atribuição
    
    while (!queue.isEmpty()) {                    // Máximo n iterações
        V current = queue.poll();                 // 1 atribuição + 1 chamada função (O(1))
        topologicalOrder.add(current);            // 1 chamada função (O(1))
        processedCount++;                          // 1 incremento
        
        Collection<V> neighbors = g.adjVertices(current);  // 1 atribuição + 1 chamada função (O(k))
        if (neighbors != null) {                   // 1 comparação
            for (V neighbor : neighbors) {         // k iterações (k = número de vizinhos)
                int newDegree = inDegree.get(neighbor) - 1;  // 1 atribuição + 1 chamada função (O(1)) + 1 subtração
                inDegree.put(neighbor, newDegree);  // 1 chamada função (O(1) amortizado)
                if (newDegree == 0) {             // 1 comparação
                    queue.offer(neighbor);        // 1 chamada função (O(1))
                }
            }
        }
    }
    
    // 4. Verificar se há ciclos
    if (processedCount != g.numVertices()) {      // 1 comparação + 1 chamada função (O(1))
        return null;                              // 1 retorno (se condição verdadeira)
    }
    
    return topologicalOrder;  // 1 retorno
}
```

**Análise passo a passo:**

#### Passo 1: Inicializar in-degree
- Loop 1: n iterações × 1 operação = **n**
- Loop 2: n iterações × (1 + n + 1 + 1 + 1) = **n × (n + 4)** = **n² + 4n**
- **Total passo 1: n² + 5n**

#### Passo 2: Inicializar fila
- Loop: n iterações × (1 + 1 + 1) = **3n**

#### Passo 3: Processar vértices
- Inicialização: **2**
- Loop while: máximo n iterações
  - Por iteração: 1 + 1 + 1 + 1 + k × (1 + 1 + 1 + 1 + 1) = **4 + 5k**
  - Total do loop: n × (4 + 5k_avg) onde k_avg = m/n (grau médio)
  - Como cada aresta é processada exatamente uma vez: **4n + 5m**

#### Passo 4: Verificar ciclos
- **1**

**Expressão completa:**
- T(n) = (n² + 5n) + 3n + 2 + (4n + 5m) + 1 = **n² + 12n + 5m + 3**

### 5.3 Análise de Casos

**Melhor caso:** grafo vazio (m = 0) → T(n) = n² + 12n + 3

**Pior caso:** grafo completo (m = n²) → T(n) = n² + 12n + 5n² + 3 = **6n² + 12n + 3**

**Caso médio:** Em grafos esparsos típicos (m ≈ n), temos T(n) = n² + 12n + 5n + 3 = **n² + 17n + 3**. Como o termo n² domina, o caso médio tende para o pior caso.

### 5.4 Simplificação Big-Oh

**T(n) = O(n² + m)**

**S(n) = O(n)** - para HashMap de in-degree, fila e lista de resultado

**Complexidade Final: O(n² + m)**

**Explicação:** O algoritmo de Kahn é eficiente, mas o cálculo inicial de in-degree usando `incomingEdges()` (que é O(n) para cada vértice) domina a complexidade. 

---

## 6. Algorithms.findCycleVertices(Graph<V, E> g)

### 6.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo simples + recursão
- **Recursivo:** Sim (DFS recursivo)
- **Algoritmo:** DFS com marcação de cores (WHITE/GRAY/BLACK)

### 6.2 Análise de Operações Primitivas

```java
public static <V, E> Set<V> findCycleVertices(Graph<V, E> g) {
    Map<V, Integer> color = new HashMap<>();     // 1 atribuição + 1 criação objeto
    Set<V> cycleVertices = new HashSet<>();      // 1 atribuição + 1 criação objeto
    
    for (V vert : g.vertices()) {                // n iterações
        color.put(vert, 0);                      // 1 chamada função (O(1) amortizado)
    }
    
    for (V vert : g.vertices()) {                // n iterações
        if (color.get(vert) == 0) {              // 1 comparação + 1 chamada função (O(1))
            dfsCycleDetection(g, vert, color, cycleVertices);  // 1 chamada função recursiva
        }
    }
    
    return cycleVertices;  // 1 retorno
}
```

**Análise da recursão `dfsCycleDetection()`:**

```java
private static <V, E> void dfsCycleDetection(Graph<V, E> g, V vert, 
                                             Map<V, Integer> color, Set<V> cycleVertices) {
    color.put(vert, 1);                          // 1 chamada função (O(1) amortizado)
    
    Collection<V> neighbors = g.adjVertices(vert);  // 1 atribuição + 1 chamada função (O(k))
    if (neighbors != null) {                     // 1 comparação
        for (V neighbor : neighbors) {            // k iterações
            Integer neighborColor = color.get(neighbor);  // 1 atribuição + 1 chamada função (O(1))
            
            if (neighborColor != null && neighborColor == 1) {  // 2 comparações
                cycleVertices.add(vert);          // 1 chamada função (O(1) amortizado)
                cycleVertices.add(neighbor);      // 1 chamada função (O(1) amortizado)
            } else if (neighborColor != null && neighborColor == 0) {  // 2 comparações
                dfsCycleDetection(g, neighbor, color, cycleVertices);  // 1 chamada recursiva
                if (cycleVertices.contains(neighbor)) {  // 1 comparação + 1 chamada função (O(1))
                    cycleVertices.add(vert);      // 1 chamada função (O(1) amortizado)
                }
            }
        }
    }
    
    color.put(vert, 2);                          // 1 chamada função (O(1) amortizado)
}
```

**Análise da recursão:**
- Cada vértice é visitado no máximo uma vez: **n chamadas**
- Cada aresta é percorrida no máximo uma vez: **m arestas processadas**
- Por chamada: 1 + k × (1 + 1 + 2 + 1 + 1 + 2 + 1 + 1 + 1) = **1 + 11k**
- Total: n + 11m (cada aresta contribui com operações)

**Expressão completa:**
- Inicialização: **2**
- Loop de inicialização: n × 1 = **n**
- Loop principal: n iterações × (1 + 1 + complexidade recursiva)
- Recursão: n + 11m
- Retorno: **1**
- **Total: 3n + 11m + 1**

### 6.3 Análise de Casos

**Melhor caso:** grafo sem ciclos → T(n) = 3n + 11m + 1

**Pior caso:** grafo com ciclos → T(n) = 3n + 11m + 1

**Caso médio:** T(n) = 3n + 11m + 1 (igual em ambos os casos, pois o algoritmo sempre percorre todos os vértices e arestas)

**Nota:** A complexidade é a mesma em ambos os casos, dado que o algoritmo percorre todos os vértices e arestas para garantir detecção completa de ciclos.

### 6.4 Simplificação Big-Oh

**T(n) = O(n + m)**

**S(n) = O(n)** - para HashMap de cores, Set de vértices em ciclos e pilha de recursão

**Complexidade Final: O(n + m)**

**Explicação:** 
O DFS visita cada vértice uma vez e percorre cada aresta uma vez.
---

## 7. StationsWithIdCsvLoader.loadWithIdMap(String csvPath)

### 7.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo simples (processamento de linhas CSV)
- **Recursivo:** Não

### 7.2 Análise de Operações Primitivas

```java
public static Map<String, Station> loadWithIdMap(String csvPath) throws IOException {
    Map<String, Station> stationMap = new HashMap<>();  // 1 atribuição + 1 criação objeto
    
    CsvReader.readCsv(csvPath, (lineNo, fields) -> {   // n iterações (n = linhas no CSV)
        if (fields.length < 6) {                        // 1 comparação
            System.err.println(...);                     // 1 chamada função (O(1))
            return;                                      // 1 retorno (se condição verdadeira)
        }
        
        String idStr = fields[0].trim();                // 1 atribuição + 1 chamada função (O(1))
        String name = fields[1].trim();                 // 1 atribuição + 1 chamada função (O(1))
        double latitude = Double.parseDouble(fields[2].trim());  // 1 atribuição + 2 chamadas função (O(1))
        double longitude = Double.parseDouble(fields[3].trim());  // 1 atribuição + 2 chamadas função (O(1))
        
        // Validações: 3 comparações
        if (name.isEmpty()) {                           // 1 comparação
            return;                                     // 1 retorno (se condição verdadeira)
        }
        if (latitude < -90.0 || latitude > 90.0) {      // 2 comparações
            return;                                     // 1 retorno (se condição verdadeira)
        }
        if (longitude < -180.0 || longitude > 180.0) {  // 2 comparações
            return;                                     // 1 retorno (se condição verdadeira)
        }
        
        Station station = new Station(...);            // 1 atribuição + 1 criação objeto (O(1))
        stationMap.put(idStr, station);                // 1 chamada função (O(1) amortizado)
    });
    
    return stationMap;  // 1 retorno
}
```

**Operações primitivas por linha:**
- Caminho normal: 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 3 + 1 + 1 = **13 operações**
- Caminho com retorno: 1 + 1 = **2 operações**

**Expressão completa:**
- Inicialização: **1 + 1 = 2**
- Loop: n × 13 = **13n** (assumindo todas as linhas válidas)
- Retorno: **1**
- **Total: 13n + 3**

### 7.3 Análise de Casos

**Melhor caso:** todas as linhas inválidas → T(n) = 2n + 3

**Pior caso:** todas as linhas válidas → T(n) = 13n + 3

**Caso médio:** Assumindo que a maioria das linhas são válidas (probabilidade alta), o caso médio tende para o pior caso: T(n) ≈ 13n + 3

### 7.4 Simplificação Big-Oh

**T(n) = O(n)**

**S(n) = O(n)** - para armazenar o mapa de estações

**Complexidade Final: O(n)**

**Explicação:** O método processa cada linha do CSV uma vez, realizando operações constantes por linha. O número de operações é linear no número de linhas. Em CSVs típicos, a maioria das linhas são válidas, então o comportamento prático é próximo do pior caso.

---

## 8. StationToStationCsvLoader.load(String csvPath, Map<String, Station> stationMap)

### 8.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo simples (processamento de linhas CSV)
- **Recursivo:** Não

### 8.2 Análise de Operações Primitivas

```java
public static CsvValidatorResult<Edge<Station, Double>> load(
        String csvPath, 
        Map<String, Station> stationMap) throws IOException {
    
    CsvValidatorResult<Edge<Station, Double>> result = new CsvValidatorResult<>();  // 1 atribuição + 1 criação objeto
    
    CsvReader.readCsv(csvPath, (lineNo, fields) -> {   // m iterações (m = linhas no CSV)
        if (fields.length < 5) {                        // 1 comparação
            result.addError(...);                       // 1 chamada função (O(1))
            return;                                     // 1 retorno (se condição verdadeira)
        }
        
        String fromKey = fields[0].trim();             // 1 atribuição + 1 chamada função (O(1))
        String toKey = fields[1].trim();               // 1 atribuição + 1 chamada função (O(1))
        double dist = Double.parseDouble(fields[2].trim());      // 1 atribuição + 2 chamadas função (O(1))
        int capacity = Integer.parseInt(fields[3].trim());       // 1 atribuição + 2 chamadas função (O(1))
        double cost = Double.parseDouble(fields[4].trim());       // 1 atribuição + 2 chamadas função (O(1))
        
        // Validações: 2 comparações
        if (dist < 0) {                                 // 1 comparação
            result.addError(...);                       // 1 chamada função (O(1))
            return;                                     // 1 retorno (se condição verdadeira)
        }
        if (capacity < 0) {                             // 1 comparação
            result.addError(...);                       // 1 chamada função (O(1))
            return;                                     // 1 retorno (se condição verdadeira)
        }
        
        Station fromStation = stationMap.get(fromKey);  // 1 atribuição + 1 chamada função (O(1) amortizado)
        Station toStation = stationMap.get(toKey);      // 1 atribuição + 1 chamada função (O(1) amortizado)
        
        // Validações: 2 comparações
        if (fromStation == null) {                      // 1 comparação
            result.addError(...);                       // 1 chamada função (O(1))
            return;                                     // 1 retorno (se condição verdadeira)
        }
        if (toStation == null) {                        // 1 comparação
            result.addError(...);                       // 1 chamada função (O(1))
            return;                                     // 1 retorno (se condição verdadeira)
        }
        
        Edge<Station, Double> edge = new Edge<>(...);   // 1 atribuição + 1 criação objeto (O(1))
        result.addRecord(edge);                         // 1 chamada função (O(1) amortizado)
    });
    
    return result;  // 1 retorno
}
```

**Operações primitivas por linha:**
- Caminho normal: 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 = **18 operações**
- Caminho com retorno: 1 + 1 = **2 operações**

**Expressão completa:**
- Inicialização: **1 + 1 = 2**
- Loop: m × 18 = **18m** (assumindo todas as linhas válidas)
- Retorno: **1**
- **Total: 18m + 3**

### 8.3 Análise de Casos

**Melhor caso:** todas as linhas inválidas → T(n) = 2m + 3

**Pior caso:** todas as linhas válidas → T(n) = 18m + 3

**Caso médio:** Em CSVs reais, a maioria das linhas são válidas, então o caso médio tende para o pior caso: T(n) ≈ 18m + 3

### 8.4 Simplificação Big-Oh

**T(n) = O(m)**

**S(n) = O(m)** - para armazenar as arestas no resultado

**Complexidade Final: O(m)**

**Explicação:** Similar ao loader de estações, este método processa cada linha do CSV uma vez. O número de operações é linear no número de conexões.

---

## 9. RailwayUpgradeService.computeUpgradeOrder(String stationsCsvPath, String linesCsvPath)

### 9.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Sequencial (múltiplas etapas)
- **Recursivo:** Não

### 9.2 Análise de Operações Primitivas

```java
public TopologicalSortResult computeUpgradeOrder(String stationsCsvPath, String linesCsvPath) 
        throws IOException {
    
    // 1. Carregar estações do CSV com IDs
    Map<String, Station> stationMap = StationsWithIdCsvLoader.loadWithIdMap(stationsCsvPath);  // O(n)
    
    if (stationMap.isEmpty()) {                      // 1 comparação + 1 chamada função (O(1))
        throw new IllegalStateException(...);        // 1 lançamento exceção (O(1))
    }
    
    // 2. Carregar conexões do CSV
    CsvValidatorResult<Edge<Station, Double>> edgesResult = 
        StationToStationCsvLoader.load(linesCsvPath, stationMap);  // O(m)
    
    if (edgesResult.hasErrors()) {                   // 1 comparação + 1 chamada função (O(1))
        for (String error : edgesResult.getErrors()) {  // e iterações (e = número de erros)
            System.err.println("  " + error);        // 1 chamada função (O(1))
        }
    }
    
    if (edgesResult.getRecords().isEmpty()) {        // 1 comparação + 1 chamada função (O(1))
        throw new IllegalStateException(...);        // 1 lançamento exceção (O(1))
    }
    
    // 3. Construir grafo direcionado
    MapGraph<Station, Double> graph = new MapGraph<>(true);  // 1 atribuição + 1 criação objeto (O(1))
    
    for (Station station : stationMap.values()) {     // n iterações
        graph.addVertex(station);                     // 1 chamada função (O(1))
    }
    
    for (Edge<Station, Double> edge : edgesResult.getRecords()) {  // m iterações
        graph.addEdge(edge.getVOrig(), edge.getVDest(), edge.getWeight());  // 1 chamada função (O(1))
    }
    
    // 4. Executar ordenação topológica
    LinkedList<Station> topologicalOrder = 
        Algorithms.topologicalSort(graph);              // O(n² + m)
    
    // 5. Verificar se há ciclos
    if (topologicalOrder == null) {                   // 1 comparação
        Set<Station> cycleStations = Algorithms.findCycleVertices(graph);  // O(n + m)
        
        return new TopologicalSortResult(...);        // 1 criação objeto (O(1))
    } else {
        return new TopologicalSortResult(...);       // 1 criação objeto (O(1))
    }
}
```

**Análise passo a passo:**

#### Passo 1: Carregar estações
- **O(n)**

#### Passo 2: Carregar conexões
- **O(m)**
- Loop de erros: **O(e)** onde e = número de erros (e ≤ m)

#### Passo 3: Construir grafo
- Criação: **1**
- Adicionar vértices: n × 1 = **n**
- Adicionar arestas: m × 1 = **m**
- **Total: n + m + 1**

#### Passo 4: Ordenação topológica
- **O(n² + m)**

#### Passo 5: Detecção de ciclos (se necessário)
- **O(n + m)**

**Expressão completa:**
- **Caso sem ciclos:** n + m + e + (n + m + 1) + (n² + m) + 1 = **n² + 2n + 3m + e + 2**
- **Caso com ciclos:** n + m + e + (n + m + 1) + (n² + m) + (n + m) + 1 = **n² + 3n + 4m + e + 2**

### 9.3 Análise de Casos

**Melhor caso (sem ciclos):** T(n) = n² + 2n + 3m + e + 2

**Pior caso (com ciclos):** T(n) = n² + 3n + 4m + e + 2

**Caso médio:** Em grafos reais, a probabilidade de ciclos é baixa, então o caso médio tende para o melhor caso: T(n) ≈ n² + 2n + 3m + e + 2. Como e ≤ m e os termos lineares são dominados por n², temos T(n) ≈ n² + 3m.

### 9.4 Simplificação Big-Oh

**T(n) = O(n² + m)** onde n = número de estações, m = número de conexões

**S(n) = O(n + m)** - para armazenar grafo, mapas e resultados

**Complexidade Final: O(n² + m)**

**Explicação:** O método combina várias etapas, mas a ordenação topológica domina com O(n² + m).

---

## 10. TopologicalSortResult.toString()

### 10.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo simples (condicional)
- **Recursivo:** Não

### 10.2 Análise de Operações Primitivas

```java
public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("=== USEI11 - Directed Line Upgrade Plan ===\n\n");

    if (hasCycle) {
        sb.append("Graph contains cycles. Not possible to find an order.\n\n");

        if (cycleStations != null && !cycleStations.isEmpty()) {
            sb.append("Stations in cycles:\n");
            for (Station station : cycleStations) {
                sb.append("- ").append(station.getName()).append("\n");
            }
        }
    } else {
        sb.append("Grafo é DAG (sem ciclos)\n\n");

        if (topologicalOrder != null && !topologicalOrder.isEmpty()) {
            sb.append("Topological Upgrade Order:\n");
            int index = 1;
            for (Station station : topologicalOrder) {
                sb.append(index).append(". ").append(station.getName()).append("\n");
                index++;
            }
            sb.append("\nTotal stations: ").append(topologicalOrder.size()).append("\n");
        }
    }

    return sb.toString();
}
```

**Análise por caminho:**

#### Caminho com ciclos:
- Inicialização: **2**
- Condicionais e append fixos: **4**
- Loop: c × 2 = **2c**
- toString final: **c** (tamanho da string)
- **Total: 2c + 6**

#### Caminho sem ciclos:
- Inicialização: **2**
- Condicionais e append fixos: **4**
- Loop: n × 4 = **4n**
- toString final: **n** (tamanho da string)
- **Total: 5n + 6**

### 10.3 Análise de Casos

**Melhor caso (com ciclos, c = 0):** T(n) = 6

**Pior caso (sem ciclos, n máximo):** T(n) = 5n + 6

**Caso médio:** Em uso típico, o método é chamado após ordenação topológica, então o caso mais comum é sem ciclos. O caso médio tende para o pior caso: T(n) ≈ 5n + 6.

### 10.4 Simplificação Big-Oh

**T(n) = O(n)**

**S(n) = O(n)** - para construir a string de resultado

**Complexidade Final: O(n)**

**Explicação:** O método percorre todas as estações uma vez para construir a representação em string. O tamanho da string resultante é proporcional ao número de estações, então tanto o tempo quanto o espaço são lineares.

---

## Resumo Final

| Método | T(n) | S(n) | Observações |
|--------|------|------|--|
| `MapGraph.adjVertices()` | O(k) | O(k) | k = número de vizinhos |
| `MapGraph.incomingEdges()` | O(n) | O(inDegree) |  |
| `MatrixGraph.edges()` | O(n²) | O(m) | Limitação da matriz |
| `MatrixGraph.outgoingEdges()` | O(n) | O(outDegree) | Dominado por `key()` |
| `Algorithms.topologicalSort()` | O(n² + m) | O(n) | Dominado por cálculo in-degree |
| `Algorithms.findCycleVertices()` | O(n + m) | O(n) | DFS eficiente |
| `StationsWithIdCsvLoader.loadWithIdMap()` | O(n) | O(n) | n = linhas no CSV |
| `StationToStationCsvLoader.load()` | O(m) | O(m) | m = linhas no CSV |
| `RailwayUpgradeService.computeUpgradeOrder()` | O(n² + m) | O(n + m) | n = estações, m = conexões |
| `TopologicalSortResult.toString()` | O(n) | O(n) | n = número de estações |

- **n** = número de estações
- **m** = número de conexões
- **k** = número de vizinhos de um vértice específico

---

## Conclusão

O método principal `RailwayUpgradeService.computeUpgradeOrder()` tem complexidade **T(n) = O(n² + m)**, onde o termo dominante O(n²) vem do cálculo de in-degree na ordenação topológica usando `incomingEdges()` que é O(n) para cada vértice.

A complexidade espacial é **S(n) = O(n + m)** para armazenar o grafo, mapas e resultados.

---