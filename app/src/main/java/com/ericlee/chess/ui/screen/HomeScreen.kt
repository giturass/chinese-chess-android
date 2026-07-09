package com.ericlee.chess.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ericlee.chess.network.OnlineServerConfig
import com.ericlee.chess.ui.theme.battlefieldTexture

@Composable
fun HomeScreen(
    onStartAiGame: () -> Unit,
    onStartTwoPlayerGame: () -> Unit,
    onStartEndgame: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(OnlineServerConfig.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var showSettings by remember { mutableStateOf(false) }
    var serverUrl by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                OnlineServerConfig.SERVER_URL_KEY,
                OnlineServerConfig.DEFAULT_SERVER_URL
            ).orEmpty().ifBlank { OnlineServerConfig.DEFAULT_SERVER_URL }
        )
    }
    var serverUrlError by remember { mutableStateOf(false) }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleaned = serverUrl.trim().trimEnd('/')
                            .ifBlank { OnlineServerConfig.DEFAULT_SERVER_URL }
                        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
                            serverUrlError = true
                            return@TextButton
                        }
                        serverUrl = cleaned
                        serverUrlError = false
                        prefs.edit()
                            .putString(OnlineServerConfig.SERVER_URL_KEY, cleaned)
                            .apply()
                        showSettings = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            serverUrl = OnlineServerConfig.DEFAULT_SERVER_URL
                            serverUrlError = false
                        }
                    ) {
                        Text("恢复默认")
                    }
                    TextButton(onClick = { showSettings = false }) {
                        Text("取消")
                    }
                }
            },
            title = { Text("联机设置") },
            text = {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        serverUrlError = false
                    },
                    singleLine = true,
                    label = { Text("服务器地址") },
                    isError = serverUrlError,
                    supportingText = {
                        if (serverUrlError) {
                            Text("请输入 http:// 或 https:// 开头的地址")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .battlefieldTexture()
    ) {
        IconButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "设置",
                tint = Color(0xFFFFE4A6)
            )
        }

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
            Spacer(modifier = Modifier.height(56.dp))

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
        shape = RoundedCornerShape(50),
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
