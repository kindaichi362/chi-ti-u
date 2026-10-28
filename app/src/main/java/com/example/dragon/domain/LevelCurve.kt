package com.example.dragon.domain

/** XP needed and growth stages. Pure — every number the game shows comes from here. */
object LevelCurve {
    const val MAX_LEVEL = 50

    /** XP to go from [level] to level + 1: 100 + 20(L−1) + 5(L−1)². */
    fun xpToNext(level: Int): Long {
        val n = (level - 1).toLong()
        return 100 + 20 * n + 5 * n * n
    }

    /** Total XP needed to reach [level] from level 1. */
    fun xpForLevel(level: Int): Long = (1 until level.coerceIn(1, MAX_LEVEL)).sumOf { xpToNext(it) }

    data class Progress(val level: Int, val xpInLevel: Long, val xpNeeded: Long) {
        val isMax get() = level >= MAX_LEVEL
        val fraction: Float get() = if (isMax) 1f else (xpInLevel.toFloat() / xpNeeded).coerceIn(0f, 1f)
        val stage: Stage get() = stageOf(level)
    }

    fun progress(totalXp: Long): Progress {
        var level = 1
        var left = totalXp.coerceAtLeast(0)
        while (level < MAX_LEVEL && left >= xpToNext(level)) {
            left -= xpToNext(level)
            level++
        }
        return if (level >= MAX_LEVEL) Progress(MAX_LEVEL, 0, 0) else Progress(level, left, xpToNext(level))
    }

    enum class Stage(val number: Int, val title: String, val levels: IntRange, val blurb: String) {
        NEWBORN(1, "Sơ sinh", 1..5, "Vừa nở khỏi trứng, hay ngủ và thích được vuốt ve."),
        TODDLER(2, "Tập đi", 6..10, "Chập chững tập đi, đeo yếm vàng, cánh nhỏ đã vẫy được."),
        YOUTH(3, "Thiếu niên", 11..20, "Sừng dài hơn, thêm gai lưng, rất ham chơi."),
        ADULT(4, "Trưởng thành", 21..30, "Cánh to khoẻ, đuôi dài, chững chạc hơn."),
        ELITE(5, "Tinh anh", 31..40, "Gai ánh vàng và viên ngọc trước ngực — dấu hiệu của chủ nhân kỷ luật."),
        LEGEND(6, "Huyền thoại", 41..50, "Sừng vàng, vương miện ngọc, cánh phát sáng. Bạn đã giữ thói quen rất lâu.");
    }

    fun stageOf(level: Int): Stage = Stage.entries.first { level <= it.levels.last }
}
