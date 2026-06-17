# Technical Architecture & Algorithm Guide: PUP Lagoon App

## 1. Architectural Foundation: MVVM & Repository Pattern

The PUP Lagoon App is built using the **Model-View-ViewModel (MVVM)** pattern, ensuring a strict separation between UI logic and data management.

### 1.1 The Data Layer (`FoodRepository.kt`)
The `FoodRepository` acts as the central orchestrator. It is responsible for:
- **Asset Deserialization**: Parsing `food_records.csv`, `stall_locations.csv`, and `map_labels.csv`.
- **Index Management**: Maintaining three separate `BTree` instances for instantaneous data retrieval.
- **Geospatial Logic**: Calculating walking paths based on screen coordinates and stall sequences.

### 1.2 The State Layer (`MainViewModel.kt`)
The ViewModel manages the "Screen State" using Kotlin `StateFlow`. It utilizes `combine` and `debounce` operators to reactively filter search results as the user types, ensuring the UI doesn't stutter during heavy B-Tree lookups.

---

## 2. Data Structure: The Multi-Value B-Tree

The app utilizes a custom generic B-Tree implementation (`BTree<K, V>`) where each key $K$ maps to a `MutableList<V>`. This allows multiple food items to be indexed under the same price or category name.

### 2.1 Branching Factor & Efficiency
- **Degree ($t$)**: Set to **5**.
- **Key Capacity**: Each node holds between $t-1$ (4) and $2t-1$ (9) keys.
- **Complexity**: Search, Insert, and Range Search operations run in $O(\log_t n)$ time.

### 2.2 Range Search Implementation
The B-Tree is used for price filtering. The algorithm performs a depth-first traversal, only descending into child nodes whose key ranges overlap with the target interval $[start, end]$.

```kotlin
private fun searchRange(node: Node, start: K, end: K, result: MutableList<V>) {
    var i = 0
    // Skip keys smaller than start
    while (i < node.keys.size && node.keys[i] < start) i++

    // Process keys within range
    while (i < node.keys.size && node.keys[i] <= end) {
        if (!node.isLeaf) searchRange(node.children[i], start, end, result)
        result.addAll(node.values[i])
        i++
    }
    // Check the last child
    if (!node.isLeaf) searchRange(node.children[i], start, end, result)
}
```

---

## 3. Custom Pathfinding: The "Lagoon Loop" Algorithm

The app does not use generic graph traversal (like A*) because the walking paths are physically constrained to the circular perimeter of the lagoon. Instead, it uses a **Sequence-Based Path Generator**.

### 3.1 The Physical Backbone
The algorithm relies on a hardcoded physical sequence of stalls, ordered as they appear when walking around the lagoon:
`fullLoop = ["27", "26", "25", ..., "15", "16", "14", ..., "01"]`
*(Note: 15 and 16 are swapped in the sequence to match their actual physical placement between 17 and 14).*

### 3.2 Entry Point Hubs
The app identifies three primary entry "Hubs" corresponding to the gates:
- **Gate 3 (West)**: Connects to a walkway hub at `(1419f, 1214f)`.
- **Gate 2 (South)**: Connects to a southern walkway hub at `(1636f, 1768f)`.
- **Gate 1 (East)**: Connects to an eastern walkway hub at `(2139f, 1360f)`.

### 3.3 Path Construction Logic
When a user requests directions from a Gate to a Stall:
1. **Initialize Path**: Add the Gate's coordinates.
2. **Inject Hub**: Add the specific Hub coordinates for that Gate.
3. **Sequence Traversal**:
    - **Gate 3**: Joins the loop at Stall 27. It iterates from index 0 in `fullLoop` until it reaches the `destId`.
    - **Gate 1**: Joins between Stalls 12 and 13.
        - If `Stall ID <= 12`: Traverses the loop "Up" (Forward in array).
        - If `Stall ID > 12`: Traverses the loop "Down" (Backward in array towards 27).
4. **Finalize**: Add the specific pixel offset of the target stall.

---

## 4. Search Optimization & Data Merging

### 4.1 Index Selection Heuristic
The `search()` function avoids full-table scans by choosing the most efficient B-Tree:
1. **Category Filter Active?** -> Query `categoryTree`.
2. **Price Filter Active?** -> Query `priceTree` using `searchRange`.
3. **Text Search Only?** -> Use `nameTree` for prefix matches (`query` to `query + \uFFFF`), then fall back to a manual scan only for infix matches (e.g., finding "Burger" inside "Cheeseburger").

### 4.2 The Merging Algorithm
To prevent the UI from showing duplicate entries for items with different sizes (e.g., "Coke S", "Coke M"), the app merges them into a `MergedRecords` object.
- **Base Name Extraction**: Uses Regex `\s+(S|M|L|XL|[0-9]+pcs)$` to strip size suffixes.
- **Grouping**: Records are grouped by `StallName` and then by `BaseName`.
- **Price Aggregation**: Calculates a string range (e.g., `"₱20 - ₱50"`) and preserves a list of size-price pairs for the detail view.

---

## 5. CSV Parsing State Machine
Because CSV files may contain commas inside quoted product names (e.g., `"Chicken, Fried"`), a standard `.split(",")` would fail.
```kotlin
fun parseCsvLine(line: String): List<String> {
    var inQuotes = false
    // Iterate char by char
    // Toggle inQuotes when '"' is hit
    // Only split on ',' if inQuotes == false
}
```
This ensures data integrity when loading the B-Tree indexes during app startup.
