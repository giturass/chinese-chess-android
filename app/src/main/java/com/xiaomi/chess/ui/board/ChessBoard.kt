package com.xiaomi.chess.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.xiaomi.chess.model.*

private val BOARD_COLOR = Color(0xFFDEB887)
private val LINE_COLOR = Color(0xFF4A2800)
private val GRID_COLOR = Color(0xFF3E1F00)
private val RED_COLOR = Color(0xFFCC0000)
private val BLACK_COLOR = Color(0xFF1A1A1A)
private val SELECTED_COLOR = Color(0x80FFD700)
private val LAST_MOVE_COLOR = Color(0x8000AA00)
private val CHECK_COLOR = Color(0x80FF0000)

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

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(9f / 10f)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cellW = size.width / 10f
                    val cellH = size.height / 11f
                    val col = ((offset.x - cellW / 2) / cellW).toInt()
                    val row = if (isFlipped) {
                        9 - ((offset.y - cellH) / cellH).toInt()
                    } else {
                        ((offset.y - cellH) / cellH).toInt()
                    }
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

        // Background
        drawRect(
            color = BOARD_COLOR,
            topLeft = Offset(offsetX - cellW * 0.1f, offsetY - cellH * 0.1f),
            size = Size(cellW * 9.2f, cellH * 10.2f)
        )

        drawGrid(offsetX, offsetY, cellW, cellH)
        drawPalace(offsetX, offsetY, cellW, cellH)
        drawRiver(offsetX, offsetY, cellW, cellH)

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
            val cx = offsetX + move.toCol * cellW
            val cy = offsetY + move.toRow * cellH
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
            val cx = offsetX + col * cellW
            val cy = offsetY + row * cellH
            drawPiece(piece, cx, cy, cellW * 0.43f)
        }
    }
}

private fun DrawScope.drawGrid(offsetX: Float, offsetY: Float, cellW: Float, cellH: Float) {
    val stroke = Stroke(width = 2f)

    // Horizontal lines
    for (r in 0..9) {
        drawLine(
            color = GRID_COLOR,
            start = Offset(offsetX, offsetY + r * cellH),
            end = Offset(offsetX + 8 * cellW, offsetY + r * cellH),
            strokeWidth = 2f
        )
    }

    // Vertical lines
    for (c in 0..8) {
        if (c == 0 || c == 8) {
            drawLine(
                color = GRID_COLOR,
                start = Offset(offsetX + c * cellW, offsetY),
                end = Offset(offsetX + c * cellW, offsetY + 9 * cellH),
                strokeWidth = 2f
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
    val palaceColor = Color(0xFF808080)
    // Top palace
    drawLine(palaceColor, Offset(offsetX + 3 * cellW, offsetY), Offset(offsetX + 5 * cellW, offsetY + 2 * cellH), strokeWidth = 2f)
    drawLine(palaceColor, Offset(offsetX + 5 * cellW, offsetY), Offset(offsetX + 3 * cellW, offsetY + 2 * cellH), strokeWidth = 2f)

    // Bottom palace
    drawLine(palaceColor, Offset(offsetX + 3 * cellW, offsetY + 7 * cellH), Offset(offsetX + 5 * cellW, offsetY + 9 * cellH), strokeWidth = 2f)
    drawLine(palaceColor, Offset(offsetX + 5 * cellW, offsetY + 7 * cellH), Offset(offsetX + 3 * cellW, offsetY + 9 * cellH), strokeWidth = 2f)
}

private fun DrawScope.drawRiver(offsetX: Float, offsetY: Float, cellW: Float, cellH: Float) {
    val y = offsetY + 4.5f * cellH
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#4A2800")
            textSize = cellH * 0.55f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
        drawText("楚  河", offsetX + 2 * cellW, y + cellH * 0.18f, paint)
        drawText("汉  界", offsetX + 6 * cellW, y + cellH * 0.18f, paint)
    }
}

private fun DrawScope.drawPositionHighlight(
    row: Int, col: Int,
    offsetX: Float, offsetY: Float,
    cellW: Float, cellH: Float,
    color: Color,
    isFlipped: Boolean
) {
    val displayRow = if (isFlipped) 9 - row else row
    val cx = offsetX + col * cellW
    val cy = offsetY + displayRow * cellH
    drawCircle(color = color, radius = cellW * 0.46f, center = Offset(cx, cy))
}

private fun DrawScope.drawPiece(piece: Piece, cx: Float, cy: Float, radius: Float) {
    val color = if (piece.side == Side.RED) RED_COLOR else BLACK_COLOR
    val bgColor = Color(0xFFFFF8DC)

    // Outer circle
    drawCircle(color = bgColor, radius = radius, center = Offset(cx, cy))
    drawCircle(color = color, radius = radius, center = Offset(cx, cy), style = Stroke(width = 3f))

    // Inner circle
    drawCircle(color = bgColor, radius = radius * 0.82f, center = Offset(cx, cy))
    drawCircle(color = color, radius = radius * 0.82f, center = Offset(cx, cy), style = Stroke(width = 1.5f))

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
