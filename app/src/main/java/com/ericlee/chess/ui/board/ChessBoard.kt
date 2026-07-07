package com.ericlee.chess.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.ericlee.chess.model.*
import kotlin.math.roundToInt

private val BOARD_COLOR = Color(0xFFECCB8F)
private val BOARD_INNER_COLOR = Color(0xFFF4D9A5)
private val EDGE_COLOR = Color(0xFF6F3914)
private val GRID_COLOR = Color(0xFF4B260D)
private val RED_COLOR = Color(0xFFB32318)
private val BLACK_COLOR = Color(0xFF1F1711)
private val SELECTED_COLOR = Color(0x80F4C430)
private val LAST_MOVE_COLOR = Color(0x805CA66B)
private val CHECK_COLOR = Color(0xB8C31B12)
private val MARKER_COLOR = Color(0xFF7B481E)
private val PIECE_BG = Color(0xFFFFF4D2)

@Composable
fun ChessBoard(
    board: Board,
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

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(9f / 10f)
            .pointerInput(isFlipped, onPositionClick) {
                detectTapGestures { offset ->
                    val cellW = size.width / 10f
                    val cellH = size.height / 11f
                    val displayCol = ((offset.x - cellW / 2) / cellW).roundToInt()
                    val displayRow = ((offset.y - cellH) / cellH).roundToInt()
                    val row = toBoardRow(displayRow, isFlipped)
                    val col = toBoardCol(displayCol, isFlipped)
                    if (row in 0..9 && col in 0..8) {
                        onPositionClick(row, col)
                    }
                }
            }
    ) {
        val cellW = size.width / 10f
        val cellH = size.height / 11f
        val offsetX = cellW / 2
        val offsetY = cellH

        drawBoardBackground(offsetX, offsetY, cellW, cellH)

        drawGrid(offsetX, offsetY, cellW, cellH)
        drawPalace(offsetX, offsetY, cellW, cellH)
        drawRiver(offsetX, offsetY, cellW, cellH)
        drawPositionMarkers(offsetX, offsetY, cellW, cellH)

        // Last move highlight
        if (lastMove != null) {
            drawPositionHighlight(lastMove.fromRow, lastMove.fromCol, offsetX, offsetY, cellW, cellH, LAST_MOVE_COLOR, isFlipped)
            drawPositionHighlight(lastMove.toRow, lastMove.toCol, offsetX, offsetY, cellW, cellH, LAST_MOVE_COLOR, isFlipped)
        }

        // Selected piece highlight
        if (selectedPiece != null) {
            drawPositionHighlight(selectedPiece.row, selectedPiece.col, offsetX, offsetY, cellW, cellH, SELECTED_COLOR, isFlipped)
        }

        // Legal move dots
        for (move in legalMoves) {
            val cx = offsetX + toDisplayCol(move.toCol, isFlipped) * cellW
            val cy = offsetY + toDisplayRow(move.toRow, isFlipped) * cellH
            val target = board.getPiece(move.toRow, move.toCol)
            if (target != null) {
                drawCircle(
                    color = CHECK_COLOR,
                    radius = cellW * 0.45f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4f)
                )
            } else {
                drawCircle(
                    color = Color(0x80006600),
                    radius = cellW * 0.12f,
                    center = Offset(cx, cy)
                )
            }
        }

        // Draw pieces
        for ((row, col) in pieceMap.keys) {
            val piece = pieceMap[Pair(row, col)] ?: continue
            val cx = offsetX + toDisplayCol(col, isFlipped) * cellW
            val cy = offsetY + toDisplayRow(row, isFlipped) * cellH
            drawPiece(piece, cx, cy, cellW * 0.43f)
        }
    }
}

private fun toDisplayRow(row: Int, isFlipped: Boolean): Int = if (isFlipped) 9 - row else row

private fun toDisplayCol(col: Int, isFlipped: Boolean): Int = if (isFlipped) 8 - col else col

private fun toBoardRow(displayRow: Int, isFlipped: Boolean): Int = if (isFlipped) 9 - displayRow else displayRow

private fun toBoardCol(displayCol: Int, isFlipped: Boolean): Int = if (isFlipped) 8 - displayCol else displayCol

private fun DrawScope.drawBoardBackground(offsetX: Float, offsetY: Float, cellW: Float, cellH: Float) {
    val outerTopLeft = Offset(offsetX - cellW * 0.34f, offsetY - cellH * 0.38f)
    val outerSize = Size(cellW * 8.68f, cellH * 9.76f)
    val innerTopLeft = Offset(offsetX - cellW * 0.18f, offsetY - cellH * 0.22f)
    val innerSize = Size(cellW * 8.36f, cellH * 9.44f)

    drawRoundRect(
        color = EDGE_COLOR,
        topLeft = outerTopLeft,
        size = outerSize,
        cornerRadius = CornerRadius(cellW * 0.16f, cellW * 0.16f)
    )
    drawRoundRect(
        color = BOARD_COLOR,
        topLeft = Offset(outerTopLeft.x + cellW * 0.07f, outerTopLeft.y + cellH * 0.07f),
        size = Size(outerSize.width - cellW * 0.14f, outerSize.height - cellH * 0.14f),
        cornerRadius = CornerRadius(cellW * 0.12f, cellW * 0.12f)
    )
    drawRect(color = BOARD_INNER_COLOR, topLeft = innerTopLeft, size = innerSize)
    drawRoundRect(
        color = MARKER_COLOR,
        topLeft = innerTopLeft,
        size = innerSize,
        cornerRadius = CornerRadius(cellW * 0.08f, cellW * 0.08f),
        style = Stroke(width = 3f)
    )
}

private fun DrawScope.drawGrid(offsetX: Float, offsetY: Float, cellW: Float, cellH: Float) {
    // Horizontal lines
    for (r in 0..9) {
        drawLine(
            color = GRID_COLOR,
            start = Offset(offsetX, offsetY + r * cellH),
            end = Offset(offsetX + 8 * cellW, offsetY + r * cellH),
            strokeWidth = if (r == 0 || r == 9) 3.2f else 2f
        )
    }

    // Vertical lines
    for (c in 0..8) {
        if (c == 0 || c == 8) {
            drawLine(
                color = GRID_COLOR,
                start = Offset(offsetX + c * cellW, offsetY),
                end = Offset(offsetX + c * cellW, offsetY + 9 * cellH),
                strokeWidth = if (c == 0 || c == 8) 3.2f else 2f
            )
        } else {
            drawLine(
                color = GRID_COLOR,
                start = Offset(offsetX + c * cellW, offsetY),
                end = Offset(offsetX + c * cellW, offsetY + 4 * cellH),
                strokeWidth = 2f
            )
            drawLine(
                color = GRID_COLOR,
                start = Offset(offsetX + c * cellW, offsetY + 5 * cellH),
                end = Offset(offsetX + c * cellW, offsetY + 9 * cellH),
                strokeWidth = 2f
            )
        }
    }
}

private fun DrawScope.drawPalace(offsetX: Float, offsetY: Float, cellW: Float, cellH: Float) {
    val palaceColor = MARKER_COLOR
    // Top palace
    drawLine(palaceColor, Offset(offsetX + 3 * cellW, offsetY), Offset(offsetX + 5 * cellW, offsetY + 2 * cellH), strokeWidth = 2f)
    drawLine(palaceColor, Offset(offsetX + 5 * cellW, offsetY), Offset(offsetX + 3 * cellW, offsetY + 2 * cellH), strokeWidth = 2f)

    // Bottom palace
    drawLine(palaceColor, Offset(offsetX + 3 * cellW, offsetY + 7 * cellH), Offset(offsetX + 5 * cellW, offsetY + 9 * cellH), strokeWidth = 2f)
    drawLine(palaceColor, Offset(offsetX + 5 * cellW, offsetY + 7 * cellH), Offset(offsetX + 3 * cellW, offsetY + 9 * cellH), strokeWidth = 2f)
}

private fun DrawScope.drawRiver(offsetX: Float, offsetY: Float, cellW: Float, cellH: Float) {
    val y = offsetY + 4.5f * cellH
    drawLine(
        color = MARKER_COLOR,
        start = Offset(offsetX + cellW * 0.3f, y),
        end = Offset(offsetX + cellW * 3.7f, y),
        strokeWidth = 1.4f
    )
    drawLine(
        color = MARKER_COLOR,
        start = Offset(offsetX + cellW * 4.3f, y),
        end = Offset(offsetX + cellW * 7.7f, y),
        strokeWidth = 1.4f
    )
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#6F3914")
            textSize = cellH * 0.55f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            letterSpacing = 0.08f
            isAntiAlias = true
        }
        drawText("楚  河", offsetX + 2 * cellW, y + cellH * 0.18f, paint)
        drawText("汉  界", offsetX + 6 * cellW, y + cellH * 0.18f, paint)
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
    isFlipped: Boolean
) {
    val displayRow = toDisplayRow(row, isFlipped)
    val displayCol = toDisplayCol(col, isFlipped)
    val cx = offsetX + displayCol * cellW
    val cy = offsetY + displayRow * cellH
    drawCircle(color = color, radius = cellW * 0.46f, center = Offset(cx, cy))
}

private fun DrawScope.drawPiece(piece: Piece, cx: Float, cy: Float, radius: Float) {
    val color = if (piece.side == Side.RED) RED_COLOR else BLACK_COLOR
    val shadowColor = Color(0x55000000)

    drawCircle(color = shadowColor, radius = radius * 1.03f, center = Offset(cx + radius * 0.06f, cy + radius * 0.08f))
    drawCircle(color = Color(0xFF7A4318), radius = radius * 1.02f, center = Offset(cx, cy))
    drawCircle(color = PIECE_BG, radius = radius * 0.94f, center = Offset(cx, cy))
    drawCircle(color = color, radius = radius * 0.94f, center = Offset(cx, cy), style = Stroke(width = 3.2f))

    drawCircle(color = Color(0xFFFFF9E7), radius = radius * 0.76f, center = Offset(cx, cy))
    drawCircle(color = color, radius = radius * 0.76f, center = Offset(cx, cy), style = Stroke(width = 1.4f))

    // Character
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
        drawText(piece.char, cx, textY, paint)
    }
}
