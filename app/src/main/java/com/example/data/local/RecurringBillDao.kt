package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RecurringBill
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringBillDao {
    @Query("SELECT * FROM recurring_bills WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveBills(): Flow<List<RecurringBill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: RecurringBill): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<RecurringBill>)

    @Update
    suspend fun updateBill(bill: RecurringBill)

    @Delete
    suspend fun deleteBill(bill: RecurringBill)
}
