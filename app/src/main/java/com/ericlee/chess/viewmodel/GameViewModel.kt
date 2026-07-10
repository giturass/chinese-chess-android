package com.ericlee.chess.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ericlee.chess.engine.ChessEngine
import com.ericlee.chess.model.*
import com.ericlee.chess.network.OnlineGameClient
import com.ericlee.chess.network.OnlineMoveDto
import com.ericlee.chess.network.OnlineSessionState
import com.ericlee.chess.network.OnlineSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _selectedPiece = MutableStateFlow<Piece?>(null)
    val selectedPiece: StateFlow<Piece?> = _selectedPiece.asStateFlow()

    private val _legalMoves = MutableStateFlow<List<Move>>(emptyList())
    val legalMoves: StateFlow<List<Move>> = _legalMoves.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _onlineSession = MutableStateFlow(OnlineSessionState())
    val onlineSession: StateFlow<OnlineSessionState> = _onlineSession.asStateFlow()

    private var engine: ChessEngine? = null
    private var gameVersion = 0
    private var onlineClient: OnlineGameClient? = null
    private var onlinePollJob: Job? = null
    private var onlineFlippedOverride: Boolean? = null
    private var onlineMoveInFlight = false
    private var onlineRevision = 0L
    private var lastOnlineServerUrl = ""
    private var lastOnlineRoomId = ""
    private var lastOnlinePlayerId = ""

    fun startGame(
        mode: GameMode,
        difficulty: Int = 3,
        flipped: Boolean = false,
        humanSide: Side = Side.RED
    ) {
        gameVersion++
        _gameState.value = GameState(
            mode = mode,
            aiDifficulty = difficulty,
            isFlipped = flipped,
            humanSide = humanSide
        )
        _selectedPiece.value = null
        _legalMoves.value = emptyList()
        _isAiThinking.value = false
        _statusMessage.value = when (mode) {
            GameMode.AI -> if (humanSide == Side.RED) "红方先手" else "红方先手，AI 先行"
            GameMode.LOCAL -> "红方先手"
            GameMode.ONLINE -> "正在连接房间"
            GameMode.ENDGAME -> "残局挑战"
        }
        if (mode == GameMode.AI) {
            engine = ChessEngine(humanSide.opposite())
            if (humanSide == Side.BLACK) {
                triggerAiMove()
            }
        }
    }

    fun loadEndgame(puzzle: EndgamePuzzle) {
        gameVersion++
        val board = Board(puzzle.pieces.map { it.toPiece() }.toMutableList())
        engine = ChessEngine(Side.BLACK)
        _gameState.value = GameState(
            board = board,
            currentSide = Side.RED,
            mode = GameMode.ENDGAME,
            isFlipped = false
        )
        _selectedPiece.value = null
        _legalMoves.value = emptyList()
        _statusMessage.value = "红方先手，破解 ${puzzle.name}"
    }

    fun onPositionClick(row: Int, col: Int) {
        val state = _gameState.value
        if (state.status != GameStatus.PLAYING) return
        if (_isAiThinking.value) return

        if (state.mode == GameMode.AI && state.currentSide != state.humanSide) return
        if (state.mode == GameMode.ENDGAME && state.currentSide != Side.RED) return
        if (state.mode == GameMode.ONLINE) {
            val session = _onlineSession.value
            if (!session.canMove || state.currentSide != session.side) return
        }

        val clickedPiece = state.board.getPiece(row, col)

        if (_selectedPiece.value != null) {
            val move = _legalMoves.value.find { it.toRow == row && it.toCol == col }
            if (move != null) {
                executeMove(move)
                return
            }
        }

        if (clickedPiece != null && clickedPiece.side == state.currentSide) {
            _selectedPiece.value = clickedPiece
            _legalMoves.value = state.legalMovesForPiece(clickedPiece)
        } else {
            _selectedPiece.value = null
            _legalMoves.value = emptyList()
        }
    }

    private fun executeMove(move: Move) {
        val state = _gameState.value
        if (state.isLongCheckMove(move)) {
            _statusMessage.value = "禁止长将，请改走其他着法"
            _selectedPiece.value = null
            _legalMoves.value = emptyList()
            return
        }

        val newState = state.makeMove(move)
        _gameState.value = newState
        _selectedPiece.value = null
        _legalMoves.value = emptyList()

        updateStatusMessage(newState)

        if (newState.mode == GameMode.ONLINE) {
            sendOnlineMove(move)
            return
        }

        if (newState.status == GameStatus.PLAYING &&
            newState.mode == GameMode.ENDGAME &&
            newState.currentSide == Side.BLACK
        ) {
            triggerEndgameDefense()
            return
        }

        if (newState.status == GameStatus.PLAYING &&
            newState.mode == GameMode.AI &&
            newState.currentSide != newState.humanSide
        ) {
            triggerAiMove()
        }
    }

    private fun triggerAiMove() {
        _isAiThinking.value = true
        val state = _gameState.value
        val boardSnapshot = state.board.copy()
        val depth = state.aiDifficulty
        val version = gameVersion
        val aiSide = state.humanSide.opposite()
        val positionCounts = state.positionOccurrences()

        viewModelScope.launch {
            val move = withContext(Dispatchers.Default) {
                engine?.findBestMove(boardSnapshot, depth, positionCounts)
            }

            if (version != gameVersion) return@launch

            if (move != null && _gameState.value.currentSide == aiSide) {
                val newState = _gameState.value.makeMove(move)
                _gameState.value = newState
                updateStatusMessage(newState)
            }
            _isAiThinking.value = false
        }
    }

    private fun triggerEndgameDefense() {
        _isAiThinking.value = true
        val state = _gameState.value
        val boardSnapshot = state.board.copy()
        val version = gameVersion
        val positionCounts = state.positionOccurrences()

        viewModelScope.launch {
            val move = withContext(Dispatchers.Default) {
                engine?.findBestMove(boardSnapshot, 2, positionCounts)
            }

            if (version != gameVersion) return@launch

            if (move != null && _gameState.value.currentSide == Side.BLACK) {
                val newState = _gameState.value.makeMove(move)
                _gameState.value = newState
                updateStatusMessage(newState)
            }
            _isAiThinking.value = false
        }
    }

    private fun updateStatusMessage(state: GameState) {
        _statusMessage.value = when (state.status) {
            GameStatus.RED_WIN -> "红方获胜！"
            GameStatus.BLACK_WIN -> "黑方获胜！"
            GameStatus.STALEMATE -> "和棋（困毙）"
            GameStatus.DRAW -> "和棋"
            GameStatus.PLAYING -> {
                val sideName = if (state.currentSide == Side.RED) "红方" else "黑方"
                if (state.isInCheck) {
                    "${sideName}被将军，必须应将"
                } else {
                    "轮到${sideName}"
                }
            }
        }
    }

    fun undoMove(side: Side? = null) {
        val state = _gameState.value
        if (state.mode == GameMode.ONLINE) return
        if (state.moveHistory.isEmpty()) return
        if (_isAiThinking.value) return
        if (side != null && state.mode == GameMode.LOCAL && state.lastMoveSide != side) return

        if (state.mode == GameMode.AI || state.mode == GameMode.ENDGAME) {
            // Undo both engine and human move when the engine has answered.
            var s = state.undoLastMove()
            val humanSide = if (state.mode == GameMode.ENDGAME) Side.RED else state.humanSide
            if (s.moveHistory.isNotEmpty() && s.currentSide != humanSide) {
                s = s.undoLastMove()
            }
            _gameState.value = s
        } else {
            _gameState.value = state.undoLastMove()
        }

        _selectedPiece.value = null
        _legalMoves.value = emptyList()
        updateStatusMessage(_gameState.value)
    }

    fun toggleBoardFlipped() {
        val state = _gameState.value
        if (state.mode == GameMode.ONLINE) {
            onlineFlippedOverride = !state.isFlipped
        }
        _gameState.value = state.copy(isFlipped = !state.isFlipped)
        _selectedPiece.value = null
        _legalMoves.value = emptyList()
    }

    fun resign(side: Side? = null) {
        val state = _gameState.value
        if (state.mode == GameMode.ONLINE) {
            sendOnlineAction("resign")
            return
        }

        gameVersion++
        _isAiThinking.value = false
        val resigningSide = side ?: state.currentSide
        val winner = if (resigningSide == Side.RED) GameStatus.BLACK_WIN else GameStatus.RED_WIN
        _gameState.value = state.copy(status = winner)
        updateStatusMessage(_gameState.value)
    }

    fun agreeDraw(requester: Side? = null) {
        val state = _gameState.value
        if (state.mode == GameMode.ONLINE) {
            sendOnlineAction("draw")
            return
        }

        gameVersion++
        _isAiThinking.value = false
        _gameState.value = state.copy(status = GameStatus.DRAW)
        _selectedPiece.value = null
        _legalMoves.value = emptyList()
        _statusMessage.value = if (requester != null) {
            val sideName = if (requester == Side.RED) "红方" else "黑方"
            "${sideName}请求求和，双方同意和棋"
        } else {
            "双方同意和棋"
        }
    }

    fun startOnlineGame(
        roomId: String,
        serverUrl: String,
        preferredSide: Side? = null
    ) {
        val cleanedRoomId = roomId.trim()
        val cleanedServerUrl = serverUrl.trim().trimEnd('/')
        if (cleanedRoomId.isBlank()) {
            _onlineSession.value = OnlineSessionState(message = "请输入房间号")
            return
        }
        if (!cleanedServerUrl.startsWith("http://") && !cleanedServerUrl.startsWith("https://")) {
            _onlineSession.value = OnlineSessionState(
                serverUrl = cleanedServerUrl,
                roomId = cleanedRoomId,
                message = "请输入有效的服务器地址"
            )
            return
        }

        val previousSession = _onlineSession.value
        val previousPlayerId = previousSession.playerId.takeIf {
            previousSession.roomId == cleanedRoomId &&
                previousSession.serverUrl == cleanedServerUrl &&
                it.isNotBlank()
        } ?: lastOnlinePlayerId.takeIf {
            lastOnlineRoomId == cleanedRoomId &&
                lastOnlineServerUrl == cleanedServerUrl &&
                it.isNotBlank()
        }
        onlinePollJob?.cancel()
        gameVersion++
        val version = gameVersion
        onlineFlippedOverride = null
        onlineMoveInFlight = false
        onlineRevision = 0L
        val client = OnlineGameClient(cleanedServerUrl)
        onlineClient = client
        _gameState.value = GameState(mode = GameMode.ONLINE)
        _selectedPiece.value = null
        _legalMoves.value = emptyList()
        _isAiThinking.value = false
        _statusMessage.value = "正在连接房间 $cleanedRoomId"
        _onlineSession.value = OnlineSessionState(
            serverUrl = cleanedServerUrl,
            roomId = cleanedRoomId,
            connecting = true,
            message = "正在连接"
        )

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    client.join(cleanedRoomId, previousPlayerId, preferredSide)
                }
            }.onSuccess { snapshot ->
                if (version != gameVersion) return@onSuccess
                lastOnlineServerUrl = cleanedServerUrl
                lastOnlineRoomId = snapshot.roomId
                lastOnlinePlayerId = snapshot.playerId
                onlineRevision = snapshot.revision
                applyOnlineSnapshot(snapshot)
                _onlineSession.value = OnlineSessionState(
                    serverUrl = cleanedServerUrl,
                    roomId = snapshot.roomId,
                    playerId = snapshot.playerId,
                    side = snapshot.side,
                    connected = true,
                    playerCount = snapshot.playerCount,
                    pendingAction = snapshot.pendingAction,
                    revision = snapshot.revision,
                    message = snapshot.message.ifBlank { "已连接" }
                )
                startOnlinePolling()
            }.onFailure { error ->
                if (version != gameVersion) return@onFailure
                _statusMessage.value = "联机失败"
                _onlineSession.value = OnlineSessionState(
                    serverUrl = cleanedServerUrl,
                    roomId = cleanedRoomId,
                    message = error.message ?: "联机失败"
                )
            }
        }
    }

    fun disconnectOnline() {
        gameVersion++
        val client = onlineClient
        val session = _onlineSession.value
        onlinePollJob?.cancel()
        onlinePollJob = null
        onlineClient = null
        onlineMoveInFlight = false
        onlineRevision = 0L
        _onlineSession.value = OnlineSessionState()
        if (client != null && session.roomId.isNotBlank() && session.playerId.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    client.leave(session.roomId, session.playerId)
                }
            }
        }
    }

    fun requestOnlineUndo() {
        sendOnlineAction("undo")
    }

    fun resetOnlineGame() {
        sendOnlineAction("reset")
    }

    fun respondOnlineRequest(accepted: Boolean) {
        sendOnlineAction(if (accepted) "accept" else "reject")
    }

    private fun sendOnlineMove(move: Move) {
        val client = onlineClient ?: return
        val session = _onlineSession.value
        if (!session.canMove) return

        onlineMoveInFlight = true
        val version = gameVersion
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    client.sendMove(
                        roomId = session.roomId,
                        playerId = session.playerId,
                        move = OnlineMoveDto.fromMove(move)
                    )
                }
            }.onSuccess { snapshot ->
                if (version != gameVersion) return@onSuccess
                onlineMoveInFlight = false
                if (snapshot.revision < onlineRevision) return@onSuccess
                applyOnlineSnapshot(snapshot)
                _onlineSession.value = _onlineSession.value.copy(
                    connected = true,
                    connecting = false,
                    playerCount = snapshot.playerCount,
                    pendingAction = snapshot.pendingAction,
                    revision = snapshot.revision,
                    message = snapshot.message.ifBlank { "已同步" }
                )
            }.onFailure { error ->
                if (version != gameVersion) return@onFailure
                onlineMoveInFlight = false
                _onlineSession.value = _onlineSession.value.copy(
                    connected = false,
                    message = error.message ?: "同步失败"
                )
            }
        }
    }

    private fun sendOnlineAction(action: String) {
        val client = onlineClient ?: return
        val session = _onlineSession.value
        if (!session.canMove) return
        val version = gameVersion

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    client.sendAction(session.roomId, session.playerId, action)
                }
            }.onSuccess { snapshot ->
                if (version != gameVersion) return@onSuccess
                if (snapshot.revision < onlineRevision) return@onSuccess
                applyOnlineSnapshot(snapshot)
                _onlineSession.value = _onlineSession.value.copy(
                    connected = true,
                    playerCount = snapshot.playerCount,
                    pendingAction = snapshot.pendingAction,
                    revision = snapshot.revision,
                    message = snapshot.message.ifBlank { "已同步" }
                )
            }.onFailure { error ->
                if (version != gameVersion) return@onFailure
                _onlineSession.value = _onlineSession.value.copy(
                    connected = false,
                    message = error.message ?: "同步失败"
                )
            }
        }
    }

    private fun startOnlinePolling() {
        onlinePollJob?.cancel()
        val version = gameVersion
        onlinePollJob = viewModelScope.launch {
            while (isActive && version == gameVersion) {
                val startedAt = System.currentTimeMillis()
                pollOnlineSnapshot(waitForChange = true, version = version)
                val elapsed = System.currentTimeMillis() - startedAt
                if (elapsed < MIN_ONLINE_POLL_INTERVAL_MS) {
                    delay(MIN_ONLINE_POLL_INTERVAL_MS - elapsed)
                }
            }
        }
    }

    private suspend fun pollOnlineSnapshot(waitForChange: Boolean = false, version: Int = gameVersion) {
        val client = onlineClient ?: return
        val session = _onlineSession.value
        if (version != gameVersion) return
        if (session.playerId.isBlank() || session.roomId.isBlank()) return
        if (onlineMoveInFlight) return
        val beforeRevision = onlineRevision
        val longPoll = waitForChange && beforeRevision > 0L

        runCatching {
            withContext(Dispatchers.IO) {
                client.snapshot(
                    roomId = session.roomId,
                    playerId = session.playerId,
                    sinceRevision = beforeRevision.takeIf { longPoll },
                    waitMs = if (longPoll) ONLINE_STATE_WAIT_MS else 0
                )
            }
        }.onSuccess { snapshot ->
            if (version != gameVersion) return@onSuccess
            if (snapshot.revision < onlineRevision) return@onSuccess
            if (onlineMoveInFlight && snapshot.revision <= onlineRevision) return@onSuccess
            applyOnlineSnapshot(snapshot)
            _onlineSession.value = _onlineSession.value.copy(
                connected = true,
                connecting = false,
                playerCount = snapshot.playerCount,
                pendingAction = snapshot.pendingAction,
                revision = snapshot.revision,
                message = snapshot.message.ifBlank { "已连接" }
            )
        }.onFailure { error ->
            if (version != gameVersion) return@onFailure
            _onlineSession.value = _onlineSession.value.copy(
                connected = false,
                message = error.message ?: "连接中断"
            )
        }
    }

    private fun applyOnlineSnapshot(snapshot: OnlineSnapshot) {
        onlineRevision = maxOf(onlineRevision, snapshot.revision)
        val playerSide = snapshot.side
        val previousState = _gameState.value
        var state = GameState(
            mode = GameMode.ONLINE,
            isFlipped = onlineFlippedOverride ?: (playerSide == Side.BLACK),
            humanSide = playerSide
        )
        snapshot.moves.forEach { moveDto ->
            if (state.status == GameStatus.PLAYING) {
                state = state.makeMove(moveDto.toMove())
            }
        }
        if (snapshot.status != GameStatus.PLAYING) {
            state = state.copy(status = snapshot.status)
        }

        val boardChanged = previousState.mode != GameMode.ONLINE ||
            previousState.moveHistory.size != state.moveHistory.size ||
            previousState.status != state.status ||
            previousState.currentSide != state.currentSide
        _gameState.value = state
        if (boardChanged) {
            _selectedPiece.value = null
            _legalMoves.value = emptyList()
        }
        updateStatusMessage(state)
        if (state.status == GameStatus.PLAYING && snapshot.playerCount < 2) {
            _statusMessage.value = "等待对手加入"
        } else if (state.status == GameStatus.PLAYING && snapshot.pendingAction != null) {
            _statusMessage.value = snapshot.message
        }
    }

    override fun onCleared() {
        onlinePollJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val ONLINE_STATE_WAIT_MS = 15_000
        const val MIN_ONLINE_POLL_INTERVAL_MS = 250L
    }
}
