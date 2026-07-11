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
    onStartEndgame: () -> Unit,
    audioMuted: Boolean,
    onAudioMutedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(OnlineServerConfig.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var serverUrl by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                OnlineServerConfig.SERVER_URL_KEY,
                OnlineServerConfig.DEFAULT_SERVER_URL
            ).orEmpty().ifBlank { OnlineServerConfig.DEFAULT_SERVER_URL }
        )
    }
    var serverUrlError by remember { mutableStateOf(false) }
    var settingsPane by remember { mutableStateOf<HomeSettingsPane?>(null) }

    when (settingsPane) {
        HomeSettingsPane.MENU -> SettingsMenuDialog(
            audioMuted = audioMuted,
            onDismiss = {
                settingsPane = null
            },
            onAudioMutedChange = onAudioMutedChange,
            onOpenOnlineSettings = { settingsPane = HomeSettingsPane.ONLINE }
        )

        HomeSettingsPane.ONLINE -> OnlineSettingsDialog(
            serverUrl = serverUrl,
            serverUrlError = serverUrlError,
            onServerUrlChange = {
                serverUrl = it
                serverUrlError = false
            },
            onRestoreDefault = {
                serverUrl = OnlineServerConfig.DEFAULT_SERVER_URL
                serverUrlError = false
            },
            onBack = { settingsPane = HomeSettingsPane.MENU },
            onDismiss = {
                settingsPane = null
            },
            onSave = {
                val cleaned = serverUrl.trim().trimEnd('/')
                    .ifBlank { OnlineServerConfig.DEFAULT_SERVER_URL }
                if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
                    serverUrlError = true
                } else {
                    serverUrl = cleaned
                    serverUrlError = false
                    prefs.edit()
                        .putString(OnlineServerConfig.SERVER_URL_KEY, cleaned)
                        .apply()
                    settingsPane = null
                }
            }
        )

        null -> Unit
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .battlefieldTexture()
    ) {
        IconButton(
            onClick = {
                settingsPane = HomeSettingsPane.MENU
            },
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
                subtitle = "与AI切磋",
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
private fun SettingsMenuDialog(
    audioMuted: Boolean,
    onDismiss: () -> Unit,
    onAudioMutedChange: (Boolean) -> Unit,
    onOpenOnlineSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        title = { Text("设置") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsSwitchRow(
                    title = "静音",
                    subtitle = "背景音乐与吃子音效",
                    checked = audioMuted,
                    onCheckedChange = onAudioMutedChange
                )
                SettingsMenuButton(
                    title = "联机设置",
                    subtitle = "服务器地址与房间连接",
                    onClick = onOpenOnlineSettings
                )
            }
        }
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF4A2C18)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFE4A6)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFFFFF0D4).copy(alpha = 0.78f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun SettingsMenuButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFF4A2C18),
            contentColor = Color(0xFFFFE4A6)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFFFFF0D4).copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun OnlineSettingsDialog(
    serverUrl: String,
    serverUrlError: Boolean,
    onServerUrlChange: (String) -> Unit,
    onRestoreDefault: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRestoreDefault) {
                    Text("恢复默认")
                }
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            }
        },
        title = { Text("联机设置") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onServerUrlChange,
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
        }
    )
}

private enum class HomeSettingsPane {
    MENU,
    ONLINE
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
