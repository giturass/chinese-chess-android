package com.ericlee.chess.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ericlee.chess.ui.theme.battlefieldTexture
import kotlinx.coroutines.delay

@Composable
fun AppSplashScreen(onFinished: () -> Unit) {
    var animationStarted by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "splashAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.78f,
        animationSpec = tween(durationMillis = 650),
        label = "splashScale"
    )

    LaunchedEffect(Unit) {
        animationStarted = true
        delay(1_200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .battlefieldTexture(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                modifier = Modifier.size(132.dp),
                shape = CircleShape,
                color = Color(0xFFE5B76A),
                contentColor = Color(0xFF9E1C13),
                shadowElevation = 12.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "帅",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "中 国 象 棋",
                color = Color(0xFFFFE4A6),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
