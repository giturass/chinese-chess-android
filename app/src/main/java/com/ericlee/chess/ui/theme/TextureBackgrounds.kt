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

fun Modifier.stoneChamberTexture(): Modifier = drawBehind {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF030405),
                Color(0xFF101315),
                Color(0xFF24292B),
                Color(0xFF090A0B)
            )
        )
    )

    val blockHeight = (size.height / 8.5f).coerceAtLeast(72f)
    var blockTop = -blockHeight * 0.35f
    var row = 0
    while (blockTop < size.height) {
        val blockWidth = (size.width / 3.15f).coerceAtLeast(96f)
        var blockLeft = if (row % 2 == 0) -blockWidth * 0.38f else 0f
        drawLine(
            color = Color.Black.copy(alpha = 0.48f),
            start = Offset(0f, blockTop),
            end = Offset(size.width, blockTop),
            strokeWidth = 2.4f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.035f),
            start = Offset(0f, blockTop + 2f),
            end = Offset(size.width, blockTop + 2f),
            strokeWidth = 1.1f
        )
        while (blockLeft < size.width) {
            drawLine(
                color = Color.Black.copy(alpha = 0.34f),
                start = Offset(blockLeft, blockTop),
                end = Offset(blockLeft, blockTop + blockHeight),
                strokeWidth = 2f
            )
            blockLeft += blockWidth
        }
        blockTop += blockHeight
        row++
    }

    for (i in 0..34) {
        val cx = size.width * ((i * 37 % 100) / 100f)
        val cy = size.height * ((i * 53 % 100) / 100f)
        val radius = 42f + (i % 5) * 18f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.035f),
                    Color(0xFF5E686B).copy(alpha = 0.045f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = radius
            ),
            radius = radius,
            center = Offset(cx, cy)
        )
    }

    for (i in 0..18) {
        val startX = size.width * ((i * 41 % 100) / 100f)
        val startY = size.height * (0.08f + (i * 17 % 70) / 100f)
        val crack = Path().apply {
            moveTo(startX, startY)
            var x = startX
            var y = startY
            for (step in 1..4) {
                x += sin((i + step) * 1.7f) * size.width * 0.035f
                y += size.height * (0.018f + (step % 2) * 0.012f)
                lineTo(x, y)
            }
        }
        drawPath(
            path = crack,
            color = Color.Black.copy(alpha = 0.26f),
            style = Stroke(width = 1.2f + (i % 3) * 0.45f)
        )
        if (i % 2 == 0) {
            drawPath(
                path = crack,
                color = Color.White.copy(alpha = 0.028f),
                style = Stroke(width = 0.8f)
            )
        }
    }

    val floorTop = size.height * 0.64f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x00101010),
                Color(0xFF1A1D1F),
                Color(0xFF070809)
            ),
            startY = floorTop - size.height * 0.12f,
            endY = size.height
        ),
        topLeft = Offset(0f, floorTop),
        size = Size(size.width, size.height - floorTop)
    )
    for (i in 0..8) {
        val x = size.width * i / 8f
        drawLine(
            color = Color.Black.copy(alpha = 0.30f),
            start = Offset(size.width / 2f, floorTop),
            end = Offset(x, size.height),
            strokeWidth = 1.6f
        )
    }
    for (i in 1..5) {
        val y = floorTop + (size.height - floorTop) * (i / 5f) * (i / 5f)
        drawLine(
            color = Color.Black.copy(alpha = 0.34f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.8f
        )
    }

    val lightCenter = Offset(size.width * 0.5f, -size.height * 0.08f)
    val beam = Path().apply {
        moveTo(size.width * 0.42f, 0f)
        lineTo(size.width * 0.08f, size.height * 0.72f)
        lineTo(size.width * 0.92f, size.height * 0.72f)
        lineTo(size.width * 0.58f, 0f)
        close()
    }
    drawPath(
        path = beam,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x55FFE5B0),
                Color(0x22D6B06A),
                Color.Transparent
            ),
            startY = 0f,
            endY = size.height * 0.74f
        )
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x77FFE8BB),
                Color(0x24C79A55),
                Color.Transparent
            ),
            center = lightCenter,
            radius = size.width * 0.58f
        ),
        radius = size.width * 0.58f,
        center = lightCenter
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.70f)
            ),
            center = Offset(size.width * 0.5f, size.height * 0.42f),
            radius = maxOf(size.width, size.height) * 0.72f
        ),
        topLeft = Offset.Zero,
        size = size
    )
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
