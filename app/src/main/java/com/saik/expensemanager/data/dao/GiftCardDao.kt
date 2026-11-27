package com.saik.expensemanager.data.dao

import androidx.room.*
import com.saik.expensemanager.data.model.GiftCard
import kotlinx.coroutines.flow.Flow

@Dao
interface GiftCardDao {
    @Insert suspend fun insert(card: GiftCard): Long
    @Update suspend fun update(card: GiftCard)
    @Delete suspend fun delete(card: GiftCard)

    @Query("SELECT * FROM gift_cards ORDER BY purchaseDate DESC")
    fun getAll(): Flow<List<GiftCard>>

    @Query("SELECT * FROM gift_cards WHERE id = :id")
    suspend fun getById(id: Long): GiftCard?

    @Query("SELECT * FROM gift_cards WHERE linkedAccountId = :accountId LIMIT 1")
    suspend fun getByAccountId(accountId: Long): GiftCard?

    @Query("UPDATE gift_cards SET isUsed = 1, usedDate = :date WHERE id = :id")
    suspend fun markUsed(id: Long, date: Long)

    @Query("UPDATE gift_cards SET isUsed = 0, usedDate = NULL WHERE id = :id")
    suspend fun markUnused(id: Long)

    @Query("SELECT * FROM gift_cards WHERE entryTransactionId = :txId")
    suspend fun getByEntryTransactionId(txId: Long): List<GiftCard>

    @Query("DELETE FROM gift_cards WHERE entryTransactionId = :txId")
    suspend fun deleteByEntryTransactionId(txId: Long)
}
