package com.ericlee.chess.ui.screen

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
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
import com.ericlee.chess.ui.component.InAppDialog
import com.ericlee.chess.ui.theme.battlefieldTexture
import com.ericlee.chess.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalGameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var gameStarted by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<PendingLocalAction?>(null) }
    var confirmAction by remember { mutableStateOf<PendingBoardConfirm?>(null) }

    val state by viewModel.gameState.collectAsState()
    val selectedPiece by viewModel.selectedPiece.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val activeGameStarted by viewModel.activeGameStarted.collectAsState()

    LaunchedEffect(activeGameStarted, state.mode) {
        if (activeGameStarted && state.mode == GameMode.LOCAL) {
            gameStarted = true
        }
    }

    val topSide = if (state.isFlipped) Side.RED else Side.BLACK
    val bottomSide = topSide.opposite()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("双人•本地") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "返回")
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
                    .battlefieldTexture()
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
                            onHint = {},
                            onNewGame = {
                                confirmAction = PendingBoardConfirm(BoardConfirmAction.NEW_GAME, topSide)
                            },
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
                            onHint = {},
                            onNewGame = {
                                confirmAction = PendingBoardConfirm(BoardConfirmAction.NEW_GAME, bottomSide)
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        pendingAction?.let { action ->
            val viewer = action.viewerSide()
            InAppDialog(
                modifier = Modifier.graphicsLayer(rotationZ = if (viewer == topSide) 180f else 0f),
                onDismissRequest = { pendingAction = null },
                title = { Text(action.title()) },
                content = { Text(action.message()) },
                buttons = {
                    TextButton(onClick = { pendingAction = null }) {
                        Text(action.dismissText())
                    }
                    TextButton(
                        onClick = {
                            when (action.type) {
                                LocalActionType.UNDO -> viewModel.undoMove(action.requester)
                            }
                            pendingAction = null
                        }
                    ) {
                        Text(action.confirmText())
                    }
                }
            )
        }

        confirmAction?.let { pending ->
            val action = pending.action
            InAppDialog(
                modifier = Modifier.graphicsLayer(
                    rotationZ = if (pending.viewer == topSide) 180f else 0f
                ),
                onDismissRequest = { confirmAction = null },
                title = { Text(action.title) },
                content = { Text(action.message) },
                buttons = {
                    TextButton(onClick = { confirmAction = null }) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            when (action) {
                                BoardConfirmAction.NEW_GAME -> viewModel.startGame(
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
                }
            )
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
            text = "选择谁在下方",
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
            Text("执红在下", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(
            onClick = { onSelectSide(Side.BLACK) },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Text("执黑在下", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private enum class LocalActionType {
    UNDO
}

private enum class BoardConfirmAction(
    val title: String,
    val message: String
) {
    NEW_GAME("确认开始新局？", "当前棋局和历史记录会被清空。")
}

private data class PendingBoardConfirm(
    val action: BoardConfirmAction,
    val viewer: Side
)

private data class PendingLocalAction(
    val requester: Side,
    val type: LocalActionType
) {
    fun viewerSide(): Side = when (type) {
        LocalActionType.UNDO -> requester.opposite()
    }

    fun message(): String = when (type) {
        LocalActionType.UNDO -> "${requester.displayName()}请求悔棋，请${viewerSide().displayName()}确认。"
    }

    fun title(): String = when (type) {
        LocalActionType.UNDO -> "对方请求悔棋"
    }

    fun confirmText(): String = "同意"

    fun dismissText(): String = "拒绝"
}
