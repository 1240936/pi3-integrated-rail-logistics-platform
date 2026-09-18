# USEI13 - Análise de Complexidade Temporal

## Metodologia

O documento segue a metodologia sugerida nos slides fornecidos nas aulas de ESINF, analisando individualmente os métodos implementados na USEI13,

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
1. Identificar as possíveis e as suas probabilidades
2. Calcular a complexidade para cada entrada
3. Calcular a média ponderada: Σ(probabilidade × complexidade)

Quando o caso médio é difícil de modelar, assume-se que tende para o pior caso. (Informação dos slides de ESINF)

---

## 1. HubCentralityService.loadEdgesWithDistance()

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
            String fromKey = fields[0].trim();                      // 1 atribuição + 1 chamada
            String toKey = fields[1].trim();                        // 1 atribuição + 1 chamada
            double dist = Double.parseDouble(fields[2].trim());     // 1 atribuição + 1 chamada O(1)

            if (dist < 0) {                                         // 1 comparação
                result.addError(...);                               // 1 chamada função
                return;
            }

            Station fromStation = stationMap.get(fromKey);          // 1 atribuição + 1 lookup O(1)
            Station toStation = stationMap.get(toKey);              // 1 atribuição + 1 lookup O(1)

            if (fromStation == null) {                              // 1 comparação
                result.addError(...);                               // 1 chamada função
                return;
            }
            if (toStation == null) {                                // 1 comparação
                result.addError(...);                               // 1 chamada função
                return;
            }

            Edge<Station, Double> edge = new Edge<>(...);           // 1 criação objeto
            result.addRecord(edge);                                  // 1 chamada função O(1)
        } catch (...) {
            result.addError(...);                                   // tratamento erro
        }
    });

    return result;                                                  // 1 retorno
}
```

**Operações primitivas por linha CSV:**
- Validação: 1 comparação
- Parsing: 3 atribuições + 3 chamadas O(1) = **6**
- Validação distância: 1 comparação + 1 chamada (condicional) = **2** (no melhor caso)
- Lookups: 2 lookups O(1) = **2**
- Validações null: 2 comparações + 2 chamadas (condicionais) = **4** (no melhor caso)
- Criação e adição: 1 criação + 1 chamada O(1) = **2**
- **Total por linha válida: ~16 operações**

**Expressão completa:**
- Inicialização: **1**
- Loop sobre m linhas CSV: **m × 16 = 16m**
- Retorno: **1**
- **Total: 16m + 2**

### 1.3 Análise de Casos

**Melhor caso:** todas as linhas são válidas → T(n) = 16m + 2

**Pior caso:** todas as linhas são válidas → T(n) = 16m + 2 (igual ao melhor caso, pois sempre processa todas as linhas)

**Caso médio:** T(n) = 16m + 2 (igual ao pior caso, pois sempre percorre todas as linhas do CSV)

### 1.4 Simplificação Big-Oh

**T(n) = O(m)** onde m = número de linhas no CSV (conexões)

**S(n) = O(m)** - espaço para armazenar todas as arestas

**Complexidade Final: O(m)**

**Explicação:** O método percorre todas as linhas do CSV uma vez, processando cada linha em tempo constante O(1). A complexidade é linear no número de conexões.

---

## 2. HubCentralityService.findStationByName()

### 2.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo simples
- **Recursivo:** Não

### 2.2 Análise de Operações Primitivas

```java
private Station findStationByName(Map<String, Station> stationMap, String name) {
    for (Station station : stationMap.values()) {        // n iterações
        if (station.getName().equals(name)) {            // 1 chamada + 1 comparação
            return station;                              // 1 retorno (se encontrado)
        }
    }
    return null;                                         // 1 retorno (se não encontrado)
}
```

**Operações primitivas por iteração:**
- Chamada `getName()`: O(1)
- Comparação `equals()`: O(k) onde k = comprimento do nome (assumido constante)
- **Total por iteração: O(1)** (assumindo nomes de comprimento constante)

**Expressão completa:**
- Melhor caso (encontrado na primeira posição): **1 + 1 = 2**
- Pior caso (não encontrado ou encontrado na última posição): **n × 1 + 1 = n + 1**

### 2.3 Análise de Casos

**Melhor caso:** estação encontrada na primeira posição → T(n) = 2

**Pior caso:** estação não encontrada ou encontrada na última posição → T(n) = n + 1

**Caso médio:** T(n) = (n + 1) / 2 ≈ n/2 (assumindo distribuição uniforme)

### 2.4 Simplificação Big-Oh

**T(n) = O(n)** onde n = número de estações

**S(n) = O(1)** - não usa espaço adicional

**Complexidade Final: O(n)**

**Explicação:** No pior caso, o método percorre todas as n estações para encontrar uma por nome. Este método é chamado durante a construção do grafo não direcionado.

---

## 3. HubCentralityService.buildUndirectedGraph()

### 3.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclos aninhados
- **Recursivo:** Não

### 3.2 Análise de Operações Primitivas

```java
private MapGraph<Station, Double> buildUndirectedGraph(
        Map<String, Station> stationMap,
        List<Edge<Station, Double>> edges) {

    MapGraph<Station, Double> graph = new MapGraph<>(false);  // 1

    // Add all vertices
    for (Station station : stationMap.values()) {             // n iterações
        graph.addVertex(station);                             // 1 chamada O(1)
    }

    Map<String, Double> edgeMap = new HashMap<>();            // 1

    // Process edges
    for (Edge<Station, Double> edge : edges) {               // m iterações
        Station from = edge.getVOrig();                       // 1 atribuição
        Station to = edge.getVDest();                         // 1 atribuição
        Double weight = edge.getWeight();                     // 1 atribuição

        String key1 = from.getName() + "|" + to.getName();   // 1 concatenação O(k)
        String key2 = to.getName() + "|" + from.getName();   // 1 concatenação O(k)

        if (edgeMap.containsKey(key2)) {                      // 1 lookup O(1)
            Double existingWeight = edgeMap.get(key2);        // 1 lookup O(1)
            if (weight < existingWeight) {                    // 1 comparação
                edgeMap.remove(key2);                         // 1 remoção O(1)
                edgeMap.put(key1, weight);                    // 1 inserção O(1)
            }
        } else if (!edgeMap.containsKey(key1)) {              // 1 lookup O(1)
            edgeMap.put(key1, weight);                        // 1 inserção O(1)
        } else {
            Double existingWeight = edgeMap.get(key1);       // 1 lookup O(1)
            if (weight < existingWeight) {                    // 1 comparação
                edgeMap.put(key1, weight);                    // 1 inserção O(1)
            }
        }
    }

    // Add edges to undirected graph
    for (Map.Entry<String, Double> entry : edgeMap.entrySet()) {  // até m iterações
        String[] parts = entry.getKey().split("\\|");        // 1 split O(k)
        if (parts.length == 2) {                              // 1 comparação
            Station from = findStationByName(stationMap, parts[0]);  // O(n)
            Station to = findStationByName(stationMap, parts[1]);    // O(n)
            if (from != null && to != null) {                 // 2 comparações
                graph.addEdge(from, to, entry.getValue());    // 1 chamada O(1)
            }
        }
    }

    return graph;                                             // 1 retorno
}
```

**Operações primitivas:**

**Fase 1 - Adicionar vértices:**
- Loop: n × 1 = **n**

**Fase 2 - Processar arestas:**
- Por iteração: ~10 operações O(1) = **10**
- Loop: m × 10 = **10m**

**Fase 3 - Adicionar arestas ao grafo:**
- Por iteração: 1 split O(k) + 2 × O(n) + 3 operações = **O(n)** (dominado por `findStationByName`)
- Loop: até m iterações × O(n) = **O(m × n)**

**Expressão completa:**
- Inicialização: **2**
- Adicionar vértices: **n**
- Processar arestas: **10m**
- Adicionar arestas: **O(m × n)** (dominante)
- Retorno: **1**
- **Total: O(m × n)**

### 3.3 Análise de Casos

**Melhor caso:** todas as estações são encontradas rapidamente (cache/otimização) → T(n) = O(m × n), mas com constante menor

**Pior caso:** todas as estações precisam ser pesquisadas → T(n) = O(m × n)

**Caso médio:** T(n) = O(m × n) (tende para o pior caso devido ao `findStationByName`)

**Nota:** Este método poderia ser otimizado usando um mapa de nome → Station em vez de buscar por nome a cada iteração, reduzindo a complexidade para O(m).

### 3.4 Simplificação Big-Oh

**T(n) = O(m × n)** onde n = número de estações, m = número de conexões

**S(n) = O(n + m)** - espaço para grafo e mapa de arestas

**Complexidade Final: O(m × n)**

**Explicação:** O método é dominado pela busca linear de estações por nome (`findStationByName`) dentro do loop que processa todas as arestas. Cada busca é O(n) e há até m iterações.

---

## 4. HubCentralityService.computeHubCentrality() - Inicialização de Matrizes

### 4.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclos aninhados
- **Recursivo:** Não

### 4.2 Análise de Operações Primitivas

```java
// Initialize distance matrix
for (int i = 0; i < n; i++) {                    // n iterações
    for (int j = 0; j < n; j++) {                // n iterações
        if (i == j) {                            // 1 comparação
            dist[i][j] = 0.0;                    // 1 atribuição
            paths[i][j] = 1;                     // 1 atribuição
        } else {
            dist[i][j] = Double.MAX_VALUE;       // 1 atribuição
            paths[i][j] = 0;                     // 1 atribuição
        }
    }
}
```

**Operações primitivas por iteração interna:**
- Comparação: **1**
- Atribuições: **2**
- **Total: 3**

**Expressão completa:**
- Loops aninhados: n × n × 3 = **3n²**

### 4.3 Análise de Casos

**Melhor caso:** T(n) = 3n²

**Pior caso:** T(n) = 3n²

**Caso médio:** T(n) = 3n² (sempre executa o mesmo número de operações)

### 4.4 Simplificação Big-Oh

**T(n) = O(n²)**

**S(n) = O(n²)** - espaço para matrizes dist e paths

**Complexidade Final: O(n²)**

**Explicação:** Inicialização de duas matrizes n × n requer n² operações.

---

## 5. HubCentralityService.computeHubCentrality() - Preencher Arestas Diretas

### 5.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo simples
- **Recursivo:** Não

### 5.2 Análise de Operações Primitivas

```java
// Fill direct edges
for (Edge<Station, Double> edge : graph.edges()) {    // m iterações
    int u = graph.key(edge.getVOrig());               // 1 chamada O(1)
    int v = graph.key(edge.getVDest());               // 1 chamada O(1)
    double weight = edge.getWeight();                  // 1 chamada O(1)
    dist[u][v] = weight;                              // 1 atribuição
    dist[v][u] = weight;                              // 1 atribuição (grafo não direcionado)
    paths[u][v] = 1;                                  // 1 atribuição
    paths[v][u] = 1;                                  // 1 atribuição
}
```

**Operações primitivas por iteração:**
- Chamadas `key()`: 2 × O(1) = **2**
- Chamada `getWeight()`: **1**
- Atribuições: **4**
- **Total: 7**

**Expressão completa:**
- Loop: m × 7 = **7m**

### 5.3 Análise de Casos

**Melhor caso:** T(n) = 7m

**Pior caso:** T(n) = 7m

**Caso médio:** T(n) = 7m (sempre executa o mesmo número de operações)

### 5.4 Simplificação Big-Oh

**T(n) = O(m)** onde m = número de arestas

**S(n) = O(1)** - não usa espaço adicional

**Complexidade Final: O(m)**

**Explicação:** O método percorre todas as m arestas uma vez, atualizando as matrizes em tempo constante por aresta.

---

## 6. HubCentralityService.computeHubCentrality() - Algoritmo Floyd-Warshall

### 6.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclos aninhados (3 níveis)
- **Recursivo:** Não

### 6.2 Análise de Operações Primitivas

```java
// Floyd-Warshall to compute shortest paths and count paths
for (int k = 0; k < n; k++) {                                    // n iterações
    for (int i = 0; i < n; i++) {                                // n iterações
        for (int j = 0; j < n; j++) {                            // n iterações
            if (dist[i][k] != Double.MAX_VALUE &&                // 2 comparações + 2 acessos
                dist[k][j] != Double.MAX_VALUE) {                // array O(1)
                double newDist = dist[i][k] + dist[k][j];        // 1 soma + 2 acessos
                if (dist[i][j] > newDist) {                       // 1 comparação + 1 acesso
                    dist[i][j] = newDist;                        // 1 atribuição + 1 acesso
                    paths[i][j] = paths[i][k] * paths[k][j];    // 1 multiplicação + 3 acessos
                } else if (dist[i][j] == newDist &&              // 1 comparação + 1 acesso
                          i != k && j != k) {                    // 2 comparações
                    paths[i][j] += paths[i][k] * paths[k][j];    // 1 soma + multiplicação + 3 acessos
                }
            }
        }
    }
}
```

**Operações primitivas por iteração interna (pior caso - condição verdadeira):**
- Comparações: 2 + 1 + 2 = **5**
- Acessos array: 2 + 2 + 1 + 1 + 3 + 1 + 3 = **13**
- Operações aritméticas: 1 + 1 + 1 = **3**
- Atribuições: **2**
- **Total: ~23 operações**

**Expressão completa:**
- Loops aninhados: n × n × n × 23 = **23n³**

### 6.3 Análise de Casos

**Melhor caso:** grafo desconectado (muitas comparações falsas) → T(n) ≈ 23n³ (ainda percorre todas as células)

**Pior caso:** grafo denso (todas as comparações verdadeiras) → T(n) = 23n³

**Caso médio:** T(n) ≈ 23n³ (tende para o pior caso, pois sempre percorre todas as células da matriz)

### 6.4 Simplificação Big-Oh

**T(n) = O(n³)**

**S(n) = O(n²)** - espaço para matrizes dist e paths (já alocadas)

**Complexidade Final: O(n³)**

**Explicação:** O algoritmo Floyd-Warshall usa três loops aninhados, cada um executado n vezes, resultando em n³ iterações. Este é o método clássico para calcular caminhos mais curtos entre todos os pares de vértices.

---

## 7. HubCentralityService.computeHubCentrality() - Cálculo de Betweenness

### 7.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclos aninhados (3 níveis)
- **Recursivo:** Não

### 7.2 Análise de Operações Primitivas

```java
// Compute betweenness centrality
for (int k = 0; k < n; k++) {                                    // n iterações
    for (int i = 0; i < n; i++) {                                // n iterações
        for (int j = i + 1; j < n; j++) {                        // (n-i-1) iterações em média
            if (i == k || j == k) continue;                      // 2 comparações

            if (dist[i][j] == Double.MAX_VALUE ||                // 1 comparação + 1 acesso
                paths[i][j] == 0) continue;                      // 1 comparação + 1 acesso

            if (dist[i][k] != Double.MAX_VALUE &&                // 1 comparação + 1 acesso
                dist[k][j] != Double.MAX_VALUE) {                // 1 comparação + 1 acesso
                double pathThroughK = dist[i][k] + dist[k][j];   // 1 soma + 2 acessos
                if (Math.abs(dist[i][j] - pathThroughK) < 1e-9) { // 1 subtração + abs + comparação + acesso
                    if (paths[i][k] > 0 &&                       // 1 comparação + acesso
                        paths[k][j] > 0 &&                       // 1 comparação + acesso
                        paths[i][j] > 0) {                      // 1 comparação + acesso
                        double contribution = ...;               // 1 divisão + multiplicação + 3 acessos
                        betweenness[k] += contribution;          // 1 soma + acesso
                    }
                }
            }
        }
    }
}
```

**Operações primitivas por iteração interna (pior caso - todas condições verdadeiras):**
- Comparações: 2 + 2 + 2 + 1 + 3 = **10**
- Acessos array: 1 + 1 + 1 + 1 + 2 + 1 + 3 + 1 = **11**
- Operações aritméticas: 1 + 1 + 1 + 1 = **4**
- Atribuições: **2**
- **Total: ~27 operações**

**Número de iterações:**
- Loop externo k: n iterações
- Loop médio i: n iterações
- Loop interno j: (n-i-1) iterações em média
- Total: Σ(i=0 to n-1) Σ(j=i+1 to n-1) = n(n-1)/2 por k
- **Total: n × n × (n-1)/2 ≈ n³/2**

**Expressão completa:**
- Total de iterações: **n³/2**
- Por iteração (pior caso): **27**
- **Total: 27n³/2 ≈ 13.5n³**

### 7.3 Análise de Casos

**Melhor caso:** grafo desconectado (muitos `continue`) → T(n) ≈ O(n³) (ainda percorre todas as combinações)

**Pior caso:** grafo denso e conectado (todas condições verdadeiras) → T(n) = 13.5n³

**Caso médio:** T(n) ≈ O(n³) (tende para o pior caso devido ao número fixo de combinações a verificar)

### 7.4 Simplificação Big-Oh

**T(n) = O(n³)**

**S(n) = O(n)** - espaço para array betweenness

**Complexidade Final: O(n³)**

**Explicação:** O cálculo de betweenness requer verificar todas as combinações de vértices (i, j, k), resultando em complexidade cúbica. Para grafos não direcionados, verificamos apenas pares (i, j) onde i < j, mas ainda temos O(n³) combinações.

---

## 8. HubCentralityService.computeHubCentrality() - Cálculo de Degree e Strength

### 8.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclo simples aninhado
- **Recursivo:** Não

### 8.2 Análise de Operações Primitivas

```java
// Compute degree and strength for each vertex
int[] degree = new int[n];                                    // 1 criação array
double[] strength = new double[n];                            // 1 criação array

for (Station station : vertices) {                            // n iterações
    int idx = graph.key(station);                             // 1 chamada O(1)
    Collection<Edge<Station, Double>> outgoingEdges =         // 1 atribuição +
        graph.outgoingEdges(station);                          // 1 chamada O(k)
    if (outgoingEdges != null) {                              // 1 comparação
        degree[idx] = outgoingEdges.size();                   // 1 atribuição + 1 chamada O(1)
        for (Edge<Station, Double> edge : outgoingEdges) {   // k iterações
            strength[idx] += edge.getWeight();                // 1 soma + 1 chamada O(1)
        }
    }
}
```

**Operações primitivas:**

**Por vértice (fora do loop interno):**
- Chamada `key()`: O(1) = **1**
- Chamada `outgoingEdges()`: O(k) onde k = grau do vértice
- Comparação: **1**
- Atribuição degree: **1**
- **Total: k + 3**

**Por aresta (dentro do loop interno):**
- Chamada `getWeight()`: O(1) = **1**
- Soma: **1**
- **Total: 2**

**Expressão completa:**
- Inicialização arrays: **2**
- Loop externo: n × (k_avg + 3) onde k_avg = grau médio = m/n
- Loop interno: Σ(k sobre todos vértices) × 2 = m × 2
- **Total: 2 + n × (m/n + 3) + 2m = 2 + m + 3n + 2m = 3m + 3n + 2**

### 8.3 Análise de Casos

**Melhor caso:** grafo vazio (sem arestas) → T(n) = 3n + 2

**Pior caso:** grafo completo → T(n) = 3m + 3n + 2 onde m = n(n-1)/2

**Caso médio:** T(n) = 3m + 3n + 2 (tende para o pior caso, pois sempre percorre todas as arestas)

### 8.4 Simplificação Big-Oh

**T(n) = O(n + m)** onde n = número de vértices, m = número de arestas

**S(n) = O(n)** - espaço para arrays degree e strength

**Complexidade Final: O(n + m)**

**Explicação:** O método percorre todos os n vértices e, para cada vértice, percorre todas as suas k arestas. A soma de todos os graus é 2m (para grafo não direcionado), resultando em O(n + m).

---

## 9. HubCentralityService.computeHubCentrality() - Cálculo de Harmonic Closeness

### 9.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclos aninhados
- **Recursivo:** Não

### 9.2 Análise de Operações Primitivas

```java
// Compute harmonic closeness centrality
double[] harmonicCloseness = new double[n];                  // 1 criação array

for (int i = 0; i < n; i++) {                                // n iterações
    double sum = 0.0;                                         // 1 atribuição
    for (int j = 0; j < n; j++) {                            // n iterações
        if (i != j &&                                         // 1 comparação
            dist[i][j] != Double.MAX_VALUE &&                // 1 comparação + 1 acesso
            dist[i][j] > 0) {                                 // 1 comparação + 1 acesso
            sum += 1.0 / dist[i][j];                          // 1 divisão + 1 soma + 1 acesso
        }
    }
    harmonicCloseness[i] = sum;                              // 1 atribuição
}
```

**Operações primitivas por iteração interna (quando condição verdadeira):**
- Comparações: **3**
- Acessos array: **3**
- Operações aritméticas: **2** (divisão + soma)
- **Total: 8**

**Expressão completa:**
- Inicialização: **1**
- Loop externo: n iterações
- Loop interno: n iterações × 8 (no pior caso, todas as células são válidas)
- **Total: 1 + n × n × 8 = 8n² + 1**

### 9.3 Análise de Casos

**Melhor caso:** grafo desconectado (muitas comparações falsas) → T(n) ≈ 8n² (ainda percorre todas as células)

**Pior caso:** grafo completo e conectado → T(n) = 8n² + 1

**Caso médio:** T(n) ≈ 8n² (tende para o pior caso, pois sempre percorre todas as células da matriz dist)

### 9.4 Simplificação Big-Oh

**T(n) = O(n²)**

**S(n) = O(n)** - espaço para array harmonicCloseness

**Complexidade Final: O(n²)**

**Explicação:** O método percorre a matriz de distâncias n × n, calculando a soma harmônica para cada vértice. Como a matriz já foi calculada pelo Floyd-Warshall, apenas acessamos seus valores.

---

## 10. HubCentralityService.computeHubCentrality() - Normalização e HubScore

### 10.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Ciclos simples
- **Recursivo:** Não

### 10.2 Análise de Operações Primitivas

```java
// Normalize all measures to [0,1]
double maxBetweenness = Arrays.stream(betweenness).max().orElse(1.0);        // O(n)
double maxHarmonicCloseness = Arrays.stream(harmonicCloseness).max().orElse(1.0);  // O(n)
double maxStrength = Arrays.stream(strength).max().orElse(1.0);              // O(n)
double minStrength = Arrays.stream(strength).min().orElse(0.0);               // O(n)

double[] betweennessNorm = new double[n];                                    // 1
double[] harmonicClosenessNorm = new double[n];                              // 1
double[] strengthNorm = new double[n];                                        // 1

for (int i = 0; i < n; i++) {                                                // n iterações
    betweennessNorm[i] = maxBetweenness > 0 ?                                // 1 comparação
        betweenness[i] / maxBetweenness : 0.0;                                // 1 divisão + 1 acesso
    harmonicClosenessNorm[i] = maxHarmonicCloseness > 0 ?                   // 1 comparação
        harmonicCloseness[i] / maxHarmonicCloseness : 0.0;                  // 1 divisão + 1 acesso
    strengthNorm[i] = (maxStrength - minStrength) > 0 ?                     // 1 comparação + 1 subtração
        (strength[i] - minStrength) / (maxStrength - minStrength) : 0.0;     // 1 subtração + 1 divisão + 2 acessos
}

// Compute HubScore
List<HubScoreResult> results = new ArrayList<>();                            // 1
for (int i = 0; i < n; i++) {                                                // n iterações
    Station station = vertices.get(i);                                       // 1 acesso lista O(1)
    String stationId = stationToIdMap.get(station);                           // 1 lookup O(1)
    double hubScore = 0.35 * betweennessNorm[i] +                            // 2 multiplicações + 1 soma + 3 acessos
                     0.35 * harmonicClosenessNorm[i] +
                     0.30 * strengthNorm[i];

    results.add(new HubScoreResult(...));                                     // 1 criação objeto + 1 adição O(1)
}

// Sort by hubScore descending
results.sort((a, b) -> Double.compare(b.getHubScore(), a.getHubScore()));     // O(n log n)
```

**Operações primitivas:**

**Normalização:**
- Cálculo de máximos/mínimos: 4 × O(n) = **4n**
- Inicialização arrays: **3**
- Loop de normalização: n × 9 = **9n**

**Cálculo HubScore:**
- Inicialização lista: **1**
- Loop: n × 8 = **8n**

**Ordenação:**
- Sort: **O(n log n)**

**Expressão completa:**
- Normalização: 4n + 3 + 9n = **13n + 3**
- HubScore: 1 + 8n = **8n + 1**
- Ordenação: **O(n log n)** (dominante)
- **Total: O(n log n)**

### 10.3 Análise de Casos

**Melhor caso:** T(n) = O(n log n) (ordenação é sempre O(n log n))

**Pior caso:** T(n) = O(n log n)

**Caso médio:** T(n) = O(n log n) (ordenação é sempre O(n log n))

### 10.4 Simplificação Big-Oh

**T(n) = O(n log n)** onde n = número de estações

**S(n) = O(n)** - espaço para arrays normalizados e lista de resultados

**Complexidade Final: O(n log n)**

**Explicação:** A ordenação da lista de resultados domina esta fase, com complexidade O(n log n) usando o algoritmo de ordenação padrão do Java (Timsort).

---

## 11. HubCentralityService.computeHubCentrality() - Método Principal

### 11.1 Identificação do Tipo de Algoritmo

- **Tipo:** Determinístico
- **Estrutura:** Sequencial (combina múltiplas etapas)
- **Recursivo:** Não

### 11.2 Análise de Operações Primitivas

O método principal combina todas as etapas anteriores:

1. **Carregar estações:** O(n) - usa `StationsWithIdCsvLoader.loadWithIdMap()`
2. **Carregar conexões:** O(m) - `loadEdgesWithDistance()`
3. **Construir grafo não direcionado:** O(m × n) - `buildUndirectedGraph()`
4. **Criar mapeamento reverso:** O(n) - loop simples
5. **Inicializar matrizes:** O(n²)
6. **Preencher arestas diretas:** O(m)
7. **Floyd-Warshall:** O(n³)
8. **Cálculo betweenness:** O(n³)
9. **Cálculo degree/strength:** O(n + m)
10. **Cálculo harmonic closeness:** O(n²)
11. **Normalização e HubScore:** O(n log n)

**Expressão completa:**
- O(n) + O(m) + O(m × n) + O(n) + O(n²) + O(m) + O(n³) + O(n³) + O(n + m) + O(n²) + O(n log n)
- Simplificando: **O(m × n) + O(n³) + O(n log n)**
- Termo dominante: **O(n³)** (para n grande)

### 11.3 Análise de Casos

**Melhor caso:** T(n) = O(n³) (Floyd-Warshall e betweenness dominam)

**Pior caso:** T(n) = O(n³) (igual ao melhor caso)

**Caso médio:** T(n) = O(n³) (tende para o pior caso devido aos algoritmos cúbicos)

### 11.4 Simplificação Big-Oh

**T(n) = O(n³)** onde n = número de estações

**S(n) = O(n²)** - espaço dominante para matrizes dist e paths (n × n cada)

**Complexidade Final: O(n³)**

**Explicação:** O método principal é dominado pelos algoritmos Floyd-Warshall e cálculo de betweenness, ambos com complexidade O(n³). Para grafos grandes, estes termos dominam sobre os termos O(m × n) e O(n log n).

---

## Resumo Final

| Método | T(n) | S(n) | Observações |
|--------|------|------|-------------|
| `loadEdgesWithDistance()` | O(m) | O(m) | m = número de conexões |
| `findStationByName()` | O(n) | O(1) | Busca linear |
| `buildUndirectedGraph()` | O(m × n) | O(n + m) | Dominado por busca por nome |
| Inicialização matrizes | O(n²) | O(n²) | Matrizes dist e paths |
| Preencher arestas diretas | O(m) | O(1) |  |
| Floyd-Warshall | O(n³) | O(n²) | Algoritmo clássico |
| Cálculo betweenness | O(n³) | O(n) | Verifica todas combinações |
| Cálculo degree/strength | O(n + m) | O(n) |  |
| Cálculo harmonic closeness | O(n²) | O(n) | Usa matriz dist calculada |
| Normalização e HubScore | O(n log n) | O(n) | Ordenação domina |
| `computeHubCentrality()` | **O(n³)** | **O(n²)** | n = estações, m = conexões |

- **n** = número de estações
- **m** = número de conexões

---

## Conclusão

O método principal `HubCentralityService.computeHubCentrality()` tem complexidade temporal **T(n) = O(n³)**, onde o termo dominante O(n³) vem do algoritmo Floyd-Warshall para calcular caminhos mais curtos entre todos os pares de vértices e do cálculo de betweenness centrality, que também requer O(n³) operações.

A complexidade espacial é **S(n) = O(n²)** para armazenar as matrizes de distâncias e contagem de caminhos (dist e paths), cada uma com tamanho n × n.

**Observações:**
- Para grafos muito grandes (n > 1000), o algoritmo pode ser lento devido à complexidade cúbica.
- Uma otimização possível seria usar o algoritmo de Brandes para betweenness, que tem complexidade O(n × m) para grafos não ponderados, mas requer adaptação para grafos ponderados.
- O método `buildUndirectedGraph()` poderia ser otimizado para O(m) usando um mapa de nome → Station em vez de busca linear, mas isso não alteraria a complexidade geral O(n³).

