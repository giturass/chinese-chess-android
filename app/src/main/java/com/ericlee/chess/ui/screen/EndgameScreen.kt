package com.ericlee.chess.ui.screen

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ericlee.chess.data.EndgameRepository
import com.ericlee.chess.model.EndgamePuzzle
import com.ericlee.chess.model.GameMode
import com.ericlee.chess.model.GameStatus
import com.ericlee.chess.model.Side
import com.ericlee.chess.ui.board.ChessBoard
import com.ericlee.chess.ui.component.InAppDialog
import com.ericlee.chess.ui.theme.battlefieldTexture
import com.ericlee.chess.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndgameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    var selectedPuzzle by remember { mutableStateOf<EndgamePuzzle?>(null) }
    var showHint by remember { mutableStateOf(false) }
    val puzzles = remember { EndgameRepository.getPuzzles() }
    val context = LocalContext.current
    val progressPrefs = remember {
        context.getSharedPreferences(ENDGAME_PROGRESS_PREFS, Context.MODE_PRIVATE)
    }
    var completedIds by remember {
        mutableStateOf(progressPrefs.loadCompletedPuzzleIds())
    }
    val categories = remember(puzzles) {
        listOf(ALL_ENDGAME_CATEGORY) + puzzles.map { it.category }.distinct()
    }
    var selectedCategory by rememberSaveable { mutableStateOf(ALL_ENDGAME_CATEGORY) }
    val visiblePuzzles = remember(puzzles, selectedCategory) {
        if (selectedCategory == ALL_ENDGAME_CATEGORY) {
            puzzles
        } else {
            puzzles.filter { it.category == selectedCategory }
        }
    }

    val state by viewModel.gameState.collectAsState()
    val selectedPiece by viewModel.selectedPiece.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val activeGameStarted by viewModel.activeGameStarted.collectAsState()
    val activeEndgamePuzzleId by viewModel.activeEndgamePuzzleId.collectAsState()

    val leavePuzzle = {
        showHint = false
        viewModel.leaveEndgamePuzzle()
        selectedPuzzle = null
    }

    BackHandler {
        if (selectedPuzzle != null) {
            leavePuzzle()
        } else {
            onBack()
        }
    }

    LaunchedEffect(activeGameStarted, activeEndgamePuzzleId, state.mode) {
        if (activeGameStarted && state.mode == GameMode.ENDGAME) {
            selectedPuzzle = puzzles.firstOrNull { it.id == activeEndgamePuzzleId }
        }
    }

    val puzzle = selectedPuzzle
    Box(modifier = Modifier.fillMaxSize()) {
        if (puzzle != null) {
            GameContent(
                puzzle = puzzle,
                state = state,
                selectedPiece = selectedPiece,
                legalMoves = legalMoves,
                statusMessage = statusMessage,
                onBack = leavePuzzle,
                onReset = { viewModel.loadEndgame(puzzle) },
                onUndo = { viewModel.undoMove() },
                onPositionClick = { row, col -> viewModel.onPositionClick(row, col) },
                onShowHint = { showHint = true },
                onSolved = { solvedId ->
                    if (solvedId !in completedIds) {
                        val updated = completedIds + solvedId
                        completedIds = updated
                        progressPrefs.saveCompletedPuzzleIds(updated)
                    }
                }
            )
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = { Text("残局挑战") },
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
                        .battlefieldTexture()
                        .padding(padding)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            EndgameProgressHeader(
                                completedCount = completedIds.size,
                                totalCount = puzzles.size
                            )
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(categories) { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { selectedCategory = category },
                                        label = { Text(category) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFFFD36A),
                                            selectedLabelColor = Color(0xFF24150D),
                                            containerColor = Color(0xBB1C1510),
                                            labelColor = Color(0xFFFFE4A6)
                                        )
                                    )
                                }
                            }
                        }
                        items(visiblePuzzles) { puzzle ->
                            PuzzleCard(
                                puzzle = puzzle,
                                completed = puzzle.id in completedIds,
                                onClick = {
                                    showHint = false
                                    selectedPuzzle = puzzle
                                    viewModel.loadEndgame(puzzle)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (puzzle != null && showHint) {
            InAppDialog(
                onDismissRequest = { showHint = false },
                title = { Text("提示") },
                content = { Text(puzzle.hint) },
                buttons = {
                    TextButton(
                        onClick = { showHint = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2F251C))
                    ) {
                        Text("知道了")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameContent(
    puzzle: EndgamePuzzle,
    state: com.ericlee.chess.model.GameState,
    selectedPiece: com.ericlee.chess.model.Piece?,
    legalMoves: List<com.ericlee.chess.model.Move>,
    statusMessage: String,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onUndo: () -> Unit,
    onPositionClick: (Int, Int) -> Unit,
    onShowHint: () -> Unit,
    onSolved: (Int) -> Unit
) {
    val topSide = if (state.isFlipped) Side.RED else Side.BLACK
    val bottomSide = topSide.opposite()

    LaunchedEffect(state.status, puzzle.id) {
        if (state.status == GameStatus.RED_WIN) {
            onSolved(puzzle.id)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(puzzle.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {},
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
                .padding(padding)
                .battlefieldTexture()
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            EndgameGameLayout(
                modifier = Modifier.fillMaxSize(),
                board = {
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
                },
                controlPanel = {
                    EndgameControlPanel(
                        state = state,
                        statusMessage = statusMessage,
                        side = bottomSide,
                        showActions = true,
                        onUndo = onUndo,
                        onHint = onShowHint,
                        onReset = onReset,
                        modifier = Modifier
                            .layoutId("panel")
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun EndgameGameLayout(
    board: @Composable () -> Unit,
    controlPanel: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Layout(
        modifier = modifier,
        content = {
            board()
            controlPanel()
        }
    ) { measurables, constraints ->
        val gap = 4.dp.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val panelPlaceable = measurables.first { it.layoutId == "panel" }.measure(loose)
        val boardMaxHeight = (
            constraints.maxHeight - (panelPlaceable.height + gap) * 2
        ).coerceAtLeast(0)
        val boardPlaceable = measurables.first { it.layoutId == "board" }
            .measure(loose.copy(maxHeight = boardMaxHeight))
        val boardY = ((constraints.maxHeight - boardPlaceable.height) / 2).coerceAtLeast(0)
        val panelY = boardY + boardPlaceable.height + gap

        layout(constraints.maxWidth, constraints.maxHeight) {
            boardPlaceable.place((constraints.maxWidth - boardPlaceable.width) / 2, boardY)
            panelPlaceable.place((constraints.maxWidth - panelPlaceable.width) / 2, panelY)
        }
    }
}

@Composable
private fun PuzzleCard(
    puzzle: EndgamePuzzle,
    completed: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (completed) Color(0xEAF7E5C7) else Color(0xCC1E1610)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = puzzle.category,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (completed) Color(0xFF8C4A16) else Color(0xFFFFD36A)
                )
                Text(
                    text = puzzle.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (completed) Color(0xFF3A2210) else Color(0xFFFFF0D4)
                )
                Text(
                    text = "${puzzle.goal} · ${puzzle.description}",
                    fontSize = 14.sp,
                    color = if (completed) Color(0xFF3A2210).copy(alpha = 0.70f)
                    else Color(0xFFFFF0D4).copy(alpha = 0.72f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "★".repeat(puzzle.difficulty),
                    fontSize = 16.sp,
                    color = if (completed) Color(0xFFB32318) else Color(0xFFFFD36A)
                )
                Text(
                    text = if (completed) "已通关" else "挑战",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (completed) Color(0xFFB32318) else Color(0xFFFFF0D4).copy(alpha = 0.76f)
                )
            }
        }
    }
}

@Composable
private fun EndgameProgressHeader(
    completedCount: Int,
    totalCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xCC1E1610),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "残局闯关",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFE4A6)
                )
            }
            Text(
                text = "$completedCount/$totalCount",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD36A)
            )
        }
    }
}

private fun android.content.SharedPreferences.loadCompletedPuzzleIds(): Set<Int> =
    getStringSet(ENDGAME_PROGRESS_KEY, emptySet())
        .orEmpty()
        .mapNotNull { it.toIntOrNull() }
        .toSet()

private fun android.content.SharedPreferences.saveCompletedPuzzleIds(ids: Set<Int>) {
    edit()
        .putStringSet(ENDGAME_PROGRESS_KEY, ids.map { it.toString() }.toSet())
        .apply()
}

private const val ALL_ENDGAME_CATEGORY = "全部"
private const val ENDGAME_PROGRESS_PREFS = "endgame_progress"
private const val ENDGAME_PROGRESS_KEY = "completed_ids"
