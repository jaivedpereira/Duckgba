package com.duckgba

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.duckgba.ui.game.GameScreen
import com.duckgba.ui.home.HomeScreen
import com.duckgba.ui.settings.SettingsScreen
import com.duckgba.ui.theme.DuckgbaTheme

/**
 * Single-activity host. Three screens are stacked using Navigation-Compose:
 *  - home: list of imported ROMs + entry points to settings/import
 *  - settings: emulator preferences
 *  - game: the actual emulator surface
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DuckgbaTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    color = Color.Black
                ) {
                    DuckgbaApp()
                }
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val GAME = "game/{romId}"
    fun game(romId: String) = "game/${Uri.encode(romId)}"
}

@Composable
private fun DuckgbaApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onPlayRom = { entry -> navController.navigate(Routes.game(entry.id)) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.GAME) { backStackEntry ->
            val romId = backStackEntry.arguments?.getString("romId").orEmpty()
            GameScreen(
                romId = Uri.decode(romId),
                onExit = { navController.popBackStack() }
            )
        }
    }
}
