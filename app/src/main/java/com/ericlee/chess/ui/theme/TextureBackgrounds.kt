package com.ericlee.chess.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

fun Modifier.woodTexture(): Modifier = drawBehind {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF3A1F10),
                Color(0xFF6B3C1C),
                Color(0xFF9B602A),
                Color(0xFF5A2F16)
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    )

    val plankWidth = (size.width / 4.2f).coerceAtLeast(92f)
    var seamX = plankWidth
    while (seamX < size.width) {
        drawLine(
            color = Color.Black.copy(alpha = 0.24f),
            start = Offset(seamX, 0f),
            end = Offset(seamX, size.height),
            strokeWidth = 2.2f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(seamX + 2f, 0f),
            end = Offset(seamX + 2f, size.height),
            strokeWidth = 1.2f
        )
        seamX += plankWidth
    }

    for (i in 0..42) {
        val baseX = size.width * i / 42f
        val drift = sin(i * 1.7f) * 18f
        val alpha = 0.08f + (i % 5) * 0.015f
        drawLine(
            color = Color(0xFFFFD08A).copy(alpha = alpha),
            start = Offset(baseX + drift, 0f),
            end = Offset(baseX - drift * 0.35f, size.height),
            strokeWidth = 1.1f + (i % 3) * 0.45f
        )
        if (i % 3 == 0) {
            drawLine(
                color = Color.Black.copy(alpha = 0.12f),
                start = Offset(baseX + drift + 5f, 0f),
                end = Offset(baseX - drift * 0.2f + 5f, size.height),
                strokeWidth = 1.5f
            )
        }
    }

    for (i in 0..10) {
        val cx = size.width * ((i * 29 % 100) / 100f)
        val cy = size.height * ((i * 47 % 100) / 100f)
        val radius = 24f + (i % 4) * 9f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF2E170C).copy(alpha = 0.26f),
                    Color(0xFF7B451D).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = radius
            ),
            radius = radius,
            center = Offset(cx, cy)
        )
    }
}
