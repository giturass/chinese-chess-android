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
import com.xiaomi.chess.model.GameStatus
import com.xiaomi.chess.ui.board.ChessBoard
import com.xiaomi.chess.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalGameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.startGame(GameMode.LOCAL)
    }

    val state by viewModel.gameState.collectAsState()
    val selectedPiece by viewModel.selectedPiece.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("双人对战") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.startGame(GameMode.LOCAL)
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
                    Text(
                        text = "第 ${state.moveHistory.size / 2 + 1} 回合",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Chess board
            ChessBoard(
                board = state.board,
                currentSide = state.currentSide,
                selectedPiece = selectedPiece,
                legalMoves = legalMoves,
                lastMove = state.lastMove,
                isFlipped = false,
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
                    enabled = state.moveHistory.isNotEmpty()
                ) {
                    Icon(Icons.Default.Undo, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("悔棋")
                }

                OutlinedButton(
                    onClick = { viewModel.resign() },
                    enabled = state.status == GameStatus.PLAYING
                ) {
                    Text("认输")
                }
            }
        }
    }
}
