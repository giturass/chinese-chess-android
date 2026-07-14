package com.ericlee.chess.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ericlee.chess.model.GameMode
import com.ericlee.chess.model.GameState
import com.ericlee.chess.model.Move
import com.ericlee.chess.model.Piece
import com.ericlee.chess.model.Side
import com.ericlee.chess.ui.board.ChessBoard
import com.ericlee.chess.ui.component.InAppDialog
import com.ericlee.chess.ui.theme.battlefieldTexture
import com.ericlee.chess.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var difficulty by remember { mutableIntStateOf(3) }
    var gameStarted by remember { mutableStateOf(false) }
    var selectedHumanSide by remember { mutableStateOf<Side?>(null) }
    var confirmAction by remember { mutableStateOf<AiConfirmAction?>(null) }

    val state by viewModel.gameState.collectAsState()
    val selectedPiece by viewModel.selectedPiece.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val activeGameStarted by viewModel.activeGameStarted.collectAsState()

    LaunchedEffect(activeGameStarted, state.mode) {
        if (activeGameStarted && state.mode == GameMode.AI) {
            difficulty = state.aiDifficulty
            selectedHumanSide = state.humanSide
            gameStarted = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("人机对战") },
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
            if (!gameStarted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .battlefieldTexture()
                        .padding(padding)
                ) {
                    val humanSide = selectedHumanSide
                    if (humanSide == null) {
                        AiSideSelector(
                            onSelectSide = { selectedHumanSide = it },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        DifficultySelector(
                            humanSide = humanSide,
                            onBackToSide = { selectedHumanSide = null },
                            onSelectDifficulty = { selectedDifficulty ->
                                difficulty = selectedDifficulty
                                viewModel.startGame(
                                    mode = GameMode.AI,
                                    difficulty = selectedDifficulty,
                                    flipped = humanSide == Side.BLACK,
                                    humanSide = humanSide
                                )
                                gameStarted = true
                            },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .battlefieldTexture()
                        .padding(padding)
                ) {
                    AiGameContent(
                        state = state,
                        selectedPiece = selectedPiece,
                        legalMoves = legalMoves,
                        statusMessage = statusMessage,
                        isAiThinking = isAiThinking,
                        onPositionClick = { row, col -> viewModel.onPositionClick(row, col) },
                        onUndo = { confirmAction = AiConfirmAction.UNDO },
                        onHint = { viewModel.hintMove() },
                        onNewGame = { confirmAction = AiConfirmAction.NEW_GAME }
                    )
                }
            }
        }

        confirmAction?.let { action ->
            InAppDialog(
                onDismissRequest = { confirmAction = null },
                title = { Text(action.title) },
                content = { Text(action.message) },
                buttons = {
                    TextButton(onClick = { confirmAction = null }) {
                        Text("取消")
                    }
                    TextButton(onClick = {
                    when (action) {
                        AiConfirmAction.NEW_GAME -> viewModel.startGame(
                            mode = GameMode.AI,
                            difficulty = difficulty,
                            flipped = state.humanSide == Side.BLACK,
                            humanSide = state.humanSide
                        )
                        AiConfirmAction.UNDO -> viewModel.undoMove()
                    }
                    confirmAction = null
                    }) {
                        Text("确认")
                    }
                }
            )
        }
    }
}

@Composable
private fun AiGameContent(
    state: GameState,
    selectedPiece: Piece?,
    legalMoves: List<Move>,
    statusMessage: String,
    isAiThinking: Boolean,
    onPositionClick: (Int, Int) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onNewGame: () -> Unit
) {
    Layout(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        content = {
            if (isAiThinking) {
                AiThinkingBadge(modifier = Modifier.layoutId("aiStatus"))
            }
            Box(modifier = Modifier.layoutId("board")) {
                ChessBoard(
                    board = state.board,
                    currentSide = state.currentSide,
                    status = state.status,
                    selectedPiece = selectedPiece,
                    legalMoves = legalMoves,
                    lastMove = state.lastMove,
                    isFlipped = state.isFlipped,
                    onPositionClick = onPositionClick
                )
            }
            AiControlPanel(
                state = state,
                statusMessage = statusMessage,
                isAiThinking = isAiThinking,
                onUndo = onUndo,
                onHint = onHint,
                onNewGame = onNewGame,
                modifier = Modifier
                    .layoutId("panel")
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    ) { measurables, constraints ->
        val gap = 4.dp.roundToPx()
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val panelPlaceable = measurables
            .first { it.layoutId == "panel" }
            .measure(looseConstraints)
        val boardHeight = (constraints.maxHeight - panelPlaceable.height - gap).coerceAtLeast(0)
        val boardPlaceable = measurables
            .first { it.layoutId == "board" }
            .measure(looseConstraints.copy(maxHeight = boardHeight))
        val aiStatusPlaceable = measurables
            .firstOrNull { it.layoutId == "aiStatus" }
            ?.measure(looseConstraints)
        val contentHeight = boardPlaceable.height + gap + panelPlaceable.height
        val boardY = ((constraints.maxHeight - contentHeight) / 2).coerceAtLeast(0)
        val panelY = boardY + boardPlaceable.height + gap
        val aiStatusY = ((boardY - (aiStatusPlaceable?.height ?: 0) - gap * 2)).coerceAtLeast(0)

        layout(constraints.maxWidth, constraints.maxHeight) {
            aiStatusPlaceable?.place((constraints.maxWidth - aiStatusPlaceable.width) / 2, aiStatusY)
            boardPlaceable.place((constraints.maxWidth - boardPlaceable.width) / 2, boardY)
            panelPlaceable.place((constraints.maxWidth - panelPlaceable.width) / 2, panelY)
        }
    }
}

@Composable
private fun AiThinkingBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aiThinking")
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 680),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiThinkingPulse"
    )
    val dotPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aiThinkingDots"
    )
    val dots = when (dotPhase.toInt().coerceIn(0, 2)) {
        0 -> "."
        1 -> ".."
        else -> "..."
    }

    Surface(
        modifier = modifier.graphicsLayer(alpha = pulse),
        color = Color(0xCC1B1714),
        contentColor = Color(0xFFFFE4A6),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Text(
            text = "AI思考中$dots",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSideSelector(
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
            text = "人机对弈",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFE4A6)
        )
        Text(
            text = "选择先后手",
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
            Text("我先下", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(
            onClick = { onSelectSide(Side.BLACK) },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Text("AI先下", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultySelector(
    humanSide: Side,
    onBackToSide: () -> Unit,
    onSelectDifficulty: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val levels = listOf(
        DifficultyLevel(1, "入门", "快速应手，适合熟悉规则", "★"),
        DifficultyLevel(2, "进阶", "会主动兑子与防守", "★★"),
        DifficultyLevel(3, "劲敌", "搜索更深，攻守更稳", "★★★"),
        DifficultyLevel(4, "宗师", "更谨慎地计算杀招", "★★★★")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "人机对弈",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFE4A6)
        )
        Text(
            text = if (humanSide == Side.RED) "我先下 · 选择难度" else "AI先下 · 选择难度",
            fontSize = 15.sp,
            color = Color(0xFFFFF0D4).copy(alpha = 0.72f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        FilledTonalButton(onClick = onBackToSide) {
            Text("重选先后手")
        }

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (level in levels) {
                Button(
                    onClick = { onSelectDifficulty(level.value) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC2D1A0A)),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = level.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFF0D4)
                            )
                            Text(
                                text = level.description,
                                fontSize = 13.sp,
                                color = Color(0xFFFFF0D4).copy(alpha = 0.72f)
                            )
                        }
                        Text(
                            text = level.stars,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFE4A6)
                        )
                    }
                }
            }
        }
    }
}

private enum class AiConfirmAction(
    val title: String,
    val message: String
) {
    NEW_GAME("确认开始新局？", "当前棋局和历史记录会被清空。"),
    UNDO("确认悔棋？", "将撤回你和 AI 的最近一轮走棋。")
}

private data class DifficultyLevel(
    val value: Int,
    val name: String,
    val description: String,
    val stars: String
)
