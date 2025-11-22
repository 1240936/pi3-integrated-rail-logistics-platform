# Otimização da Construção da KD2D Tree

## Problema Atual

A implementação atual usa AVL Trees para construir a KD-tree, mas tem complexidade temporal **O(n log² n)** porque:

1. Em cada nível recursivo, fazemos `inOrder()` completo - **O(n)** por nível
2. Reconstruímos AVL Trees completamente para cada subproblema - **O(n log n)** por nível
3. Com log n níveis, isso resulta em **O(n log² n)** no total

Para 64k estações, isso resulta em ~3 minutos de construção.

## Soluções Possíveis

### Opção 1: Manter AVL Trees mas Otimizar (Recomendado para cumprir requisitos)

**Vantagens:**
- ✅ Cumpre os requisitos USEI06/USEI07 (usa AVL Trees)
- ✅ Melhora significativa de performance

**Desvantagens:**
- ⚠️ Ainda tem complexidade O(n log² n), mas com constantes menores

**Implementação:**
- Usar `findKthElement` para encontrar medianas (já fazemos)
- Fazer `inOrder()` apenas uma vez por nível (já fazemos)
- Otimizar a reconstrução das árvores usando inserção em batch quando possível

### Opção 2: Usar Arrays Pré-ordenados (Mais rápido, mas não cumpre requisitos)

**Vantagens:**
- ✅ Complexidade O(n log n) - muito mais rápido (~800ms)
- ✅ Constantes muito menores

**Desvantagens:**
- ❌ Não usa AVL Trees como requerido pela professora
- ❌ Pode não ser aceite nos requisitos USEI07

**Implementação:**
- Pré-ordenar arrays uma vez
- Usar array slicing para dividir
- Evitar reconstrução completa

## Recomendação

Se a professora **exigir** o uso de AVL Trees, devemos manter a Opção 1 mas com otimizações:
- Garantir que `inOrder()` é feito apenas uma vez por nível
- Otimizar a reconstrução das árvores
- Considerar usar uma estrutura auxiliar para evitar reconstruções desnecessárias

Se a professora for **flexível** sobre a implementação (desde que use árvores no resultado final), a Opção 2 é muito melhor em termos de performance.

## Nota Importante

A KD-tree final **é sempre uma árvore**, independentemente de como a construímos. As AVL Trees são apenas ferramentas auxiliares durante a construção. A questão é se a professora quer ver o uso explícito de AVL Trees na construção ou apenas no resultado final.

