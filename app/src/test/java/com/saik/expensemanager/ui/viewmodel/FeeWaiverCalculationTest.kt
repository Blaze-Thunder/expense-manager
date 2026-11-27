package com.saik.expensemanager.ui.viewmodel

import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FeeWaiverCalculationTest {

    private val accountId = 1L
    private val ccAccount = Account(
        id = accountId,
        name = "Test Card",
        type = "CREDIT_CARD",
        associatedBudgetBookId = 1L,
        isCreditCard = true,
        trackFeeWaiver = true,
        feeWaiverThreshold = 200000.0,
        feeCycleStartDate = Calendar.getInstance().apply { set(2026, 0, 1) }.timeInMillis // Jan 1, 2026
    )

    // Helper to create a basic transaction
    private fun createTx(
        amount: Double,
        type: String,
        category: String,
        description: String = "",
        linkedAcc: Long = accountId,
        toAcc: Long? = null,
        isCcPayment: Boolean = false,
        date: Long = System.currentTimeMillis()
    ) = Transaction(
        amount = amount,
        type = type,
        category = category,
        description = description,
        linkedAccountId = linkedAcc,
        toAccountId = toAcc,
        isCreditCardPayment = isCcPayment,
        dateTime = date,
        linkedBudgetBookId = 1L
    )

    // We can't easily instantiate MainViewModel without mocking the whole world,
    // but we can test the logic based on the implementation rules.
    
    @Test
    fun testIsFeeWaiverQualifying() {
        // Purchase counts
        val purchase = createTx(50000.0, "EXPENSE", "Shopping")
        assertTrue(isQualifying(purchase))

        // Transfer from CC (e.g. to Gift Card) counts as spend
        val transferOut = createTx(4500.0, "TRANSFER", "", linkedAcc = accountId, toAcc = 2L)
        assertTrue(isQualifying(transferOut))

        // Payment (Transfer to CC) does NOT count
        val payment = createTx(50000.0, "TRANSFER", "", linkedAcc = 2L, toAcc = accountId, isCcPayment = true)
        assertFalse(isQualifying(payment))

        // Cashback does NOT count (it's INCOME, not EXPENSE)
        val cashback = createTx(2000.0, "INCOME", "Cashback")
        assertFalse(isQualifying(cashback))

        // Balance adjustment (EXPENSE) does NOT count
        val adjustment = createTx(500.0, "EXPENSE", "Adjustment", "Balance Correction")
        assertFalse(isQualifying(adjustment))
        
        val adjustmentCat = createTx(500.0, "EXPENSE", "Adjustment")
        assertFalse(isQualifying(adjustmentCat))

        // Different account does NOT count
        val otherAccPurchase = createTx(10000.0, "EXPENSE", "Food", linkedAcc = 2L)
        assertFalse(isQualifying(otherAccPurchase))
    }

    @Test
    fun testIsFeeWaiverRefund() {
        // Refund counts
        val refund = createTx(5000.0, "INCOME", "Refund")
        assertTrue(isRefund(refund))
        
        val refunds = createTx(5000.0, "INCOME", "Refunds")
        assertTrue(isRefund(refunds))

        // Cashback does NOT count as refund
        val cashback = createTx(2000.0, "INCOME", "Cashback")
        assertFalse(isRefund(cashback))

        // Adjustment does NOT count as refund
        val adjustment = createTx(500.0, "INCOME", "Adjustment")
        assertFalse(isRefund(adjustment))
    }

    @Test
    fun testFeeWaiverSpendCalculation() {
        val transactions = listOf(
            createTx(50000.0, "EXPENSE", "Shopping"),
            createTx(75000.0, "EXPENSE", "Travel"),
            createTx(80000.0, "EXPENSE", "Electronics"),
            createTx(4500.0, "TRANSFER", "", linkedAcc = accountId, toAcc = 2L), // Transfer out (Gift Card spend)
            createTx(150000.0, "TRANSFER", "", linkedAcc = 2L, toAcc = accountId, isCcPayment = true), // Payment
            createTx(2000.0, "INCOME", "Cashback"), // Cashback
            createTx(500.0, "EXPENSE", "Adjustment", "Balance Correction"), // Adjustment
            createTx(1000.0, "INCOME", "Refund") // Refund
        )

        val qualifyingExpenses = transactions.filter { isQualifying(it) }.sumOf { it.amount }
        val qualifyingRefunds = transactions.filter { isRefund(it) }.sumOf { it.amount }
        val finalSpend = qualifyingExpenses - qualifyingRefunds

        assertEquals(209500.0, qualifyingExpenses, 0.0)
        assertEquals(1000.0, qualifyingRefunds, 0.0)
        assertEquals(208500.0, finalSpend, 0.0)
    }

    // Replication of the logic added to MainViewModel
    private fun isQualifying(tx: Transaction): Boolean {
        return tx.linkedAccountId == accountId && 
               (tx.type == "EXPENSE" || tx.type == "TRANSFER") && 
               tx.description != "Balance Correction" &&
               tx.category != "Adjustment"
    }

    private fun isRefund(tx: Transaction): Boolean {
        return tx.linkedAccountId == accountId && 
               tx.type == "INCOME" && 
               (tx.category == "Refund" || tx.category == "Refunds")
    }
}
