package com.ericlee.chess.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ericlee.chess.model.GameMode
import com.ericlee.chess.model.GameStatus
import com.ericlee.chess.model.Side
import com.ericlee.chess.ui.board.ChessBoard
import com.ericlee.chess.ui.theme.woodTexture
import com.ericlee.chess.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalGameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    var gameStarted by remember { mutableStateOf(false) }
    var bottomSide by remember { mutableStateOf(Side.RED) }
    var pendingAction by remember { mutableStateOf<PendingLocalAction?>(null) }

    val state by viewModel.gameState.collectAsState()
    val selectedPiece by viewModel.selectedPiece.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val topSide = bottomSide.opposite()

    pendingAction?.let { action ->
        val viewer = action.viewerSide()
        AlertDialog(
            modifier = Modifier.graphicsLayer(rotationZ = if (viewer == topSide) 180f else 0f),
            onDismissRequest = { pendingAction = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (action.type) {
                            LocalActionType.UNDO -> viewModel.undoMove(action.requester)
                            LocalActionType.DRAW -> viewModel.agreeDraw(action.requester)
                            LocalActionType.RESIGN -> viewModel.resign(action.requester)
                        }
                        pendingAction = null
                    }
                ) {
                    Text(if (action.type == LocalActionType.RESIGN) "确认" else "同意")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(if (action.type == LocalActionType.RESIGN) "取消" else "拒绝")
                }
            },
            title = { Text("请${viewer.displayName()}确认") },
            text = { Text(action.message()) }
        )
    }

    if (gameStarted && state.status != GameStatus.PLAYING) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingAction = null
                        viewModel.startGame(
                            mode = GameMode.LOCAL,
                            flipped = bottomSide == Side.BLACK
                        )
                    }
                ) {
                    Text("进入新局")
                }
            },
            dismissButton = {
                TextButton(onClick = onBack) {
                    Text("返回首页")
                }
            },
            title = { Text("棋局结束") },
            text = { Text("$statusMessage\n是否进入新局？") }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("双人对战") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xAA2D1A0A),
                    titleContentColor = Color(0xFFFFE4A6),
                    navigationIconContentColor = Color(0xFFFFE4A6)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .woodTexture()
                .padding(padding)
        ) {
            if (!gameStarted) {
                LocalSideSelector(
                    onSelect = { selectedSide ->
                        bottomSide = selectedSide
                        viewModel.startGame(
                            mode = GameMode.LOCAL,
                            flipped = selectedSide == Side.BLACK
                        )
                        gameStarted = true
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LocalControlPanel(
                        state = state,
                        statusMessage = statusMessage,
                        onUndo = { side -> pendingAction = PendingLocalAction(side, LocalActionType.UNDO) },
                        onDraw = { side -> pendingAction = PendingLocalAction(side, LocalActionType.DRAW) },
                        onResign = { side -> pendingAction = PendingLocalAction(side, LocalActionType.RESIGN) },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ChessBoard(
                        board = state.board,
                        currentSide = state.currentSide,
                        selectedPiece = selectedPiece,
                        legalMoves = legalMoves,
                        lastMove = state.lastMove,
                        isFlipped = state.isFlipped,
                        onPositionClick = { row, col -> viewModel.onPositionClick(row, col) }
                    )
                }
            }
        }
    }
}

private enum class LocalActionType {
    UNDO,
    DRAW,
    RESIGN
}

private data class PendingLocalAction(
    val requester: Side,
    val type: LocalActionType
) {
    fun viewerSide(): Side = when (type) {
        LocalActionType.UNDO, LocalActionType.DRAW -> requester.opposite()
        LocalActionType.RESIGN -> requester
    }

    fun message(): String = when (type) {
        LocalActionType.UNDO -> "${requester.displayName()}请求悔棋，请${viewerSide().displayName()}确认。"
        LocalActionType.DRAW -> "${requester.displayName()}提出求和，请${viewerSide().displayName()}确认。"
        LocalActionType.RESIGN -> "${requester.displayName()}确认认输后，${requester.opposite().displayName()}获胜。"
    }
}

@Composable
private fun LocalSideSelector(
    onSelect: (Side) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEAF7E5C7)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "选择下方执棋",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A2A13)
            )
            Text(
                text = "红方先手，棋盘按所选一方朝下摆放。",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
            OutlinedButton(
                onClick = { onSelect(Side.RED) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("执红在下", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onSelect(Side.BLACK) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("执黑在下", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
