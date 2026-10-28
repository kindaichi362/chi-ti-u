package com.example.dragon.domain

import com.example.dragon.data.DragonPet

/**
 * Energy and happiness drift down while the app is closed but never reach zero — leaving the app
 * for a while makes the dragon sleepy, it never gets sick or loses levels. Bond never decays.
 */
object CareRules {
    const val ENERGY_FLOOR = 15.0
    const val HAPPINESS_FLOOR = 20.0
    private const val ENERGY_PER_HOUR = 3.0
    private const val HAPPINESS_PER_HOUR = 2.0
    /** Care earns 1 XP per action, at most this many per day, so tapping cannot replace real missions. */
    const val CARE_XP_PER_DAY = 5

    enum class Action(val title: String, val emoji: String, val clip: String) {
        FEED("Cho ăn", "🍎", "eat"),
        PET("Vuốt ve", "🤲", "touch"),
        PLAY("Chơi", "🧶", "play"),
        SLEEP("Ru ngủ", "🌙", "sleep")
    }

    /** The pet's stats as they are at [now]. */
    fun decayed(pet: DragonPet, now: Long): DragonPet {
        val hours = ((now - pet.statsAt).coerceAtLeast(0)) / 3_600_000.0
        return pet.copy(
            energy = (pet.energy - hours * ENERGY_PER_HOUR).coerceAtLeast(minOf(pet.energy, ENERGY_FLOOR)),
            happiness = (pet.happiness - hours * HAPPINESS_PER_HOUR).coerceAtLeast(minOf(pet.happiness, HAPPINESS_FLOOR)),
            statsAt = now
        )
    }

    fun apply(pet: DragonPet, action: Action, now: Long): DragonPet {
        val p = decayed(pet, now)
        fun c(v: Double) = v.coerceIn(0.0, 100.0)
        return when (action) {
            Action.FEED -> p.copy(energy = c(p.energy + 20), happiness = c(p.happiness + 5), bond = c(p.bond + 1))
            Action.PET -> p.copy(happiness = c(p.happiness + 10), bond = c(p.bond + 2))
            Action.PLAY -> p.copy(happiness = c(p.happiness + 20), energy = c(maxOf(ENERGY_FLOOR, p.energy - 10)), bond = c(p.bond + 3))
            Action.SLEEP -> p.copy(energy = c(p.energy + 40))
        }
    }

    /** What the dragon says on the Overview card. */
    fun mood(pet: DragonPet, recordedToday: Boolean, streak: Int): String = when {
        pet.energy < 30 -> "Mình buồn ngủ quá… ru mình ngủ chút nhé 😴"
        !recordedToday -> "Hôm nay mình chưa được ăn khoản nào. Ghi một khoản cho mình nhé!"
        streak >= 7 -> "Chuỗi $streak ngày rồi! Mình tự hào về bạn lắm 🔥"
        pet.happiness < 40 -> "Chơi với mình một chút được không?"
        else -> "Hôm nay ghi chép đều quá, mình no căng bụng 😋"
    }
}
