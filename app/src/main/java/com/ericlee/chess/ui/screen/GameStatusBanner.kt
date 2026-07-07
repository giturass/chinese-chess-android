package com.ericlee.chess.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ericlee.chess.model.GameState
import com.ericlee.chess.model.GameStatus
import com.ericlee.chess.model.Side

@Composable
fun GameStatusBanner(
    state: GameState,
    statusMessage: String,
    modifier: Modifier = Modifier,
    isAiThinking: Boolean = false,
    metaText: String? = null
) {
    val currentSideName = if (state.currentSide == Side.RED) "红方" else "黑方"
    val isCheck = state.status == GameStatus.PLAYING && state.isInCheck
    val headline = when (state.status) {
        GameStatus.RED_WIN -> "红方获胜"
        GameStatus.BLACK_WIN -> "黑方获胜"
        GameStatus.STALEMATE -> "和棋"
        GameStatus.DRAW -> "和棋"
        GameStatus.PLAYING -> if (isCheck) "将军！${currentSideName}受将" else "轮到${currentSideName}"
    }
    val detail = when {
        isAiThinking -> "AI 正在思考，请稍候"
        isCheck -> "必须先应将，解除将军后才能继续进攻"
        state.status == GameStatus.RED_WIN || state.status == GameStatus.BLACK_WIN -> statusMessage
        else -> statusMessage
    }

    val accent = when {
        state.status == GameStatus.RED_WIN -> Color(0xFF9F2119)
        state.status == GameStatus.BLACK_WIN -> Color(0xFF17110D)
        isCheck -> Color(0xFF9F2119)
        else -> Color(0xFF2F251C)
    }
    val container = when {
        state.status != GameStatus.PLAYING -> Color(0xFFEAD8B8)
        isCheck -> Color(0xFFECD6C6)
        else -> Color(0xFFEFE2C8)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(accent, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = headline,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    Text(
                        text = detail,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            if (isAiThinking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = accent
                )
            } else if (metaText != null) {
                Surface(
                    color = accent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = metaText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }
        }
    }
}
