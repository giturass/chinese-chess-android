package com.ericlee.chess.engine

import com.ericlee.chess.model.Board
import com.ericlee.chess.model.Move
import com.ericlee.chess.model.PieceType
import com.ericlee.chess.model.Side

class ChessEngine(private val aiSide: Side = Side.BLACK) {

    private var nodesSearched = 0
    private val transpositionTable = HashMap<String, HashEntry>(HASH_LIMIT)
    private val killers = Array(MAX_PLY) { IntArray(2) }
    private val history = HashMap<Int, Int>()

    fun findBestMove(
        board: Board,
        depth: Int,
        positionCounts: Map<String, Int> = emptyMap()
    ): Move? {
        nodesSearched = 0
        transpositionTable.clear()
        history.clear()
        for (row in killers) {
            row[0] = 0
            row[1] = 0
        }

        val searchDepth = depth.coerceIn(1, 5) + 1
        val counts = positionCounts.toMutableMap()
        if (counts.isEmpty()) {
            counts.increment(board.positionKey(aiSide))
        }

        var bestMove: Move? = null
        var alpha = -MATE_SCORE
        var beta = MATE_SCORE

        for (currentDepth in 1..searchDepth) {
            val result = searchRoot(board, currentDepth, alpha, beta, counts)
            if (result.move != null) {
                bestMove = result.move
            }

            if (result.score <= alpha || result.score >= beta) {
                val widened = searchRoot(board, currentDepth, -MATE_SCORE, MATE_SCORE, counts)
                if (widened.move != null) {
                    bestMove = widened.move
                }
                alpha = widened.score - ASPIRATION_WINDOW
                beta = widened.score + ASPIRATION_WINDOW
            } else {
                alpha = result.score - ASPIRATION_WINDOW
                beta = result.score + ASPIRATION_WINDOW
            }
        }

        return bestMove
    }

    private fun searchRoot(
        board: Board,
        depth: Int,
        alphaStart: Int,
        betaStart: Int,
        positionCounts: MutableMap<String, Int>
    ): SearchResult {
        val moves = orderedMoves(board, legalMovesForSearch(board, aiSide, positionCounts), aiSide, null, 0)
        if (moves.isEmpty()) return SearchResult(null, -MATE_SCORE)

        var bestMove = moves.first()
        var bestScore = -MATE_SCORE
        var alpha = alphaStart
        val beta = betaStart

        for ((index, move) in moves.withIndex()) {
            val applied = applyMove(board, move, aiSide, positionCounts) ?: continue
            val givesCheck = board.isInCheck(aiSide.opposite())
            val nextDepth = depth - 1 + if (givesCheck && depth < 4) 1 else 0
            val score = if (index == 0) {
                -searchPv(board, aiSide.opposite(), nextDepth, -beta, -alpha, 1, positionCounts)
            } else {
                val scout = -searchCut(board, aiSide.opposite(), nextDepth, -alpha, 1, positionCounts)
                if (scout > alpha && scout < beta) {
                    -searchPv(board, aiSide.opposite(), nextDepth, -beta, -alpha, 1, positionCounts)
                } else {
                    scout
                }
            }
            undoAppliedMove(board, applied, positionCounts)

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

    private fun searchPv(
        board: Board,
        side: Side,
        depth: Int,
        alphaStart: Int,
        beta: Int,
        ply: Int,
        positionCounts: MutableMap<String, Int>
    ): Int {
        if (ply >= MAX_PLY) return Evaluator.evaluate(board, side)
        if (board.findKing(side) == null) return -MATE_SCORE + ply
        if (board.findKing(side.opposite()) == null) return MATE_SCORE - ply
        if (depth <= 0) return quiescence(board, side, alphaStart, beta, ply, 0, positionCounts)

        nodesSearched++
        val alphaOriginal = alphaStart
        var alpha = alphaStart
        val key = board.positionKey(side)
        val hashMove = transpositionTable[key]?.takeIf { it.depth >= depth }?.let { entry ->
            val hit = entry.valueFor(alpha, beta)
            if (hit != null) return hit
            entry.moveKey
        }

        val moves = orderedMoves(board, legalMovesForSearch(board, side, positionCounts), side, hashMove, ply)
        if (moves.isEmpty()) return if (board.isInCheck(side)) -MATE_SCORE + ply else 0

        var bestMoveKey = 0
        var bestScore = -MATE_SCORE

        for ((index, move) in moves.withIndex()) {
            val applied = applyMove(board, move, side, positionCounts) ?: continue
            val givesCheck = board.isInCheck(side.opposite())
            val nextDepth = depth - 1 + if (givesCheck && depth < 4) 1 else 0
            val score = if (index == 0) {
                -searchPv(board, side.opposite(), nextDepth, -beta, -alpha, ply + 1, positionCounts)
            } else {
                val scout = -searchCut(board, side.opposite(), nextDepth, -alpha, ply + 1, positionCounts)
                if (scout > alpha && scout < beta) {
                    -searchPv(board, side.opposite(), nextDepth, -beta, -alpha, ply + 1, positionCounts)
                } else {
                    scout
                }
            }
            undoAppliedMove(board, applied, positionCounts)

            if (score > bestScore) {
                bestScore = score
                bestMoveKey = move.key()
            }
            if (score > alpha) {
                alpha = score
                if (alpha >= beta) {
                    rememberQuietMove(board, move, depth, ply)
                    storeHash(key, depth, score, HashFlag.LOWER, bestMoveKey)
                    return alpha
                }
            }
        }

        val flag = if (bestScore <= alphaOriginal) HashFlag.UPPER else HashFlag.EXACT
        storeHash(key, depth, bestScore, flag, bestMoveKey)
        return bestScore
    }

    private fun searchCut(
        board: Board,
        side: Side,
        depth: Int,
        beta: Int,
        ply: Int,
        positionCounts: MutableMap<String, Int>
    ): Int {
        if (ply >= MAX_PLY) return Evaluator.evaluate(board, side)
        if (board.findKing(side) == null) return -MATE_SCORE + ply
        if (board.findKing(side.opposite()) == null) return MATE_SCORE - ply
        if (depth <= 0) return quiescence(board, side, beta - 1, beta, ply, 0, positionCounts)

        nodesSearched++
        val key = board.positionKey(side)
        val hashMove = transpositionTable[key]?.takeIf { it.depth >= depth }?.let { entry ->
            val hit = entry.valueFor(beta - 1, beta)
            if (hit != null) return hit
            entry.moveKey
        }

        if (depth >= 3 && !board.isInCheck(side) && hasNonKingMaterial(board, side)) {
            val nullScore = -searchCut(board, side.opposite(), depth - 3, 1 - beta, ply + 1, positionCounts)
            if (nullScore >= beta) {
                storeHash(key, depth, nullScore, HashFlag.LOWER, 0)
                return nullScore
            }
        }

        val moves = orderedMoves(board, legalMovesForSearch(board, side, positionCounts), side, hashMove, ply)
        if (moves.isEmpty()) return if (board.isInCheck(side)) -MATE_SCORE + ply else 0

        var bestScore = -MATE_SCORE
        var bestMoveKey = 0
        for (move in moves) {
            val applied = applyMove(board, move, side, positionCounts) ?: continue
            val givesCheck = board.isInCheck(side.opposite())
            val nextDepth = depth - 1 + if (givesCheck && depth < 4) 1 else 0
            val score = -searchCut(board, side.opposite(), nextDepth, 1 - beta, ply + 1, positionCounts)
            undoAppliedMove(board, applied, positionCounts)

            if (score > bestScore) {
                bestScore = score
                bestMoveKey = move.key()
            }
            if (score >= beta) {
                rememberQuietMove(board, move, depth, ply)
                storeHash(key, depth, score, HashFlag.LOWER, bestMoveKey)
                return score
            }
        }

        storeHash(key, depth, bestScore, HashFlag.UPPER, bestMoveKey)
        return bestScore
    }

    private fun quiescence(
        board: Board,
        side: Side,
        alphaStart: Int,
        beta: Int,
        ply: Int,
        qDepth: Int,
        positionCounts: MutableMap<String, Int>
    ): Int {
        nodesSearched++
        val inCheck = board.isInCheck(side)
        var alpha = alphaStart
        if (qDepth >= MAX_QUIESCENCE_DEPTH && inCheck) {
            return Evaluator.evaluate(board, side)
        }

        if (!inCheck) {
            val standPat = Evaluator.evaluate(board, side)
            if (standPat >= beta) return beta
            if (standPat > alpha) alpha = standPat
            if (qDepth >= MAX_QUIESCENCE_DEPTH) return alpha
        }

        val moves = legalMovesForSearch(board, side, positionCounts)
            .filter { inCheck || isTacticalMove(board, it, side) }
        if (moves.isEmpty()) return if (inCheck) -MATE_SCORE + ply else alpha

        for (move in orderedMoves(board, moves, side, null, ply)) {
            val applied = applyMove(board, move, side, positionCounts) ?: continue
            val score = -quiescence(board, side.opposite(), -beta, -alpha, ply + 1, qDepth + 1, positionCounts)
            undoAppliedMove(board, applied, positionCounts)

            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }

        return alpha
    }

    private fun legalMovesForSearch(
        board: Board,
        side: Side,
        positionCounts: Map<String, Int>
    ): List<Move> =
        board.getAllLegalMoves(side).filterNot { move ->
            wouldCauseLongCheck(board, move, side, positionCounts)
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
        val actualMove = move.copy(captured = board.makeMove(move), side = side)
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

    private fun orderedMoves(
        board: Board,
        moves: List<Move>,
        side: Side,
        hashMove: Int?,
        ply: Int
    ): List<Move> =
        moves.sortedByDescending { move ->
            val key = move.key()
            if (hashMove != null && key == hashMove) return@sortedByDescending 1_000_000

            var score = history[key] ?: 0
            if (ply < killers.size) {
                if (killers[ply][0] == key) score += 80_000
                if (killers[ply][1] == key) score += 70_000
            }

            val movingPiece = board.getPiece(move.fromRow, move.fromCol)
            val target = board.getPiece(move.toRow, move.toCol)
            if (target != null) {
                score += 200_000 + pieceValue(target.type) * 24 -
                    pieceValue(movingPiece?.type ?: PieceType.PAWN)
            }

            val actualMove = move.copy(captured = board.makeMove(move))
            if (board.isInCheck(side.opposite())) score += 45_000
            if (movingPiece != null && target == null &&
                board.isSquareAttacked(move.toRow, move.toCol, side.opposite())
            ) {
                score -= pieceValue(movingPiece.type) / 2
            }
            board.undoMove(actualMove)

            score
        }

    private fun isTacticalMove(board: Board, move: Move, side: Side): Boolean {
        if (board.getPiece(move.toRow, move.toCol) != null) return true

        val actualMove = move.copy(captured = board.makeMove(move))
        val givesCheck = board.isInCheck(side.opposite())
        board.undoMove(actualMove)

        return givesCheck
    }

    private fun rememberQuietMove(board: Board, move: Move, depth: Int, ply: Int) {
        if (board.getPiece(move.toRow, move.toCol) != null) return
        val key = move.key()
        history[key] = ((history[key] ?: 0) + depth * depth).coerceAtMost(1_000_000)
        if (ply in killers.indices && killers[ply][0] != key) {
            killers[ply][1] = killers[ply][0]
            killers[ply][0] = key
        }
    }

    private fun storeHash(key: String, depth: Int, value: Int, flag: HashFlag, moveKey: Int) {
        if (transpositionTable.size > HASH_LIMIT) {
            transpositionTable.clear()
        }
        val current = transpositionTable[key]
        if (current == null || depth >= current.depth) {
            transpositionTable[key] = HashEntry(depth, value, flag, moveKey)
        }
    }

    private fun hasNonKingMaterial(board: Board, side: Side): Boolean =
        board.getPiecesBySide(side).any { it.type != PieceType.KING && it.type != PieceType.ADVISOR && it.type != PieceType.ELEPHANT }

    private fun Move.key(): Int =
        (fromRow shl 12) or (fromCol shl 8) or (toRow shl 4) or toCol

    private fun pieceValue(type: PieceType): Int = when (type) {
        PieceType.KING -> 100000
        PieceType.ROOK -> 1000
        PieceType.CANNON -> 450
        PieceType.KNIGHT -> 430
        PieceType.PAWN -> 100
        PieceType.ADVISOR -> 200
        PieceType.ELEPHANT -> 200
    }

    fun getSearchInfo(): String = "ElephantEye-style PVS searched $nodesSearched nodes"

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

    private enum class HashFlag {
        EXACT, LOWER, UPPER
    }

    private data class HashEntry(
        val depth: Int,
        val value: Int,
        val flag: HashFlag,
        val moveKey: Int
    ) {
        fun valueFor(alpha: Int, beta: Int): Int? = when (flag) {
            HashFlag.EXACT -> value
            HashFlag.LOWER -> if (value >= beta) value else null
            HashFlag.UPPER -> if (value <= alpha) value else null
        }
    }

    private companion object {
        const val MATE_SCORE = 1_000_000
        const val MAX_PLY = 64
        const val MAX_QUIESCENCE_DEPTH = 5
        const val HASH_LIMIT = 60_000
        const val ASPIRATION_WINDOW = 80
    }
}
