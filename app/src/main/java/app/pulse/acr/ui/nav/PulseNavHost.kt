package app.pulse.acr.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.pulse.acr.ui.detail.RecordingDetailScreen
import app.pulse.acr.ui.library.LibraryScreen
import app.pulse.acr.ui.onboarding.OnboardingScreen
import app.pulse.acr.ui.settings.SettingsScreen
import app.pulse.acr.ui.shared.AppViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}"
    fun detail(id: Long) = "detail/$id"
}

@Composable
fun PulseNavHost(
    appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
) {
    val ready by appViewModel.ready.collectAsStateWithLifecycle()
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    if (!ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val start = if (settings.onboardingDone && settings.consentAccepted) {
        Routes.LIBRARY
    } else {
        Routes.ONBOARDING
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.LIBRARY) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenRecording = { id -> navController.navigate(Routes.detail(id)) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            RecordingDetailScreen(
                recordingId = id,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
