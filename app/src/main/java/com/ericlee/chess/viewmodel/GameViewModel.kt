package com.ericlee.chess.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ericlee.chess.engine.ChessEngine
import com.ericlee.chess.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private var engine: ChessEngine? = null
    private var gameVersion = 0

    fun startGame(mode: GameMode, difficulty: Int = 3, flipped: Boolean = false) {
        gameVersion++
        _gameState.value = GameState(mode = mode, aiDifficulty = difficulty, isFlipped = flipped)
        _selectedPiece.value = null
        _legalMoves.value = emptyList()
        _isAiThinking.value = false
        _statusMessage.value = when (mode) {
            GameMode.AI -> "红方先手，请出棋"
            GameMode.LOCAL -> "红方先手，请出棋"
            GameMode.ENDGAME -> "残局挑战"
        }
        if (mode == GameMode.AI) {
            engine = ChessEngine(Side.BLACK)
        }
    }

    fun loadEndgame(puzzle: EndgamePuzzle) {
        gameVersion++
        val board = Board(puzzle.pieces.map { it.toPiece() }.toMutableList())
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

        // In AI mode, only allow clicks when it's human's turn (RED)
        if (state.mode == GameMode.AI && state.currentSide != Side.RED) return

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
            _legalMoves.value = state.board.getLegalMovesForPiece(clickedPiece)
        } else {
            _selectedPiece.value = null
            _legalMoves.value = emptyList()
        }
    }

    private fun executeMove(move: Move) {
        val newState = _gameState.value.makeMove(move)
        _gameState.value = newState
        _selectedPiece.value = null
        _legalMoves.value = emptyList()

        updateStatusMessage(newState)

        if (newState.status == GameStatus.PLAYING && newState.mode == GameMode.AI && newState.currentSide == Side.BLACK) {
            triggerAiMove()
        }
    }

    private fun triggerAiMove() {
        _isAiThinking.value = true
        val state = _gameState.value
        val boardSnapshot = state.board.copy()
        val depth = state.aiDifficulty
        val version = gameVersion

        viewModelScope.launch {
            val move = withContext(Dispatchers.Default) {
                engine?.findBestMove(boardSnapshot, depth)
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
                    "请${sideName}出棋"
                }
            }
        }
    }

    fun undoMove(side: Side? = null) {
        val state = _gameState.value
        if (state.moveHistory.isEmpty()) return
        if (_isAiThinking.value) return
        if (side != null && state.mode == GameMode.LOCAL && state.lastMoveSide != side) return

        if (state.mode == GameMode.AI) {
            // Undo both AI and human move
            var s = state.undoLastMove()
            if (s.moveHistory.isNotEmpty() && s.currentSide == Side.BLACK) {
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
        _gameState.value = state.copy(isFlipped = !state.isFlipped)
        _selectedPiece.value = null
        _legalMoves.value = emptyList()
    }

    fun resign(side: Side? = null) {
        gameVersion++
        _isAiThinking.value = false
        val state = _gameState.value
        val resigningSide = side ?: state.currentSide
        val winner = if (resigningSide == Side.RED) GameStatus.BLACK_WIN else GameStatus.RED_WIN
        _gameState.value = state.copy(status = winner)
        updateStatusMessage(_gameState.value)
    }
}
