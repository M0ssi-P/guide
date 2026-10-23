package ui.screens.table

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class BookPagerState(pageCount: Int, initialPage: Int = 0) {
    var pageCount: Int = pageCount
        internal set

    /** Index of the page currently settled/shown (updates the instant a turn fully commits). */
    var currentPage by mutableIntStateOf(initialPage)
        internal set

    /** -1f (mid-turn to previous) .. 0f (settled) .. +1f (mid-turn to next). */
    internal val progress = Animatable(0f)

    internal val busy get() = progress.isRunning

    internal suspend fun snapBack() {
        progress.animateTo(0f, spring(stiffness = 380f))
    }

    internal suspend fun commit(direction: Int) {
        progress.animateTo(direction.toFloat(), spring(stiffness = 220f, dampingRatio = 0.86f))
        currentPage = (currentPage + direction).coerceIn(0, pageCount - 1)
        progress.snapTo(0f)
    }
}

@Composable
fun rememberBookPagerState(pageCount: Int, initialPage: Int = 0): BookPagerState =
    remember(pageCount) { BookPagerState(pageCount, initialPage) }

fun BookPagerState.next(scope: CoroutineScope) {
    if (busy || currentPage >= pageCount - 1) return
    scope.launch { commit(1) }
}

fun BookPagerState.previous(scope: CoroutineScope) {
    if (busy || currentPage <= 0) return
    scope.launch { commit(-1) }
}

@Composable
fun BookPageTurn(
    state: BookPagerState,
    modifier: Modifier = Modifier,
    enableDrag: Boolean = true,
    shadowColor: Color = Color.Black,
    page: @Composable (index: Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var widthPx by remember { mutableFloatStateOf(1f) }

    val raw = state.progress.value // -1..1
    val dir = if (raw >= 0f) 1 else -1
    val t = abs(raw) // 0..1, how far through the turn we are
    val neighbourIndex = state.currentPage + dir
    val hasNeighbour = neighbourIndex in 0 until state.pageCount

    Box(
        modifier = modifier
            .onGloballyPositioned { widthPx = it.size.width.coerceAtLeast(1).toFloat() }
            .pointerInput(enableDrag, state.pageCount) {
                if (!enableDrag) return@pointerInput
                var dragTotal = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = {
                        scope.launch {
                            val forward = state.progress.value > 0f
                            val committed = abs(state.progress.value) > 0.32f
                            val canGo = if (forward) state.currentPage < state.pageCount - 1
                                        else state.currentPage > 0
                            if (committed && canGo) {
                                state.commit(if (forward) 1 else -1)
                            } else {
                                state.snapBack()
                            }
                        }
                    },
                    onDragCancel = { scope.launch { state.snapBack() } },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragTotal += dragAmount
                        // dragging left (negative dragAmount sum) turns forward to the next page
                        val delta = (-dragTotal / widthPx).coerceIn(-1f, 1f)
                        val allowed = when {
                            delta > 0f && state.currentPage >= state.pageCount - 1 -> 0f
                            delta < 0f && state.currentPage <= 0 -> 0f
                            else -> delta
                        }
                        scope.launch { state.progress.snapTo(allowed) }
                    }
                )
            }
    ) {
        // outgoing (currently settled) page — hinges on the edge it's turning away from
        val outgoingOrigin = if (dir == 1) TransformOrigin(1f, 0.5f) else TransformOrigin(0f, 0.5f)
        val outgoingAlpha = if (t > 0.92f) ((1f - t) / 0.08f).coerceIn(0f, 1f) else 1f
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = -90f * t * dir
                    cameraDistance = 14f * density.density
                    transformOrigin = outgoingOrigin
                    alpha = outgoingAlpha
                }
        ) {
            page(state.currentPage)
        }

        // incoming neighbour, revealed as it rotates in from behind the outgoing page
        if (hasNeighbour && t > 0.001f) {
            val incomingOrigin = if (dir == 1) TransformOrigin(0f, 0.5f) else TransformOrigin(1f, 0.5f)
            val incomingAlpha = (t / 0.08f).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = 90f * (1f - t) * dir
                        cameraDistance = 14f * density.density
                        transformOrigin = incomingOrigin
                        alpha = incomingAlpha
                    }
            ) {
                page(neighbourIndex)
            }
        }

        // soft directional shadow that peaks mid-turn, selling the fold at the spine
        val shadowStrength = (sin(PI.toFloat() * t) * 0.32f).coerceIn(0f, 0.32f)
        if (shadowStrength > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = if (dir == 1)
                                listOf(Color.Transparent, shadowColor.copy(alpha = shadowStrength))
                            else
                                listOf(shadowColor.copy(alpha = shadowStrength), Color.Transparent)
                        )
                    )
            )
        }
    }
}
