package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<Account>)

    @Update
    suspend fun updateAccount(account: Account)

    @Query("UPDATE accounts SET balance = balance + :amountDelta WHERE id = :id")
    suspend fun adjustAccountBalance(id: Long, amountDelta: Double)

    @Query("UPDATE accounts SET balance = :newBalance WHERE id = :id")
    suspend fun setAccountBalance(id: Long, newBalance: Double)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    @Query("UPDATE accounts SET isArchived = 1 WHERE id = :id")
    suspend fun archiveAccountById(id: Long)
}
