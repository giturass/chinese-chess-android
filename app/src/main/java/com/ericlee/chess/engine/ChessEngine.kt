package com.ericlee.chess.engine

import com.ericlee.chess.model.Board
import com.ericlee.chess.model.Move
import com.ericlee.chess.model.PieceType
import com.ericlee.chess.model.Side
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

class ChessEngine(private val aiSide: Side = Side.BLACK) {

    private var nodesSearched = 0

    fun findBestMove(
        board: Board,
        depth: Int,
        positionCounts: Map<String, Int> = emptyMap()
    ): Move? {
        nodesSearched = 0

        val rootCounts = positionCounts.toMutableMap()
        if (rootCounts.isEmpty()) {
            rootCounts.increment(board.positionKey(aiSide))
        }

        val root = SearchNode(
            board = board.copy(),
            sideToMove = aiSide,
            parent = null,
            move = null,
            prior = 1.0,
            positionCounts = rootCounts
        )
        root.expand()
        if (root.children.isEmpty()) return null

        repeat(iterationsForDifficulty(depth)) {
            runSimulation(root)
        }

        return root.children.maxWithOrNull(
            compareBy<SearchNode> { it.visits }.thenBy { it.meanValueForParent() }
        )?.move
    }

    private fun runSimulation(root: SearchNode) {
        nodesSearched++
        var node = root

        while (node.children.isNotEmpty()) {
            node = node.selectChild()
        }

        val value = node.terminalValue() ?: run {
            node.expand()
            node.evaluate()
        }

        var backedValue = value
        var cursor: SearchNode? = node
        while (cursor != null) {
            cursor.visits += 1
            cursor.valueSum += backedValue
            backedValue = -backedValue
            cursor = cursor.parent
        }
    }

    private inner class SearchNode(
        val board: Board,
        val sideToMove: Side,
        val parent: SearchNode?,
        val move: Move?,
        val prior: Double,
        val positionCounts: MutableMap<String, Int>
    ) {
        var visits: Int = 0
        var valueSum: Double = 0.0
        val children: MutableList<SearchNode> = mutableListOf()

        fun expand() {
            if (children.isNotEmpty() || terminalValue() != null) return

            val moves = legalMovesForSearch(board, sideToMove, positionCounts)
            if (moves.isEmpty()) return

            val policies = policyScores(moves)
            for ((index, move) in moves.withIndex()) {
                val childBoard = board.copy()
                val actualMove = move.copy(captured = childBoard.makeMove(move), side = sideToMove)
                val nextSide = sideToMove.opposite()
                val childCounts = positionCounts.toMutableMap()
                childCounts.increment(childBoard.positionKey(nextSide))
                children += SearchNode(
                    board = childBoard,
                    sideToMove = nextSide,
                    parent = this,
                    move = actualMove,
                    prior = policies[index],
                    positionCounts = childCounts
                )
            }
        }

        fun selectChild(): SearchNode {
            val parentVisits = visits.coerceAtLeast(1)
            return children.maxByOrNull { child ->
                child.meanValueForParent() +
                    PUCT_EXPLORATION * child.prior * sqrt(parentVisits.toDouble()) / (1 + child.visits)
            } ?: children.first()
        }

        fun meanValue(): Double =
            if (visits == 0) 0.0 else valueSum / visits

        fun meanValueForParent(): Double = -meanValue()

        fun evaluate(): Double {
            val raw = Evaluator.evaluate(board, sideToMove).coerceIn(-VALUE_CLAMP, VALUE_CLAMP)
            return raw / VALUE_CLAMP.toDouble()
        }

        fun terminalValue(): Double? {
            if (board.findKing(sideToMove) == null) return -1.0
            if (board.findKing(sideToMove.opposite()) == null) return 1.0

            val moves = legalMovesForSearch(board, sideToMove, positionCounts)
            if (moves.isNotEmpty()) return null
            return if (board.isInCheck(sideToMove)) -1.0 else 0.0
        }

        private fun policyScores(moves: List<Move>): List<Double> {
            val scores = moves.map { move ->
                val childBoard = board.copy()
                childBoard.makeMove(move)
                Evaluator.evaluate(childBoard, sideToMove) +
                    moveUrgency(board, move, sideToMove)
            }
            val maxScore = scores.maxOrNull() ?: 0
            val weights = scores.map { score ->
                exp(((score - maxScore).coerceAtLeast(-700)) / POLICY_TEMPERATURE)
            }
            val total = weights.sum().takeIf { it > 0.0 } ?: moves.size.toDouble()
            return weights.map { it / total }
        }
    }

    private fun legalMovesForSearch(
        board: Board,
        side: Side,
        positionCounts: Map<String, Int>
    ): List<Move> {
        return board.getAllLegalMoves(side).filterNot { move ->
            wouldCauseLongCheck(board, move, side, positionCounts)
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

    private fun moveUrgency(board: Board, move: Move, side: Side): Int {
        val movingPiece = board.getPiece(move.fromRow, move.fromCol)
        val target = board.getPiece(move.toRow, move.toCol)
        var score = 0

        if (target != null) {
            score += pieceValue(target.type) * 4 - pieceValue(movingPiece?.type ?: PieceType.PAWN)
            if (target.type == PieceType.KING) score += WIN_BONUS
        }

        val actualMove = move.copy(captured = board.makeMove(move))
        if (board.isInCheck(side.opposite())) score += 480
        if (movingPiece != null && board.isSquareAttacked(move.toRow, move.toCol, side.opposite())) {
            score -= pieceValue(movingPiece.type) / 3
        }
        board.undoMove(actualMove)

        return score
    }

    private fun iterationsForDifficulty(depth: Int): Int = when (depth.coerceIn(1, 5)) {
        1 -> 120
        2 -> 240
        3 -> 420
        4 -> 700
        else -> 980
    }

    private fun pieceValue(type: PieceType): Int = when (type) {
        PieceType.KING -> 100000
        PieceType.ROOK -> 1000
        PieceType.CANNON -> 450
        PieceType.KNIGHT -> 430
        PieceType.PAWN -> 100
        PieceType.ADVISOR -> 200
        PieceType.ELEPHANT -> 200
    }

    fun getSearchInfo(): String {
        val strength = ln(nodesSearched.coerceAtLeast(1).toDouble() + 1.0).toInt()
        return "AlphaZero MCTS searched $nodesSearched nodes (strength $strength)"
    }

    private fun MutableMap<String, Int>.increment(key: String) {
        this[key] = (this[key] ?: 0) + 1
    }

    private companion object {
        const val PUCT_EXPLORATION = 1.35
        const val POLICY_TEMPERATURE = 260.0
        const val VALUE_CLAMP = 2400
        const val WIN_BONUS = 100000
    }
}
