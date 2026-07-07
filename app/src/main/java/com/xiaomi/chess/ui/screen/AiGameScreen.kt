package com.xiaomi.chess.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaomi.chess.model.GameMode
import com.xiaomi.chess.ui.board.ChessBoard
import com.xiaomi.chess.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    var difficulty by remember { mutableIntStateOf(3) }
    var gameStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startGame(GameMode.AI, difficulty)
        gameStarted = true
    }

    val state by viewModel.gameState.collectAsState()
    val selectedPiece by viewModel.selectedPiece.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("人机对战") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.startGame(GameMode.AI, difficulty)
                    }) {
                        Icon(Icons.Default.Refresh, "重新开始")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!gameStarted) {
                DifficultySelector(difficulty = difficulty) {
                    difficulty = it
                    viewModel.startGame(GameMode.AI, difficulty)
                    gameStarted = true
                }
            } else {
                // Status bar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusMessage,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isAiThinking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                // Chess board
                ChessBoard(
                    board = state.board,
                    currentSide = state.currentSide,
                    selectedPiece = selectedPiece,
                    legalMoves = legalMoves,
                    lastMove = state.lastMove,
                    isFlipped = state.isFlipped,
                    onPositionClick = { row, col -> viewModel.onPositionClick(row, col) }
                )

                // Control buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { viewModel.undoMove() },
                        enabled = state.moveHistory.isNotEmpty() && !isAiThinking
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("悔棋")
                    }

                    OutlinedButton(
                        onClick = { viewModel.resign() },
                        enabled = state.status == com.xiaomi.chess.model.GameStatus.PLAYING
                    ) {
                        Text("认输")
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultySelector(
    difficulty: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "选择难度",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        val levels = listOf(
            1 to "简单",
            2 to "中等",
            3 to "困难",
            4 to "大师"
        )

        for ((level, name) in levels) {
            Button(
                onClick = { onSelect(level) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp)
            ) {
                Text(name, fontSize = 18.sp)
            }
        }
    }
}
