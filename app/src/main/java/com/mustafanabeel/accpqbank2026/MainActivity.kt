package com.mustafanabeel.accpqbank2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mustafanabeel.accpqbank2026.ui.screens.BookmarksScreen
import com.mustafanabeel.accpqbank2026.ui.screens.HomeScreen
import com.mustafanabeel.accpqbank2026.ui.screens.QuizScreen
import com.mustafanabeel.accpqbank2026.ui.screens.ResultsScreen
import com.mustafanabeel.accpqbank2026.ui.screens.SessionSetupScreen
import com.mustafanabeel.accpqbank2026.ui.theme.AccpTheme
import com.mustafanabeel.accpqbank2026.ui.viewmodel.QBankViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccpTheme {
                // Arabic RTL Application Shell
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val qBankViewModel: QBankViewModel = viewModel()

                        NavHost(
                            navController = navController,
                            startDestination = "home"
                        ) {
                            composable("home") {
                                HomeScreen(
                                    viewModel = qBankViewModel,
                                    onNavigateToSetup = { navController.navigate("setup") },
                                    onNavigateToQuiz = { navController.navigate("quiz") },
                                    onNavigateToBookmarks = { navController.navigate("bookmarks") }
                                )
                            }
                            composable("setup") {
                                SessionSetupScreen(
                                    viewModel = qBankViewModel,
                                    onBack = { navController.popBackStack() },
                                    onStartQuiz = { navController.navigate("quiz") }
                                )
                            }
                            composable("quiz") {
                                QuizScreen(
                                    viewModel = qBankViewModel,
                                    onNavigateBack = { navController.popBackStack("home", false) },
                                    onNavigateToResults = { navController.navigate("results") }
                                )
                            }
                            composable("results") {
                                ResultsScreen(
                                    viewModel = qBankViewModel,
                                    onNavigateHome = { navController.popBackStack("home", false) },
                                    onRetryIncorrect = {
                                        qBankViewModel.retryIncorrectQuestions {
                                            navController.navigate("quiz")
                                        }
                                    },
                                    onNewQuiz = { navController.navigate("setup") }
                                )
                            }
                            composable("bookmarks") {
                                BookmarksScreen(
                                    viewModel = qBankViewModel,
                                    onBack = { navController.popBackStack() },
                                    onStartBookmarkQuiz = { navController.navigate("quiz") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
