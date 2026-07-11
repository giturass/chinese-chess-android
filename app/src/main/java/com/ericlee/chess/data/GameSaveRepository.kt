package com.ericlee.chess.data

import android.content.Context
import com.ericlee.chess.model.Board
import com.ericlee.chess.model.GameMode
import com.ericlee.chess.model.GameState
import com.ericlee.chess.model.GameStatus
import com.ericlee.chess.model.Move
import com.ericlee.chess.model.Piece
import com.ericlee.chess.model.PieceType
import com.ericlee.chess.model.Side
import com.google.gson.Gson

class GameSaveRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(state: GameState, endgamePuzzleId: Int?) {
        val data = SavedGameData.from(state, endgamePuzzleId)
        prefs.edit()
            .putString(KEY_SAVED_GAME, gson.toJson(data))
            .apply()
    }

    fun load(): SavedGameData? =
        prefs.getString(KEY_SAVED_GAME, null)
            ?.let { json -> runCatching { gson.fromJson(json, SavedGameData::class.java) }.getOrNull() }

    fun loadSummary(): SavedGameSummary? =
        load()?.takeIf { it.status == GameStatus.PLAYING }?.let { data ->
            SavedGameSummary(
                mode = data.mode,
                moveCount = data.moveHistory.size,
                savedAt = data.savedAt,
                endgamePuzzleId = data.endgamePuzzleId
            )
        }

    fun clear() {
        prefs.edit().remove(KEY_SAVED_GAME).apply()
    }

    private companion object {
        const val PREFS_NAME = "game_save"
        const val KEY_SAVED_GAME = "saved_game"
    }
}

data class SavedGameSummary(
    val mode: GameMode,
    val moveCount: Int,
    val savedAt: Long,
    val endgamePuzzleId: Int?
)

data class SavedGameData(
    val board: List<SavedPiece>,
    val currentSide: Side,
    val initialBoard: List<SavedPiece>,
    val initialSide: Side,
    val mode: GameMode,
    val status: GameStatus,
    val moveHistory: List<SavedMove>,
    val aiDifficulty: Int,
    val isFlipped: Boolean,
    val humanSide: Side,
    val endgamePuzzleId: Int?,
    val savedAt: Long
) {
    fun toGameState(): GameState = GameState(
        board = Board(board.map { it.toPiece() }.toMutableList()),
        currentSide = currentSide,
        initialBoard = Board(initialBoard.map { it.toPiece() }.toMutableList()),
        initialSide = initialSide,
        mode = mode,
        status = status,
        moveHistory = moveHistory.map { it.toMove() }.toMutableList(),
        aiDifficulty = aiDifficulty,
        isFlipped = isFlipped,
        humanSide = humanSide
    )

    companion object {
        fun from(state: GameState, endgamePuzzleId: Int?): SavedGameData = SavedGameData(
            board = state.board.pieces.map { SavedPiece.from(it) },
            currentSide = state.currentSide,
            initialBoard = state.initialBoard.pieces.map { SavedPiece.from(it) },
            initialSide = state.initialSide,
            mode = state.mode,
            status = state.status,
            moveHistory = state.moveHistory.map { SavedMove.from(it) },
            aiDifficulty = state.aiDifficulty,
            isFlipped = state.isFlipped,
            humanSide = state.humanSide,
            endgamePuzzleId = endgamePuzzleId,
            savedAt = System.currentTimeMillis()
        )
    }
}

data class SavedMove(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val captured: SavedPiece?,
    val side: Side?,
    val pieceType: PieceType?
) {
    fun toMove(): Move = Move(
        fromRow = fromRow,
        fromCol = fromCol,
        toRow = toRow,
        toCol = toCol,
        captured = captured?.toPiece(),
        side = side,
        pieceType = pieceType
    )

    companion object {
        fun from(move: Move): SavedMove = SavedMove(
            fromRow = move.fromRow,
            fromCol = move.fromCol,
            toRow = move.toRow,
            toCol = move.toCol,
            captured = move.captured?.let { SavedPiece.from(it) },
            side = move.side,
            pieceType = move.pieceType
        )
    }
}

data class SavedPiece(
    val side: Side,
    val type: PieceType,
    val row: Int,
    val col: Int
) {
    fun toPiece(): Piece = Piece(side, type, row, col)

    companion object {
        fun from(piece: Piece): SavedPiece = SavedPiece(
            side = piece.side,
            type = piece.type,
            row = piece.row,
            col = piece.col
        )
    }
}
