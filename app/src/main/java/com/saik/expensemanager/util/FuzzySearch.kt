package com.saik.expensemanager.util

/**
 * Lightweight fuzzy/typo-tolerant search utility.
 *
 * Matching tiers (highest priority first):
 * 1. **Substring** — query appears contiguously in candidate → score 1.0
 * 2. **Subsequence** — query characters appear in order (abbreviations like "amz" for "amazon") → 0.85
 * 3. **Levenshtein** — edit-distance based similarity normalized by query length → 0.0–1.0
 *
 * For multi-word candidates: single-word queries are scored against each candidate word
 * (best match wins); multi-word queries average their per-word best scores.
 *
 * All matching is case-insensitive with whitespace normalization.
 */
object FuzzySearch {

    /** Reduced threshold since we now have stronger tiered matching. */
    const val DEFAULT_THRESHOLD = 0.55

    /**
     * Returns `true` if [query] fuzzily matches [candidate], `false` otherwise.
     */
    fun matches(query: String, candidate: String, threshold: Double = DEFAULT_THRESHOLD): Boolean {
        return similarity(query, candidate) >= threshold
    }

    /**
     * Returns a similarity score between 0.0 (completely different) and 1.0 (exact/substring match).
     */
    fun similarity(query: String, candidate: String): Double {
        val q = normalise(query)
        val c = normalise(candidate)

        if (q.isEmpty() || c.isEmpty()) return 0.0

        // T1. Exact substring match — perfect
        if (c.contains(q, ignoreCase = true)) return 1.0

        // Split into words
        val qWords = q.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val cWords = c.split("\\s+".toRegex()).filter { it.isNotBlank() }

        if (cWords.size == 1 && qWords.size == 1) {
            return wordSimilarity(q, c)
        }

        // Multi-word: for each query word, take the best-matching candidate word
        val bestScores = qWords.map { qw ->
            cWords.maxOf { cw -> wordSimilarity(qw, cw) }
        }

        return bestScores.average()
    }

    // --- internals ---

    private fun normalise(s: String): String =
        s.trim().replace(Regex("\\s+"), " ").lowercase()

    /**
     * Score how well a single query-word matches a single candidate-word.
     */
    private fun wordSimilarity(qw: String, cw: String): Double {
        if (qw.isEmpty() || cw.isEmpty()) return 0.0

        // 1. Substring
        if (cw.contains(qw, ignoreCase = true)) return 1.0

        // 2. Subsequence — abbreviations like "amz" in "amazon"
        if (isSubsequence(qw, cw)) {
            // Score based on how dense the match is (query chars / candidate chars)
            val density = qw.length.toDouble() / cw.length.toDouble()
            // Base 0.85, with denser matches getting higher scores
            return (0.85 + density * 0.15).coerceIn(0.0, 1.0)
        }

        // 3. Levenshtein fallback — normalize by query length so short queries
        //    can still match long words whose first few chars are similar
        val distance = levenshteinDistance(qw, cw)
        val penalty = distance.toDouble() / qw.length.toDouble()
        return (1.0 - penalty).coerceIn(0.0, 1.0)
    }

    /**
     * Returns true if all chars of [query] appear in [text] in order
     * (not necessarily contiguously). Case-sensitive comparison assumed.
     */
    private fun isSubsequence(query: String, text: String): Boolean {
        var qi = 0
        var ti = 0
        while (qi < query.length && ti < text.length) {
            if (query[qi] == text[ti]) qi++
            ti++
        }
        return qi == query.length
    }

    /**
     * Normalised Levenshtein similarity for tests/external use.
     * Uses the same scoring as wordSimilarity's fallback path.
     */
    internal fun levenshteinSimilarity(a: String, b: String): Double {
        return wordSimilarity(a.lowercase(), b.lowercase())
    }

    /**
     * Levenshtein (edit) distance — single-row DP.
     */
    private fun levenshteinDistance(a: String, b: String): Int {
        val aLen = a.length
        val bLen = b.length
        if (aLen == 0) return bLen
        if (bLen == 0) return aLen

        var prevRow = IntArray(bLen + 1) { it }
        var currRow = IntArray(bLen + 1)

        for (i in 1..aLen) {
            currRow[0] = i
            for (j in 1..bLen) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                currRow[j] = minOf(
                    currRow[j - 1] + 1,   // insertion
                    prevRow[j] + 1,       // deletion
                    prevRow[j - 1] + cost // substitution
                )
            }
            val tmp = prevRow
            prevRow = currRow
            currRow = tmp
        }
        return prevRow[bLen]
    }
}