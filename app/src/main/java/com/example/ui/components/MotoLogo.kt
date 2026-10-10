package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpaceDark
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MotoLogo(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    showSubtext: Boolean = true,
    isPulsing: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "moto_logo_anim")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (isPulsing) 0.94f else 1f,
        targetValue = if (isPulsing) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            // Background glow canvas
            Canvas(modifier = Modifier.size(size * pulseScale)) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val radius = this.size.minDimension / 2f

                // Outer ambient glow ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.25f),
                            ElectricViolet.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius
                )

                // Cybernetic dashed orbital ring
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.6f),
                    radius = radius * 0.85f,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Inner core ring
                drawCircle(
                    color = ElectricViolet.copy(alpha = 0.8f),
                    radius = radius * 0.68f,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Rotating cybernetic dots
            Canvas(
                modifier = Modifier
                    .size(size)
                    .rotate(rotation)
            ) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val radius = this.size.minDimension * 0.42f

                for (i in 0 until 4) {
                    val angle = Math.toRadians((i * 90).toDouble())
                    val x = center.x + (radius * cos(angle)).toFloat()
                    val y = center.y + (radius * sin(angle)).toFloat()

                    drawCircle(
                        color = if (i % 2 == 0) NeonCyan else ElectricViolet,
                        radius = 3.5.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            // Core emblem center
            Box(
                modifier = Modifier
                    .size(size * 0.52f)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(SpaceDark, DeepVoid)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    color = NeonCyan,
                    fontSize = (size.value * 0.32f).sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (showSubtext) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MOTO AI",
                color = NeonCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "CYBERNETIC ASSISTANT",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
        }
    }
}
