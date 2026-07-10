package com.ericlee.chess.ui.board

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.ericlee.chess.model.*
import kotlin.math.min
import kotlin.math.roundToInt

private val WOOD_BASE = Color(0xFFC47A34)
private val WOOD_LIGHT = Color(0xFFE7B96B)
private val WOOD_MID = Color(0xFFA95D24)
private val WOOD_DARK = Color(0xFF5A2B10)
private val EDGE_COLOR = Color(0xFF2D1609)
private val GRID_COLOR = Color(0xFF3D1E0C)
private val RED_COLOR = Color(0xFFB32318)
private val BLACK_COLOR = Color(0xFF1F1711)
private val LAST_MOVE_COLOR = Color(0x805CA66B)
private val CHECK_COLOR = Color(0xB8C31B12)
private val MARKER_COLOR = Color(0xFF5C2D10)
private val PIECE_BG = Color(0xFFF6D495)

@Composable
fun ChessBoard(
    board: Board,
    currentSide: Side,
    status: GameStatus = GameStatus.PLAYING,
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
            .aspectRatio(9f / 10f)
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
        drawWoodRiverText(offsetX, offsetY, cellW, cellH)
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
                selectedPiece,
                offsetX,
                offsetY,
                cellW,
                cellH,
                isFlipped,
                pulse
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

        val alertText = boardAlertText(status, checkedKing != null)
        if (alertText != null) {
            drawBoardAlert(alertText, status, cellW)
        }
    }
}

private fun boardAlertText(status: GameStatus, isCheck: Boolean): String? = when (status) {
    GameStatus.RED_WIN -> "红方胜"
    GameStatus.BLACK_WIN -> "黑方胜"
    GameStatus.STALEMATE -> "和棋"
    GameStatus.DRAW -> "和棋"
    GameStatus.PLAYING -> if (isCheck) "将军" else null
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
    val overheadLight = Offset(size.width / 2f, outerTopLeft.y - cellH * 1.6f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x66FFE8B5),
                Color(0x1AD6AE68),
                Color.Transparent
            ),
            center = overheadLight,
            radius = cellW * 6.6f
        ),
        radius = cellW * 6.6f,
        center = overheadLight
    )

    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x66000000), Color(0xC8000000)),
            center = Offset(size.width / 2f, outerTopLeft.y + outerSize.height * 0.56f),
            radius = cellW * 5.8f
        ),
        topLeft = Offset(outerTopLeft.x + cellW * 0.1f, outerTopLeft.y + cellH * 0.16f),
        size = outerSize,
        cornerRadius = CornerRadius(cellW * 0.16f, cellW * 0.16f)
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(EDGE_COLOR, Color(0xFF6E3715), Color(0xFF211006)),
            start = outerTopLeft,
            end = Offset(outerTopLeft.x + outerSize.width, outerTopLeft.y + outerSize.height)
        ),
        topLeft = outerTopLeft,
        size = outerSize,
        cornerRadius = CornerRadius(cellW * 0.16f, cellW * 0.16f)
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF8F4D1D), Color(0xFFC9823C), Color(0xFF5E2B0F)),
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
                WOOD_LIGHT,
                WOOD_BASE,
                WOOD_MID,
                Color(0xFFD8994D),
                WOOD_DARK
            ),
            start = Offset(innerTopLeft.x, innerTopLeft.y),
            end = Offset(innerTopLeft.x + innerSize.width, innerTopLeft.y + innerSize.height)
        ),
        topLeft = innerTopLeft,
        size = innerSize,
        cornerRadius = CornerRadius(cellW * 0.08f, cellW * 0.08f)
    )

    val plankWidth = innerSize.width / 4f
    for (i in 1..3) {
        val x = innerTopLeft.x + plankWidth * i
        drawLine(
            color = Color.Black.copy(alpha = 0.25f),
            start = Offset(x, innerTopLeft.y + cellH * 0.05f),
            end = Offset(x, innerTopLeft.y + innerSize.height - cellH * 0.05f),
            strokeWidth = 2.6f
        )
        drawLine(
            color = Color(0xFFFFD48A).copy(alpha = 0.16f),
            start = Offset(x + 2f, innerTopLeft.y + cellH * 0.08f),
            end = Offset(x + 2f, innerTopLeft.y + innerSize.height - cellH * 0.08f),
            strokeWidth = 1.2f
        )
    }

    for (i in 0..72) {
        val baseX = innerTopLeft.x + innerSize.width * (i + 0.25f) / 73f
        val phase = i * 0.57f
        val alpha = 0.11f + (i % 4) * 0.018f
        drawWoodGrainLine(
            baseX = baseX,
            top = innerTopLeft.y + cellH * 0.08f,
            bottom = innerTopLeft.y + innerSize.height - cellH * 0.08f,
            amplitude = cellW * (0.035f + (i % 5) * 0.014f),
            phase = phase,
            color = if (i % 3 == 0) Color.Black.copy(alpha = alpha) else Color(0xFFFFD487).copy(alpha = alpha),
            strokeWidth = 0.9f + (i % 3) * 0.35f
        )
    }

    val knots = listOf(
        Offset(innerTopLeft.x + innerSize.width * 0.20f, innerTopLeft.y + innerSize.height * 0.22f),
        Offset(innerTopLeft.x + innerSize.width * 0.62f, innerTopLeft.y + innerSize.height * 0.42f),
        Offset(innerTopLeft.x + innerSize.width * 0.38f, innerTopLeft.y + innerSize.height * 0.76f),
        Offset(innerTopLeft.x + innerSize.width * 0.82f, innerTopLeft.y + innerSize.height * 0.70f)
    )
    for ((index, center) in knots.withIndex()) {
        val w = cellW * (0.42f + (index % 2) * 0.12f)
        val h = cellH * (0.21f + (index % 3) * 0.05f)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF3D1B09).copy(alpha = 0.36f),
                    Color(0xFF7B3B13).copy(alpha = 0.24f),
                    Color.Transparent
                ),
                center = center,
                radius = w
            ),
            topLeft = Offset(center.x - w, center.y - h),
            size = Size(w * 2f, h * 2f)
        )
        for (ring in 0..2) {
            val scale = 0.48f + ring * 0.22f
            drawOval(
                color = Color(0xFF2E1306).copy(alpha = 0.22f - ring * 0.04f),
                topLeft = Offset(center.x - w * scale, center.y - h * scale),
                size = Size(w * scale * 2f, h * scale * 2f),
                style = Stroke(width = 1.2f)
            )
        }
    }

    drawRoundRect(
        color = MARKER_COLOR.copy(alpha = 0.92f),
        topLeft = innerTopLeft,
        size = innerSize,
        cornerRadius = CornerRadius(cellW * 0.08f, cellW * 0.08f),
        style = Stroke(width = 3f)
    )
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x4CFFF2C6),
                Color(0x18FFE0A0),
                Color.Transparent
            ),
            center = Offset(innerTopLeft.x + innerSize.width / 2f, innerTopLeft.y + innerSize.height * 0.08f),
            radius = innerSize.width * 0.72f
        ),
        topLeft = innerTopLeft,
        size = innerSize,
        cornerRadius = CornerRadius(cellW * 0.08f, cellW * 0.08f)
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x28FFF0C0),
                Color.Transparent,
                Color(0x26000000)
            ),
            startY = innerTopLeft.y,
            endY = innerTopLeft.y + innerSize.height
        ),
        topLeft = innerTopLeft,
        size = innerSize,
        cornerRadius = CornerRadius(cellW * 0.08f, cellW * 0.08f)
    )
    drawRoundRect(
        color = Color(0xFFFFD48A).copy(alpha = 0.22f),
        topLeft = Offset(outerTopLeft.x + cellW * 0.12f, outerTopLeft.y + cellH * 0.12f),
        size = Size(outerSize.width - cellW * 0.24f, outerSize.height - cellH * 0.24f),
        cornerRadius = CornerRadius(cellW * 0.11f, cellW * 0.11f),
        style = Stroke(width = cellW * 0.045f)
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.18f),
        topLeft = Offset(outerTopLeft.x + cellW * 0.2f, outerTopLeft.y + cellH * 0.2f),
        size = Size(outerSize.width - cellW * 0.4f, outerSize.height - cellH * 0.4f),
        cornerRadius = CornerRadius(cellW * 0.09f, cellW * 0.09f),
        style = Stroke(width = cellW * 0.035f)
    )
}

private fun DrawScope.drawWoodGrainLine(
    baseX: Float,
    top: Float,
    bottom: Float,
    amplitude: Float,
    phase: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path().apply {
        moveTo(baseX, top)
        val segments = 18
        for (index in 1..segments) {
            val t = index / segments.toFloat()
            val y = top + (bottom - top) * t
            val x = baseX +
                kotlin.math.sin(t * 10.5f + phase) * amplitude +
                kotlin.math.sin(t * 24f + phase * 0.7f) * amplitude * 0.35f
            lineTo(x, y)
        }
    }
    drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
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
        color = Color.Black.copy(alpha = 0.30f),
        start = Offset(start.x + 1.2f, start.y + 1.2f),
        end = Offset(end.x + 1.2f, end.y + 1.2f),
        strokeWidth = strokeWidth + 1.2f
    )
    drawLine(
        color = Color(0xFFFFD48A).copy(alpha = 0.26f),
        start = Offset(start.x - 0.9f, start.y - 0.9f),
        end = Offset(end.x - 0.9f, end.y - 0.9f),
        strokeWidth = strokeWidth
    )
    drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth + 0.4f)
}

private fun DrawScope.drawWoodRiverText(offsetX: Float, offsetY: Float, cellW: Float, cellH: Float) {
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
                colors = listOf(Color(0xFFE4A85B), Color(0xFF9E5622), Color(0xFF4D220B)),
                start = topLeft,
                end = Offset(topLeft.x + plaqueSize.width, topLeft.y + plaqueSize.height)
            ),
            topLeft = topLeft,
            size = plaqueSize,
            cornerRadius = CornerRadius(cellH * 0.08f, cellH * 0.08f)
        )
        drawRoundRect(
            color = Color(0xFFFFD48A).copy(alpha = 0.22f),
            topLeft = Offset(topLeft.x + 1.2f, topLeft.y + 1.2f),
            size = plaqueSize,
            cornerRadius = CornerRadius(cellH * 0.08f, cellH * 0.08f),
            style = Stroke(width = 1.4f)
        )
        drawRoundRect(
            color = Color(0xFF2B1205).copy(alpha = 0.52f),
            topLeft = topLeft,
            size = plaqueSize,
            cornerRadius = CornerRadius(cellH * 0.08f, cellH * 0.08f),
            style = Stroke(width = 2.2f)
        )
    }

    drawContext.canvas.nativeCanvas.apply {
        val highlightPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#F6CA80")
            textSize = cellH * 0.46f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            letterSpacing = 0.12f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
        }
        val carvedPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#351506")
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
        drawEngravedLine(
            start = Offset(cx + sx * gap, cy + sy * gap),
            end = Offset(cx + sx * (gap + len), cy + sy * gap),
            strokeWidth = strokeWidth,
            color = MARKER_COLOR
        )
        drawEngravedLine(
            start = Offset(cx + sx * gap, cy + sy * gap),
            end = Offset(cx + sx * gap, cy + sy * (gap + len)),
            strokeWidth = strokeWidth,
            color = MARKER_COLOR
        )
    }
}

private fun DrawScope.drawPositionHighlight(
    piece: Piece,
    offsetX: Float, offsetY: Float,
    cellW: Float, cellH: Float,
    isFlipped: Boolean,
    pulse: Float
) {
    val displayRow = toDisplayRow(piece.row, isFlipped)
    val displayCol = toDisplayCol(piece.col, isFlipped)
    val cx = offsetX + displayCol * cellW
    val cy = offsetY + displayRow * cellH
    val center = Offset(cx, cy)
    val color = if (piece.side == Side.RED) RED_COLOR else BLACK_COLOR

    drawCircle(color = color.copy(alpha = 0.18f), radius = cellW * 0.66f * pulse, center = center)
    drawCircle(color = color.copy(alpha = 0.18f), radius = cellW * 0.5f, center = center)
    drawCircle(
        color = color.copy(alpha = 0.72f),
        radius = cellW * 0.53f,
        center = center,
        style = Stroke(width = 4.4f)
    )
    drawCircle(
        color = color,
        radius = cellW * 0.4f * pulse,
        center = center,
        style = Stroke(width = 4.5f)
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

private fun DrawScope.drawBoardAlert(text: String, status: GameStatus, cellW: Float) {
    val isFinal = status != GameStatus.PLAYING
    val fillColor = when (status) {
        GameStatus.BLACK_WIN -> BLACK_COLOR
        GameStatus.RED_WIN -> RED_COLOR
        GameStatus.STALEMATE, GameStatus.DRAW -> Color(0xFF5C2D10)
        GameStatus.PLAYING -> Color(0xFFC31B12)
    }
    val center = Offset(size.width / 2f, size.height / 2f)
    val textSize = cellW * if (isFinal) 1.14f else 1.02f
    val panelAlpha = if (isFinal) 0.52f else 0.34f
    val textAlpha = if (isFinal) 0.84f else 0.72f

    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFE8AC).copy(alpha = panelAlpha),
                Color(0xFFD29045).copy(alpha = panelAlpha * 0.62f),
                Color.Transparent
            ),
            center = center,
            radius = cellW * 3.4f
        ),
        topLeft = Offset(center.x - cellW * 3.15f, center.y - cellW * 1.08f),
        size = Size(cellW * 6.3f, cellW * 2.16f),
        cornerRadius = CornerRadius(cellW * 0.24f, cellW * 0.24f)
    )

    drawContext.canvas.nativeCanvas.apply {
        val y = center.y + textSize * 0.34f
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xCCFFE9B1).toArgb()
            style = Paint.Style.STROKE
            strokeWidth = textSize * 0.12f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            this.textSize = textSize
            setShadowLayer(textSize * 0.12f, 0f, textSize * 0.06f, Color(0x661D0E06).toArgb())
        }
        val fillPaint = Paint(strokePaint).apply {
            color = fillColor.copy(alpha = textAlpha).toArgb()
            style = Paint.Style.FILL
            strokeWidth = 0f
            setShadowLayer(textSize * 0.04f, 0f, 0f, Color(0x4DFFF2C3).toArgb())
        }
        drawText(text, center.x, y, strokePaint)
        drawText(text, center.x, y, fillPaint)
    }
}

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
