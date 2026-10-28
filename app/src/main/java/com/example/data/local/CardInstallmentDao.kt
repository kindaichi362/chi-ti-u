package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CardInstallment
import kotlinx.coroutines.flow.Flow

@Dao
interface CardInstallmentDao {
    @Query("SELECT * FROM card_installments WHERE cardId = :cardId ORDER BY firstDueDate, id")
    fun forCard(cardId: Long): Flow<List<CardInstallment>>

    @Query("SELECT * FROM card_installments WHERE id = :id")
    suspend fun byId(id: Long): CardInstallment?

    @Query("SELECT COUNT(*) FROM card_installments WHERE cardId = :cardId")
    suspend fun countForCard(cardId: Long): Int

    @Insert suspend fun insert(item: CardInstallment): Long
    @Update suspend fun update(item: CardInstallment)
    @Delete suspend fun delete(item: CardInstallment)
}
