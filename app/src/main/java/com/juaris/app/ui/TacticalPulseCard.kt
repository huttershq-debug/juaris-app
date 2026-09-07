package com.juaris.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TacticalPulseCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.fillMaxWidth().clickable { onClick() }
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        shape = RoundedCornerShape(4.dp), // Eckiger, technischer Look
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF020804) // Tieftiefes Schwarz mit Grünstich
        ),
        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.6f)), // Leuchtender Neon-Rahmen
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = cardModifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

