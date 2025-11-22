## KD2DTree Build Evolution

This document summarizes how the KD-tree construction worked originally (with AVL helpers) and how it works now (with pre-sorted arrays). It highlights when `ArrayList` was involved, why two AVL trees appeared in the legacy approach, and what changed in the new version.

---

### Original Approach (AVL rebuild per recursion)

**High-level flow**
1. Group stations by `(latitude, longitude)` into a `Map<String, List<Station>>`. Each bucket is an `ArrayList`, sorted by station name for deterministic ordering.
2. Create two AVL trees:
   - `latTree` sorted by latitude, second by longitude, finally by name.
   - `lonTree` sorted by longitude, second by latitude, finally by name.
3. Insert exactly one representative from each coordinate bucket into both trees. These AVL trees are only a *construction aid*; they are not the final KD2D structure.
4. Recursively build the KD tree (`buildFromAVLTrees`):
   - If splitting by latitude, use `latTree`; otherwise use `lonTree`.
   - Find the median node via `findKthElement(medianIndex)`.
   - Retrieve and attach the full bucket of stations to the new `KD2DNode`.
   - Run `inOrder()` on the AVL tree to extract all remaining representatives in sorted order.
   - Partition those representatives into left and right subsets (comparing latitude or longitude, then name).
   - Build *new* AVL trees for each subset (left latitude tree, left longitude tree, etc.).
   - Recurse with those freshly built trees, flipping the split dimension.

**Where `ArrayList` shows up**
- Buckets (`List<Station>`) holding stations sharing the same coordinate.
- Temporary `ArrayList` collections (`leftStations`, `rightStations`) used when splitting the `inOrder()` traversal results.

**Why two AVL trees were used**
- The median axis alternates between latitude and longitude. To get a median quickly for whatever axis is active, you needed both sort orders available.
- After each split, new AVL trees were rebuilt for both dimensions so the next recursion could still pick medians efficiently.
- The final KD tree structure is the only persistent tree; the AVL trees were scaffolding.

**Trade-offs**
- Simpler to implement with existing AVL structures.
- Rebuilding AVL trees and performing full `inOrder()` traversals at every recursion level led to roughly `O(n log² n)` work.

---

### New Approach (Pre-sorted array slicing)

**High-level flow**
1. Same initial grouping: `Map<String, List<Station>>` buckets (each an `ArrayList` sorted by name).
2. Build a single array (`KDPoint[]`) of unique coordinate representatives (one per bucket).
3. Clone that array twice:
   - `pointsByLat` sorted with the latitude comparator.
   - `pointsByLon` sorted with the longitude comparator.
4. Recursively build the KD tree (`buildFromArrays`):
   - If splitting by latitude, take the median from `pointsByLat`; otherwise from `pointsByLon`.
   - Create the `KD2DNode` and attach the full bucket (list) from the coordinate map.
   - Partition the arrays by slicing them, preserving the sort order for the next recursion.
   - No trees are rebuilt—just hand smaller array segments to the recursive calls.

**Where `ArrayList` shows up now**
- Still used to hold buckets of stations.
- Used transiently when partitioning the opposite-sorted array (e.g., building `leftLonList`) before converting back to `KDPoint[]`.

**Why the AVL trees disappeared**
- Median selection comes from array indices (`array[length/2]`), so no balanced tree structure is necessary for ordering.
- Sorting the arrays once up front replaces the need to rebuild sorted AVL trees at every level.

**Trade-offs**
- Slightly more bookkeeping with arrays, but only `O(n log n)` work overall (initial sort plus linear partitions).
- Keeps the KD-tree output identical while removing the heaviest part of the old algorithm.

---

### Summary

| Aspect                   | Legacy AVL rebuild approach                          | New array-slicing approach                      |
|--------------------------|-------------------------------------------------------|-------------------------------------------------|
| Sorting helper           | Two AVL trees (latitude & longitude) per recursion    | Two sorted arrays built once                    |
| Bucket storage           | `ArrayList` per coordinate (still true)               | Same                                            |
| Median selection         | `findKthElement` on AVL trees                         | Array index (`sortedArray[mid]`)                |
| Splitting work           | `inOrder()` traversal + rebuild AVL trees             | Array slicing + set membership                  |
| Complexity               | ~`O(n log² n)`                                        | `O(n log n)`                                    |
| Persistent tree result   | Single KD2D tree                                      | Single KD2D tree                                |

In both versions, the KD2D tree remains the end product. The migration replaced the heavy AVL rebuild machinery with pre-sorted arrays, dramatically reducing build time while using the same station buckets and KD node structure.



