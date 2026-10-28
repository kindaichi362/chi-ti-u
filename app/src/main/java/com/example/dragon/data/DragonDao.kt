package com.example.dragon.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DragonDao {
    @Query("SELECT * FROM dragon_pet WHERE id = 'local' LIMIT 1")
    fun pet(): Flow<DragonPet?>

    @Query("SELECT * FROM dragon_pet WHERE id = 'local' LIMIT 1")
    suspend fun petOnce(): DragonPet?

    @Upsert
    suspend fun savePet(pet: DragonPet)

    /** Returns -1 when the key already exists, i.e. the reward was granted before. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLedger(entry: DragonLedgerEntry): Long

    @Query("SELECT COALESCE(SUM(xp), 0) FROM dragon_ledger")
    fun totalXp(): Flow<Long>

    @Query("SELECT COALESCE(SUM(xp), 0) FROM dragon_ledger")
    suspend fun totalXpOnce(): Long

    @Query("SELECT COALESCE(SUM(coins), 0) FROM dragon_ledger")
    fun coins(): Flow<Long>

    @Query("SELECT COALESCE(SUM(coins), 0) FROM dragon_ledger")
    suspend fun coinsOnce(): Long

    @Query("SELECT id FROM dragon_ledger WHERE id LIKE :prefix || '%'")
    suspend fun keysWithPrefix(prefix: String): List<String>

    @Query("SELECT id FROM dragon_ledger WHERE createdAt >= :since")
    fun keysSince(since: Long): Flow<List<String>>

    @Query("SELECT * FROM dragon_ledger ORDER BY createdAt DESC LIMIT :limit")
    fun history(limit: Int = 100): Flow<List<DragonLedgerEntry>>
}
