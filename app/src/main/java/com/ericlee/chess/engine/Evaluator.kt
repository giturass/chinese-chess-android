package com.ericlee.chess.engine

import com.ericlee.chess.model.*

object Evaluator {

    private val pieceValue = mapOf(
        PieceType.KING to 100000,
        PieceType.ADVISOR to 200,
        PieceType.ELEPHANT to 200,
        PieceType.ROOK to 1000,
        PieceType.KNIGHT to 430,
        PieceType.CANNON to 450,
        PieceType.PAWN to 100
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
            val safetyValue = getSafetyValue(board, piece)
            val mobilityValue = getMobilityValue(board, piece)
            val total = baseValue + posValue + safetyValue + mobilityValue

            score += if (piece.side == side) total else -total
        }

        if (board.isInCheck(side.opposite())) score += 180
        if (board.isInCheck(side)) score -= 220
        score += getKingSafety(board, side)
        score -= getKingSafety(board, side.opposite())

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

    private fun getSafetyValue(board: Board, piece: Piece): Int {
        if (piece.type == PieceType.KING) return 0

        val value = pieceValue[piece.type] ?: 0
        val attacked = board.isSquareAttacked(piece.row, piece.col, piece.side.opposite())
        val protected = board.isPieceProtected(piece)

        return when {
            attacked && !protected -> -value / 3
            attacked && protected -> -value / 8
            protected -> value / 18
            else -> 0
        }
    }

    private fun getMobilityValue(board: Board, piece: Piece): Int {
        val mobility = board.getCandidateMoves(piece).size
        val weight = when (piece.type) {
            PieceType.ROOK -> 4
            PieceType.CANNON -> 3
            PieceType.KNIGHT -> 3
            PieceType.PAWN -> 2
            else -> 1
        }
        return mobility * weight
    }

    private fun getKingSafety(board: Board, side: Side): Int {
        val king = board.findKing(side) ?: return -100000
        var score = 0
        val defenders = board.getPiecesBySide(side).count { piece ->
            piece.type != PieceType.KING &&
                kotlin.math.abs(piece.row - king.row) <= 2 &&
                kotlin.math.abs(piece.col - king.col) <= 2
        }
        score += defenders * 12

        val opponentPressure = board.getPiecesBySide(side.opposite()).count { piece ->
            kotlin.math.abs(piece.row - king.row) <= 3 &&
                kotlin.math.abs(piece.col - king.col) <= 3
        }
        score -= opponentPressure * 18

        return score
    }
}
