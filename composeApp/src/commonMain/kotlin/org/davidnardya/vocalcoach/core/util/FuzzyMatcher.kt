package org.davidnardya.vocalcoach.core.util

import kotlin.math.max
import kotlin.math.min

object FuzzyMatcher {
    /**
     * Calculates the normalized Levenshtein similarity between two strings.
     * 1.0 means exact match, 0.0 means completely different.
     */
    fun similarity(s1: String, s2: String): Float {
        val str1 = s1.lowercase().trim()
        val str2 = s2.lowercase().trim()
        if (str1 == str2) return 1.0f
        if (str1.isEmpty() || str2.isEmpty()) return 0.0f

        val distance = levenshtein(str1, str2)
        val maxLength = max(str1.length, str2.length)
        return 1.0f - (distance.toFloat() / maxLength.toFloat())
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
