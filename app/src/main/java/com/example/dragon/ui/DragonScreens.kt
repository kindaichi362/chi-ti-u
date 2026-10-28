package com.example.dragon.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dragon.data.DragonLedgerEntry
import com.example.dragon.data.Grant
import com.example.dragon.domain.CareRules
import com.example.dragon.domain.LevelCurve
import com.example.dragon.domain.MissionEngine
import com.example.dragon.render.DragonFace
import com.example.dragon.render.DragonView
import com.example.dragon.render.dragonStageArt
import com.example.dragon.render.dragonThumb
import com.example.ui.components.AppCard
import com.example.ui.components.SegmentedTabs
import com.example.ui.components.ThresholdBar
import com.example.ui.theme.AppPalette
import com.example.ui.theme.Radius
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Gold = Color(0xFFE0A526)
private val GoldSoft = Color(0xFFFFF1CC)

private fun fmt(n: Long) = String.format(Locale.US, "%,d", n).replace(',', '.')

// ---------------------------------------------------------------------------------------------
// Overview card
// ---------------------------------------------------------------------------------------------

/** The dragon on the Overview tab: picture, level, XP, streak and one line of speech. Tap to enter Dragon World. */
@Composable
fun DragonCard(state: DragonState, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val p = AppPalette
    val prog = state.progress
    AppCard(modifier = modifier.testTag("dragon_card"), onClick = onOpen, contentPadding = Space.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(84.dp).clip(Radius.lg)
                    .background(Brush.verticalGradient(listOf(p.accentSoft, p.surface2))),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(dragonThumb(state.stage.number)), contentDescription = state.pet.name, modifier = Modifier.fillMaxSize().padding(4.dp))
            }
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.pet.name, fontWeight = FontWeight.Bold, fontSize = TextSize.base, color = p.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(6.dp))
                    LevelBadge(prog.level)
                    if (state.streak > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text("🔥 ${state.streak}", fontSize = TextSize.xs, fontWeight = FontWeight.SemiBold, color = p.textSecondary)
                    }
                }
                Spacer(Modifier.height(6.dp))
                ThresholdBar(prog.fraction.toDouble(), color = p.accent, height = 6.dp)
                Spacer(Modifier.height(6.dp))
                Text(state.mood, fontSize = TextSize.xs, color = p.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
            }
            if (state.claimable > 0) {
                Spacer(Modifier.width(Space.sm))
                Box(Modifier.size(22.dp).clip(CircleShape).background(p.expense), contentAlignment = Alignment.Center) {
                    Text("${state.claimable}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LevelBadge(level: Int) {
    Text(
        "Lv.$level", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A5300),
        modifier = Modifier.clip(Radius.pill).background(GoldSoft).padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

// ---------------------------------------------------------------------------------------------
// Reward toast
// ---------------------------------------------------------------------------------------------

/** Small banner "+20 XP · +5 xu" for 2 seconds; level-ups get a bigger line. Never blocks input. */
@Composable
fun RewardToast(vm: DragonViewModel, modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<Grant?>(null) }
    LaunchedEffect(vm) {
        vm.events.collect { g ->
            current = g
            delay(if (g.leveledUp) 3200 else 2200)
            current = null
            delay(250)
        }
    }
    AnimatedVisibility(current != null, modifier = modifier, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
        val g = current ?: return@AnimatedVisibility
        Row(
            Modifier.statusBarsPadding().padding(top = Space.sm).clip(Radius.pill)
                .background(Color(0xFF1E2B25)).padding(horizontal = Space.lg, vertical = 10.dp)
                .testTag("reward_toast"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (g.leveledUp) "🎉" else "🐉", fontSize = 18.sp)
            Spacer(Modifier.width(Space.sm))
            Column {
                Text(
                    if (g.leveledUp) "Mộc Mộc lên cấp ${g.levelAfter}!" else g.title,
                    color = Color.White, fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString { append("+${g.xp} XP"); if (g.coins != 0) append(" · +${g.coins} xu") },
                    color = Color(0xFFFFD873), fontSize = TextSize.xs, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Dragon World
// ---------------------------------------------------------------------------------------------

private enum class WorldTab(val title: String, val emoji: String) {
    CARE("Chăm sóc", "💚"), MISSIONS("Nhiệm vụ", "📜"), HOUSE("Nhà", "🏠"), SHOP("Cửa hàng", "🛍️"), BAG("Kho", "🎒")
}

@Composable
fun ScreenDragonWorld(vm: DragonViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val p = AppPalette
    val state by vm.state.collectAsState()
    val clip by vm.clip.collectAsState()
    var tab by rememberSaveable { mutableStateOf(WorldTab.CARE) }
    var showProfile by remember { mutableStateOf(false) }
    val prog = state.progress

    Column(modifier.fillMaxSize().background(p.background).statusBarsPadding().testTag("dragon_world")) {
        // Top bar: back · name + level + XP · coins
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.sm, vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = p.textPrimary) }
            Column(Modifier.weight(1f).clip(Radius.md).clickable { showProfile = true }.padding(horizontal = 4.dp, vertical = 2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.pet.name, fontWeight = FontWeight.Bold, fontSize = TextSize.lg, color = p.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(6.dp))
                    LevelBadge(prog.level)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ThresholdBar(prog.fraction.toDouble(), color = p.accent, height = 5.dp, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    Text(if (prog.isMax) "Tối đa" else "${fmt(prog.xpInLevel)}/${fmt(prog.xpNeeded)} XP", fontSize = 11.sp, color = p.textSecondary)
                }
            }
            Spacer(Modifier.width(Space.sm))
            Row(
                Modifier.clip(Radius.pill).background(GoldSoft).padding(horizontal = 10.dp, vertical = 6.dp).testTag("dragon_coins"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoinDot()
                Spacer(Modifier.width(4.dp))
                Text(fmt(state.coins), fontWeight = FontWeight.Bold, fontSize = TextSize.sm, color = Color(0xFF7A5300))
            }
            Spacer(Modifier.width(Space.sm))
        }

        // Room + dragon
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = Space.page).clip(Radius.xl).background(roomBrush(p.isDark))) {
            // Floor
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.3f).background(if (p.isDark) Color(0xFF2A2620) else Color(0xFFEADFCB)))
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp).size(width = 170.dp, height = 26.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.10f)))
            DragonView(
                stage = state.stage.number, clip = clip, onTap = vm::touch,
                // Older stages stand taller in the room, so growth is visible.
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.56f + 0.045f * state.stage.number).padding(bottom = 22.dp)
            )
            SpeechBubble(state.mood, moodFace(state), Modifier.align(Alignment.TopStart).padding(Space.md))
        }

        // Panel
        Column(Modifier.fillMaxWidth().heightIn(min = 250.dp).height(290.dp).padding(horizontal = Space.page)) {
            Spacer(Modifier.height(Space.md))
            when (tab) {
                WorldTab.CARE -> CarePanel(state, vm::care)
                WorldTab.MISSIONS -> MissionsPanel(state, vm)
                WorldTab.HOUSE -> ComingSoon("🏠", "Nhà của Mộc Mộc", "Phòng 3D, kéo thả đồ nội thất theo lưới, mở rộng nhà theo cấp. Đang làm ở giai đoạn 3 — xu bạn kiếm bây giờ vẫn được giữ nguyên để dùng sau.")
                WorldTab.SHOP -> ComingSoon("🛍️", "Cửa hàng", "Đồ ăn, nội thất, trang phục mua bằng xu (không bao giờ bằng tiền thật). Đang làm ở giai đoạn 3.")
                WorldTab.BAG -> ComingSoon("🎒", "Kho đồ", "Nơi chứa đồ đã mua. Mở cùng Cửa hàng ở giai đoạn 3.")
            }
        }

        // Bottom bar
        Row(
            Modifier.fillMaxWidth().background(p.card).border(1.dp, p.borderSubtle).navigationBarsPadding().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WorldTab.entries.forEach { t ->
                val selected = t == tab
                Column(
                    Modifier.weight(1f).clip(Radius.md).clickable { tab = t }.padding(vertical = 6.dp).testTag("dragon_tab_${t.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        Text(t.emoji, fontSize = 20.sp, modifier = Modifier.alpha(if (selected) 1f else 0.55f))
                        if (t == WorldTab.MISSIONS && state.claimable > 0) {
                            Box(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-2).dp).size(9.dp).clip(CircleShape).background(p.expense))
                        }
                    }
                    Text(t.title, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) p.accent else p.textSecondary, maxLines = 1)
                }
            }
        }
    }

    if (showProfile) DragonProfileDialog(state, onRename = vm::rename, onDismiss = { showProfile = false })
}

private fun roomBrush(dark: Boolean) = if (dark) Brush.verticalGradient(listOf(Color(0xFF1F2C33), Color(0xFF263A35)))
else Brush.verticalGradient(listOf(Color(0xFFDDF0F6), Color(0xFFF3F8EC)))

@Composable
private fun SpeechBubble(text: String, face: DragonFace, modifier: Modifier = Modifier) {
    val p = AppPalette
    Row(
        modifier.widthIn(max = 260.dp).clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)).background(p.card.copy(alpha = 0.94f)).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(face.res), contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = TextSize.xs, color = p.textPrimary, lineHeight = 16.sp, modifier = Modifier.padding(end = 6.dp))
    }
}

/** Face from the character sheet that matches what the dragon is saying. */
fun moodFace(state: DragonState): DragonFace = when {
    state.pet.energy < 30 -> DragonFace.SAD
    !state.recordedToday -> DragonFace.CUTE
    state.streak >= 7 -> DragonFace.LAUGH
    state.pet.happiness < 40 -> DragonFace.SAD
    else -> DragonFace.HAPPY
}

@Composable
private fun StatBar(label: String, emoji: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    val p = AppPalette
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$emoji $label", fontSize = TextSize.xs, color = p.textSecondary, modifier = Modifier.weight(1f), maxLines = 1)
            Text("${value.toInt()}", fontSize = TextSize.xs, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
        }
        Spacer(Modifier.height(4.dp))
        ThresholdBar(value / 100.0, color = color, height = 6.dp)
    }
}

@Composable
private fun CarePanel(state: DragonState, onCare: (CareRules.Action) -> Unit) {
    val p = AppPalette
    Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        StatBar("Năng lượng", "⚡", state.pet.energy, Color(0xFFF2B33D), Modifier.weight(1f))
        StatBar("Vui vẻ", "😊", state.pet.happiness, Color(0xFF5FC9A6), Modifier.weight(1f))
        StatBar("Gắn bó", "💞", state.pet.bond, Color(0xFFF08AA5), Modifier.weight(1f))
    }
    Spacer(Modifier.height(Space.lg))
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        CareRules.Action.entries.forEach { a ->
            Column(
                Modifier.weight(1f).clip(Radius.lg).background(p.card).border(1.dp, p.border, Radius.lg)
                    .clickable { onCare(a) }.padding(vertical = Space.md).testTag("care_${a.name.lowercase()}"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(a.emoji, fontSize = 26.sp)
                Spacer(Modifier.height(4.dp))
                Text(a.title, fontSize = TextSize.xs, fontWeight = FontWeight.SemiBold, color = p.textPrimary, maxLines = 1)
            }
        }
    }
    Spacer(Modifier.height(Space.md))
    Text(
        "Chạm vào Mộc Mộc để vuốt ve. Chăm sóc cho tối đa ${CareRules.CARE_XP_PER_DAY} XP mỗi ngày — muốn lớn nhanh hãy làm nhiệm vụ ghi chép.",
        fontSize = TextSize.xs, color = p.textTertiary, lineHeight = 16.sp
    )
}

@Composable
private fun MissionsPanel(state: DragonState, vm: DragonViewModel) {
    var sub by rememberSaveable { mutableStateOf(0) }
    val history by vm.history.collectAsState()
    SegmentedTabs(listOf("Nhiệm vụ", "Lịch sử thưởng"), sub, { sub = it })
    Spacer(Modifier.height(Space.sm))
    if (sub == 0) {
        val order = listOf(MissionEngine.State.CLAIMABLE, MissionEngine.State.READY, MissionEngine.State.IN_PROGRESS, MissionEngine.State.DONE)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Space.sm), contentPadding = PaddingValues(bottom = Space.md)) {
            items(state.missions.sortedBy { order.indexOf(it.state) }, key = { it.key }) { m -> MissionRow(m) { vm.claim(m.key) } }
        }
    } else {
        HistoryList(history)
    }
}

@Composable
private fun MissionRow(m: MissionEngine.Mission, onClaim: () -> Unit) {
    val p = AppPalette
    val done = m.state == MissionEngine.State.DONE
    AppCard(contentPadding = Space.md, modifier = Modifier.alpha(if (done) 0.6f else 1f).testTag("mission_${m.key.substringBefore(':')}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.period, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = p.accent,
                        modifier = Modifier.clip(Radius.pill).background(p.accentSoft).padding(horizontal = 6.dp, vertical = 1.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(m.title, fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = p.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(2.dp))
                Text(m.detail, fontSize = TextSize.xs, color = p.textSecondary, maxLines = 2, lineHeight = 15.sp)
                if (!done && m.progress != null && m.state == MissionEngine.State.IN_PROGRESS) {
                    Spacer(Modifier.height(6.dp))
                    ThresholdBar(m.progress.toDouble(), color = p.accent, height = 4.dp)
                }
            }
            Spacer(Modifier.width(Space.sm))
            Column(horizontalAlignment = Alignment.End) {
                Text("+${m.xp} XP", fontSize = TextSize.xs, fontWeight = FontWeight.Bold, color = p.accent)
                Text("+${m.coins} xu", fontSize = TextSize.xs, color = Color(0xFF9A6B00))
                Spacer(Modifier.height(4.dp))
                when (m.state) {
                    MissionEngine.State.CLAIMABLE -> Text(
                        "Nhận", color = p.onAccent, fontSize = TextSize.xs, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(Radius.pill).background(p.accent).clickable(onClick = onClaim).padding(horizontal = 12.dp, vertical = 6.dp).testTag("claim_${m.key.substringBefore(':')}")
                    )
                    MissionEngine.State.DONE -> Text("✓ Xong", fontSize = TextSize.xs, color = p.income, fontWeight = FontWeight.SemiBold)
                    MissionEngine.State.READY -> Text("Đang cộng…", fontSize = TextSize.xs, color = p.textTertiary)
                    MissionEngine.State.IN_PROGRESS -> Unit
                }
            }
        }
    }
}

@Composable
private fun HistoryList(history: List<DragonLedgerEntry>) {
    val p = AppPalette
    if (history.isEmpty()) {
        Text("Chưa có phần thưởng nào. Ghi một khoản thu chi để Mộc Mộc nhận XP đầu tiên!", fontSize = TextSize.sm, color = p.textSecondary, modifier = Modifier.padding(Space.md))
        return
    }
    val df = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    LazyColumn(contentPadding = PaddingValues(bottom = Space.md)) {
        items(history, key = { it.id }) { e ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.title, fontSize = TextSize.sm, color = p.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(df.format(Date(e.createdAt)), fontSize = 11.sp, color = p.textTertiary)
                }
                Text(
                    buildString { if (e.xp != 0) append("+${e.xp} XP"); if (e.coins != 0) { if (isNotEmpty()) append(" · "); append("${if (e.coins > 0) "+" else ""}${e.coins} xu") } },
                    fontSize = TextSize.xs, fontWeight = FontWeight.SemiBold, color = p.accent
                )
            }
        }
    }
}

@Composable
private fun ComingSoon(emoji: String, title: String, text: String) {
    val p = AppPalette
    Column(Modifier.fillMaxWidth().padding(top = Space.lg), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 36.sp)
        Spacer(Modifier.height(Space.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = TextSize.base, color = p.textPrimary)
            Spacer(Modifier.width(6.dp))
            Text("Sắp mở", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = p.textSecondary,
                modifier = Modifier.clip(Radius.pill).background(p.surface2).padding(horizontal = 8.dp, vertical = 2.dp))
        }
        Spacer(Modifier.height(Space.sm))
        Text(text, fontSize = TextSize.sm, color = p.textSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp)
    }
}

// ---------------------------------------------------------------------------------------------
// Profile: rename + stage gallery
// ---------------------------------------------------------------------------------------------

@Composable
private fun DragonProfileDialog(state: DragonState, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    val p = AppPalette
    var name by remember { mutableStateOf(state.pet.name) }
    val level = state.progress.level
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hồ sơ rồng") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(20) }, singleLine = true, label = { Text("Tên") },
                    trailingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Space.md))
                Text("Cấp $level · ${state.stage.title} · tổng ${fmt(state.totalXp)} XP", fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                Text(state.stage.blurb, fontSize = TextSize.xs, color = p.textSecondary)
                Spacer(Modifier.height(Space.md))
                Text("Các giai đoạn", fontSize = TextSize.xs, color = p.textSecondary)
                Spacer(Modifier.height(Space.xs))
                BoxWithConstraints {
                    val cell = (maxWidth - 10.dp) / 3
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        LevelCurve.Stage.entries.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                row.forEach { s ->
                                    val unlocked = level >= s.levels.first
                                    Column(Modifier.width(cell).clip(Radius.md).background(p.surface2).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Image(
                                            painterResource(dragonStageArt(s.number)), contentDescription = s.title,
                                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).alpha(if (unlocked) 1f else 0.35f),
                                            colorFilter = if (unlocked) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                                        )
                                        Text(s.title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = p.textPrimary, maxLines = 1)
                                        Text("Lv.${s.levels.first}–${s.levels.last}", fontSize = 9.sp, color = p.textTertiary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank() && name.trim() != state.pet.name) onRename(name); onDismiss() }) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}

/** Drawn coin: the 🪙 emoji is missing from fonts before Android 11. */
@Composable
private fun CoinDot() {
    Box(
        Modifier.size(16.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(Color(0xFFFFD86B), Gold)))
            .border(1.dp, Color(0xFFB9821A), CircleShape),
        contentAlignment = Alignment.Center
    ) { Text("x", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF7A5300)) }
}
