package com.ericlee.chess.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ericlee.chess.model.GameState
import com.ericlee.chess.model.GameStatus
import com.ericlee.chess.model.Side
import com.ericlee.chess.model.toChineseNotation

@Composable
fun LocalControlPanel(
    state: GameState,
    statusMessage: String,
    onUndo: (Side) -> Unit,
    onDraw: (Side) -> Unit,
    onResign: (Side) -> Unit,
    modifier: Modifier = Modifier
) {
    GameInfoPanel(
        state = state,
        statusMessage = statusMessage,
        accentSide = null,
        modifier = modifier
    ) {
        SideActionRow(
            side = Side.RED,
            canUndo = state.status == GameStatus.PLAYING && state.lastMoveSide == Side.RED,
            canAct = state.status == GameStatus.PLAYING,
            onUndo = { onUndo(Side.RED) },
            onDraw = { onDraw(Side.RED) },
            onResign = { onResign(Side.RED) }
        )
        SideActionRow(
            side = Side.BLACK,
            canUndo = state.status == GameStatus.PLAYING && state.lastMoveSide == Side.BLACK,
            canAct = state.status == GameStatus.PLAYING,
            onUndo = { onUndo(Side.BLACK) },
            onDraw = { onDraw(Side.BLACK) },
            onResign = { onResign(Side.BLACK) }
        )
    }
}

@Composable
fun PlayerControlPanel(
    side: Side,
    state: GameState,
    statusMessage: String,
    onUndo: (Side) -> Unit,
    onDraw: (Side) -> Unit,
    onResign: (Side) -> Unit,
    modifier: Modifier = Modifier
) {
    GameInfoPanel(
        state = state,
        statusMessage = statusMessage,
        accentSide = side,
        modifier = modifier
    ) {
        SideActionRow(
            side = side,
            canUndo = state.status == GameStatus.PLAYING && state.lastMoveSide == side,
            canAct = state.status == GameStatus.PLAYING,
            onUndo = { onUndo(side) },
            onDraw = { onDraw(side) },
            onResign = { onResign(side) }
        )
    }
}

@Composable
fun AiControlPanel(
    state: GameState,
    statusMessage: String,
    isAiThinking: Boolean,
    onUndo: () -> Unit,
    onDraw: () -> Unit,
    onResign: () -> Unit,
    modifier: Modifier = Modifier
) {
    GameInfoPanel(
        state = state,
        statusMessage = statusMessage,
        accentSide = state.humanSide,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onUndo,
                enabled = state.status == GameStatus.PLAYING &&
                    state.moveHistory.any { it.side == state.humanSide } &&
                    !isAiThinking,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text("悔棋", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onDraw,
                enabled = state.status == GameStatus.PLAYING && !isAiThinking,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text("求和", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onResign,
                enabled = state.status == GameStatus.PLAYING && !isAiThinking,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text("认输", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun OnlineControlPanel(
    state: GameState,
    statusMessage: String,
    side: Side,
    showActions: Boolean,
    connectionReady: Boolean,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onDraw: () -> Unit,
    onResign: () -> Unit,
    modifier: Modifier = Modifier
) {
    GameInfoPanel(
        state = state,
        statusMessage = statusMessage,
        accentSide = side,
        modifier = modifier
    ) {
        if (showActions) {
            SideActionRow(
                side = side,
                canUndo = canUndo,
                canAct = connectionReady && state.status == GameStatus.PLAYING,
                onUndo = onUndo,
                onDraw = onDraw,
                onResign = onResign
            )
        }
    }
}

@Composable
fun EndgameControlPanel(
    state: GameState,
    statusMessage: String,
    side: Side,
    showActions: Boolean,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    GameInfoPanel(
        state = state,
        statusMessage = statusMessage,
        accentSide = side,
        modifier = modifier
    ) {
        if (showActions) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onUndo,
                    enabled = state.moveHistory.isNotEmpty() && state.status == GameStatus.PLAYING,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("悔棋", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onHint,
                    enabled = state.status == GameStatus.PLAYING,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("提示", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("重置", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun GameInfoPanel(
    state: GameState,
    statusMessage: String,
    modifier: Modifier = Modifier,
    accentSide: Side? = null,
    actions: @Composable ColumnScope.() -> Unit
) {
    val accent = accentSide?.accentColor() ?: state.accentColor()
    val detailColor = if (accentSide == Side.BLACK) Color(0xFF111111) else Color(0xFF352112)
    val notation = state.fullMoveNotation()
    val notationScrollState = rememberScrollState()
    val headline = state.headline()
    var notationExpanded by rememberSaveable { mutableStateOf(false) }
    val hasMoves = state.moveHistory.isNotEmpty()
    val showExpandedNotation = notationExpanded && hasMoves
    val detailText = when {
        showExpandedNotation -> statusMessage
        hasMoves -> "棋谱：${notation.collapsed}"
        else -> statusMessage
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xEAF7E5C7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = headline,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = detailText,
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                color = detailColor.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (hasMoves) {
                                Text(
                                    text = if (showExpandedNotation) "折叠" else "展开",
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clickable { notationExpanded = !notationExpanded },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }
                        }
                    }
                }

                actions()
            }

            if (showExpandedNotation) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 128.dp),
                        color = Color(0xF8F7E5C7),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "完整棋谱",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = detailColor.copy(alpha = 0.86f)
                                )
                                Text(
                                    text = "折叠",
                                    modifier = Modifier.clickable { notationExpanded = false },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 86.dp)
                                    .verticalScroll(notationScrollState),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                notation.lines.forEach { line ->
                                    MoveNotationRow(
                                        line = line,
                                        textColor = detailColor.copy(alpha = 0.86f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SideActionRow(
    side: Side,
    canUndo: Boolean,
    canAct: Boolean,
    onUndo: () -> Unit,
    onDraw: () -> Unit,
    onResign: () -> Unit
) {
    val contentColor = if (side == Side.RED) Color(0xFFB32318) else Color(0xFF111111)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = side.displayName(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (side == Side.RED) Color(0xFFB32318) else Color(0xFF241B14),
            modifier = Modifier.width(42.dp)
        )
        OutlinedButton(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
        ) {
            Text("悔棋", fontSize = 13.sp)
        }
        OutlinedButton(
            onClick = onDraw,
            enabled = canAct,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
        ) {
            Text("求和", fontSize = 13.sp)
        }
        OutlinedButton(
            onClick = onResign,
            enabled = canAct,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
        ) {
            Text("认输", fontSize = 13.sp)
        }
    }
}

fun Side.displayName(): String = if (this == Side.RED) "红方" else "黑方"

private fun Side.accentColor(): Color = if (this == Side.RED) Color(0xFFB32318) else Color(0xFF111111)

private fun GameState.headline(): String = when (status) {
    GameStatus.RED_WIN -> "红方获胜"
    GameStatus.BLACK_WIN -> "黑方获胜"
    GameStatus.STALEMATE -> "和棋"
    GameStatus.DRAW -> "和棋"
    GameStatus.PLAYING -> if (isInCheck) "将军：${currentSide.displayName()}应将" else "轮到${currentSide.displayName()}"
}

private fun GameState.accentColor(): Color = when {
    status == GameStatus.RED_WIN -> Color(0xFFB32318)
    status == GameStatus.BLACK_WIN -> Color(0xFF241B14)
    isInCheck -> Color(0xFFC62828)
    currentSide == Side.RED -> Color(0xFFB32318)
    else -> Color(0xFF241B14)
}

private fun GameState.fullMoveNotation(): MoveNotationText {
    if (moveHistory.isEmpty()) return MoveNotationText("尚未行棋", emptyList())
    val lines = moveHistory
        .chunked(2)
        .mapIndexed { index, pair ->
            MoveNotationLine(
                number = index + 1,
                redMove = pair.getOrNull(0)?.toChineseNotation().orEmpty(),
                blackMove = pair.getOrNull(1)?.toChineseNotation().orEmpty()
            )
        }
    return MoveNotationText(
        collapsed = lines.last().collapsedText(),
        lines = lines
    )
}

@Composable
private fun MoveNotationRow(
    line: MoveNotationLine,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${line.number}.",
            modifier = Modifier.width(28.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.End
        )
        Text(
            text = line.redMove,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            fontSize = 12.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (line.blackMove.isNotBlank()) {
            Text(
                text = line.blackMove,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                fontSize = 12.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun MoveNotationLine.collapsedText(): String = buildString {
    append(number)
    append(".")
    append(redMove)
    if (blackMove.isNotBlank()) {
        append("  ")
        append(blackMove)
    }
}

private data class MoveNotationText(
    val collapsed: String,
    val lines: List<MoveNotationLine>
)

private data class MoveNotationLine(
    val number: Int,
    val redMove: String,
    val blackMove: String
)
