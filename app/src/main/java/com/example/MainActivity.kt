package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.providers.AppViewModel
import com.example.screens.dirigente.DirigenteDashboard
import com.example.screens.levita.LevitaDashboard
import com.example.screens.pastor.PastorDashboard
import com.example.screens.pastor.PastorEdicao
import com.example.screens.secretario.SecretarioDashboard
import com.example.screens.splash.SplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            val themePref by viewModel.themePreference.collectAsState()
            val isDark = when (themePref) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                val activePerfil by viewModel.perfilAtuante.collectAsState()

                // State-driven inner sub-navigation (e.g. for Pastor Edit)
                var currentScreen by remember { mutableStateOf("dashboard") }
                var editingEsbocoUuid by remember { mutableStateOf<String?>(null) }

                // Reset sub-navigation when profile shifts
                LaunchedEffect(activePerfil) {
                    currentScreen = "dashboard"
                    editingEsbocoUuid = null
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (activePerfil) {
                        null -> {
                            SplashScreen(
                                viewModel = viewModel,
                                onNavigateToProfile = {
                                    // Welcome profile chosen! State updates reactively.
                                }
                            )
                        }
                        "pastor" -> {
                            if (currentScreen == "pastor_edicao") {
                                PastorEdicao(
                                    viewModel = viewModel,
                                    esbocoUuid = editingEsbocoUuid,
                                    onNavigateBack = { currentScreen = "dashboard" }
                                )
                            } else {
                                PastorDashboard(
                                    viewModel = viewModel,
                                    onNavigateToEdit = { uuid ->
                                        editingEsbocoUuid = uuid
                                        currentScreen = "pastor_edicao"
                                    },
                                    onTrocarPerfil = {
                                        viewModel.selectPerfil(null)
                                    }
                                )
                            }
                        }
                        "levita" -> {
                            LevitaDashboard(
                                viewModel = viewModel,
                                onTrocarPerfil = {
                                    viewModel.selectPerfil(null)
                                }
                            )
                        }
                        "secretario" -> {
                            SecretarioDashboard(
                                viewModel = viewModel,
                                onTrocarPerfil = {
                                    viewModel.selectPerfil(null)
                                }
                            )
                        }
                        "dirigente" -> {
                            DirigenteDashboard(
                                viewModel = viewModel,
                                onTrocarPerfil = {
                                    viewModel.selectPerfil(null)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
