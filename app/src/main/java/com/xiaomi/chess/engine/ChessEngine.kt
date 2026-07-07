package com.xiaomi.chess.engine

import com.xiaomi.chess.model.*
import kotlin.math.max
import kotlin.math.min

class ChessEngine(private val aiSide: Side = Side.BLACK) {

    private var nodesSearched = 0
    private val transpositionTable = HashMap<Long, Int>(100000)

    fun findBestMove(board: Board, depth: Int): Move? {
        nodesSearched = 0
        transpositionTable.clear()

        val moves = board.getAllLegalMoves(aiSide)
        if (moves.isEmpty()) return null

        val sortedMoves = sortMoves(board, moves)
        var bestMove = sortedMoves[0]
        var bestScore = Int.MIN_VALUE

        for (move in sortedMoves) {
            val captured = board.makeMove(move)
            val score = -alphaBeta(board, depth - 1, Int.MIN_VALUE + 1, Int.MAX_VALUE - 1, aiSide.opposite())
            board.undoMove(move)

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }

        return bestMove
    }

    private fun alphaBeta(
        board: Board,
        depth: Int,
        alpha: Int,
        beta: Int,
        currentSide: Side
    ): Int {
        nodesSearched++

        if (depth <= 0) {
            return Evaluator.evaluate(board, currentSide)
        }

        val moves = board.getAllLegalMoves(currentSide)
        if (moves.isEmpty()) {
            return if (board.isInCheck(currentSide)) -100000 + (100 - depth) else 0
        }

        var a = alpha
        val sortedMoves = sortMoves(board, moves)

        for (move in sortedMoves) {
            val captured = board.makeMove(move)
            val score = -alphaBeta(board, depth - 1, -beta, -a, currentSide.opposite())
            board.undoMove(move)

            if (score >= beta) {
                return beta
            }
            if (score > a) {
                a = score
            }
        }

        return a
    }

    private fun sortMoves(board: Board, moves: List<Move>): List<Move> {
        return moves.sortedByDescending { move ->
            var score = 0
            val target = board.getPiece(move.toRow, move.toCol)
            if (target != null) {
                score += pieceValueForSort(target.type) * 10 - pieceValueForSort(
                    board.getPiece(move.fromRow, move.fromCol)?.type ?: PieceType.PAWN
                )
            }
            if (board.getPiece(move.toRow, move.toCol) == null) {
                val midR = (move.fromRow + move.toRow) / 2
                val midC = (move.fromCol + move.toCol) / 2
                if (board.isInCheck(board.getPiece(move.fromRow, move.fromCol)?.side?.opposite() ?: Side.RED)) {
                    score += 5
                }
            }
            score
        }
    }

    private fun pieceValueForSort(type: PieceType): Int = when (type) {
        PieceType.KING -> 10000
        PieceType.ROOK -> 100
        PieceType.CANNON -> 45
        PieceType.KNIGHT -> 45
        PieceType.PAWN -> 10
        PieceType.ADVISOR -> 20
        PieceType.ELEPHANT -> 20
    }

    fun getSearchInfo(): String = "Searched $nodesSearched nodes"
}
