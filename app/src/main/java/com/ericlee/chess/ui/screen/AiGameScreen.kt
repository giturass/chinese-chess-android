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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
fun AiGameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    var difficulty by remember { mutableIntStateOf(3) }
    var humanSide by remember { mutableStateOf(Side.RED) }
    var gameStarted by remember { mutableStateOf(false) }

    val state by viewModel.gameState.collectAsState()
    val selectedPiece by viewModel.selectedPiece.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    if (gameStarted && state.status != GameStatus.PLAYING) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.startGame(
                            mode = GameMode.AI,
                            difficulty = difficulty,
                            flipped = humanSide == Side.BLACK,
                            humanSide = humanSide
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
                title = { Text("人机对战") },
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
                AiSetupSelector(
                    selectedSide = humanSide,
                    onSideChange = { humanSide = it },
                    onSelectDifficulty = {
                        difficulty = it
                        viewModel.startGame(
                            mode = GameMode.AI,
                            difficulty = it,
                            flipped = humanSide == Side.BLACK,
                            humanSide = humanSide
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
                    AiControlPanel(
                        state = state,
                        statusMessage = statusMessage,
                        isAiThinking = isAiThinking,
                        difficulty = difficulty,
                        onUndo = { viewModel.undoMove() },
                        onDraw = { viewModel.agreeDraw(humanSide) },
                        onResign = { viewModel.resign(humanSide) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSetupSelector(
    selectedSide: Side,
    onSideChange: (Side) -> Unit,
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
            text = "先选执棋，再选难度开局",
            fontSize = 15.sp,
            color = Color(0xFFFFF0D4).copy(alpha = 0.72f)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = selectedSide == Side.RED,
                onClick = { onSideChange(Side.RED) },
                label = { Text("执红") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedSide == Side.BLACK,
                onClick = { onSideChange(Side.BLACK) },
                label = { Text("执黑") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        for (level in levels) {
            ElevatedCard(
                onClick = { onSelectDifficulty(level.value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xEAF7E5C7)
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = level.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A2A13)
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
