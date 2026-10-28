package com.example.dragon.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The dragon. One row per phone (id "local"); level and coins are not stored here — they are
 * always summed from [DragonLedgerEntry], so a crash between two writes can never make them
 * disagree with the history.
 *
 * Care stats are stored as of [statsAt] and decay lazily when read (see CareRules).
 */
@Entity(tableName = "dragon_pet")
data class DragonPet(
    @PrimaryKey val id: String = LOCAL_ID,
    val name: String = "Mộc Mộc",
    val energy: Double = 80.0,
    val happiness: Double = 80.0,
    val bond: Double = 10.0,
    val statsAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object { const val LOCAL_ID = "local" }
}

/**
 * Append-only record of every XP / coin change. [id] is the idempotency key
 * ("first_tx:2026-10-28", "goal:12", "buy:<uuid>"…): the primary key makes a second grant of the
 * same reward impossible, however many times a screen or a reconcile pass asks for it.
 */
@Entity(tableName = "dragon_ledger", indices = [Index("createdAt")])
data class DragonLedgerEntry(
    @PrimaryKey val id: String,
    val kind: String,
    val title: String,
    val xp: Int,
    val coins: Int,
    val createdAt: Long = System.currentTimeMillis()
)

object LedgerKind {
    const val MISSION = "MISSION"
    const val CARE = "CARE"
    const val PURCHASE = "PURCHASE"
}
