package com.ericlee.chess.ui.board

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.ericlee.chess.model.*
import kotlin.math.min
import kotlin.math.roundToInt

private val STONE_BASE = Color(0xFF687371)
private val STONE_LIGHT = Color(0xFF9BA8A3)
private val STONE_MID = Color(0xFF77837F)
private val STONE_DARK = Color(0xFF303C3B)
private val EDGE_COLOR = Color(0xFF24302F)
private val GRID_COLOR = Color(0xFF263433)
private val RED_COLOR = Color(0xFFB32318)
private val BLACK_COLOR = Color(0xFF1F1711)
private val SELECTED_COLOR = Color(0x80E8D07B)
private val LAST_MOVE_COLOR = Color(0x805CA66B)
private val CHECK_COLOR = Color(0xB8C31B12)
private val MARKER_COLOR = Color(0xFF2D3C3A)
private val PIECE_BG = Color(0xFFF6D495)

@Composable
fun ChessBoard(
    board: Board,
    currentSide: Side,
    selectedPiece: Piece?,
    legalMoves: List<Move>,
    lastMove: Move?,
    isFlipped: Boolean,
    onPositionClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pieceMap = remember(board.pieces.toList()) {
        board.pieces.associateBy { Pair(it.row, it.col) }
    }
    val checkedKing = remember(board.pieces.toList(), currentSide) {
        if (board.isInCheck(currentSide)) board.findKing(currentSide) else null
    }
    val transition = rememberInfiniteTransition(label = "boardEffects")
    val pulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 760),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val ripplePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripplePhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.86f)
            .pointerInput(isFlipped, onPositionClick) {
                detectTapGestures { offset ->
                    val cellW = min(size.width / 9.15f, size.height / 10.35f)
                    val cellH = cellW
                    val offsetX = (size.width - 8f * cellW) / 2f
                    val offsetY = (size.height - 9f * cellH) / 2f
                    val displayCol = ((offset.x - offsetX) / cellW).roundToInt()
                    val displayRow = ((offset.y - offsetY) / cellH).roundToInt()
                    val row = toBoardRow(displayRow, isFlipped)
                    val col = toBoardCol(displayCol, isFlipped)
                    if (row in 0..9 && col in 0..8) {
                        onPositionClick(row, col)
                    }
                }
            }
    ) {
        val cellW = min(size.width / 9.15f, size.height / 10.35f)
        val cellH = cellW
        val offsetX = (size.width - 8f * cellW) / 2f
        val offsetY = (size.height - 9f * cellH) / 2f

        drawBoardBackground(offsetX, offsetY, cellW, cellH)

        drawGrid(offsetX, offsetY, cellW, cellH)
        drawPalace(offsetX, offsetY, cellW, cellH)
        drawStoneRiverText(offsetX, offsetY, cellW, cellH)
        drawPositionMarkers(offsetX, offsetY, cellW, cellH)

        if (lastMove != null) {
            drawMoveEffect(lastMove, offsetX, offsetY, cellW, cellH, isFlipped, currentSide.opposite(), pulse)
            if (lastMove.captured != null) {
                val center = boardPositionCenter(lastMove.toRow, lastMove.toCol, offsetX, offsetY, cellW, cellH, isFlipped)
                drawCaptureEffect(center, cellW, ripplePhase)
            }
        }

        if (selectedPiece != null) {
            drawPositionHighlight(
                selectedPiece.row,
                selectedPiece.col,
                offsetX,
                offsetY,
                cellW,
                cellH,
                SELECTED_COLOR,
                isFlipped,
                ripplePhase
            )
        }

        for (move in legalMoves) {
            val cx = offsetX + toDisplayCol(move.toCol, isFlipped) * cellW
            val cy = offsetY + toDisplayRow(move.toRow, isFlipped) * cellH
            val target = board.getPiece(move.toRow, move.toCol)
            if (target != null) {
                drawCircle(
                    color = CHECK_COLOR,
                    radius = cellW * 0.48f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4.5f)
                )
                drawCaptureBurst(cx, cy, cellW * 0.28f, CHECK_COLOR, 0.85f)
            } else {
                drawCircle(
                    color = Color(0xA85E7F30),
                    radius = cellW * 0.13f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0x556FA23A),
                    radius = cellW * 0.23f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )
            }
        }

        if (checkedKing != null) {
            drawCheckEffect(checkedKing, offsetX, offsetY, cellW, cellH, isFlipped, pulse)
        }

        for ((row, col) in pieceMap.keys) {
            val piece = pieceMap[Pair(row, col)] ?: continue
            val cx = offsetX + toDisplayCol(col, isFlipped) * cellW
            val cy = offsetY + toDisplayRow(row, isFlipped) * cellH
            drawPiece(piece, cx, cy, cellW * 0.43f, isFlipped)
        }
    }
}

private fun toDisplayRow(row: Int, isFlipped: Boolean): Int = if (isFlipped) 9 - row else row

private fun toDisplayCol(col: Int, isFlipped: Boolean): Int = if (isFlipped) 8 - col else col

private fun toBoardRow(displayRow: Int, isFlipped: Boolean): Int = if (isFlipped) 9 - displayRow else displayRow

private fun toBoardCol(displayCol: Int, isFlipped: Boolean): Int = if (isFlipped) 8 - displayCol else displayCol

private fun DrawScope.drawBoardBackground(offsetX: Float, offsetY: Float, cellW: Float, cellH: Float) {
    val outerTopLeft = Offset(offsetX - cellW * 0.45f, offsetY - cellH * 0.45f)
    val outerSize = Size(cellW * 8.9f, cellH * 9.9f)
    val innerTopLeft = Offset(offsetX - cellW * 0.2f, offsetY - cellH * 0.2f)
    val innerSize = Size(cellW * 8.4f, cellH * 9.4f)

    drawRoundRect(
        color = EDGE_COLOR,
        topLeft = outerTopLeft,
        size = outerSize,
        cornerRadius = CornerRadius(cellW * 0.16f, cellW * 0.16f)
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(STONE_LIGHT, STONE_MID, STONE_BASE, STONE_DARK),
            start = outerTopLeft,
            end = Offset(outerTopLeft.x + outerSize.width, outerTopLeft.y + outerSize.height)
        ),
        topLeft = Offset(outerTopLeft.x + cellW * 0.07f, outerTopLeft.y + cellH * 0.07f),
        size = Size(outerSize.width - cellW * 0.14f, outerSize.height - cellH * 0.14f),
        cornerRadius = CornerRadius(cellW * 0.12f, cellW * 0.12f)
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFA8B1AA),
                Color(0xFF7D8985),
                Color(0xFF65706E),
                Color(0xFF8D9994)
            ),
            start = Offset(innerTopLeft.x, innerTopLeft.y),
            end = Offset(innerTopLeft.x + innerSize.width, innerTopLeft.y + innerSize.height)
        ),
        topLeft = innerTopLeft,
        size = innerSize,
        cornerRadius = CornerRadius(cellW * 0.08f, cellW * 0.08f)
    )
    for (i in 0..22) {
        val y = innerTopLeft.y + innerSize.height * (i + 0.35f) / 23f
        val xDrift = if (i % 2 == 0) cellW * 0.22f else -cellW * 0.14f
        drawLine(
            color = Color.White.copy(alpha = 0.06f),
            start = Offset(innerTopLeft.x + cellW * 0.15f + xDrift, y),
            end = Offset(innerTopLeft.x + innerSize.width - cellW * 0.16f - xDrift, y + cellH * 0.08f),
            strokeWidth = 1.1f
        )
        if (i % 3 == 0) {
            drawLine(
                color = Color.Black.copy(alpha = 0.08f),
                start = Offset(innerTopLeft.x + cellW * 0.2f, y + cellH * 0.08f),
                end = Offset(innerTopLeft.x + innerSize.width - cellW * 0.2f, y + cellH * 0.16f),
                strokeWidth = 1.4f
            )
        }
    }
    for (i in 0..34) {
        val x = innerTopLeft.x + innerSize.width * ((i * 37 % 100) / 100f)
        val y = innerTopLeft.y + innerSize.height * ((i * 53 % 100) / 100f)
        val r = cellW * (0.018f + (i % 4) * 0.006f)
        drawCircle(Color.Black.copy(alpha = 0.05f), radius = r, center = Offset(x, y))
        drawCircle(Color.White.copy(alpha = 0.04f), radius = r * 0.55f, center = Offset(x - r * 0.25f, y - r * 0.25f))
    }
    drawRoundRect(
        color = MARKER_COLOR.copy(alpha = 0.92f),
        topLeft = innerTopLeft,
        size = innerSize,
        cornerRadius = CornerRadius(cellW * 0.08f, cellW * 0.08f),
        style = Stroke(width = 3f)
    )
}

private fun DrawScope.drawGrid(offsetX: Float, offsetY: Float, cellW: Float, cellH: Float) {
    // Horizontal lines
    for (r in 0..9) {
        drawEngravedLine(
            start = Offset(offsetX, offsetY + r * cellH),
            end = Offset(offsetX + 8 * cellW, offsetY + r * cellH),
            strokeWidth = if (r == 0 || r == 9) 3.6f else 2.2f
        )
    }

    // Vertical lines
    for (c in 0..8) {
        if (c == 0 || c == 8) {
            drawEngravedLine(
                start = Offset(offsetX + c * cellW, offsetY),
                end = Offset(offsetX + c * cellW, offsetY + 9 * cellH),
                strokeWidth = 3.6f
            )
        } else {
            drawEngravedLine(
                start = Offset(offsetX + c * cellW, offsetY),
                end = Offset(offsetX + c * cellW, offsetY + 4 * cellH),
                strokeWidth = 2.2f
            )
            drawEngravedLine(
                start = Offset(offsetX + c * cellW, offsetY + 5 * cellH),
                end = Offset(offsetX + c * cellW, offsetY + 9 * cellH),
                strokeWidth = 2.2f
            )
        }
    }
}

private fun DrawScope.drawPalace(offsetX: Float, offsetY: Float, cellW: Float, cellH: Float) {
    val palaceColor = MARKER_COLOR
    // Top palace
    drawEngravedLine(Offset(offsetX + 3 * cellW, offsetY), Offset(offsetX + 5 * cellW, offsetY + 2 * cellH), 2.2f, palaceColor)
    drawEngravedLine(Offset(offsetX + 5 * cellW, offsetY), Offset(offsetX + 3 * cellW, offsetY + 2 * cellH), 2.2f, palaceColor)

    // Bottom palace
    drawEngravedLine(Offset(offsetX + 3 * cellW, offsetY + 7 * cellH), Offset(offsetX + 5 * cellW, offsetY + 9 * cellH), 2.2f, palaceColor)
    drawEngravedLine(Offset(offsetX + 5 * cellW, offsetY + 7 * cellH), Offset(offsetX + 3 * cellW, offsetY + 9 * cellH), 2.2f, palaceColor)
}

private fun DrawScope.drawEngravedLine(
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    color: Color = GRID_COLOR
) {
    drawLine(
        color = Color.White.copy(alpha = 0.22f),
        start = Offset(start.x - 0.8f, start.y - 0.8f),
        end = Offset(end.x - 0.8f, end.y - 0.8f),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = Color.Black.copy(alpha = 0.25f),
        start = Offset(start.x + 1.0f, start.y + 1.0f),
        end = Offset(end.x + 1.0f, end.y + 1.0f),
        strokeWidth = strokeWidth
    )
    drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth)
}

private fun DrawScope.drawStoneRiverText(offsetX: Float, offsetY: Float, cellW: Float, cellH: Float) {
    val top = offsetY + 4f * cellH
    val bottom = offsetY + 5f * cellH
    val y = (top + bottom) / 2f
    val plaqueSize = Size(cellW * 2.22f, cellH * 0.72f)
    val plaques = listOf(
        "楚 河" to Offset(offsetX + cellW * 0.82f, y - plaqueSize.height / 2f),
        "漢 界" to Offset(offsetX + cellW * 4.96f, y - plaqueSize.height / 2f)
    )

    for ((_, topLeft) in plaques) {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF8E9995), Color(0xFF65716F), Color(0xFF4A5654)),
                start = topLeft,
                end = Offset(topLeft.x + plaqueSize.width, topLeft.y + plaqueSize.height)
            ),
            topLeft = topLeft,
            size = plaqueSize,
            cornerRadius = CornerRadius(cellH * 0.08f, cellH * 0.08f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = Offset(topLeft.x + 1.2f, topLeft.y + 1.2f),
            size = plaqueSize,
            cornerRadius = CornerRadius(cellH * 0.08f, cellH * 0.08f),
            style = Stroke(width = 1.4f)
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.24f),
            topLeft = topLeft,
            size = plaqueSize,
            cornerRadius = CornerRadius(cellH * 0.08f, cellH * 0.08f),
            style = Stroke(width = 2.2f)
        )
    }

    drawContext.canvas.nativeCanvas.apply {
        val highlightPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#B8C4BC")
            textSize = cellH * 0.46f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            letterSpacing = 0.12f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
        }
        val carvedPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#263130")
            textSize = cellH * 0.46f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            letterSpacing = 0.12f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
        }
        for ((text, topLeft) in plaques) {
            val centerX = topLeft.x + plaqueSize.width / 2f
            val baseline = y - (carvedPaint.descent() + carvedPaint.ascent()) / 2f
            drawText(text, centerX - 1.4f, baseline - 1.4f, highlightPaint)
            drawText(text, centerX + 1.6f, baseline + 1.6f, carvedPaint)
            carvedPaint.style = android.graphics.Paint.Style.STROKE
            carvedPaint.strokeWidth = 1.2f
            drawText(text, centerX, baseline, carvedPaint)
            carvedPaint.style = android.graphics.Paint.Style.FILL
        }
    }
}

private fun DrawScope.drawPositionMarkers(offsetX: Float, offsetY: Float, cellW: Float, cellH: Float) {
    val points = listOf(
        2 to 1, 2 to 7,
        3 to 0, 3 to 2, 3 to 4, 3 to 6, 3 to 8,
        6 to 0, 6 to 2, 6 to 4, 6 to 6, 6 to 8,
        7 to 1, 7 to 7
    )
    for ((row, col) in points) {
        drawMarker(row, col, offsetX, offsetY, cellW, cellH)
    }
}

private fun DrawScope.drawMarker(row: Int, col: Int, offsetX: Float, offsetY: Float, cellW: Float, cellH: Float) {
    val cx = offsetX + col * cellW
    val cy = offsetY + row * cellH
    val len = cellW * 0.13f
    val gap = cellW * 0.09f
    val strokeWidth = 1.6f
    val corners = listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)

    for ((sx, sy) in corners) {
        if ((col == 0 && sx < 0) || (col == 8 && sx > 0)) continue
        drawLine(
            color = MARKER_COLOR,
            start = Offset(cx + sx * gap, cy + sy * gap),
            end = Offset(cx + sx * (gap + len), cy + sy * gap),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = MARKER_COLOR,
            start = Offset(cx + sx * gap, cy + sy * gap),
            end = Offset(cx + sx * gap, cy + sy * (gap + len)),
            strokeWidth = strokeWidth
        )
    }
}

private fun DrawScope.drawPositionHighlight(
    row: Int, col: Int,
    offsetX: Float, offsetY: Float,
    cellW: Float, cellH: Float,
    color: Color,
    isFlipped: Boolean,
    ripplePhase: Float
) {
    val displayRow = toDisplayRow(row, isFlipped)
    val displayCol = toDisplayCol(col, isFlipped)
    val cx = offsetX + displayCol * cellW
    val cy = offsetY + displayRow * cellH
    val center = Offset(cx, cy)
    drawCircle(color = color.copy(alpha = 0.24f), radius = cellW * 0.42f, center = center)
    for (i in 0..2) {
        val phase = (ripplePhase + i / 3f) % 1f
        val alpha = (1f - phase) * 0.34f
        drawCircle(
            color = Color(0xFFEBD989).copy(alpha = alpha),
            radius = cellW * (0.38f + phase * 0.42f),
            center = center,
            style = Stroke(width = 3.2f * (1f - phase).coerceAtLeast(0.25f))
        )
    }
    drawCircle(
        color = Color(0xFFFFF0A6).copy(alpha = 0.62f),
        radius = cellW * 0.45f,
        center = center,
        style = Stroke(width = 2.4f)
    )
}

private fun DrawScope.drawMoveEffect(
    move: Move,
    offsetX: Float,
    offsetY: Float,
    cellW: Float,
    cellH: Float,
    isFlipped: Boolean,
    fallbackSide: Side,
    pulse: Float
) {
    val from = boardPositionCenter(move.fromRow, move.fromCol, offsetX, offsetY, cellW, cellH, isFlipped)
    val to = boardPositionCenter(move.toRow, move.toCol, offsetX, offsetY, cellW, cellH, isFlipped)
    val moveSide = move.side ?: fallbackSide
    val color = if (moveSide == Side.RED) RED_COLOR else Color(0xFF231711)
    val glow = if (moveSide == Side.RED) Color(0xFFDC3B2E) else Color(0xFF0F0B08)

    drawLine(
        color = glow.copy(alpha = 0.44f),
        start = from,
        end = to,
        strokeWidth = cellW * 0.14f
    )
    drawLine(
        color = Color(0xFFE8C277).copy(alpha = 0.62f),
        start = from,
        end = to,
        strokeWidth = cellW * 0.045f
    )
    drawMoveArrowHead(from, to, glow.copy(alpha = 0.78f), cellW * 0.22f)

    drawCircle(color = LAST_MOVE_COLOR.copy(alpha = 0.82f), radius = cellW * 0.52f, center = from)
    drawCircle(color = color.copy(alpha = 0.24f), radius = cellW * 0.66f * pulse, center = to)
    drawCircle(color = Color(0xFFE8C277).copy(alpha = 0.72f), radius = cellW * 0.53f, center = to, style = Stroke(width = 5f))
    drawCircle(color = color, radius = cellW * 0.4f * pulse, center = to, style = Stroke(width = 4.5f))

    if (move.captured != null) {
        drawCaptureBurst(to.x, to.y, cellW * 0.43f, CHECK_COLOR, 1f)
    }
}

private fun DrawScope.drawMoveArrowHead(from: Offset, to: Offset, color: Color, size: Float) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    if (dx == 0f && dy == 0f) return

    val angle = kotlin.math.atan2(dy, dx)
    val left = angle + Math.PI.toFloat() * 0.82f
    val right = angle - Math.PI.toFloat() * 0.82f
    val path = Path().apply {
        moveTo(to.x, to.y)
        lineTo(
            to.x + kotlin.math.cos(left).toFloat() * size,
            to.y + kotlin.math.sin(left).toFloat() * size
        )
        lineTo(
            to.x + kotlin.math.cos(right).toFloat() * size,
            to.y + kotlin.math.sin(right).toFloat() * size
        )
        close()
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.drawCaptureBurst(cx: Float, cy: Float, radius: Float, color: Color, scale: Float) {
    val burst = radius * scale
    for (i in 0 until 8) {
        val angle = Math.PI * 2.0 * i / 8.0
        val inner = burst * 0.62f
        val outer = burst * 1.08f
        val start = Offset(
            x = cx + kotlin.math.cos(angle).toFloat() * inner,
            y = cy + kotlin.math.sin(angle).toFloat() * inner
        )
        val end = Offset(
            x = cx + kotlin.math.cos(angle).toFloat() * outer,
            y = cy + kotlin.math.sin(angle).toFloat() * outer
        )
        drawLine(color = color, start = start, end = end, strokeWidth = 2.4f)
    }
}

private fun DrawScope.drawCaptureEffect(center: Offset, cellW: Float, phase: Float) {
    val ringRadius = cellW * (0.28f + phase * 0.55f)
    drawCircle(
        color = Color(0xFFF1D78D).copy(alpha = (1f - phase) * 0.46f),
        radius = ringRadius,
        center = center,
        style = Stroke(width = cellW * 0.06f * (1f - phase).coerceAtLeast(0.25f))
    )
    drawCircle(
        color = CHECK_COLOR.copy(alpha = (1f - phase) * 0.18f),
        radius = cellW * (0.44f + phase * 0.48f),
        center = center
    )
    for (i in 0 until 14) {
        val angle = Math.PI * 2.0 * i / 14.0
        val drift = cellW * (0.2f + phase * (0.36f + (i % 3) * 0.05f))
        val x = center.x + kotlin.math.cos(angle).toFloat() * drift
        val y = center.y + kotlin.math.sin(angle).toFloat() * drift
        drawCircle(
            color = Color(0xFFE8D3A0).copy(alpha = (1f - phase) * (0.38f + (i % 2) * 0.12f)),
            radius = cellW * (0.025f + (i % 4) * 0.006f),
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawCheckEffect(
    king: Piece,
    offsetX: Float,
    offsetY: Float,
    cellW: Float,
    cellH: Float,
    isFlipped: Boolean,
    pulse: Float
) {
    val center = boardPositionCenter(king.row, king.col, offsetX, offsetY, cellW, cellH, isFlipped)
    drawCircle(color = CHECK_COLOR.copy(alpha = 0.42f), radius = cellW * 0.58f * pulse, center = center)
    drawCircle(color = CHECK_COLOR, radius = cellW * 0.49f * pulse, center = center, style = Stroke(width = 4.2f))
    drawCaptureBurst(center.x, center.y, cellW * 0.45f, CHECK_COLOR, pulse)
}

private fun boardPositionCenter(
    row: Int,
    col: Int,
    offsetX: Float,
    offsetY: Float,
    cellW: Float,
    cellH: Float,
    isFlipped: Boolean
): Offset = Offset(
    x = offsetX + toDisplayCol(col, isFlipped) * cellW,
    y = offsetY + toDisplayRow(row, isFlipped) * cellH
)

private fun DrawScope.drawPiece(piece: Piece, cx: Float, cy: Float, radius: Float, isFlipped: Boolean) {
    val color = if (piece.side == Side.RED) RED_COLOR else BLACK_COLOR
    val shadowColor = Color(0x55000000)

    drawCircle(color = shadowColor, radius = radius * 1.08f, center = Offset(cx + radius * 0.1f, cy + radius * 0.14f))
    drawCircle(color = Color(0xFF7A4318), radius = radius * 1.02f, center = Offset(cx, cy))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFCEB), PIECE_BG, Color(0xFFE5BD73)),
            center = Offset(cx - radius * 0.36f, cy - radius * 0.42f),
            radius = radius * 1.35f
        ),
        radius = radius * 0.94f,
        center = Offset(cx, cy)
    )
    drawCircle(color = color, radius = radius * 0.94f, center = Offset(cx, cy), style = Stroke(width = 3.2f))

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFF2C0)),
            center = Offset(cx - radius * 0.22f, cy - radius * 0.28f),
            radius = radius * 0.88f
        ),
        radius = radius * 0.76f,
        center = Offset(cx, cy)
    )
    drawCircle(color = color, radius = radius * 0.76f, center = Offset(cx, cy), style = Stroke(width = 1.4f))
    drawCircle(color = Color(0x8AFFFFFF), radius = radius * 0.18f, center = Offset(cx - radius * 0.32f, cy - radius * 0.36f))

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = when (piece.side) {
                Side.RED -> android.graphics.Color.parseColor("#CC0000")
                Side.BLACK -> android.graphics.Color.parseColor("#1A1A1A")
            }
            textSize = radius * 1.2f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        val textY = cy - (paint.descent() + paint.ascent()) / 2
        val rotateText = (piece.side == Side.BLACK) != isFlipped
        save()
        if (rotateText) {
            rotate(180f, cx, cy)
        }
        drawText(piece.char, cx, textY, paint)
        restore()
    }
}
