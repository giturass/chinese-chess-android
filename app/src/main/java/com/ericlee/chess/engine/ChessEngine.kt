package com.ericlee.chess.engine

import com.ericlee.chess.model.Board
import com.ericlee.chess.model.Move
import com.ericlee.chess.model.PieceType
import com.ericlee.chess.model.Side
import kotlin.math.abs

class ChessEngine(private val aiSide: Side = Side.BLACK) {

    private var nodesSearched = 0

    fun findBestMove(
        board: Board,
        depth: Int,
        positionCounts: Map<String, Int> = emptyMap()
    ): Move? {
        nodesSearched = 0

        val searchDepth = depth.coerceIn(1, 5)
        val counts = positionCounts.toMutableMap()
        if (counts.isEmpty()) {
            counts.increment(board.positionKey(aiSide))
        }

        var bestMove: Move? = null
        for (currentDepth in 1..searchDepth) {
            val result = searchRoot(board, currentDepth, counts)
            if (result.move != null) {
                bestMove = result.move
            }
        }

        return bestMove
    }

    private fun searchRoot(
        board: Board,
        depth: Int,
        positionCounts: MutableMap<String, Int>
    ): SearchResult {
        val moves = legalMovesForSearch(board, aiSide, positionCounts)
        if (moves.isEmpty()) return SearchResult(null, Int.MIN_VALUE + 1)

        var bestMove = moves.first()
        var bestScore = Int.MIN_VALUE + 1
        var alpha = Int.MIN_VALUE + 1
        val beta = Int.MAX_VALUE - 1

        for (move in sortMoves(board, moves)) {
            val appliedMove = applyMove(board, move, aiSide, positionCounts) ?: continue
            val score = -alphaBeta(
                board = board,
                depth = depth - 1,
                alpha = -beta,
                beta = -alpha,
                currentSide = aiSide.opposite(),
                positionCounts = positionCounts,
                ply = 1
            )
            undoAppliedMove(board, appliedMove, positionCounts)

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            if (score > alpha) {
                alpha = score
            }
        }

        return SearchResult(bestMove, bestScore)
    }

    private fun alphaBeta(
        board: Board,
        depth: Int,
        alpha: Int,
        beta: Int,
        currentSide: Side,
        positionCounts: MutableMap<String, Int>,
        ply: Int
    ): Int {
        nodesSearched++

        if (board.findKing(currentSide) == null) return -WIN_SCORE + ply
        if (board.findKing(currentSide.opposite()) == null) return WIN_SCORE - ply

        if (depth <= 0) {
            return quiescence(board, alpha, beta, currentSide, positionCounts, ply, 0)
        }

        val moves = legalMovesForSearch(board, currentSide, positionCounts)
        if (moves.isEmpty()) {
            return if (board.isInCheck(currentSide)) -WIN_SCORE + ply else 0
        }

        var a = alpha
        for (move in sortMoves(board, moves)) {
            val appliedMove = applyMove(board, move, currentSide, positionCounts) ?: continue
            val score = -alphaBeta(
                board = board,
                depth = depth - 1,
                alpha = -beta,
                beta = -a,
                currentSide = currentSide.opposite(),
                positionCounts = positionCounts,
                ply = ply + 1
            )
            undoAppliedMove(board, appliedMove, positionCounts)

            if (score >= beta) return beta
            if (score > a) a = score
        }

        return a
    }

    private fun quiescence(
        board: Board,
        alpha: Int,
        beta: Int,
        currentSide: Side,
        positionCounts: MutableMap<String, Int>,
        ply: Int,
        qDepth: Int
    ): Int {
        nodesSearched++

        val inCheck = board.isInCheck(currentSide)
        val standPat = Evaluator.evaluate(board, currentSide)
        if (!inCheck && standPat >= beta) return beta

        var a = if (inCheck) alpha else maxOf(alpha, standPat)
        if (qDepth >= MAX_QUIESCENCE_DEPTH) return a

        val moves = legalMovesForSearch(board, currentSide, positionCounts)
            .filter { isTacticalMove(board, it, currentSide) }
        if (moves.isEmpty() && inCheck) return -WIN_SCORE + ply

        for (move in sortMoves(board, moves)) {
            val appliedMove = applyMove(board, move, currentSide, positionCounts) ?: continue
            val score = -quiescence(
                board = board,
                alpha = -beta,
                beta = -a,
                currentSide = currentSide.opposite(),
                positionCounts = positionCounts,
                ply = ply + 1,
                qDepth = qDepth + 1
            )
            undoAppliedMove(board, appliedMove, positionCounts)

            if (score >= beta) return beta
            if (score > a) a = score
        }

        return a
    }

    private fun legalMovesForSearch(
        board: Board,
        side: Side,
        positionCounts: MutableMap<String, Int>
    ): List<Move> {
        return board.getAllLegalMoves(side).filter { move ->
            !wouldCauseLongCheck(board, move, side, positionCounts)
        }
    }

    private fun wouldCauseLongCheck(
        board: Board,
        move: Move,
        side: Side,
        positionCounts: Map<String, Int>
    ): Boolean {
        val actualMove = move.copy(captured = board.makeMove(move))
        val targetSide = side.opposite()
        val givesCheck = board.isInCheck(targetSide)
        val repeated = (positionCounts[board.positionKey(targetSide)] ?: 0) >= 2
        board.undoMove(actualMove)
        return givesCheck && repeated
    }

    private fun applyMove(
        board: Board,
        move: Move,
        side: Side,
        positionCounts: MutableMap<String, Int>
    ): AppliedMove? {
        val actualMove = move.copy(captured = board.makeMove(move))
        val nextSide = side.opposite()
        val key = board.positionKey(nextSide)
        if (board.isInCheck(nextSide) && (positionCounts[key] ?: 0) >= 2) {
            board.undoMove(actualMove)
            return null
        }
        positionCounts.increment(key)
        return AppliedMove(actualMove, key)
    }

    private fun undoAppliedMove(
        board: Board,
        appliedMove: AppliedMove,
        positionCounts: MutableMap<String, Int>
    ) {
        positionCounts.decrement(appliedMove.positionKey)
        board.undoMove(appliedMove.move)
    }

    private fun isTacticalMove(board: Board, move: Move, side: Side): Boolean {
        if (board.getPiece(move.toRow, move.toCol) != null) return true
        if (board.isInCheck(side)) return true

        val actualMove = move.copy(captured = board.makeMove(move))
        val givesCheck = board.isInCheck(side.opposite())
        board.undoMove(actualMove)

        return givesCheck
    }

    private fun sortMoves(board: Board, moves: List<Move>): List<Move> {
        return moves.sortedByDescending { move ->
            var score = 0
            val movingPiece = board.getPiece(move.fromRow, move.fromCol)
            val target = board.getPiece(move.toRow, move.toCol)

            if (target != null) {
                score += pieceValueForSort(target.type) * 20 -
                    pieceValueForSort(movingPiece?.type ?: PieceType.PAWN)
                if (target.type == PieceType.KING) score += WIN_SCORE / 10
            }

            if (movingPiece != null) {
                val actualMove = move.copy(captured = board.makeMove(move))
                val movedPiece = board.getPiece(move.toRow, move.toCol)
                if (board.isInCheck(movingPiece.side.opposite())) {
                    score += 900
                }
                if (movedPiece != null) {
                    if (board.isSquareAttacked(move.toRow, move.toCol, movingPiece.side.opposite())) {
                        score -= pieceValueForSort(movedPiece.type) / 2
                    }
                    if (board.isPieceProtected(movedPiece)) {
                        score += pieceValueForSort(movedPiece.type) / 5
                    }
                }
                board.undoMove(actualMove)

                score += forwardProgressBonus(movingPiece.side, move.fromRow, move.toRow, movingPiece.type)
                score -= abs(move.toCol - 4)
            }

            score
        }
    }

    private fun forwardProgressBonus(side: Side, fromRow: Int, toRow: Int, type: PieceType): Int {
        val forward = if (side == Side.RED) fromRow - toRow else toRow - fromRow
        return when (type) {
            PieceType.PAWN -> forward * 18
            PieceType.KNIGHT, PieceType.CANNON -> forward * 4
            else -> 0
        }
    }

    private fun pieceValueForSort(type: PieceType): Int = when (type) {
        PieceType.KING -> 100000
        PieceType.ROOK -> 1000
        PieceType.CANNON -> 450
        PieceType.KNIGHT -> 430
        PieceType.PAWN -> 100
        PieceType.ADVISOR -> 200
        PieceType.ELEPHANT -> 200
    }

    fun getSearchInfo(): String = "Searched $nodesSearched nodes"

    private fun MutableMap<String, Int>.increment(key: String) {
        this[key] = (this[key] ?: 0) + 1
    }

    private fun MutableMap<String, Int>.decrement(key: String) {
        val value = (this[key] ?: 0) - 1
        if (value <= 0) {
            remove(key)
        } else {
            this[key] = value
        }
    }

    private data class SearchResult(val move: Move?, val score: Int)

    private data class AppliedMove(val move: Move, val positionKey: String)

    private companion object {
        const val WIN_SCORE = 1_000_000
        const val MAX_QUIESCENCE_DEPTH = 4
    }
}
