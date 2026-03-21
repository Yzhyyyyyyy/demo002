package com.example.demo002

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    val startX: Float,
    val startY: Float,
    val size: Float,
    val speedOffset: Float,
    val color: Color
)

@Composable
fun MyFirstScreen(onStartClick: () -> Unit = {}) {

    val particles = remember {
        List(14) { index ->
            Particle(
                startX = Random.nextFloat(),
                startY = if (index < 9)
                    Random.nextFloat() * 0.45f + 0.50f
                else
                    Random.nextFloat() * 0.8f + 0.1f,
                size = Random.nextFloat() * 6f + 3f,
                speedOffset = Random.nextFloat(),
                color = if (Random.nextBoolean())
                    Color(0xFF7DD3FC)
                else
                    Color(0xFFD8B4FE)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")

    // ✅ 逆时针：0f → -360f
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val centerGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "centerGlowAlpha"
    )

    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {

        // 层1：主光晕（逆时针旋转）
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7DD3FC).copy(alpha = glowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.1f),
                    radius = size.width * 0.75f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD8B4FE).copy(alpha = glowAlpha - 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.2f, size.height * 0.8f),
                    radius = size.width * 0.75f
                )
            )
        }

        // 层2：中心微光
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE).copy(alpha = centerGlowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.width * 0.55f
                )
            )
        }

        // 层3：漂浮粒子
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { particle ->
                val progress = (particleProgress + particle.speedOffset) % 1f
                val currentY = (particle.startY - progress * 0.15f) * size.height
                val currentX = (particle.startX + sin(progress * Math.PI * 2).toFloat() * 0.03f) * size.width
                val alpha = if (progress < 0.5f) progress * 2f else (1f - progress) * 2f

                drawCircle(
                    color = particle.color.copy(alpha = alpha * 0.55f),
                    radius = particle.size,
                    center = Offset(currentX, currentY)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun MyFirstScreenPreview() {
    MyFirstScreen()
}