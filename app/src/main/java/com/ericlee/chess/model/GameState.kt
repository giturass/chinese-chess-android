package com.ericlee.chess.model

enum class GameMode {
    AI, LOCAL, ENDGAME
}

enum class GameStatus {
    PLAYING, RED_WIN, BLACK_WIN, STALEMATE, DRAW
}

data class GameState(
    val board: Board = Board(),
    val currentSide: Side = Side.RED,
    val mode: GameMode = GameMode.LOCAL,
    val status: GameStatus = GameStatus.PLAYING,
    val moveHistory: MutableList<Move> = mutableListOf(),
    val aiDifficulty: Int = 3,
    val isFlipped: Boolean = false
) {
    fun makeMove(move: Move): GameState {
        val captured = board.makeMove(move)
        val actualMove = move.copy(captured = captured)
        val nextSide = currentSide.opposite()

        val newStatus = when {
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
}
