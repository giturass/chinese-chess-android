package com.ericlee.chess.ui.screen

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.ericlee.chess.ui.theme.battlefieldTexture
import com.ericlee.chess.ui.theme.stoneChamberTexture
import com.ericlee.chess.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    var difficulty by remember { mutableIntStateOf(3) }
    var gameStarted by remember { mutableStateOf(false) }
    var selectedHumanSide by remember { mutableStateOf<Side?>(null) }
    var confirmAction by remember { mutableStateOf<AiConfirmAction?>(null) }

    val state by viewModel.gameState.collectAsState()
    val selectedPiece by viewModel.selectedPiece.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (action) {
                            AiConfirmAction.FLIP -> {
                                val nextHumanSide = state.humanSide.opposite()
                                difficulty = state.aiDifficulty
                                viewModel.startGame(
                                    mode = GameMode.AI,
                                    difficulty = state.aiDifficulty,
                                    flipped = nextHumanSide == Side.BLACK,
                                    humanSide = nextHumanSide
                                )
                            }
                            AiConfirmAction.RESET -> viewModel.startGame(
                                mode = GameMode.AI,
                                difficulty = difficulty,
                                flipped = state.humanSide == Side.BLACK,
                                humanSide = state.humanSide
                            )
                        }
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
                title = { Text("人机对战") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (gameStarted) {
                        FilledTonalButton(
                            onClick = { confirmAction = AiConfirmAction.FLIP },
                            enabled = !isAiThinking,
                            modifier = Modifier.padding(end = 6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = null)
                            Text("调转")
                        }
                        Button(
                            onClick = { confirmAction = AiConfirmAction.RESET },
                            enabled = !isAiThinking,
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
                    .stoneChamberTexture()
                    .padding(padding)
            ) {
                AiGameContent(
                    state = state,
                    selectedPiece = selectedPiece,
                    legalMoves = legalMoves,
                    statusMessage = statusMessage,
                    isAiThinking = isAiThinking,
                    onPositionClick = { row, col -> viewModel.onPositionClick(row, col) },
                    onUndo = { viewModel.undoMove() },
                    onDraw = { viewModel.agreeDraw(state.humanSide) },
                    onResign = { viewModel.resign(state.humanSide) }
                )
            }
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
    onDraw: () -> Unit,
    onResign: () -> Unit
) {
    Layout(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        content = {
            ChessBoard(
                board = state.board,
                currentSide = state.currentSide,
                status = state.status,
                selectedPiece = selectedPiece,
                legalMoves = legalMoves,
                lastMove = state.lastMove,
                isFlipped = state.isFlipped,
                onPositionClick = onPositionClick,
                modifier = Modifier.layoutId("board")
            )
            AiControlPanel(
                state = state,
                statusMessage = statusMessage,
                isAiThinking = isAiThinking,
                onUndo = onUndo,
                onDraw = onDraw,
                onResign = onResign,
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
        val boardY = 0
        val panelY = boardY + boardPlaceable.height + gap

        layout(constraints.maxWidth, constraints.maxHeight) {
            boardPlaceable.place((constraints.maxWidth - boardPlaceable.width) / 2, boardY)
            panelPlaceable.place((constraints.maxWidth - panelPlaceable.width) / 2, panelY)
        }
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
    FLIP("确认调转先后手？", "会重新开局并互换先后手。AI 先下时将执红先行。"),
    RESET("确认重置棋盘？", "当前棋局和历史记录会被清空。")
}

private data class DifficultyLevel(
    val value: Int,
    val name: String,
    val description: String,
    val stars: String
)
