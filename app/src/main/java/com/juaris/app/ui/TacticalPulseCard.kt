package com.juaris.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val NeonGiftgruen = Color(0xFF00FF66)

@Composable
fun TacticalPulseCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tactical_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val activeGlowColor = NeonGiftgruen.copy(alpha = pulseAlpha)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .shadow(
                    elevation = (10 * pulseAlpha).dp,
                    shape = RoundedCornerShape(4.dp),
                    ambientColor = activeGlowColor,
                    spotColor = activeGlowColor
                ),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.5.dp, activeGlowColor),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF030503).copy(alpha = 0.95f)
            )
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .shadow(
                    elevation = (10 * pulseAlpha).dp,
                    shape = RoundedCornerShape(4.dp),
                    ambientColor = activeGlowColor,
                    spotColor = activeGlowColor
                ),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.5.dp, activeGlowColor),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF030503).copy(alpha = 0.95f)
            )
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

