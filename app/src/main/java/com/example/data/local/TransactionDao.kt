package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE debtId = :debtId ORDER BY timestamp DESC")
    fun getTransactionsForDebt(debtId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE debtId = :debtId ORDER BY timestamp DESC")
    suspend fun getTransactionsForDebtOnce(debtId: Long): List<Transaction>
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM transactions WHERE sourceAccountId = :accountId OR destinationAccountId = :accountId")
    suspend fun countTransactionsForAccount(accountId: Long): Int

    @Query("SELECT * FROM transactions WHERE sourceAccountId = :accountId OR destinationAccountId = :accountId ORDER BY timestamp DESC")
    suspend fun getTransactionsForAccountOnce(accountId: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE sourceAccountId = :accountId OR destinationAccountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
}
