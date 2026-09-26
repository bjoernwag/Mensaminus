package com.bjwag.mensaminus.utils

import kotlin.math.max

fun String.similarityTo(other: String): Double {
    val maxLen = max(this.length, other.length)
    if (maxLen == 0) return 1.0
    val distance = this.levenshtein(other)
    return 1.0 - (distance.toDouble() / maxLen)
}

fun String.levenshtein(other: String): Int {
    val cost = IntArray(other.length + 1) { it }
    for (i in 1..this.length) {
        var prevCost = i
        for (j in 1..other.length) {
            val match = if (this[i - 1] == other[j - 1]) 0 else 1
            val newCost = minOf(cost[j] + 1, prevCost + 1, cost[j - 1] + match)
            cost[j - 1] = prevCost
            prevCost = newCost
        }
        cost[other.length] = prevCost
    }
    return cost[other.length]
}