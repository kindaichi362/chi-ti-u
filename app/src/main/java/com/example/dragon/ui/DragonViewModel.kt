package com.example.dragon.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.TransactionType
import com.example.data.model.countsInReports
import com.example.data.repository.FinanceRepository
import com.example.dragon.data.DragonPet
import com.example.dragon.data.DragonRepository
import com.example.dragon.data.Grant
import com.example.dragon.domain.CareRules
import com.example.dragon.domain.LevelCurve
import com.example.dragon.domain.MissionEngine
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DragonState(
    val pet: DragonPet = DragonPet(),
    val totalXp: Long = 0,
    val coins: Long = 0,
    val missions: List<MissionEngine.Mission> = emptyList(),
    val streak: Int = 0,
    val recordedToday: Boolean = false,
    val loaded: Boolean = false
) {
    val progress: LevelCurve.Progress get() = LevelCurve.progress(totalXp)
    val stage: LevelCurve.Stage get() = progress.stage
    val mood: String get() = CareRules.mood(pet, recordedToday, streak)
    val claimable: Int get() = missions.count { it.state == MissionEngine.State.CLAIMABLE }
}

/** One-shot animation request; [nonce] makes the same clip replay when asked twice. */
data class ClipRequest(val clip: String, val nonce: Long = System.nanoTime())

/**
 * Dragon Home state. Reads finance data only through FinanceRepository's flows and never writes it.
 * Missions are reconciled whenever that data changes (and once a minute for day rollover), so a
 * reward lands right after a transaction is saved, on app open, and after a restore — without
 * touching the finance code paths.
 */
@OptIn(FlowPreview::class)
class DragonViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val finance = FinanceRepository(database)
    private val repo = DragonRepository(database)

    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    private val input = combine(finance.allTransactions, finance.allBudgets, finance.allGoals, clock) { t, b, g, now ->
        MissionEngine.Input(t, b, g, now)
    }

    private val _events = MutableSharedFlow<Grant>(extraBufferCapacity = 16)
    /** Rewards just written — shown as a small toast wherever the user is. */
    val events: SharedFlow<Grant> = _events.asSharedFlow()

    private val _clip = MutableStateFlow(ClipRequest("idle", 0))
    val clip: StateFlow<ClipRequest> = _clip.asStateFlow()

    val state: StateFlow<DragonState> = combine(repo.pet, repo.totalXp, repo.coins, repo.grantedKeys, input) { pet, xp, coins, keys, inp ->
        val today = MissionEngine.dayKey(inp.now)
        DragonState(
            pet = CareRules.decayed(pet, inp.now),
            totalXp = xp,
            coins = coins,
            missions = MissionEngine.evaluate(inp, keys),
            streak = MissionEngine.currentStreak(inp.transactions, inp.now),
            recordedToday = inp.transactions.any { it.countsInReports && it.type != TransactionType.ADJUSTMENT && MissionEngine.dayKey(it.timestamp) == today },
            loaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DragonState())

    init {
        viewModelScope.launch { repo.ensurePet() }
        input.debounce(400).onEach { inp -> repo.reconcile(inp).forEach { emit(it) } }.launchIn(viewModelScope)
    }

    private suspend fun emit(grant: Grant) {
        _events.emit(grant)
        if (grant.leveledUp) _clip.value = ClipRequest("levelup") else if (grant.xp >= 20) _clip.value = ClipRequest("happy")
    }

    fun care(action: CareRules.Action) {
        _clip.value = ClipRequest(action.clip)
        viewModelScope.launch { repo.care(action)?.let { emit(it) } }
    }

    fun touch() = care(CareRules.Action.PET)

    fun claim(key: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val inp = MissionEngine.Input(finance.allTransactions.first(), finance.allBudgets.first(), finance.allGoals.first(), now)
            repo.claim(inp, key)?.let { emit(it) }
        }
    }

    fun reportViewed() {
        viewModelScope.launch { repo.reportViewed()?.let { emit(it) } }
    }

    fun rename(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.rename(name) }
    }

    val history = repo.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
