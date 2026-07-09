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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ericlee.chess.model.GameMode
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
    var pendingAction by remember { mutableStateOf<PendingLocalAction?>(null) }
    var confirmAction by remember { mutableStateOf<BoardConfirmAction?>(null) }

    val state by viewModel.gameState.collectAsState()
    val selectedPiece by viewModel.selectedPiece.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val topSide = if (state.isFlipped) Side.RED else Side.BLACK
    val bottomSide = topSide.opposite()

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
                    Text("同意")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text("拒绝")
                }
            },
            title = { Text(action.title()) },
            text = { Text(action.message()) }
        )
    }

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (action) {
                            BoardConfirmAction.FLIP -> viewModel.toggleBoardFlipped()
                            BoardConfirmAction.RESET -> viewModel.startGame(
                                mode = GameMode.LOCAL,
                                flipped = state.isFlipped
                            )
                        }
                        pendingAction = null
                        confirmAction = null
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text("取消")
                }
            },
            title = { Text(action.title) },
            text = { Text(action.message) }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("双人对战 · 本地") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (gameStarted) {
                        FilledTonalButton(
                            onClick = { confirmAction = BoardConfirmAction.FLIP },
                            modifier = Modifier.padding(end = 6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = null)
                            Text("调转")
                        }
                        Button(
                            onClick = { confirmAction = BoardConfirmAction.RESET },
                            modifier = Modifier.padding(end = 8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("重置")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xAA2D1A0A),
                    titleContentColor = Color(0xFFFFE4A6),
                    navigationIconContentColor = Color(0xFFFFE4A6),
                    actionIconContentColor = Color(0xFFFFE4A6)
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
                    onSelectSide = { side ->
                        viewModel.startGame(
                            mode = GameMode.LOCAL,
                            flipped = side == Side.BLACK
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
                    PlayerControlPanel(
                        side = topSide,
                        state = state,
                        statusMessage = statusMessage,
                        onUndo = { side -> pendingAction = PendingLocalAction(side, LocalActionType.UNDO) },
                        onDraw = { side -> pendingAction = PendingLocalAction(side, LocalActionType.DRAW) },
                        onResign = { side -> pendingAction = PendingLocalAction(side, LocalActionType.RESIGN) },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .graphicsLayer(rotationZ = 180f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ChessBoard(
                        board = state.board,
                        currentSide = state.currentSide,
                        status = state.status,
                        selectedPiece = selectedPiece,
                        legalMoves = legalMoves,
                        lastMove = state.lastMove,
                        isFlipped = state.isFlipped,
                        onPositionClick = { row, col -> viewModel.onPositionClick(row, col) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    PlayerControlPanel(
                        side = bottomSide,
                        state = state,
                        statusMessage = statusMessage,
                        onUndo = { side -> pendingAction = PendingLocalAction(side, LocalActionType.UNDO) },
                        onDraw = { side -> pendingAction = PendingLocalAction(side, LocalActionType.DRAW) },
                        onResign = { side -> pendingAction = PendingLocalAction(side, LocalActionType.RESIGN) },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalSideSelector(
    onSelectSide: (Side) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "本地双人",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFE4A6)
        )
        Text(
            text = "选择下方所执方",
            fontSize = 15.sp,
            color = Color(0xFFFFF0D4).copy(alpha = 0.72f)
        )
        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = { onSelectSide(Side.RED) },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Text("下方执红", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(
            onClick = { onSelectSide(Side.BLACK) },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Text("下方执黑", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private enum class LocalActionType {
    UNDO,
    DRAW,
    RESIGN
}

private enum class BoardConfirmAction(
    val title: String,
    val message: String
) {
    FLIP("确认调转红黑？", "棋盘方向会立即调转，当前棋局不会重置。"),
    RESET("确认重置棋盘？", "当前棋局和历史记录会被清空。")
}

private data class PendingLocalAction(
    val requester: Side,
    val type: LocalActionType
) {
    fun viewerSide(): Side = when (type) {
        LocalActionType.UNDO, LocalActionType.DRAW, LocalActionType.RESIGN -> requester.opposite()
    }

    fun message(): String = when (type) {
        LocalActionType.UNDO -> "${requester.displayName()}请求悔棋，请${viewerSide().displayName()}确认。"
        LocalActionType.DRAW -> "${requester.displayName()}请求求和，请${viewerSide().displayName()}确认。"
        LocalActionType.RESIGN -> "${requester.displayName()}请求认输，请${viewerSide().displayName()}确认。"
    }

    fun title(): String = when (type) {
        LocalActionType.UNDO -> "对方请求悔棋"
        LocalActionType.DRAW -> "对方请求求和"
        LocalActionType.RESIGN -> "对方请求认输"
    }
}
