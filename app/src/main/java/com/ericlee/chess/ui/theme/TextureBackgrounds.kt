package com.ericlee.chess.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ericlee.chess.R
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

fun Modifier.stoneChamberTexture(): Modifier = composed {
    val resources = LocalContext.current.resources
    val background = androidx.compose.runtime.remember {
        BitmapFactory.decodeResource(resources, R.drawable.background).asImageBitmap()
    }
    drawWithCache {
        val scale = max(size.width / background.width, size.height / background.height)
        val dstSize = IntSize(
            width = (background.width * scale).roundToInt(),
            height = (background.height * scale).roundToInt()
        )
        val dstOffset = IntOffset(
            x = ((size.width - dstSize.width) / 2f).roundToInt(),
            y = ((size.height - dstSize.height) / 2f).roundToInt()
        )
        val floorLightCenter = Offset(size.width * 0.5f, size.height * 0.56f)

        onDrawBehind {
            drawRect(Color(0xFF060607))
            drawImage(
                image = background,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(background.width, background.height),
                dstOffset = dstOffset,
                dstSize = dstSize
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x62FFE7B0),
                        Color(0x24D4A25F),
                        Color.Transparent
                    ),
                    center = floorLightCenter,
                    radius = size.width * 0.62f
                ),
                topLeft = Offset(size.width * 0.12f, size.height * 0.34f),
                size = Size(size.width * 0.76f, size.height * 0.42f)
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x42000000),
                        Color(0xD8000000)
                    ),
                    center = floorLightCenter,
                    radius = max(size.width, size.height) * 0.58f
                )
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.18f)
            )
        }
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
