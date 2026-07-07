package com.ericlee.chess.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import com.ericlee.chess.model.Side
import com.ericlee.chess.ui.board.ChessBoard
import com.ericlee.chess.viewmodel.GameViewModel

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
                    Button(
                        onClick = { viewModel.startGame(GameMode.LOCAL) },
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重置棋盘")
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
            GameStatusBanner(
                state = state,
                statusMessage = statusMessage,
                metaText = "第 ${state.moveHistory.size / 2 + 1} 回合",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            PlayerActionPanel(
                side = Side.BLACK,
                canUndo = state.lastMoveSide == Side.BLACK,
                canResign = state.status == GameStatus.PLAYING,
                onUndo = { viewModel.undoMove(Side.BLACK) },
                onResign = { viewModel.resign(Side.BLACK) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            ChessBoard(
                board = state.board,
                currentSide = state.currentSide,
                selectedPiece = selectedPiece,
                legalMoves = legalMoves,
                lastMove = state.lastMove,
                isFlipped = false,
                onPositionClick = { row, col -> viewModel.onPositionClick(row, col) }
            )

            PlayerActionPanel(
                side = Side.RED,
                canUndo = state.lastMoveSide == Side.RED,
                canResign = state.status == GameStatus.PLAYING,
                onUndo = { viewModel.undoMove(Side.RED) },
                onResign = { viewModel.resign(Side.RED) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PlayerActionPanel(
    side: Side,
    canUndo: Boolean,
    canResign: Boolean,
    onUndo: () -> Unit,
    onResign: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sideName = if (side == Side.RED) "红方" else "黑方"
    val accent = if (side == Side.RED) Color(0xFFB3261E) else Color(0xFF2B2118)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2D7)),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sideName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                modifier = Modifier.width(42.dp)
            )
            OutlinedButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("悔棋", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onResign,
                enabled = canResign,
                modifier = Modifier.weight(1f)
            ) {
                Text("认输", fontSize = 13.sp)
            }
        }
    }
}
