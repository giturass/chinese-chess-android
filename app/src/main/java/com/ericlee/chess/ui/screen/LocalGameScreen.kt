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
    var pendingAction by remember { mutableStateOf<PendingLocalAction?>(null) }
    val topSide = if (state.isFlipped) Side.RED else Side.BLACK
    val bottomSide = topSide.opposite()

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (action.type) {
                            LocalActionType.UNDO -> viewModel.undoMove(action.requester)
                            LocalActionType.RESIGN -> viewModel.resign(action.requester)
                        }
                        pendingAction = null
                    }
                ) {
                    Text("同意")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text("不同意")
                }
            },
            title = { Text("${action.type.label}请求") },
            text = {
                Text(
                    "${sideLabel(action.requester)}请求${action.type.label}，需要${sideLabel(action.requester.opposite())}同意。"
                )
            }
        )
    }

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
                    FilledTonalButton(
                        onClick = {
                            pendingAction = null
                            viewModel.toggleBoardFlipped()
                        },
                        modifier = Modifier.padding(end = 6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("调转")
                    }
                    Button(
                        onClick = {
                            pendingAction = null
                            viewModel.startGame(GameMode.LOCAL, flipped = state.isFlipped)
                        },
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
                side = topSide,
                canUndo = state.lastMoveSide == topSide,
                canResign = state.status == GameStatus.PLAYING,
                onUndo = { pendingAction = PendingLocalAction(topSide, LocalActionType.UNDO) },
                onResign = { pendingAction = PendingLocalAction(topSide, LocalActionType.RESIGN) },
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
                isFlipped = state.isFlipped,
                onPositionClick = { row, col -> viewModel.onPositionClick(row, col) }
            )

            PlayerActionPanel(
                side = bottomSide,
                canUndo = state.lastMoveSide == bottomSide,
                canResign = state.status == GameStatus.PLAYING,
                onUndo = { pendingAction = PendingLocalAction(bottomSide, LocalActionType.UNDO) },
                onResign = { pendingAction = PendingLocalAction(bottomSide, LocalActionType.RESIGN) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

private enum class LocalActionType(val label: String) {
    UNDO("悔棋"),
    RESIGN("认输")
}

private data class PendingLocalAction(
    val requester: Side,
    val type: LocalActionType
)

private fun sideLabel(side: Side): String = if (side == Side.RED) "红方" else "黑方"

@Composable
private fun PlayerActionPanel(
    side: Side,
    canUndo: Boolean,
    canResign: Boolean,
    onUndo: () -> Unit,
    onResign: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sideName = sideLabel(side)
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
