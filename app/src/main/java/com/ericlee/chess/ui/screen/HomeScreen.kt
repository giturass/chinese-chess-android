package com.ericlee.chess.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ericlee.chess.ui.theme.battlefieldTexture

@Composable
fun HomeScreen(
    onStartAiGame: () -> Unit,
    onStartTwoPlayerGame: () -> Unit,
    onStartEndgame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .battlefieldTexture()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "中 国 象 棋",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFE4A6)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Chinese Chess",
                fontSize = 16.sp,
                color = Color(0xFFFFF0D4).copy(alpha = 0.72f)
            )

            Spacer(modifier = Modifier.height(64.dp))

            MenuButton(
                text = "人机对战",
                subtitle = "与AI对弈",
                onClick = onStartAiGame
            )

            Spacer(modifier = Modifier.height(16.dp))

            MenuButton(
                text = "双人对战",
                subtitle = "本地或联机",
                onClick = onStartTwoPlayerGame
            )

            Spacer(modifier = Modifier.height(16.dp))

            MenuButton(
                text = "残局挑战",
                subtitle = "经典残局破解",
                onClick = onStartEndgame
            )
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xCC2D1A0A)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}
