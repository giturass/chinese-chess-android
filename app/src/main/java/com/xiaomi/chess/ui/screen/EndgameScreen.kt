package com.xiaomi.chess.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.xiaomi.chess.data.EndgameRepository
import com.xiaomi.chess.model.EndgamePuzzle
import com.xiaomi.chess.model.GameStatus
import com.xiaomi.chess.ui.board.ChessBoard
import com.xiaomi.chess.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndgameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    var selectedPuzzle by remember { mutableStateOf<EndgamePuzzle?>(null) }
    val puzzles = remember { EndgameRepository.getPuzzles() }

    val state by viewModel.gameState.collectAsState()
    val selectedPiece by viewModel.selectedPiece.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    if (selectedPuzzle != null) {
        GameContent(
            puzzle = selectedPuzzle!!,
            state = state,
            selectedPiece = selectedPiece,
            legalMoves = legalMoves,
            statusMessage = statusMessage,
            onBack = { selectedPuzzle = null },
            onReset = { viewModel.loadEndgame(selectedPuzzle!!) },
            onUndo = { viewModel.undoMove() },
            onPositionClick = { row, col -> viewModel.onPositionClick(row, col) },
            onShowHint = { /* TODO: show hint */ }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("残局挑战") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "返回")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                items(puzzles) { puzzle ->
                    PuzzleCard(
                        puzzle = puzzle,
                        onClick = {
                            selectedPuzzle = puzzle
                            viewModel.loadEndgame(puzzle)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun GameContent(
    puzzle: EndgamePuzzle,
    state: com.xiaomi.chess.model.GameState,
    selectedPiece: com.xiaomi.chess.model.Piece?,
    legalMoves: List<com.xiaomi.chess.model.Move>,
    statusMessage: String,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onUndo: () -> Unit,
    onPositionClick: (Int, Int) -> Unit,
    onShowHint: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(puzzle.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onReset) {
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
            // Puzzle info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = statusMessage,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = puzzle.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "难度: ${"★".repeat(puzzle.difficulty)}${"☆".repeat(5 - puzzle.difficulty)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
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
                isFlipped = state.isFlipped,
                onPositionClick = onPositionClick
            )

            // Control buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onUndo,
                    enabled = state.moveHistory.isNotEmpty()
                ) {
                    Icon(Icons.Default.Undo, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("悔棋")
                }

                OutlinedButton(onClick = onShowHint) {
                    Text("提示")
                }
            }

            // Win/Lose message
            if (state.status != GameStatus.PLAYING) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = statusMessage,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onReset) {
                            Text("再来一局")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PuzzleCard(
    puzzle: EndgamePuzzle,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = puzzle.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = puzzle.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                text = "★".repeat(puzzle.difficulty),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
