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
    val moveOffset: Int = 0,
    val totalMoves: Int = 0,
    val pendingAction: OnlinePendingAction? = null,
    val actionReceipt: OnlineActionReceipt? = null,
    val playerCount: Int = 0,
    val revision: Long = 0L,
    val message: String = ""
)

data class OnlineActionReceipt(
    val id: String = "",
    val type: String = "",
    val requester: Side = Side.RED,
    val responder: Side = Side.BLACK,
    val accepted: Boolean = false
) {
    val message: String
        get() = when (type) {
            "undo" -> "对方已同意悔棋"
            "draw" -> "对方已同意和棋"
            else -> "对方已同意请求"
        }
}

data class OnlinePendingAction(
    val type: String = "",
    val requester: Side = Side.RED,
    val target: Side = Side.BLACK
) {
    val title: String
        get() = when (type) {
            "undo" -> "对方请求悔棋"
            "draw" -> "对方请求求和"
            else -> "对方发来请求"
        }

    val message: String
        get() = when (type) {
            "undo" -> "${requester.displayName()}请求撤回上一步，是否同意？"
            "draw" -> "${requester.displayName()}请求和棋，是否同意？"
            else -> "${requester.displayName()}发来请求，是否同意？"
        }
}

data class OnlineJoinRequest(
    val playerId: String? = null,
    val preferredSide: Side? = null
)

data class OnlineMoveRequest(
    val playerId: String,
    val move: OnlineMoveDto,
    val requestId: String,
    val expectedRevision: Long,
    val baseMoveCount: Int,
    val knownMoveCount: Int
)

data class OnlineActionRequest(
    val playerId: String,
    val action: String,
    val requestId: String,
    val expectedRevision: Long,
    val knownMoveCount: Int
)

data class OnlineLeaveRequest(
    val playerId: String
)

data class OnlineSessionState(
    val serverUrl: String = "",
    val roomId: String = "",
    val playerId: String = "",
    val side: Side? = null,
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val reconnecting: Boolean = false,
    val movePending: Boolean = false,
    val playerCount: Int = 0,
    val pendingAction: OnlinePendingAction? = null,
    val actionReceipt: OnlineActionReceipt? = null,
    val revision: Long = 0L,
    val message: String = ""
) {
    val hasJoinedRoom: Boolean get() = roomId.isNotBlank() && playerId.isNotBlank() && side != null
    val canAct: Boolean get() = connected && !movePending && side != null && playerCount >= 2
    val canMove: Boolean get() = canAct
}

private fun Side.displayName(): String = if (this == Side.RED) "红方" else "黑方"
