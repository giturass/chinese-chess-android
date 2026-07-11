package com.ericlee.chess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ericlee.chess.audio.GameAudio
import com.ericlee.chess.ui.screen.AiGameScreen
import com.ericlee.chess.ui.screen.EndgameScreen
import com.ericlee.chess.ui.screen.HomeScreen
import com.ericlee.chess.ui.screen.LocalGameScreen
import com.ericlee.chess.ui.screen.OnlineGameScreen
import com.ericlee.chess.ui.screen.TwoPlayerScreen
import com.ericlee.chess.ui.theme.ChineseChessTheme
import com.ericlee.chess.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()
        setContent {
            ChineseChessTheme {
                ChineseChessApp()
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
}

@Composable
fun ChineseChessApp() {
    val navController = rememberNavController()
    val gameViewModel: GameViewModel = viewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val audio = remember(context) { GameAudio(context) }
    val state by gameViewModel.gameState.collectAsState()
    var heardMoveCount by remember { mutableStateOf(state.moveHistory.size) }

    DisposableEffect(lifecycleOwner, audio) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> audio.resumeBackground()
                Lifecycle.Event.ON_STOP -> audio.pauseBackground()
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

    LaunchedEffect(state.moveHistory.size, state.lastMove) {
        if (state.moveHistory.size > heardMoveCount) {
            state.lastMove?.let { audio.playMove(capture = it.captured != null) }
        }
        heardMoveCount = state.moveHistory.size
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onStartAiGame = { navController.navigate("ai") },
                onStartTwoPlayerGame = { navController.navigate("two_player") },
                onStartEndgame = { navController.navigate("endgame") }
            )
        }
        composable("ai") {
            AiGameScreen(
                viewModel = gameViewModel,
                onBack = { navController.popBackStack() }
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
                onBack = { navController.popBackStack() }
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}
