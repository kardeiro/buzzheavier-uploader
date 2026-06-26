package com.buzzheavier.uploader.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.buzzheavier.uploader.ui.screens.upload.UploadScreen
import com.buzzheavier.uploader.ui.screens.files.FilesScreen
import com.buzzheavier.uploader.ui.screens.settings.SettingsScreen

object Routes {
    const val UPLOAD = "upload"
    const val FILES = "files"
    const val FILES_DIR = "files/{directoryId}"
    const val SETTINGS = "settings"
}

private val springSpec = spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)

@Composable
fun BuzzHeavierNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.UPLOAD,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it / 3 }) +
                fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(initialScale = 0.95f, animationSpec = springSpec)
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }) +
                fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) +
                scaleOut(targetScale = 0.95f, animationSpec = springSpec)
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }) +
                fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(initialScale = 0.95f, animationSpec = springSpec)
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it / 3 }) +
                fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) +
                scaleOut(targetScale = 0.95f, animationSpec = springSpec)
        }
    ) {
        composable(Routes.UPLOAD) {
            UploadScreen(
                onNavigateToFiles = { navController.navigate(Routes.FILES) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.FILES) {
            FilesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDirectory = { dirId -> navController.navigate("files/$dirId") }
            )
        }
        composable(
            route = Routes.FILES_DIR,
            arguments = listOf(navArgument("directoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val directoryId = backStackEntry.arguments?.getString("directoryId") ?: ""
            FilesScreen(
                directoryId = directoryId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDirectory = { dirId -> navController.navigate("files/$dirId") }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
