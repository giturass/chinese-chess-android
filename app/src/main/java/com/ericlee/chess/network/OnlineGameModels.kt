package com.ericlee.chess.network

import com.ericlee.chess.model.GameStatus
import com.ericlee.chess.model.Move
import com.ericlee.chess.model.Side

data class OnlineMoveDto(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int
) {
    fun toMove(): Move = Move(fromRow, fromCol, toRow, toCol)

    companion object {
        fun fromMove(move: Move): OnlineMoveDto = OnlineMoveDto(
            fromRow = move.fromRow,
            fromCol = move.fromCol,
            toRow = move.toRow,
            toCol = move.toCol
        )
    }
}

data class OnlineSnapshot(
    val roomId: String = "",
    val playerId: String = "",
    val side: Side = Side.RED,
    val status: GameStatus = GameStatus.PLAYING,
    val moves: List<OnlineMoveDto> = emptyList(),
    val playerCount: Int = 0,
    val message: String = ""
)

data class OnlineJoinRequest(
    val playerId: String? = null
)

data class OnlineMoveRequest(
    val playerId: String,
    val move: OnlineMoveDto
)

data class OnlineActionRequest(
    val playerId: String,
    val action: String
)

data class OnlineSessionState(
    val roomId: String = "",
    val playerId: String = "",
    val side: Side? = null,
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val playerCount: Int = 0,
    val message: String = ""
) {
    val canMove: Boolean get() = connected && side != null && playerCount >= 2
}
