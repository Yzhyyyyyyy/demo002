package com.example.demo002

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

enum class SplashPhase {
    DRAGGING,
    TEXT_FADE_OUT,
    BALLS_MOVE,
    FINISHED
}

@Composable
fun StartAnimationScreen(onSplashFinished: () -> Unit = {}) {
    val offsetY = remember { Animatable(0f) }
    val maxSwipe = 600f
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    var hasVibrated by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(SplashPhase.DRAGGING) }

    // ★ 新增：松手时用这个快照判断，而不是实时 progress
    var progressSnapshot by remember { mutableStateOf(0f) }

    val rawProgress = (-offsetY.value / maxSwipe).coerceIn(0f, 1f)
    val progress = rawProgress

    val textFadeOut = remember { Animatable(1f) }
    val ballMoveProgress = remember { Animatable(0f) }
    val overlapAlpha = remember { Animatable(1f) }

    val startPhase2 = {
        scope.launch {
            phase = SplashPhase.TEXT_FADE_OUT
            textFadeOut.animateTo(
                0f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
            phase = SplashPhase.BALLS_MOVE
            launch {
                overlapAlpha.animateTo(
                    0f,
                    animationSpec = tween(700, delayMillis = 100, easing = EaseInOutSine)
                )
            }
            ballMoveProgress.animateTo(
                1f,
                animationSpec = tween(850, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
            )
            phase = SplashPhase.FINISHED
            onSplashFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    },
                    onDragEnd = {
                        hasVibrated = false
                        // ★ 改动1：用 progressSnapshot 判断，不用实时 progress
                        if (progressSnapshot > 0.35f) {
                            scope.launch {
                                offsetY.animateTo(
                                    -maxSwipe,
                                    tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))
                                )
                                startPhase2()
                            }
                        } else {
                            scope.launch {
                                offsetY.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (phase != SplashPhase.DRAGGING) return@detectVerticalDragGestures
                        change.consume()
                        scope.launch {
                            val dampingFactor = 1f - (rawProgress * 0.5f)
                            val newOffset = (offsetY.value + dragAmount * dampingFactor).coerceAtMost(0f)
                            offsetY.snapTo(newOffset)
                            // ★ 改动2：每次拖动都更新快照
                            progressSnapshot = (-newOffset / maxSwipe).coerceIn(0f, 1f)

                            if (progress > 0.35f && !hasVibrated) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                                hasVibrated = true
                            } else if (progress <= 0.35f) {
                                hasVibrated = false
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val moveDistanceY = canvasHeight * 0.73f * progress

            val blueDragX = canvasWidth * 0.2f
            val blueDragY = canvasHeight * 0.1f + moveDistanceY

            val purpleDragX = canvasWidth * 0.8f
            val purpleDragY = canvasHeight * 0.9f - moveDistanceY

            val blueTargetX = canvasWidth * 0.8f
            val blueTargetY = canvasHeight * 0.1f

            val purpleTargetX = canvasWidth * 0.2f
            val purpleTargetY = canvasHeight * 0.8f

            val t = ballMoveProgress.value
            val smoothT = t

            val blueCtrlX = canvasWidth * 0.85f
            val blueCtrlY = canvasHeight * 0.85f
            val blueX = lerp(lerp(blueDragX, blueCtrlX, smoothT), lerp(blueCtrlX, blueTargetX, smoothT), smoothT)
            val blueY = lerp(lerp(blueDragY, blueCtrlY, smoothT), lerp(blueCtrlY, blueTargetY, smoothT), smoothT)

            val purpleCtrlX = canvasWidth * 0.15f
            val purpleCtrlY = canvasHeight * 0.15f
            val purpleX = lerp(lerp(purpleDragX, purpleCtrlX, smoothT), lerp(purpleCtrlX, purpleTargetX, smoothT), smoothT)
            val purpleY = lerp(lerp(purpleDragY, purpleCtrlY, smoothT), lerp(purpleCtrlY, purpleTargetY, smoothT), smoothT)

            val dx = blueX - purpleX
            val dy = blueY - purpleY
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            val maxDist = canvasWidth * 1.5f
            val overlapFactor = (1f - (dist / maxDist).coerceIn(0f, 1f)) * overlapAlpha.value

            val blueAlpha = if (phase == SplashPhase.DRAGGING) 0.65f
            else (0.65f - overlapFactor * 0.30f).coerceIn(0.25f, 0.65f)

            val purpleAlpha = if (phase == SplashPhase.DRAGGING) 0.60f
            else (0.60f - overlapFactor * 0.30f).coerceIn(0.20f, 0.60f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7DD3FC).copy(alpha = blueAlpha), Color.Transparent),
                    center = Offset(blueX, blueY),
                    radius = canvasWidth * 0.75f
                ),
                center = Offset(blueX, blueY)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFD8B4FE).copy(alpha = purpleAlpha), Color.Transparent),
                    center = Offset(purpleX, purpleY),
                    radius = canvasWidth * 0.75f
                ),
                center = Offset(purpleX, purpleY)
            )
        }

        val textAlpha = when (phase) {
            SplashPhase.DRAGGING -> progress.coerceIn(0f, 1f)
            else -> textFadeOut.value
        }

        Text(
            text = "WISH\nHUNTER",
            textAlign = TextAlign.Center,
            lineHeight = 52.sp,
            style = TextStyle(
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                color = Color(0xFF1C1C1E)
            ),
            modifier = Modifier
                .graphicsLayer {
                    val scaleValue = if (phase == SplashPhase.DRAGGING) 6f - 5f * progress else 1f
                    scaleX = scaleValue
                    scaleY = scaleValue
                    alpha = textAlpha
                }
        )

        val infiniteTransition = rememberInfiniteTransition(label = "hint_anim")
        val arrowOffsetY by infiniteTransition.animateFloat(
            initialValue = 6f,
            targetValue = -6f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "arrow_bounce"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .alpha((1f - progress * 3f).coerceIn(0f, 1f))
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Swipe Up",
                tint = Color(0xFF94A3B8),
                modifier = Modifier
                    .size(28.dp)
                    .offset(y = arrowOffsetY.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SWIPE TO OPEN",
                style = TextStyle(
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp
                )
            )
        }
    }
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun StartAnimationScreenPreview() {
    StartAnimationScreen()
}