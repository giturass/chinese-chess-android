package com.ericlee.chess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ericlee.chess.ui.screen.AiGameScreen
import com.ericlee.chess.ui.screen.EndgameScreen
import com.ericlee.chess.ui.screen.HomeScreen
import com.ericlee.chess.ui.screen.LocalGameScreen
import com.ericlee.chess.ui.theme.ChineseChessTheme
import com.ericlee.chess.viewmodel.GameViewModel

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
