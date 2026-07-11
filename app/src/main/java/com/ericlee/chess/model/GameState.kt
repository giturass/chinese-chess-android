package com.ericlee.chess.model

enum class GameMode {
    AI, LOCAL, ONLINE, ENDGAME
}

enum class GameStatus {
    PLAYING, RED_WIN, BLACK_WIN, STALEMATE, DRAW
}

data class GameState(
    val board: Board = Board(),
    val currentSide: Side = Side.RED,
    val initialBoard: Board = board.copy(),
    val initialSide: Side = Side.RED,
    val mode: GameMode = GameMode.LOCAL,
    val status: GameStatus = GameStatus.PLAYING,
    val moveHistory: MutableList<Move> = mutableListOf(),
    val aiDifficulty: Int = 3,
    val isFlipped: Boolean = false,
    val humanSide: Side = Side.RED
) {
    fun makeMove(move: Move): GameState {
        val movingPiece = board.getPiece(move.fromRow, move.fromCol) ?: return this
        if (movingPiece.side != currentSide) return this
        val targetPiece = board.getPiece(move.toRow, move.toCol)
        if (targetPiece?.side == currentSide) return this

        val captured = board.makeMove(move)
        val actualMove = move.copy(
            captured = captured,
            side = currentSide,
            pieceType = movingPiece?.type ?: move.pieceType
        )
        val nextSide = currentSide.opposite()
        val nextPositionKey = board.positionKey(nextSide)
        val nextPositionCount = (positionOccurrences()[nextPositionKey] ?: 0) + 1
        val repeatedPosition = nextPositionCount >= 3
        val nextSideInCheck = board.isInCheck(nextSide)

        val newStatus = when {
            repeatedPosition && nextSideInCheck -> {
                if (currentSide == Side.RED) GameStatus.BLACK_WIN else GameStatus.RED_WIN
            }
            repeatedPosition -> GameStatus.DRAW
            board.findKing(nextSide) == null -> {
                if (nextSide == Side.RED) GameStatus.BLACK_WIN else GameStatus.RED_WIN
            }
            board.isCheckmate(nextSide) -> {
                if (nextSide == Side.RED) GameStatus.BLACK_WIN else GameStatus.RED_WIN
            }
            board.isStalemate(nextSide) -> {
                if (nextSide == Side.RED) GameStatus.BLACK_WIN else GameStatus.RED_WIN
            }
            else -> GameStatus.PLAYING
        }

        return copy(
            currentSide = nextSide,
            status = newStatus,
            moveHistory = (moveHistory + actualMove).toMutableList()
        )
    }

    fun undoLastMove(): GameState {
        if (moveHistory.isEmpty()) return this
        val lastMove = moveHistory.last()
        board.undoMove(lastMove)
        return copy(
            currentSide = currentSide.opposite(),
            status = GameStatus.PLAYING,
            moveHistory = moveHistory.dropLast(1).toMutableList()
        )
    }

    val isInCheck: Boolean get() = board.isInCheck(currentSide)

    val lastMove: Move? get() = moveHistory.lastOrNull()

    val lastMoveSide: Side? get() = lastMove?.side ?: if (moveHistory.isNotEmpty()) currentSide.opposite() else null

    fun legalMovesForPiece(piece: Piece): List<Move> {
        return board.getLegalMovesForPiece(piece)
    }

    fun allLegalMoves(side: Side): List<Move> {
        return board.getAllLegalMoves(side)
    }

    fun isLongCheckMove(move: Move): Boolean =
        wouldCauseLongCheck(move, currentSide, positionOccurrences())

    fun positionOccurrences(): Map<String, Int> {
        val replay = initialBoard.copy()
        val counts = mutableMapOf<String, Int>()
        var sideToMove = initialSide

        counts.increment(replay.positionKey(sideToMove))
        for (historyMove in moveHistory) {
            replay.makeMove(historyMove)
            sideToMove = sideToMove.opposite()
            counts.increment(replay.positionKey(sideToMove))
        }

        return counts
    }

    private fun wouldCauseLongCheck(
        move: Move,
        movingSide: Side,
        positionCounts: Map<String, Int>
    ): Boolean {
        val movingPiece = board.getPiece(move.fromRow, move.fromCol) ?: return false
        if (movingPiece.side != movingSide) return false

        val captured = board.makeMove(move)
        val actualMove = move.copy(captured = captured)
        val targetSide = movingSide.opposite()
        val givesCheck = board.isInCheck(targetSide)
        val repeatsPosition = (positionCounts[board.positionKey(targetSide)] ?: 0) >= 2
        board.undoMove(actualMove)

        return givesCheck && repeatsPosition
    }

    private fun MutableMap<String, Int>.increment(key: String) {
        this[key] = (this[key] ?: 0) + 1
    }
}
