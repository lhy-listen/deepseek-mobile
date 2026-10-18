package com.lhy.deepseekmobile.ui

import android.app.Activity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lhy.deepseekmobile.ui.about.AboutScreen
import com.lhy.deepseekmobile.ui.chat.ChatScreen
import com.lhy.deepseekmobile.ui.modes.ModeEditorScreen
import com.lhy.deepseekmobile.ui.modes.ModesScreen
import com.lhy.deepseekmobile.ui.onboarding.OnboardingScreen
import com.lhy.deepseekmobile.ui.settings.SettingsScreen
import com.lhy.deepseekmobile.ui.theme.Accent
import com.lhy.deepseekmobile.ui.theme.DsTheme

@Composable
fun DsAppRoot(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val dark = when (settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val accent = Accent.fromId(settings.accentId)

    DsTheme(
        darkTheme = dark,
        accent = accent,
        amoled = settings.amoled,
        fontScale = settings.fontScale,
    ) {
        val context = LocalContext.current
        val view = LocalView.current
        SideEffect {
            val activity = context as? Activity ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }

        val navController = rememberNavController()
        val startDestination = remember { if (settings.onboardingDone) "chat" else "welcome" }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            composable("chat") {
                ChatScreen(
                    vm = vm,
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenModes = { navController.navigate("modes") },
                    onOpenAbout = { navController.navigate("about") },
                )
            }
            composable("welcome") {
                OnboardingScreen(vm) {
                    navController.navigate("chat") {
                        popUpTo("welcome") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            composable("modes") {
                ModesScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onEdit = { id ->
                        navController.navigate("mode_editor/${id ?: "new"}")
                    },
                )
            }
            composable("mode_editor/{modeId}") { entry ->
                val id = entry.arguments?.getString("modeId")
                ModeEditorScreen(
                    vm = vm,
                    modeId = if (id.isNullOrBlank() || id == "new") null else id,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("settings") {
                SettingsScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onAbout = { navController.navigate("about") },
                )
            }
            composable("about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
