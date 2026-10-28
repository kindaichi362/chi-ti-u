package com.example.dragon.data

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.dragon.domain.CareRules
import com.example.dragon.domain.LevelCurve
import com.example.dragon.domain.MissionEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A reward that was actually written (duplicates return null instead). */
data class Grant(val title: String, val xp: Int, val coins: Int, val levelBefore: Int, val levelAfter: Int) {
    val leveledUp get() = levelAfter > levelBefore
}

/**
 * All dragon writes. Kept apart from FinanceRepository on purpose: nothing here reads or writes a
 * finance table, it only reads finance data that the caller passes in.
 */
class DragonRepository(private val database: AppDatabase) {
    private val dao = database.dragonDao()

    val pet: Flow<DragonPet> = dao.pet().map { it ?: DragonPet() }
    val totalXp: Flow<Long> = dao.totalXp()
    val coins: Flow<Long> = dao.coins()
    val history: Flow<List<DragonLedgerEntry>> = dao.history()
    /** Every reward key written so far (a few per day — small even after years). */
    val grantedKeys: Flow<Set<String>> = dao.keysSince(0).map { it.toSet() }

    suspend fun ensurePet() {
        if (dao.petOnce() == null) dao.savePet(DragonPet())
    }

    /** Writes the reward once per [key]; level is compared inside the same transaction. */
    suspend fun grant(key: String, kind: String, title: String, xp: Int, coins: Int, now: Long = System.currentTimeMillis()): Grant? =
        database.withTransaction {
            val before = LevelCurve.progress(dao.totalXpOnce()).level
            if (dao.insertLedger(DragonLedgerEntry(key, kind, title, xp, coins, now)) == -1L) return@withTransaction null
            Grant(title, xp, coins, before, LevelCurve.progress(dao.totalXpOnce()).level)
        }

    /** Grants every automatic mission whose condition holds. Safe to call as often as data changes. */
    suspend fun reconcile(input: MissionEngine.Input): List<Grant> {
        val granted = dao.keysWithPrefix("").toSet()
        return MissionEngine.evaluate(input, granted)
            .filter { it.state == MissionEngine.State.READY }
            .mapNotNull { grant(it.key, LedgerKind.MISSION, it.title, it.xp, it.coins, input.now) }
    }

    /** Manual missions ("Chốt sổ") — only if the engine says the condition holds right now. */
    suspend fun claim(input: MissionEngine.Input, key: String): Grant? {
        val granted = dao.keysWithPrefix("").toSet()
        val mission = MissionEngine.evaluate(input, granted).firstOrNull { it.key == key && it.state == MissionEngine.State.CLAIMABLE }
            ?: return null
        return grant(mission.key, LedgerKind.MISSION, mission.title, mission.xp, mission.coins, input.now)
    }

    suspend fun reportViewed(now: Long = System.currentTimeMillis()): Grant? =
        grant("report:${MissionEngine.weekKey(now)}", LedgerKind.MISSION, "Xem báo cáo tuần này", 20, 10, now)

    /** Applies the care action; returns the care XP grant if today's cap is not reached yet. */
    suspend fun care(action: CareRules.Action, now: Long = System.currentTimeMillis()): Grant? {
        val day = MissionEngine.dayKey(now)
        val careKeys = database.withTransaction {
            val pet = dao.petOnce() ?: DragonPet(statsAt = now)
            dao.savePet(CareRules.apply(pet, action, now))
            dao.keysWithPrefix("care:$day:")
        }
        if (careKeys.size >= CareRules.CARE_XP_PER_DAY) return null
        return grant("care:$day:${careKeys.size + 1}", LedgerKind.CARE, action.title, 1, 0, now)
    }

    suspend fun rename(name: String) {
        val clean = name.trim().take(20)
        require(clean.isNotEmpty())
        database.withTransaction { dao.savePet((dao.petOnce() ?: DragonPet()).copy(name = clean)) }
    }
}
