package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan

@Composable
fun WaveformVisualizer(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    barCount: Int = 7,
    maxHeight: Dp = 32.dp,
    barWidth: Dp = 4.dp,
    activeColor: Color = NeonCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val anims = listOf(anim1, anim2, anim3, anim1, anim2, anim3, anim1)

        for (i in 0 until barCount) {
            val scale = if (isActive) anims[i % anims.size] else 0.2f
            val color = if (i % 2 == 0) activeColor else ElectricViolet

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(maxHeight * scale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}
