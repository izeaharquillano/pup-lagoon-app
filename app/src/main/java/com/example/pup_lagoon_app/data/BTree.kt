package com.example.pup_lagoon_app.data

class BTree<K : Comparable<K>, V>(private val t: Int) {
    private var root: Node<K, V>? = null

    class Node<K : Comparable<K>, V>(val t: Int, var isLeaf: Boolean) {
        val keys = mutableListOf<K>()
        val values = mutableListOf<MutableList<V>>()
        val children = mutableListOf<Node<K, V>>()

        fun findKey(k: K): Int {
            var idx = 0
            while (idx < keys.size && keys[idx] < k) {
                idx++
            }
            return idx
        }
    }

    fun search(k: K, predicate: (V) -> Boolean = { true }): List<V> {
        val result = mutableListOf<V>()
        root?.let { search(it, k, predicate, result) }
        return result
    }

    private fun search(x: Node<K, V>, k: K, predicate: (V) -> Boolean, result: MutableList<V>) {
        val i = x.findKey(k)
        if (i < x.keys.size && x.keys[i] == k) {
            x.values[i].forEach { if (predicate(it)) result.add(it) }
            // Do NOT return here if you want to support non-unique keys if B-tree structure allows it.
            // But standard B-tree search for a key finds ONE node. 
            // Our implementation stores multiples in values[i].
        }
        if (!x.isLeaf) {
            search(x.children[i], k, predicate, result)
        }
    }

    fun searchRange(start: K, end: K, predicate: (V) -> Boolean = { true }): List<V> {
        val result = mutableListOf<V>()
        root?.let { searchRange(it, start, end, predicate, result) }
        return result
    }

    private fun searchRange(x: Node<K, V>, start: K, end: K, predicate: (V) -> Boolean, result: MutableList<V>) {
        var i = 0
        while (i < x.keys.size && x.keys[i] < start) {
            i++
        }

        while (i < x.keys.size && x.keys[i] <= end) {
            if (!x.isLeaf) {
                searchRange(x.children[i], start, end, predicate, result)
            }
            x.values[i].forEach { if (predicate(it)) result.add(it) }
            i++
        }

        if (!x.isLeaf) {
            searchRange(x.children[i], start, end, predicate, result)
        }
    }

    fun insert(k: K, v: V) {
        val r = root
        if (r == null) {
            val newNode = Node<K, V>(t, true)
            newNode.keys.add(k)
            newNode.values.add(mutableListOf(v))
            root = newNode
        } else {
            if (r.keys.size == 2 * t - 1) {
                val s = Node<K, V>(t, false)
                s.children.add(r)
                splitChild(s, 0, r)
                root = s
                insertNonFull(s, k, v)
            } else {
                insertNonFull(r, k, v)
            }
        }
    }

    private fun insertNonFull(x: Node<K, V>, k: K, v: V) {
        if (x.isLeaf) {
            val idx = x.findKey(k)
            if (idx < x.keys.size && x.keys[idx] == k) {
                x.values[idx].add(v)
            } else {
                x.keys.add(idx, k)
                x.values.add(idx, mutableListOf(v))
            }
        } else {
            val idx = x.findKey(k)
            if (idx < x.keys.size && x.keys[idx] == k) {
                x.values[idx].add(v)
            } else {
                if (x.children[idx].keys.size == 2 * t - 1) {
                    splitChild(x, idx, x.children[idx])
                    if (k > x.keys[idx]) {
                        insertNonFull(x.children[idx + 1], k, v)
                    } else {
                        insertNonFull(x.children[idx], k, v)
                    }
                } else {
                    insertNonFull(x.children[idx], k, v)
                }
            }
        }
    }

    private fun splitChild(x: Node<K, V>, i: Int, y: Node<K, V>) {
        val z = Node<K, V>(t, y.isLeaf)
        for (j in 0 until t - 1) {
            z.keys.add(y.keys[j + t])
            z.values.add(y.values[j + t])
        }
        if (!y.isLeaf) {
            for (j in 0 until t) {
                z.children.add(y.children[j + t])
            }
        }
        
        x.children.add(i + 1, z)
        x.keys.add(i, y.keys[t - 1])
        x.values.add(i, y.values[t - 1])

        // Remove from y (must be in reverse order to maintain indices or just slice)
        val keysToRemove = y.keys.size - (t - 1)
        repeat(keysToRemove) {
            y.keys.removeAt(t - 1)
            y.values.removeAt(t - 1)
        }
        if (!y.isLeaf) {
            val childrenToRemove = y.children.size - t
            repeat(childrenToRemove) {
                y.children.removeAt(t)
            }
        }
    }
}
