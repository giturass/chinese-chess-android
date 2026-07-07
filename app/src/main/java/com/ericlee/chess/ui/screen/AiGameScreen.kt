package com.ericlee.chess.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ericlee.chess.model.GameMode
import com.ericlee.chess.model.GameStatus
import com.ericlee.chess.ui.board.ChessBoard
import com.ericlee.chess.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    var difficulty by remember { mutableIntStateOf(3) }
    var gameStarted by remember { mutableStateOf(false) }

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
                    if (gameStarted) {
                        FilledTonalButton(
                            onClick = { viewModel.toggleBoardFlipped() },
                            enabled = !isAiThinking,
                            modifier = Modifier.padding(end = 6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("调转")
                        }
                        Button(
                            onClick = { viewModel.startGame(GameMode.AI, difficulty, flipped = state.isFlipped) },
                            enabled = !isAiThinking,
                            modifier = Modifier.padding(end = 8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重置棋盘")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFFF7E8))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!gameStarted) {
                DifficultySelector {
                    difficulty = it
                    viewModel.startGame(GameMode.AI, it)
                    gameStarted = true
                }
            } else {
                GameStatusBanner(
                    state = state,
                    statusMessage = statusMessage,
                    isAiThinking = isAiThinking,
                    metaText = "难度 ${difficulty}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )

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
                        enabled = state.status == GameStatus.PLAYING && !isAiThinking
                    ) {
                        Text("认输")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultySelector(
    onSelect: (Int) -> Unit
) {
    val levels = listOf(
        DifficultyLevel(1, "入门", "快速应手，适合熟悉规则", "★"),
        DifficultyLevel(2, "进阶", "会主动兑子与防守", "★★"),
        DifficultyLevel(3, "劲敌", "搜索更深，攻守更稳", "★★★"),
        DifficultyLevel(4, "宗师", "更谨慎地计算杀招", "★★★★")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "人机对弈",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "择一位棋友开局",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        for (level in levels) {
            ElevatedCard(
                onClick = { onSelect(level.value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFFFFF7E8)
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = level.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6F3914)
                        )
                        Text(
                            text = level.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                    }
                    Text(
                        text = level.stars,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB3261E)
                    )
                }
            }
        }
    }
}

private data class DifficultyLevel(
    val value: Int,
    val name: String,
    val description: String,
    val stars: String
)
