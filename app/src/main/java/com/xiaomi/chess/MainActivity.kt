package com.xiaomi.chess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xiaomi.chess.ui.screen.AiGameScreen
import com.xiaomi.chess.ui.screen.EndgameScreen
import com.xiaomi.chess.ui.screen.HomeScreen
import com.xiaomi.chess.ui.screen.LocalGameScreen
import com.xiaomi.chess.ui.theme.ChineseChessTheme
import com.xiaomi.chess.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChineseChessTheme {
                ChineseChessApp()
            }
        }
    }
}

@Composable
fun ChineseChessApp() {
    val navController = rememberNavController()
    val gameViewModel: GameViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onStartAiGame = { navController.navigate("ai") },
                onStartLocalGame = { navController.navigate("local") },
                onStartEndgame = { navController.navigate("endgame") }
            )
        }
        composable("ai") {
            AiGameScreen(
                viewModel = gameViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("local") {
            LocalGameScreen(
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
