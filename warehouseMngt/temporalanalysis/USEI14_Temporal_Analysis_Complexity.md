# USEI14 – Análise de Complexidade Temporal
## MaxFlowService – Algoritmo Edmonds-Karp

---

## Metodologia
Este documento segue a metodologia apresentada nas aulas de ESINF, idêntica à utilizada na análise da USEI11.  
Cada método relevante do serviço **MaxFlowService** é analisado individualmente, identificando:

- Tipo de algoritmo
- Estrutura de controlo
- Operações dominantes
- Melhor caso, pior caso e caso médio
- Simplificação final em Big-Oh

---

## Notação
- **V** = número de vértices (estações)
- **E** = número de arestas (ligações entre estações)
- **n** = número de estações
- **m** = número de conexões
- **k** = número de vizinhos de um vértice

### Sobre T(n) e S(n)
- **T(n)** – Complexidade Temporal (tempo de execução)
- **S(n)** – Complexidade Espacial (uso de memória)

Quando o caso médio é difícil de modelar matematicamente, assume-se que tende para o pior caso, conforme indicado nos slides de ESINF.

---

## 1. MaxFlowService.computeMaxFlow(...)
### 1.1 Identificação do Tipo de Algoritmo
- Tipo: Determinístico
- Estrutura: Sequencial (múltiplas fases)
- Recursivo: Não
- Algoritmo principal: Edmonds-Karp

### 1.2 Análise de Operações Primitivas
```java
public MaxFlowResult computeMaxFlow(...) throws IOException {
    Map<String, Station> stationMap =
        StationsWithIdCsvLoader.loadWithIdMap(stationsWithIdCsvPath); // O(n)

    Station source = stationMap.get(sourceStationId); // O(1)
    Station sink = stationMap.get(sinkStationId);     // O(1)

    CsvValidatorResult<EdgeWithCapacity> edgesResult =
        loadEdgesWithCapacity(stationToStationCsvPath, stationMap); // O(m)

    MapGraph<Station, Double> graph = new MapGraph<>(true); // O(1)

    for (Station station : stationMap.values()) { // n iterações
        graph.addVertex(station);                  // O(1)
    }

    for (EdgeWithCapacity edge : edgesResult.getRecords()) { // m iterações
        graph.addEdge(from, to, capacity);                  // O(1)
    }

    double maxFlow = edmondsKarp(graph, source, sink); // O(V · E²)

    return new MaxFlowResult(...); // O(1)
}
```
### 1.3 Análise de Casos
- **Melhor caso:** Grafo pequeno ou sem caminho entre source e sink → T(n) = O(n + m)
- **Pior caso:** Execução completa do algoritmo Edmonds-Karp → T(n) = O(V · E²)
- **Caso médio:** Assume-se que tende para o pior caso.

### 1.4 Simplificação Big-Oh
- **T(n) = O(V · E²)**
- **S(n) = O(V²)**

---

## 2. MaxFlowService.edmondsKarp(Graph, source, sink)
### 2.1 Identificação do Tipo de Algoritmo
- Tipo: Determinístico
- Estrutura: Ciclo `while` com BFS interno
- Recursivo: Não
- Algoritmo: Edmonds-Karp (Ford-Fulkerson com BFS)

### 2.2 Análise de Operações Primitivas
```java
double[][] residual = new double[n][n]; // O(n²)

for (Edge<Station, Double> edge : graph.edges()) { // m iterações
residual[u][v] = edge.getWeight();             // O(1)
}

while (bfs(...)) { // até O(E) iterações
while (v != source) { // O(V)
bottleneck = Math.min(...);
}

    while (v != source) { // O(V)
        residual[u][v] -= bottleneck;
        residual[v][u] += bottleneck;
    }
}
```

### 2.3 Análise de Casos
- **Melhor caso:** Não existe caminho aumentante → T(n) = O(E)
- **Pior caso:** BFS executado O(E) vezes, cada BFS custa O(V + E)  
  → T(n) = O(E · (V + E)) = O(V · E²)
- **Caso médio:** Assume-se que tende para o pior caso.

### 2.4 Simplificação Big-Oh
- **T(n) = O(V · E²)**
- **S(n) = O(V²)**

---

## 3. MaxFlowService.bfs(...)
### 3.1 Identificação do Tipo de Algoritmo
- Tipo: Determinístico
- Estrutura: BFS clássico
- Recursivo: Não

### 3.2 Análise de Operações Primitivas
- Cada vértice é visitado uma única vez
- Cada aresta é analisada no máximo uma vez

### 3.3 Simplificação Big-Oh
- **T(n) = O(V + E)**
- **S(n) = O(V)**

---

## 4. loadEdgesWithCapacity(String, Map)
### 4.1 Identificação do Tipo de Algoritmo
- Tipo: Determinístico
- Estrutura: Ciclo simples (leitura de CSV)
- Recursivo: Não

### 4.2 Análise de Casos
- Processa cada linha do CSV exatamente uma vez
- Operações constantes por linha

### 4.3 Simplificação Big-Oh
- **T(n) = O(m)**
- **S(n) = O(m)**

---

## Resumo Final
| Método                  | T(n)       | S(n)   | Observações                |
|--------------------------|------------|--------|----------------------------|
| computeMaxFlow()         | O(V · E²)  | O(V²)  | Dominado pelo Edmonds-Karp |
| edmondsKarp()            | O(V · E²)  | O(V²)  | BFS repetido               |
| bfs()                    | O(V + E)   | O(V)   | BFS clássico               |
| loadEdgesWithCapacity()  | O(m)       | O(m)   | Leitura CSV                |

---

## Conclusão
O método principal **MaxFlowService.computeMaxFlow()** apresenta:

- **Complexidade Temporal:** T(n) = O(V · E²)
- **Complexidade Espacial:** S(n) = O(V²)

Esta complexidade resulta da utilização do algoritmo **Edmonds-Karp**, 
adequado para grafos de dimensão média, mas pouco eficiente para grafos 
grandes ou densos.
