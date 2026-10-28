package com.example.dragon.render

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.dragon.ui.ClipRequest
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Pre-rendered picture of each stage (same model, rendered offline) — used on the Overview card and as fallback. */
fun dragonThumb(stage: Int): Int = when (stage) {
    1 -> R.drawable.dragon_stage1
    2 -> R.drawable.dragon_stage2
    3 -> R.drawable.dragon_stage3
    4 -> R.drawable.dragon_stage4
    5 -> R.drawable.dragon_stage5
    else -> R.drawable.dragon_stage6
}

private object FilamentLoader {
    /** Null until tried; false on test JVMs or phones where the native libraries cannot load. */
    private var ok: Boolean? = null
    fun available(): Boolean = ok ?: run {
        ok = Build.FINGERPRINT != "robolectric" && try {
            Filament.init(); Gltfio.init(); true
        } catch (t: Throwable) {
            Log.w("DragonView", "3D unavailable, using picture", t); false
        }
        ok!!
    }
}

/**
 * The live 3D dragon on a transparent surface. Plays [clip] once (then returns to idle); idle
 * loops. Rendering stops whenever the screen is paused. Falls back to the stage picture with a
 * gentle Compose bob when 3D cannot start.
 */
@Composable
fun DragonView(stage: Int, clip: ClipRequest, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val tap = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap)
    if (!FilamentLoader.available()) {
        DragonPicture(stage, clip, modifier.then(tap))
        return
    }
    key(stage) { DragonSurface(stage, clip, modifier.then(tap)) }
}

@Composable
private fun DragonSurface(stage: Int, clip: ClipRequest, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val renderer = remember {
        runCatching { DragonRenderer(context, "dragon/mocmoc_stage$stage.glb") }
            .onFailure { Log.w("DragonView", "3D failed to start, using picture", it) }.getOrNull()
    }
    if (renderer == null) {
        DragonPicture(stage, clip, modifier)
        return
    }
    LaunchedEffect(renderer, clip) { renderer.play(clip.clip) }
    DisposableEffect(renderer, lifecycle) {
        val observer = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> renderer.resume()
                Lifecycle.Event.ON_PAUSE -> renderer.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) renderer.resume()
        onDispose {
            lifecycle.removeObserver(observer)
            renderer.destroy()
        }
    }
    AndroidView(factory = { ctx -> TextureView(ctx).also { renderer.attach(it) } }, modifier = modifier)
}

@Composable
fun DragonPicture(stage: Int, clip: ClipRequest, modifier: Modifier = Modifier) {
    val bob by rememberInfiniteTransition(label = "bob").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob"
    )
    val hop = remember { Animatable(0f) }
    LaunchedEffect(clip) {
        if (clip.nonce != 0L && clip.clip != "idle" && clip.clip != "sleep") {
            hop.animateTo(1f, tween(180)); hop.animateTo(0f, tween(320))
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painterResource(dragonThumb(stage)), contentDescription = "Rồng Mộc Mộc",
            modifier = Modifier.fillMaxSize().graphicsLayer {
                translationY = -bob * 10f - hop.value * 40f
                scaleX = 1f + bob * 0.015f; scaleY = 1f - bob * 0.01f + hop.value * 0.05f
            }
        )
    }
}

/** Minimal glTF viewer: one engine per screen, transparent background, 30 fps, node animations. */
private class DragonRenderer(private val context: Context, private val assetPath: String) : Choreographer.FrameCallback {
    private val engine = Engine.create()
    private val renderer: Renderer = engine.createRenderer()
    private val scene: Scene = engine.createScene()
    private val view: View = engine.createView()
    private val cameraEntity = EntityManager.get().create()
    private val camera: Camera = engine.createCamera(cameraEntity)
    private val sun = EntityManager.get().create()
    private val fill = EntityManager.get().create()
    private val indirect: IndirectLight
    private val materials = UbershaderProvider(engine)
    private val loader = AssetLoader(engine, materials, EntityManager.get())
    private val resources = ResourceLoader(engine)
    private var asset: FilamentAsset? = null
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
    private var swapChain: SwapChain? = null
    private val choreographer = Choreographer.getInstance()
    private var running = false
    private var destroyed = false
    private var lastFrame = 0L

    /** Rest pose of every node, restored before each frame so a clip never inherits the previous clip's pose. */
    private var rest: List<Pair<Int, FloatArray>> = emptyList()
    private var clipIndex = 0
    private var idleIndex = 0
    private var clipStart = 0L
    private var oneShot = false

    init {
        view.scene = scene
        view.camera = camera
        view.blendMode = View.BlendMode.TRANSLUCENT
        renderer.clearOptions = renderer.clearOptions.apply { clear = true; clearColor = doubleArrayOf(0.0, 0.0, 0.0, 0.0) }
        camera.setExposure(16f, 1f / 125f, 100f)

        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1f, 0.96f, 0.9f).intensity(90_000f).direction(-0.4f, -1f, -0.7f).castShadows(false)
            .build(engine, sun)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.85f, 0.95f, 1f).intensity(25_000f).direction(0.6f, -0.2f, 0.8f).castShadows(false)
            .build(engine, fill)
        scene.addEntity(sun)
        scene.addEntity(fill)
        // Soft uniform ambient so the shaded side is never black (first SH band only).
        indirect = IndirectLight.Builder().irradiance(1, floatArrayOf(1f, 1f, 1f)).intensity(22_000f).build(engine)
        scene.indirectLight = indirect

        load()
    }

    private fun load() {
        val bytes = context.assets.open(assetPath).use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).also { it.rewind() }
        val a = loader.createAsset(buffer) ?: return
        resources.loadResources(a)
        a.releaseSourceData()
        scene.addEntities(a.entities)
        val tm = engine.transformManager
        val saved = ArrayList<Pair<Int, FloatArray>>()
        for (e in a.entities) {
            val inst = tm.getInstance(e)
            if (inst != 0) saved.add(e to tm.getTransform(inst, FloatArray(16)))
        }
        rest = saved
        val animator = a.instance.animator
        idleIndex = (0 until animator.animationCount).firstOrNull { animator.getAnimationName(it) == "idle" } ?: 0
        clipIndex = idleIndex
        asset = a
        frame(a)
    }

    /** Points the camera at the model's bounding box so every stage fills the view the same way. */
    private fun frame(a: FilamentAsset) {
        val box = a.boundingBox
        val c = box.center
        val h = box.halfExtent
        val radius = maxOf(h[0], h[1], h[2]) * 1.15
        val dist = radius / Math.tan(Math.toRadians(30.0 / 2)) * 1.05
        camera.lookAt(c[0] + dist * 0.28, c[1] + dist * 0.2, c[2] + dist, c[0].toDouble(), c[1].toDouble(), c[2].toDouble(), 0.0, 1.0, 0.0)
    }

    fun attach(textureView: TextureView) {
        uiHelper.isOpaque = false
        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface, uiHelper.swapChainFlags)
            }
            override fun onDetachedFromSurface() {
                swapChain?.let { engine.destroySwapChain(it); engine.flushAndWait() }
                swapChain = null
            }
            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
                camera.setProjection(30.0, width.toDouble() / height.coerceAtLeast(1), 0.05, 50.0, Camera.Fov.VERTICAL)
            }
        }
        uiHelper.attachTo(textureView)
    }

    fun play(name: String) {
        val a = asset ?: return
        val animator = a.instance.animator
        val i = (0 until animator.animationCount).firstOrNull { animator.getAnimationName(it) == name } ?: return
        clipIndex = i
        oneShot = i != idleIndex
        clipStart = System.nanoTime()
    }

    fun resume() {
        if (running || destroyed) return
        running = true
        choreographer.postFrameCallback(this)
    }

    fun pause() {
        running = false
        choreographer.removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running || destroyed) return
        choreographer.postFrameCallback(this)
        if (frameTimeNanos - lastFrame < 30_000_000L) return  // ~30 fps is plenty for a pet and halves the battery cost
        lastFrame = frameTimeNanos
        val a = asset ?: return
        val animator = a.instance.animator
        val tm = engine.transformManager
        rest.forEach { (e, m) -> tm.setTransform(tm.getInstance(e), m) }
        if (clipStart == 0L) clipStart = frameTimeNanos
        var t = (frameTimeNanos - clipStart) / 1e9f
        val duration = animator.getAnimationDuration(clipIndex)
        if (oneShot && t >= duration) {
            // One-shot finished: back to idle.
            clipIndex = idleIndex; oneShot = false; clipStart = frameTimeNanos; t = 0f
        }
        val d = animator.getAnimationDuration(clipIndex).coerceAtLeast(0.01f)
        animator.applyAnimation(clipIndex, if (oneShot) t else t % d)
        animator.updateBoneMatrices()

        val chain = swapChain ?: return
        if (!uiHelper.isReadyToRender) return
        if (renderer.beginFrame(chain, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    fun destroy() {
        if (destroyed) return
        pause()
        destroyed = true
        uiHelper.detach()
        asset?.let { scene.removeEntities(it.entities); loader.destroyAsset(it) }
        asset = null
        resources.destroy()
        materials.destroyMaterials()
        materials.destroy()
        loader.destroy()
        engine.destroyIndirectLight(indirect)
        engine.destroyEntity(sun); engine.destroyEntity(fill)
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(cameraEntity)
        EntityManager.get().destroy(intArrayOf(sun, fill, cameraEntity))
        engine.destroy()
    }
}
