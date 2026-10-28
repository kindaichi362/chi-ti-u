package com.example.dragon.render

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.dragon.ui.ClipRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/*
 * The dragon is the user's character sheet (docs/dragon-home/character-sheet.webp), cut out by
 * tools/dragon-art: one picture per growth stage, action poses and face tiles. Motion is done here
 * with Compose transforms — breathing, hops, wobble, sparkles — so there is no 3D engine to run.
 */

/**
 * Main picture. Young stages use the sharp turnaround the user supplied (front view); later stages
 * use their picture from the character sheet.
 */
@DrawableRes
fun dragonThumb(stage: Int): Int = if (stage <= 3) R.drawable.dragon_ref_front else dragonStageArt(stage)

/** Each stage as drawn on the character sheet — used in the stage gallery, where they must differ. */
@DrawableRes
fun dragonStageArt(stage: Int): Int = when (stage) {
    1 -> R.drawable.dragon_stage1
    2 -> R.drawable.dragon_stage2
    3 -> R.drawable.dragon_stage3
    4 -> R.drawable.dragon_stage4
    5 -> R.drawable.dragon_stage5
    else -> R.drawable.dragon_stage6
}

enum class DragonFace(@DrawableRes val res: Int) {
    NORMAL(R.drawable.dragon_face_normal), HAPPY(R.drawable.dragon_face_happy), LAUGH(R.drawable.dragon_face_laugh),
    WINK(R.drawable.dragon_face_wink), SURPRISED(R.drawable.dragon_face_surprised), SAD(R.drawable.dragon_face_sad),
    ANGRY(R.drawable.dragon_face_angry), CUTE(R.drawable.dragon_face_cute)
}

/**
 * Picture for an action. The sheet draws the action poses with the young dragon, so they are used
 * up to the "Thiếu niên" stage; older stages keep their own picture and act through motion and props.
 */
@DrawableRes
private fun pictureFor(stage: Int, clip: String, look: Int): Int {
    if (stage <= 3) when (clip) {
        "eat" -> return R.drawable.dragon_pose_eat
        "play" -> return R.drawable.dragon_pose_play
        "sleep" -> return R.drawable.dragon_pose_sleep
        "happy", "touch", "levelup" -> return R.drawable.dragon_pose_happy
        "walk" -> return R.drawable.dragon_pose_walk
        "run" -> return R.drawable.dragon_pose_run
        // Idle: now and then turn to look left or right (turnaround views).
        else -> return when (look) { 1 -> R.drawable.dragon_ref_right; 3 -> R.drawable.dragon_ref_left; else -> R.drawable.dragon_ref_front }
    }
    return dragonThumb(stage)
}

private fun durationOf(clip: String): Long = when (clip) {
    "sleep" -> 4500
    "levelup" -> 2600
    "eat", "play" -> 2400
    else -> 1500
}

/** Props drawn next to older stages, whose own picture has no bowl or ball. */
private fun propFor(stage: Int, clip: String): String? = if (stage <= 3) null else when (clip) {
    "eat" -> "🍲"
    "play" -> "⚽"
    else -> null
}

@Composable
fun DragonView(stage: Int, clip: ClipRequest, onTap: () -> Unit, modifier: Modifier = Modifier) {
    // The clip being shown; one-shot clips fall back to idle after their duration.
    var active by remember { mutableStateOf("idle") }
    val hop = remember { Animatable(0f) }
    val tilt = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }
    LaunchedEffect(clip) {
        if (clip.nonce == 0L || clip.clip == "idle") return@LaunchedEffect
        active = clip.clip
        when (clip.clip) {
            "happy", "levelup" -> launch {
                repeat(if (clip.clip == "levelup") 1 else 2) {
                    hop.animateTo(1f, tween(260, easing = FastOutSlowInEasing)); hop.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                }
            }
            "touch" -> launch { for (a in listOf(-6f, 5f, -3f, 0f)) tilt.animateTo(a, tween(110)) }
            "play" -> launch { repeat(3) { hop.animateTo(0.6f, tween(200)); hop.animateTo(0f, tween(220)) } }
            "eat" -> launch { repeat(4) { tilt.animateTo(3f, tween(150)); tilt.animateTo(0f, tween(150)) } }
        }
        if (clip.clip == "levelup") launch { spin.snapTo(0f); spin.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        delay(durationOf(clip.clip))
        active = "idle"
    }

    // 0 front · 1 right · 2 front · 3 left, a step every few seconds while idle.
    var look by remember { mutableStateOf(0) }
    LaunchedEffect(active) {
        look = 0
        if (active != "idle") return@LaunchedEffect
        while (true) {
            delay(if (look % 2 == 0) 5000 else 1600)
            look = (look + 1) % 4
        }
    }
    val loop = rememberInfiniteTransition(label = "loop")
    val breath by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(if (active == "sleep") 2200 else 1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath")
    val sparkle by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "sparkle")

    BoxWithConstraints(
        modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap),
        contentAlignment = Alignment.BottomCenter
    ) {
        val h = maxHeight
        AnimatedContent(
            targetState = pictureFor(stage, active, look),
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
            label = "pose"
        ) { res ->
            Image(
                painterResource(res), contentDescription = "Rồng Mộc Mộc",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    // Breathing: a slight squash and stretch from the feet.
                    scaleY = 1f + breath * (if (active == "sleep") 0.03f else 0.018f)
                    scaleX = 1f - breath * 0.008f
                    translationY = -hop.value * h.toPx() * 0.16f
                    rotationZ = tilt.value
                    rotationY = spin.value * 360f
                }
            )
        }
        propFor(stage, active)?.let { prop ->
            Text(prop, fontSize = 40.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 4.dp))
        }
        if (active == "sleep") {
            Text(
                "💤", fontSize = 30.sp,
                modifier = Modifier.align(Alignment.TopCenter).offset(x = 60.dp, y = (40 - sparkle * 30).dp).alpha(1f - sparkle * 0.6f)
            )
        }
        if (active == "levelup") {
            // Gold sparkles circling the dragon, like the sheet's level-up glow.
            for (i in 0 until 8) {
                val a = (i / 8f + sparkle) * 2 * Math.PI
                Text(
                    "✨", fontSize = (16 + (i % 3) * 6).sp,
                    modifier = Modifier.align(Alignment.Center).offset(x = (cos(a) * 120).dp, y = (sin(a) * 90 - 20).dp)
                )
            }
        }
    }
}
