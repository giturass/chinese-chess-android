package com.ericlee.chess.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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

fun Modifier.battlefieldTexture(): Modifier = drawBehind {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF352015),
                Color(0xFF7A4324),
                Color(0xFFA66B36),
                Color(0xFF3A2417)
            )
        )
    )

    val horizon = size.height * 0.50f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFC26E).copy(alpha = 0.34f),
                Color(0xFFB15F2F).copy(alpha = 0.12f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.74f, size.height * 0.24f),
            radius = size.width * 0.42f
        ),
        radius = size.width * 0.42f,
        center = Offset(size.width * 0.74f, size.height * 0.24f)
    )

    for (i in 0..7) {
        val cy = size.height * (0.14f + i * 0.07f)
        drawOval(
            color = Color(0xFF2B1A12).copy(alpha = 0.08f + i * 0.01f),
            topLeft = Offset(-size.width * 0.18f + i * size.width * 0.11f, cy),
            size = Size(size.width * 0.78f, size.height * 0.12f)
        )
    }

    val ridge = Path().apply {
        moveTo(0f, horizon)
        lineTo(size.width * 0.16f, horizon - size.height * 0.055f)
        lineTo(size.width * 0.34f, horizon - size.height * 0.025f)
        lineTo(size.width * 0.55f, horizon - size.height * 0.07f)
        lineTo(size.width * 0.78f, horizon - size.height * 0.035f)
        lineTo(size.width, horizon - size.height * 0.06f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path = ridge, color = Color(0xFF241710).copy(alpha = 0.46f))

    val ground = Path().apply {
        moveTo(0f, size.height * 0.66f)
        lineTo(size.width * 0.22f, size.height * 0.61f)
        lineTo(size.width * 0.48f, size.height * 0.68f)
        lineTo(size.width * 0.76f, size.height * 0.63f)
        lineTo(size.width, size.height * 0.70f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path = ground, color = Color(0xFF24150D).copy(alpha = 0.40f))

    for (i in 0..11) {
        val x = size.width * (0.04f + i * 0.085f)
        val base = horizon + size.height * (0.02f + (i % 4) * 0.018f)
        val height = size.height * (0.12f + (i % 3) * 0.025f)
        drawLine(
            color = Color(0xFF1B110C).copy(alpha = 0.54f),
            start = Offset(x, base + height),
            end = Offset(x + size.width * 0.018f, base),
            strokeWidth = 2.2f
        )
        if (i % 3 == 1) {
            val banner = Path().apply {
                moveTo(x + size.width * 0.018f, base + height * 0.10f)
                lineTo(x + size.width * 0.12f, base + height * 0.16f)
                lineTo(x + size.width * 0.065f, base + height * 0.34f)
                lineTo(x + size.width * 0.018f, base + height * 0.28f)
                close()
            }
            drawPath(path = banner, color = Color(0xFF4B120D).copy(alpha = 0.62f))
            drawPath(
                path = banner,
                color = Color(0xFFE0A158).copy(alpha = 0.20f),
                style = Stroke(width = 1.2f)
            )
        }
    }

    for (i in 0..36) {
        val y = size.height * (0.66f + i / 36f * 0.28f)
        val drift = sin(i * 1.37f) * size.width * 0.05f
        drawLine(
            color = Color(0xFFFFC078).copy(alpha = 0.05f),
            start = Offset(-size.width * 0.05f + drift, y),
            end = Offset(size.width * 1.05f - drift, y + size.height * 0.035f),
            strokeWidth = 1.0f + (i % 4) * 0.4f
        )
    }
}
