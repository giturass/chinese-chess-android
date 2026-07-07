package com.xiaomi.chess.engine

import com.xiaomi.chess.model.*

object Evaluator {

    private val pieceValue = mapOf(
        PieceType.KING to 10000,
        PieceType.ADVISOR to 20,
        PieceType.ELEPHANT to 20,
        PieceType.ROOK to 100,
        PieceType.KNIGHT to 45,
        PieceType.CANNON to 45,
        PieceType.PAWN to 10
    )

    // Positional value tables (from Red's perspective, row 0 = top)
    private val pawnPosRed = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,  0,
        0,  0,  0,  0,  0,  0,  0,  0,  0,
        0,  0,  0,  0,  0,  0,  0,  0,  0,
        1,  0,  2,  0,  3,  0,  2,  0,  1,
        4,  0,  8,  0, 10,  0,  8,  0,  4,
        8, 12, 16, 18, 20, 18, 16, 12,  8,
        14, 18, 22, 25, 28, 25, 22, 18, 14,
        0,  0,  0,  0,  0,  0,  0,  0,  0,
        0,  0,  0,  0,  0,  0,  0,  0,  0,
        0,  0,  0,  0,  0,  0,  0,  0,  0
    )

    private val knightPosRed = intArrayOf(
        0, -2,  0,  0,  0,  0,  0, -2,  0,
        2,  0,  4,  0,  0,  0,  4,  0,  2,
        0,  4,  0,  8,  0,  8,  0,  4,  0,
        2,  0,  8,  0,  8,  0,  8,  0,  2,
        0,  4,  0,  8, 10,  8,  0,  4,  0,
        0,  4,  8, 10, 12, 10,  8,  4,  0,
        2,  0,  8, 10, 12, 10,  8,  0,  2,
        0,  4,  0,  8,  8,  8,  0,  4,  0,
        2,  0,  4,  0,  0,  0,  4,  0,  2,
        0, -2,  0,  0,  0,  0,  0, -2,  0
    )

    private val cannonPosRed = intArrayOf(
        0,  0,  2,  4,  4,  4,  2,  0,  0,
        0,  2,  4,  6,  6,  6,  4,  2,  0,
        2,  0,  4,  6,  8,  6,  4,  0,  2,
        0,  0,  0,  4,  6,  4,  0,  0,  0,
        0,  0,  0,  2,  4,  2,  0,  0,  0,
        0,  0, -2,  0,  2,  0, -2,  0,  0,
        0,  0,  0,  0,  0,  0,  0,  0,  0,
        0,  0,  0,  0,  0,  0,  0,  0,  0,
        0,  0,  0,  0,  0,  0,  0,  0,  0,
        0,  0,  0,  0,  0,  0,  0,  0,  0
    )

    private val rookPosRed = intArrayOf(
        0,  0,  2,  4,  6,  4,  2,  0,  0,
        0,  2,  4,  6,  8,  6,  4,  2,  0,
        0,  2,  4,  6,  8,  6,  4,  2,  0,
        0,  0,  0,  4,  6,  4,  0,  0,  0,
        0,  0,  0,  4,  6,  4,  0,  0,  0,
        0,  0,  0,  4,  6,  4,  0,  0,  0,
        0,  0,  0,  4,  6,  4,  0,  0,  0,
        0,  0,  0,  4,  6,  4,  0,  0,  0,
        0,  2,  4,  6,  8,  6,  4,  2,  0,
        0,  0,  2,  4,  6,  4,  2,  0,  0
    )

    fun evaluate(board: Board, side: Side): Int {
        var score = 0

        for (piece in board.pieces) {
            val baseValue = pieceValue[piece.type] ?: 0
            val posValue = getPositionValue(piece)
            val total = baseValue + posValue

            score += if (piece.side == side) total else -total
        }

        // Check bonus
        if (board.isInCheck(side.opposite())) score += 50
        if (board.isInCheck(side)) score -= 50

        return score
    }

    private fun getPositionValue(piece: Piece): Int {
        val row = piece.row
        val col = piece.col

        return when (piece.type) {
            PieceType.PAWN -> {
                if (piece.side == Side.RED) {
                    pawnPosRed[row * 9 + col]
                } else {
                    pawnPosRed[(9 - row) * 9 + (8 - col)]
                }
            }
            PieceType.KNIGHT -> {
                if (piece.side == Side.RED) {
                    knightPosRed[row * 9 + col]
                } else {
                    knightPosRed[(9 - row) * 9 + (8 - col)]
                }
            }
            PieceType.CANNON -> {
                if (piece.side == Side.RED) {
                    cannonPosRed[row * 9 + col]
                } else {
                    cannonPosRed[(9 - row) * 9 + (8 - col)]
                }
            }
            PieceType.ROOK -> {
                if (piece.side == Side.RED) {
                    rookPosRed[row * 9 + col]
                } else {
                    rookPosRed[(9 - row) * 9 + (8 - col)]
                }
            }
            PieceType.KING -> {
                if (piece.side == Side.RED) {
                    if (row == 9 && col == 4) 0 else -5
                } else {
                    if (row == 0 && col == 4) 0 else -5
                }
            }
            else -> 0
        }
    }
}
