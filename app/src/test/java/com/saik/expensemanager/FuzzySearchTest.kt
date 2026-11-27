package com.saik.expensemanager

import com.saik.expensemanager.util.FuzzySearch
import org.junit.Assert.*
import org.junit.Test

class FuzzySearchTest {

    @Test
    fun `amz matches amazon`() {
        val score = FuzzySearch.similarity("amz", "amazon")
        println("amz vs amazon: score=$score, threshold=${FuzzySearch.DEFAULT_THRESHOLD}")
        assertTrue("amz should match amazon", score >= FuzzySearch.DEFAULT_THRESHOLD)
    }

    @Test
    fun `amz matches full candidate`() {
        val score = FuzzySearch.similarity("amz", "amazon pay wallet load Wallet Load")
        println("amz vs 'amazon pay wallet load Wallet Load': score=$score")
        assertTrue("amz should match full candidate", score >= FuzzySearch.DEFAULT_THRESHOLD)
    }

    @Test
    fun `ama matches amazon`() {
        val score = FuzzySearch.similarity("ama", "amazon")
        println("ama vs amazon: score=$score")
        assertTrue("ama should match", score >= FuzzySearch.DEFAULT_THRESHOLD)
    }

    @Test
    fun `wal matches wallet`() {
        val score = FuzzySearch.similarity("wal", "wallet")
        println("wal vs wallet: score=$score")
        assertTrue("wal should match wallet (subsequence)", score >= FuzzySearch.DEFAULT_THRESHOLD)
    }

    @Test
    fun `amz does NOT match xyz`() {
        val score = FuzzySearch.similarity("amz", "xyz")
        println("amz vs xyz: score=$score")
        assertTrue("amz should NOT match xyz", score < FuzzySearch.DEFAULT_THRESHOLD)
    }

    @Test
    fun `substring matches directly`() {
        assertEquals(1.0, FuzzySearch.similarity("ama", "amazon"), 0.001)
    }

    @Test
    fun `exact word match is 1_0`() {
        assertEquals(1.0, FuzzySearch.similarity("wallet", "wallet"), 0.001)
    }

    @Test
    fun `typo close match`() {
        val score = FuzzySearch.similarity("walle", "wallet")
        println("walle vs wallet: score=$score")
        // "walle" is not a subsequence of "wallet" (no 'e' after 'l' in wallet)
        // but distance(wordSimilarity("walle","wamet")) should be in Levenshtein mode
        // distance=2 (insert 't', or sub 'e'→'t' + insert), penalty=2/5=0.4, score=0.6
        assertTrue("Should be a decent match", score >= 0.55)
    }

    @Test
    fun `empty query matches nothing`() {
        assertEquals(0.0, FuzzySearch.similarity("", "amazon"), 0.0)
    }

    @Test
    fun `empty candidate matches nothing`() {
        assertEquals(0.0, FuzzySearch.similarity("amz", ""), 0.0)
    }
}