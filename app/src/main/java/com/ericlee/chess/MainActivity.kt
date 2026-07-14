package com.ericlee.chess

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ericlee.chess.audio.GameAudio
import com.ericlee.chess.data.SavedGameSummary
import com.ericlee.chess.engine.PikafishEngine
import com.ericlee.chess.model.GameMode
import com.ericlee.chess.ui.component.InAppDialog
import com.ericlee.chess.ui.screen.AiGameScreen
import com.ericlee.chess.ui.screen.AppSplashScreen
import com.ericlee.chess.ui.screen.EndgameScreen
import com.ericlee.chess.ui.screen.HomeScreen
import com.ericlee.chess.ui.screen.LocalGameScreen
import com.ericlee.chess.ui.screen.OnlineGameScreen
import com.ericlee.chess.ui.screen.TwoPlayerScreen
import com.ericlee.chess.ui.theme.ChineseChessTheme
import com.ericlee.chess.viewmodel.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private var showEngineReadyDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()
        lifecycleScope.launch(Dispatchers.IO) {
            val ready = PikafishEngine.prefetchEvalFile(applicationContext) { !isActive }
            if (ready && PikafishEngine.isRestartRequired(applicationContext)) {
                withContext(Dispatchers.Main) {
                    showEngineReadyDialog = true
                }
            }
        }
        setContent {
            ChineseChessTheme {
                var showSplash by remember { mutableStateOf(true) }
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        AppSplashScreen(onFinished = { showSplash = false })
                    } else {
                        ChineseChessApp()
                    }
                    if (showEngineReadyDialog && !showSplash) {
                        InAppDialog(
                            onDismissRequest = {},
                            dismissOnOutsideClick = false,
                            title = { Text("AI引擎加载完毕") },
                            content = { Text("点击确定重启 App 生效。") },
                            buttons = {
                                TextButton(
                                    onClick = {
                                        showEngineReadyDialog = false
                                        PikafishEngine.clearRestartRequired(applicationContext)
                                        restartApp()
                                    }
                                ) {
                                    Text("确定")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun restartApp() {
        startActivity(Intent.makeRestartActivityTask(componentName))
        finishAffinity()
        exitProcess(0)
    }
}

@Composable
fun ChineseChessApp() {
    val navController = rememberNavController()
    val gameViewModel: GameViewModel = viewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioPrefs = remember(context) {
        context.getSharedPreferences(GameAudio.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var audioMuted by rememberSaveable {
        mutableStateOf(audioPrefs.getBoolean(GameAudio.MUTED_KEY, false))
    }
    val audio = remember(context) { GameAudio(context, initialMuted = audioMuted) }
    val state by gameViewModel.gameState.collectAsState()
    val savedGamePrompt by gameViewModel.savedGamePrompt.collectAsState()
    var heardMoveCount by remember { mutableStateOf(state.moveHistory.size) }

    DisposableEffect(lifecycleOwner, audio) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> audio.resumeBackground()
                Lifecycle.Event.ON_STOP -> {
                    gameViewModel.saveActiveGame()
                    audio.pauseBackground()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        audio.startBackground()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            audio.release()
        }
    }

    LaunchedEffect(audio, audioMuted) {
        audio.setMuted(audioMuted)
        audioPrefs.edit()
            .putBoolean(GameAudio.MUTED_KEY, audioMuted)
            .apply()
    }

    LaunchedEffect(state.moveHistory.size, state.lastMove) {
        if (state.moveHistory.size > heardMoveCount) {
            state.lastMove?.let { audio.playMove(capture = it.captured != null) }
        }
        heardMoveCount = state.moveHistory.size
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    onStartAiGame = { navController.navigate("ai") },
                    onStartTwoPlayerGame = { navController.navigate("two_player") },
                    onStartEndgame = { navController.navigate("endgame") },
                    audioMuted = audioMuted,
                    onAudioMutedChange = { audioMuted = it }
                )
            }
            composable("ai") {
                AiGameScreen(
                    viewModel = gameViewModel,
                    onBack = {
                        gameViewModel.leaveActiveGame()
                        navController.popBackStack()
                    }
                )
            }
            composable("two_player") {
                TwoPlayerScreen(
                    onStartLocalGame = { navController.navigate("local") },
                    onStartOnlineGame = { navController.navigate("online") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("local") {
                LocalGameScreen(
                    viewModel = gameViewModel,
                    onBack = {
                        gameViewModel.leaveActiveGame()
                        navController.popBackStack()
                    }
                )
            }
            composable("online") {
                OnlineGameScreen(
                    viewModel = gameViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("endgame") {
                EndgameScreen(
                    viewModel = gameViewModel,
                    onBack = {
                        gameViewModel.leaveActiveGame()
                        navController.popBackStack()
                    }
                )
            }
        }

        savedGamePrompt?.let { summary ->
            InAppDialog(
                onDismissRequest = {},
                dismissOnOutsideClick = false,
                title = { Text("发现未完成棋局") },
                content = { Text(summary.promptText()) },
                buttons = {
                    TextButton(onClick = { gameViewModel.discardSavedGame() }) {
                        Text("舍弃")
                    }
                    TextButton(
                        onClick = {
                            val restoredMode = gameViewModel.continueSavedGame()
                            restoredMode?.route()?.let { route ->
                                navController.navigate(route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    ) {
                        Text("继续")
                    }
                }
            )
        }
    }
}

private fun GameMode.route(): String? = when (this) {
    GameMode.AI -> "ai"
    GameMode.LOCAL -> "local"
    GameMode.ENDGAME -> "endgame"
    GameMode.ONLINE -> null
}

private fun SavedGameSummary.promptText(): String {
    val modeName = when (mode) {
        GameMode.AI -> "人机对战"
        GameMode.LOCAL -> "本地双人"
        GameMode.ONLINE -> "联机对战"
        GameMode.ENDGAME -> "残局挑战"
    }
    return "$modeName，已走 $moveCount 步。"
}
