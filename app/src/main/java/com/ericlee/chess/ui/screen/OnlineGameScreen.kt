package com.ericlee.chess.ui.screen

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ericlee.chess.model.GameStatus
import com.ericlee.chess.model.Side
import com.ericlee.chess.network.OnlineServerConfig
import com.ericlee.chess.ui.board.ChessBoard
import com.ericlee.chess.ui.theme.battlefieldTexture
import com.ericlee.chess.ui.theme.stoneChamberTexture
import com.ericlee.chess.viewmodel.GameViewModel
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineGameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.gameState.collectAsState()
    val selectedPiece by viewModel.selectedPiece.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val session by viewModel.onlineSession.collectAsState()

    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(OnlineServerConfig.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var roomId by rememberSaveable {
        mutableStateOf(prefs.getString(OnlineServerConfig.ROOM_ID_KEY, "").orEmpty())
    }
    val serverUrl = remember {
        prefs.getString(
            OnlineServerConfig.SERVER_URL_KEY,
            OnlineServerConfig.DEFAULT_SERVER_URL
        ).orEmpty().ifBlank { OnlineServerConfig.DEFAULT_SERVER_URL }
    }
    var confirmExit by remember { mutableStateOf(false) }
    val pendingAction = session.pendingAction?.takeIf { it.target == session.side }

    DisposableEffect(Unit) {
        onDispose { viewModel.disconnectOnline() }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmExit = false
                        viewModel.disconnectOnline()
                        onBack()
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) {
                    Text("取消")
                }
            },
            title = { Text("退出联机对战？") },
            text = { Text("退出后当前设备会离开房间。") }
        )
    }

    if (pendingAction != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(
                    onClick = { viewModel.respondOnlineRequest(accepted = true) }
                ) {
                    Text("同意")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.respondOnlineRequest(accepted = false) }
                ) {
                    Text("拒绝")
                }
            },
            title = { Text(pendingAction.title) },
            text = { Text(pendingAction.message) }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("双人对战 · 联机") },
                navigationIcon = {
                    IconButton(onClick = { if (session.connected) confirmExit = true else onBack() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (session.connected) {
                        FilledTonalButton(
                            onClick = { viewModel.toggleBoardFlipped() },
                            modifier = Modifier.padding(end = 6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = null)
                            Text("调转")
                        }
                        Button(
                            onClick = { viewModel.resetOnlineGame() },
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
        if (!session.connected) {
            OnlineJoinPanel(
                roomId = roomId,
                connecting = session.connecting,
                message = session.message,
                onRoomIdChange = {
                    roomId = it
                    prefs.edit().putString(OnlineServerConfig.ROOM_ID_KEY, it).apply()
                },
                onGenerateRoomId = {
                    roomId = generateRoomId()
                    prefs.edit().putString(OnlineServerConfig.ROOM_ID_KEY, roomId).apply()
                },
                onJoin = {
                    prefs.edit()
                        .putString(OnlineServerConfig.ROOM_ID_KEY, roomId)
                        .apply()
                    viewModel.startOnlineGame(roomId, serverUrl)
                },
                modifier = Modifier.padding(padding)
            )
        } else {
            val playerSide = session.side ?: Side.RED
            val connectionState = if (session.playerCount < 2) "等待对手" else session.message.ifBlank { "已连接" }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .stoneChamberTexture()
                    .padding(padding)
            ) {
                OnlineGameContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    statusBar = {
                    OnlineRoomStatusBar(
                        roomId = session.roomId,
                        connectionState = connectionState,
                        modifier = Modifier
                            .layoutId("status")
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    },
                    board = {
                    ChessBoard(
                        board = state.board,
                        currentSide = state.currentSide,
                        status = state.status,
                        selectedPiece = selectedPiece,
                        legalMoves = legalMoves,
                        lastMove = state.lastMove,
                        isFlipped = state.isFlipped,
                        onPositionClick = { row, col -> viewModel.onPositionClick(row, col) },
                        modifier = Modifier.layoutId("board")
                    )
                    },
                    controlPanel = {
                    OnlineControlPanel(
                        state = state,
                        statusMessage = statusMessage,
                        side = playerSide,
                        showActions = true,
                        canUndo = state.status == GameStatus.PLAYING && state.lastMoveSide == playerSide,
                        onUndo = { viewModel.requestOnlineUndo() },
                        onDraw = { viewModel.agreeDraw(playerSide) },
                        onResign = { viewModel.resign(playerSide) },
                        modifier = Modifier
                            .layoutId("panel")
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    }
                )
            }
        }
    }
}

@Composable
private fun OnlineGameContent(
    statusBar: @Composable () -> Unit,
    board: @Composable () -> Unit,
    controlPanel: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Layout(
        modifier = modifier,
        content = {
            statusBar()
            board()
            controlPanel()
        }
    ) { measurables, constraints ->
        val gap = 4.dp.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val statusPlaceable = measurables.first { it.layoutId == "status" }.measure(loose)
        val panelPlaceable = measurables.first { it.layoutId == "panel" }.measure(loose)
        val boardMaxHeight = (constraints.maxHeight - statusPlaceable.height - panelPlaceable.height - gap * 2)
            .coerceAtLeast(0)
        val boardPlaceable = measurables.first { it.layoutId == "board" }
            .measure(loose.copy(maxHeight = boardMaxHeight))

        val panelY = constraints.maxHeight - panelPlaceable.height
        val boardAreaTop = statusPlaceable.height + gap
        val boardAreaBottom = panelY - gap
        val boardY = (boardAreaTop + (boardAreaBottom - boardAreaTop - boardPlaceable.height) / 2)
            .coerceAtLeast(boardAreaTop)

        layout(constraints.maxWidth, constraints.maxHeight) {
            statusPlaceable.place((constraints.maxWidth - statusPlaceable.width) / 2, 0)
            boardPlaceable.place((constraints.maxWidth - boardPlaceable.width) / 2, boardY)
            panelPlaceable.place((constraints.maxWidth - panelPlaceable.width) / 2, panelY)
        }
    }
}

@Composable
private fun OnlineJoinPanel(
    roomId: String,
    connecting: Boolean,
    message: String,
    onRoomIdChange: (String) -> Unit,
    onGenerateRoomId: () -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .battlefieldTexture()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "联机房间",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFE4A6)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = roomId,
                    onValueChange = { value ->
                        onRoomIdChange(value.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(24))
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("房间号") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFFFF7D6),
                        unfocusedTextColor = Color(0xFFFFF7D6),
                        cursorColor = Color(0xFFFFD36A),
                        focusedLabelColor = Color(0xFFFFD36A),
                        unfocusedLabelColor = Color(0xFFFFE4A6),
                        focusedBorderColor = Color(0xFFFFD36A),
                        unfocusedBorderColor = Color(0xFFFFE4A6),
                        focusedContainerColor = Color(0xCC171411),
                        unfocusedContainerColor = Color(0xCC171411)
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onGenerateRoomId,
                    enabled = !connecting,
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("生成")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onJoin,
                enabled = !connecting && roomId.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(if (connecting) "连接中" else "进入房间")
            }
            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = Color(0xFFFFF0D4).copy(alpha = 0.82f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun OnlineRoomStatusBar(
    roomId: String,
    connectionState: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xF01B1714),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFFFD36A).copy(alpha = 0.9f)),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "房间号：$roomId",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFE4A6),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = connectionState,
                fontSize = 14.sp,
                color = Color(0xFFFFC857),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun generateRoomId(): String {
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return buildString {
        repeat(6) {
            append(alphabet[Random.nextInt(alphabet.length)])
        }
    }
}
